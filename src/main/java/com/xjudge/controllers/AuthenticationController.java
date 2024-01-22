package com.xjudge.controllers;

import com.xjudge.models.AuthRequest;
import com.xjudge.models.AuthResponse;
import com.xjudge.models.UserRegisterRequest;
import com.xjudge.service.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    AuthService authService;

    @Autowired
    public AuthenticationController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@RequestBody UserRegisterRequest registerRequest){
        return new ResponseEntity<>(authService.register(registerRequest) , HttpStatus.CREATED);
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> loginAuth(@RequestBody AuthRequest authRequest){
        return new ResponseEntity<>(authService.authenticate(authRequest) , HttpStatus.OK);
    }
}
