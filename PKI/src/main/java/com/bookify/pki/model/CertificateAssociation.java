package com.bookify.pki.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CertificateAssociation {
    Long issuerId;
    String alias;
    List<Long> signedCertificates;
}
