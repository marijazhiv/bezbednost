package com.bookify.pki.controller;

import com.bookify.pki.dto.CertificateDTO;
import com.bookify.pki.dto.NewCertificateDTO;
import com.bookify.pki.model.Certificate;
import com.bookify.pki.service.interfaces.ICertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/api/certificate")
public class CertificateController {

    @Autowired
    private ICertificateService certificateService;

    //dobavi podatke o sertifikatu
    @GetMapping("/{certId}")
    public ResponseEntity<CertificateDTO> getCertificate(@PathVariable Long certId) {
        Certificate c = certificateService.getCertificateById(certId);
        CertificateDTO certificateDTO = new CertificateDTO(c);
        return new ResponseEntity<>(certificateDTO, HttpStatus.OK);
    }


    //potpisani sertifikati??
    @GetMapping("/{certId}/signed")
    public ResponseEntity<List<CertificateDTO>> getCertificatesSignedSubjects(@PathVariable Long certId) {
        List<Certificate> signedCertificates = certificateService.getSignedCertificates(certId);
        List<CertificateDTO> signedCertificateDTOs = new ArrayList<>();
        for (Certificate c : signedCertificates){
            signedCertificateDTOs.add(new CertificateDTO(c));
        }
        return new ResponseEntity<>(signedCertificateDTOs, HttpStatus.OK);
    }

    //kreiranje noovg sertifikata
    @PostMapping
    public ResponseEntity<CertificateDTO> createNewCertificate(@RequestBody NewCertificateDTO newCertificateDTO){
        Certificate certificate = certificateService.createNewCertificate(newCertificateDTO);
        if(certificate == null) return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(new CertificateDTO(certificate), HttpStatus.OK);
    }

    //validiraj sertifikat
    @GetMapping("/validate/{certId}")
    public ResponseEntity<Boolean> validateCertificateChain(@PathVariable Long certId){
        if(certificateService.validateCertificateChain(certId)) return new ResponseEntity<>(true, HttpStatus.OK);
        return new ResponseEntity<>(false, HttpStatus.OK);
    }

    //brisanje sertifikata
    @DeleteMapping("/{certId}")
    public ResponseEntity<Long> deleteCertificate(@PathVariable Long certId){
        Long certificateId = certificateService.deleteCertificate(certId);
        if(certificateId == null) return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(certificateId, HttpStatus.OK);
    }


}
