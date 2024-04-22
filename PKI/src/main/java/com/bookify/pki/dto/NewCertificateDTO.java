package com.bookify.pki.dto;

import com.bookify.pki.enumerations.CertificatePurpose;
import com.bookify.pki.enumerations.CertificateType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewCertificateDTO {
    private Long issuerId;
    private String commonName;
    private String organization;
    private String organizationalUnit;
    private String email;
    private String locality;
    private String country;
    private CertificateType certificateType;
    private CertificatePurpose purpose;
    private Date notBefore;
    private Date notAfter;
}
