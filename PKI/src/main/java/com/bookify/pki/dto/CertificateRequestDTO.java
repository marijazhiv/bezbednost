package com.bookify.pki.dto;


import com.bookify.pki.enumerations.CertificateType;
import com.bookify.pki.model.CertificateRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequestDTO {
    private Long id;
    private String subjectName;
    private String locality;
    private String country;
    private String email;
    private CertificateType certificateType;

    public CertificateRequestDTO(CertificateRequest cr){
        this.id=cr.getId();
        this.subjectName=cr.getSubjectName();
        this.locality=cr.getLocality();
        this.country=cr.getCountry();
        this.email=cr.getEmail();
        this.certificateType=cr.getCertificateType();
    }



}
