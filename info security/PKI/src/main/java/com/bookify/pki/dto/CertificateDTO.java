package com.bookify.pki.dto;

import com.bookify.pki.enumerations.CertificatePurpose;
import com.bookify.pki.model.Certificate;
import com.bookify.pki.model.Issuer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CertificateDTO {
    private Long id;
    private String issuer;
    private String subject;
    private String dateFrom;
    private String dateTo;
    private CertificatePurpose certificatePurpose;

    public CertificateDTO(Certificate certificate){
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.id = certificate.getId();
        this.issuer = certificate.getX509Certificate().getIssuerX500Principal().getName();
        this.subject = certificate.getX509Certificate().getSubjectX500Principal().getName();
        this.dateTo = certificate
                .getX509Certificate()
                .getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(format);
        this.dateFrom = certificate
                .getX509Certificate()
                .getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(format);

    }

}
