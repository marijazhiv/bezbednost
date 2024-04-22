package com.bookify.pki.service;

import com.bookify.pki.builder.CertificateBuilder;
import com.bookify.pki.dto.CertificateRequestDTO;
import com.bookify.pki.dto.NewCertificateDTO;
import com.bookify.pki.enumerations.CertificatePurpose;
import com.bookify.pki.enumerations.CertificateRequestStatus;
import com.bookify.pki.enumerations.CertificateType;
import com.bookify.pki.model.Certificate;
import com.bookify.pki.model.CertificateRequest;
import com.bookify.pki.model.Issuer;
import com.bookify.pki.model.Subject;
import com.bookify.pki.repository.ICertificateRequestRepository;
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
public class CertificateService implements ICertificateService {

    @Autowired
    private ICertificateRequestRepository certificateRequestRepository;

    @Autowired
    private KeyStoreReader keyStoreReader;

    @Autowired
    private KeyStoreWriter keyStoreWriter;

    @Autowired
    private AliasMappingService aliasMappingService;

    @Value("${keystore.location}")
    private String keystoreLocation;

    @Value("${keystore.password}")
    private String keystorePassword;

    @Value("${keys.location}")
    private String keyLocation;


    public CertificateService(){
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public Certificate getCertificateById(Long id){
        String certificateAlias = aliasMappingService.getCertificateAlias(id);
        java.security.cert.Certificate c = keyStoreReader.readCertificate(keystoreLocation, keystorePassword, certificateAlias);
        return new Certificate(id, (X509Certificate) c);
    }

    @Override
    public Certificate createNewCertificate(NewCertificateDTO newCertificateDTO) {
        String certificateAlias = aliasMappingService.getCertificateAlias(newCertificateDTO.getIssuerId());
        java.security.cert.Certificate issuerCertificate = keyStoreReader.readCertificate(keystoreLocation, keystorePassword, certificateAlias);

        if(((X509Certificate) issuerCertificate).getBasicConstraints() == -1) return null;

        JcaX509CertificateHolder holder = null;
        try {
            holder = new JcaX509CertificateHolder((X509Certificate) issuerCertificate);
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }

        PrivateKey privateKey = KeyUtils.readPrivateKey(keyLocation + certificateAlias + ".key");
        Issuer issuer = new Issuer(privateKey, issuerCertificate.getPublicKey(), holder.getSubject());

        KeyPair keyPair = KeyUtils.generateKeyPair();

        Subject subject = getSubject(newCertificateDTO, keyPair);

        Date startDate = newCertificateDTO.getNotBefore();
        Date endDate = newCertificateDTO.getNotAfter();

        CertificateBuilder certificateBuilder = new CertificateBuilder();
        certificateBuilder
                .withIssuer(issuer)
                .withPurpose(newCertificateDTO.getPurpose())
                .withValidity(startDate, endDate)
                .withSubject(subject);
        X509Certificate x509Certificate = certificateBuilder.build();
        if(x509Certificate == null) return null;

        String subjectAlias = generateCertificateAlias(x509Certificate);
        saveCertificate(x509Certificate, newCertificateDTO.getIssuerId(), subjectAlias);
        if(newCertificateDTO.getCertificateType() != CertificateType.END_ENTITY) savePrivateKey(keyPair.getPrivate(), subjectAlias);
        return new Certificate(newCertificateDTO.getIssuerId(), x509Certificate);
    }

    @Override
    public boolean validateCertificateChain(Long certificateId) {
        while(true) {
            Long issuerId = aliasMappingService.getCertificatesIssuerId(certificateId);
            X509Certificate certificate = getCertificateById(certificateId).getX509Certificate();
            if(!isCertificateValid(certificate, issuerId)) return false;
            if(issuerId == certificateId) break;
        }
        return true;
    }

    @Override
    public Long deleteCertificate(Long certificateId) {
        if(certificateId == 0) return null;
        deleteCertificateChildren(certificateId);
        aliasMappingService.deleteFromParentList(certificateId);
        deleteCertificateFromKeystore(certificateId);
        return certificateId;
    }

    private void deleteCertificateFromKeystore(Long certificateId) {
        String alias = aliasMappingService.getCertificateAlias(certificateId);
        keyStoreWriter.loadKeyStore(keystoreLocation, keystorePassword.toCharArray());
        keyStoreWriter.deleteCertificate(alias);
        keyStoreWriter.saveKeyStore(keystoreLocation, keystorePassword.toCharArray());
        aliasMappingService.deleteCertificateFromFile(certificateId);
    }

    private void deleteCertificateChildren(Long certificateId){
        List<Long> certificateIds = aliasMappingService.getSignedCertificateIds(certificateId);
        if(certificateIds.isEmpty()) return;


        for(Long cerId : certificateIds){
            deleteCertificateChildren(cerId);
            deleteCertificateFromKeystore(cerId);

        }
    }

    private boolean isCertificateValid(X509Certificate certificate, Long issuerId){
        try {
            JcaX509CertificateHolder holder = new JcaX509CertificateHolder(certificate);
            X509Certificate certificateIssuer = getCertificateById(issuerId).getX509Certificate();
            certificate.verify(certificateIssuer.getPublicKey());
            return holder.isValidOn(new Date());
        } catch (CertificateException | NoSuchAlgorithmException | SignatureException | InvalidKeyException |
                 NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    private static Subject getSubject(NewCertificateDTO newCertificateDTO, KeyPair keyPair) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        builder.addRDN(BCStyle.CN, newCertificateDTO.getCommonName());
        builder.addRDN(BCStyle.C, newCertificateDTO.getCountry());
        builder.addRDN(BCStyle.L, newCertificateDTO.getLocality());
        builder.addRDN(BCStyle.E, newCertificateDTO.getEmail());
        builder.addRDN(BCStyle.O, newCertificateDTO.getOrganization());
        builder.addRDN(BCStyle.OU, newCertificateDTO.getOrganizationalUnit());

        return new Subject(keyPair.getPublic(), builder.build());
    }

    @Override
    public List<Certificate> getSignedCertificates(Long issuerId){
        List<Long> signedCertificateIds = aliasMappingService.getSignedCertificateIds(issuerId);
        List<Certificate> certificates = new ArrayList<>();
        for (Long id :signedCertificateIds){
            String certificateAlias= aliasMappingService.getCertificateAlias(id);
            java.security.cert.Certificate c=keyStoreReader.readCertificate(keystoreLocation, keystorePassword, certificateAlias);
            certificates.add(new Certificate(id, (X509Certificate) c));

        }

        return certificates;

    }

    public static Date getEndDate(JcaX509CertificateHolder holder) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.YEAR, 2);
        Date endDate = calendar.getTime();

        if(!holder.isValidOn(endDate)){
            endDate= holder.getNotAfter();
        }
        return endDate;
    }
    public static Subject getSubject(CertificateRequest request, KeyPair keyPair) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        builder.addRDN(BCStyle.CN, request.getSubjectName());
        builder.addRDN(BCStyle.C, request.getCountry());
        builder.addRDN(BCStyle.L, request.getLocality());
        builder.addRDN(BCStyle.E, request.getEmail());
        return new Subject(keyPair.getPublic(), builder.build());
    }

    void saveCertificate(X509Certificate cert,Long issuerId,String alias){

        keyStoreWriter.loadKeyStore(keystoreLocation,keystorePassword.toCharArray());
        keyStoreWriter.write(alias,cert);
        keyStoreWriter.saveKeyStore(keystoreLocation,keystorePassword.toCharArray());

        Long subjectId = new Date().getTime();
        aliasMappingService.addSignedCertificate(issuerId,subjectId,alias);
    }

    void savePrivateKey(PrivateKey privateKey,String alias){

        // Convert the key to Base64 encoding
        byte[] keyBytes = privateKey.getEncoded();
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);

        // Add PEM headers and footers
        StringBuilder pemKey = new StringBuilder();
        pemKey.append("-----BEGIN PRIVATE KEY-----\n");
        pemKey.append(base64Key).append("\n");
        pemKey.append("-----END PRIVATE KEY-----\n");

        // Write the PEM encoded private key to the file
        try (Writer writer = new FileWriter(keyLocation + alias + ".key")) {
            writer.write(pemKey.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String generateCertificateAlias(X509Certificate cert){

        return cert.getIssuerX500Principal().getName().hashCode()+cert.getSerialNumber().toString();

    }

}
