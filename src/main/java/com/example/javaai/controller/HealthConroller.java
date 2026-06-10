package com.example.javaai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthConroller {

    @GetMapping
    public String healthCheck(){
        return "OK";
    }
}
