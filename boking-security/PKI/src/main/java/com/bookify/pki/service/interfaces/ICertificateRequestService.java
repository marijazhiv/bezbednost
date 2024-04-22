package com.bookify.pki.service.interfaces;

import com.bookify.pki.dto.CertificateRequestDTO;
import com.bookify.pki.model.Certificate;
import com.bookify.pki.model.CertificateRequest;

import java.util.List;

public interface ICertificateRequestService {
    CertificateRequest getRequestById(Long id);
    List<Certificate> getSignedCertificates(Long issuerId);
    CertificateRequest createCertificateRequest(CertificateRequestDTO certificateRequestDTO);
    CertificateRequest acceptCertificateRequest(Long issuerId, Long requestId);
    CertificateRequest rejectCertificateRequest(Long requestId);
    void signCertificateRequest(CertificateRequest request,Long issuerId);
    List<CertificateRequest> getPendingCertificateRequest();

}
