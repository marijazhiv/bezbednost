package com.bookify.pki.repository;

import com.bookify.pki.enumerations.CertificateRequestStatus;
import com.bookify.pki.model.CertificateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ICertificateRequestRepository extends JpaRepository<CertificateRequest, Long> {
    List<CertificateRequest> findAllByCertificateRequestStatusIs(CertificateRequestStatus certificateRequest);
}
