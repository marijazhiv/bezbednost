package com.bookify.pki.service;

import com.bookify.pki.builder.CertificateBuilder;
import com.bookify.pki.dto.CertificateRequestDTO;
import com.bookify.pki.enumerations.CertificatePurpose;
import com.bookify.pki.enumerations.CertificateRequestStatus;
import com.bookify.pki.enumerations.CertificateType;
import com.bookify.pki.model.Certificate;
import com.bookify.pki.model.CertificateRequest;
import com.bookify.pki.model.Issuer;
import com.bookify.pki.model.Subject;
import com.bookify.pki.repository.ICertificateRequestRepository;
import com.bookify.pki.service.interfaces.ICertificateRequestService;
import com.bookify.pki.service.interfaces.ICertificateService;
import com.bookify.pki.utils.KeyStoreReader;
import com.bookify.pki.utils.KeyStoreWriter;
import com.bookify.pki.utils.KeyUtils;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@Service
public class CertificateRequestService implements ICertificateRequestService {

    @Autowired
    private ICertificateRequestRepository certificateRequestRepository;

    @Autowired
    private AliasMappingService aliasMappingService;

    @Autowired
    private KeyStoreReader keyStoreReader;

    @Autowired
    private CertificateService certificateService;


    @Value("${keystore.location}")
    private String keystoreLocation;

    @Value("${keystore.password}")
    private String keystorePassword;

    @Value("${keys.location}")
    private String keyLocation;

    public CertificateRequestService(){
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public CertificateRequest getRequestById(Long id){
        Optional<CertificateRequest> requestOptional = certificateRequestRepository.findById(id);
        return requestOptional.orElse(null);
    }

    @Override
    public List<Certificate> getSignedCertificates(Long issuerId){
        List<Long> signedCertificateIds = aliasMappingService.getSignedCertificateIds(issuerId);
        List<Certificate> certificates = new ArrayList<>();

        for (Long id : signedCertificateIds){
            String certificateAlias = aliasMappingService.getCertificateAlias(id);
            java.security.cert.Certificate c = keyStoreReader.readCertificate(keystoreLocation, keystorePassword, certificateAlias);
            certificates.add(new Certificate(id, (X509Certificate) c));
        }

        return certificates;
    }

    @Override
    public CertificateRequest createCertificateRequest(CertificateRequestDTO certificateRequestDTO) {
        CertificateRequest request = new CertificateRequest(null,
                certificateRequestDTO.getSubjectName(),
                certificateRequestDTO.getLocality(),
                certificateRequestDTO.getCountry(),
                certificateRequestDTO.getEmail(),
                CertificateType.END_ENTITY,
                CertificateRequestStatus.PENDING);
        return certificateRequestRepository.save(request);
    }

    @Override
    public CertificateRequest acceptCertificateRequest(Long issuerId, Long requestId) {

        Optional<CertificateRequest> requestOptional = certificateRequestRepository.findById(requestId);
        if(requestOptional.isEmpty()) return null;
        CertificateRequest request = requestOptional.get();

        if(request.getCertificateRequestStatus() != CertificateRequestStatus.PENDING) return null;

        signCertificateRequest(request, issuerId);
        request.setCertificateRequestStatus(CertificateRequestStatus.ACCEPTED);
        return certificateRequestRepository.save(request);
    }

    @Override
    public CertificateRequest rejectCertificateRequest(Long requestId) {
        Optional<CertificateRequest> requestOptional = certificateRequestRepository.findById(requestId);
        if(requestOptional.isEmpty()) return null;
        CertificateRequest request = requestOptional.get();

        if(request.getCertificateRequestStatus() != CertificateRequestStatus.PENDING) return null;
        request.setCertificateRequestStatus(CertificateRequestStatus.REJECTED);
        return certificateRequestRepository.save(request);
    }

    @Override
    public void signCertificateRequest(CertificateRequest request,Long issuerId) {

        String issuerAlias = aliasMappingService.getCertificateAlias(issuerId);

        java.security.cert.Certificate issuerCertificate = keyStoreReader.readCertificate(keystoreLocation, keystorePassword, issuerAlias);
        if(((X509Certificate) issuerCertificate).getBasicConstraints() == -1) return;

        try {

            PrivateKey privateKey = KeyUtils.readPrivateKey(keyLocation+issuerAlias+".key");
            JcaX509CertificateHolder holder = new JcaX509CertificateHolder((X509Certificate) issuerCertificate);

            Issuer issuer = new Issuer(privateKey, issuerCertificate.getPublicKey(), holder.getSubject());

            KeyPair keyPair = KeyUtils.generateKeyPair();
            Subject subject = CertificateService.getSubject(request, keyPair);

            Date startDate = new Date();
            Date endDate = CertificateService.getEndDate(holder);

            CertificateBuilder certificateBuilder = new CertificateBuilder();
            certificateBuilder
                    .withIssuer(issuer)
                    .withPurpose(CertificatePurpose.END_ENTITY)
                    .withSubject(subject)
                    .withValidity(startDate, endDate);

            X509Certificate x509Certificate = certificateBuilder.build();
            if(x509Certificate == null) return;

            String subjectAlias = certificateService.generateCertificateAlias(x509Certificate);

            certificateService.saveCertificate(x509Certificate, issuerId, subjectAlias);
            //certificateService.savePrivateKey(keyPair.getPrivate(), subjectAlias);

            System.out.println(x509Certificate);
        }
        catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<CertificateRequest> getPendingCertificateRequest() {
        return certificateRequestRepository.findAllByCertificateRequestStatusIs(CertificateRequestStatus.PENDING);
    }


}
