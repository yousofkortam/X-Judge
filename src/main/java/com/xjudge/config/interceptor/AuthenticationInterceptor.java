package com.xjudge.config.interceptor;

import com.xjudge.exception.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Principal connectedUser = request.getUserPrincipal();
        if (connectedUser == null) {
            throw new UnauthenticatedException("User is not authenticated");
        }
        return true;
    }

}
