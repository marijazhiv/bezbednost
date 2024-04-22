package com.bookify.pki.model;

import com.bookify.pki.enumerations.CertificateRequestStatus;
import com.bookify.pki.enumerations.CertificateType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CertificateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String subjectName;
    private String locality;
    private String country;
    private String email;

    @Enumerated(EnumType.STRING)
    private CertificateType certificateType;

    @Enumerated(EnumType.STRING)
    private CertificateRequestStatus certificateRequestStatus;

}
