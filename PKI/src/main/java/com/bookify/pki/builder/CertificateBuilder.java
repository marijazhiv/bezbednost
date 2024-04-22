package com.bookify.pki.builder;

import com.bookify.pki.enumerations.CertificatePurpose;
import com.bookify.pki.model.Issuer;
import com.bookify.pki.model.Subject;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class CertificateBuilder {

    private Subject subject;
    private Issuer issuer;
    private Date startDate;
    private Date endDate;
    private CertificatePurpose purpose;

    public CertificateBuilder withSubject(Subject subject) {
        this.subject = subject;
        return this;
    }

    public CertificateBuilder withIssuer(Issuer issuer) {
        this.issuer = issuer;
        return this;
    }

    public CertificateBuilder withValidity(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        return this;
    }

    public CertificateBuilder withPurpose(CertificatePurpose purpose){
        this.purpose = purpose;
        return this;
    }

    public X509Certificate build(){
        try {
            BigInteger randomNumber = BigInteger.valueOf(System.currentTimeMillis());
            JcaContentSignerBuilder builder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
            builder = builder.setProvider("BC");
            ContentSigner contentSigner = builder.build(issuer.getPrivateKey());

            X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(issuer.getX500Name(),
                    randomNumber,
                    startDate,
                    endDate,
                    subject.getX500Name(),
                    subject.getPublicKey());

            switch (purpose){
                case HTTPS:
                    buildHTTPSCertificate(certGen);
                    break;
                case DIGITAL_SIGNATURE:
                    buildDigitalSignatureCertificate(certGen);
                    break;
                case INTERMEDIATE_CERTIFICATE_AUTHORITY:
                    buildIntermediateCACertificate(certGen);
                    break;
                default:
                    buildEndEntityCertificate(certGen);
                    break;
            }

            X509CertificateHolder certHolder = certGen.build(contentSigner);
            JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
            certConverter = certConverter.setProvider("BC");
            return certConverter.getCertificate(certHolder);

        } catch (IllegalArgumentException | IllegalStateException | OperatorCreationException | CertificateException e) {
            e.printStackTrace();
        }
        return null;
    }
    private void buildDigitalSignatureCertificate(X509v3CertificateBuilder certificateBuilder) {
        try {
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

        } catch (CertIOException e) {
            throw new RuntimeException(e);
        }

    }

    private void buildHTTPSCertificate(X509v3CertificateBuilder certificateBuilder){
        try{
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            // This OID stands for server authentication
            certificateBuilder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.getInstance(new ASN1ObjectIdentifier("1.3.6.1.5.5.7.3.1"))));
            certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            certificateBuilder.addExtension(Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(subject.getPublicKey().getEncoded()));
            certificateBuilder.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(issuer.getPublicKey().getEncoded()));
        } catch (CertIOException e) {
            throw new RuntimeException(e);
        }
    }
    private void buildEndEntityCertificate(X509v3CertificateBuilder certificateBuilder) {
        try{
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            certificateBuilder.addExtension(Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(subject.getPublicKey().getEncoded()));
            certificateBuilder.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(issuer.getPublicKey().getEncoded()));
        } catch (CertIOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildIntermediateCACertificate(X509v3CertificateBuilder certificateBuilder) {
        try{
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
            certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            certificateBuilder.addExtension(Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(subject.getPublicKey().getEncoded()));
            certificateBuilder.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(issuer.getPublicKey().getEncoded()));
        } catch (CertIOException e) {
            throw new RuntimeException(e);
        }
    }

}
