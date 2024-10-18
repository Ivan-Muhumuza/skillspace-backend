package com.skillspace.career.controllers;

import com.skillspace.career.Client.CompanyClient;
import com.skillspace.career.Model.Career;
import com.skillspace.career.Service.CareerService;
import com.skillspace.career.dto.CareerProgramRequest;
import com.skillspace.career.dto.CompanyDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@RestController
@RequestMapping("/careers")
public class CareerController {
    @Autowired
    private final CompanyClient companyClient;

    private final CareerService careerService;



    @Autowired
    public CareerController(CareerService careerService,CompanyClient companyClient) {
        this.careerService = careerService;
        this.companyClient = companyClient;
    }


    @PutMapping("/{id}")
    public ResponseEntity<Career> updateCareer(@PathVariable Long id, @RequestBody Career careerDetails) {
        try {
            Career updatedCareer = careerService.updateCareer(id, careerDetails);
            return new ResponseEntity<>(updatedCareer, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 404 Not Found if the career does not exist
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Career>> getAllPrograms() {
        List<Career> careers = careerService.getAllPrograms();
        return new ResponseEntity<>(careers, HttpStatus.OK);
    }

    @GetMapping("/published")
    public ResponseEntity<List<Career>> getPublishedCareers() {
        List<Career> publishedCareers = careerService.getPublishedCareers();
        return new ResponseEntity<>(publishedCareers, HttpStatus.OK);
    }

    @GetMapping("/draft")
    public ResponseEntity<List<Career>> getDraftCareers() {
        List<Career> draftCareers = careerService.getDraftCareers();
        return new ResponseEntity<>(draftCareers, HttpStatus.OK);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCareer(@PathVariable Long id) {
        try {
            careerService.deleteCareer(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content if deletion was successful
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 404 Not Found if the career does not exist
        }
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<Career>> getProgramsByCompanyId(@PathVariable Long companyId) {
        List<Career> careers = careerService.getProgramsByCompanyId(companyId);
        return new ResponseEntity<>(careers, HttpStatus.OK);
    }

    @GetMapping("/company/name/{name}")
    public ResponseEntity<List<Career>> getProgramsByCompanyName(@PathVariable String name) {
        List<Career> careers = careerService.getProgramsByCompanyName(name);
        return new ResponseEntity<>(careers, HttpStatus.OK);
    }
    @GetMapping("/filter")
    public List<Career> filterProgramsByCompany(@RequestParam String companyName) {
        CompanyDTO company = companyClient.getCompanyByName(companyName);
        return careerService.getProgramsByCompanyId(company.getCompanyId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Career> getCareer(@PathVariable Long id) {
        List<Career> careers = careerService.getCareerById(id);
        if (careers.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(careers.getFirst(), HttpStatus.OK);
        }
    }

    @PostMapping("/draft")
    public ResponseEntity<?> createDraftCareerProgram(@RequestBody CareerProgramRequest request) {
        try {
            Career savedCareer = careerService.saveCareerProgramAsDraft(request);
            return ResponseEntity.ok(savedCareer);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PostMapping("/publish")
    public ResponseEntity<?> createPublishedCareerProgram(@RequestBody CareerProgramRequest request) {
        try {
            Career savedCareer = careerService.saveCareerProgramAsPublished(request);
            return ResponseEntity.ok(savedCareer);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
}
