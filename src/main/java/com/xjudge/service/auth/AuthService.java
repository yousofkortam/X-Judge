package com.xjudge.service.auth;

import com.xjudge.model.auth.*;
import org.springframework.validation.BindingResult;

import java.security.Principal;

public interface AuthService {
    RegisterResponse register(RegisterRequest registerRequest, BindingResult bindingResult);
    LoginResponse authenticate(LoginRequest loginRequest, BindingResult bindingResult);
    String verifyRegistrationToken(String token);
    void changePassword(ChangePasswordRequest changePasswordRequest, Principal connectedUser);
}
