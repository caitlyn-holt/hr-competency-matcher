package com.example.HRmatch;

import lombok.Data;

@Data
public class ApplicationRequest {
    private Long vacancyId;
    private Long candidateId;
    private String message;
}