package com.xjudge.service.token;

import com.xjudge.entity.Token;
import com.xjudge.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final TokenRepository tokenRepository;

    @Override
    public void save(Token token) {
        tokenRepository.save(token);
    }

    @Override
    public Token findByToken(String token) {
        return tokenRepository.findByToken(token).orElseThrow(
                () -> new NoSuchElementException("Invalid token")
        );
    }
}
