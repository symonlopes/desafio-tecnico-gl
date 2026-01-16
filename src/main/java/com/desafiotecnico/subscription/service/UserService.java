package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.dto.request.UserCreationRequest;
import com.desafiotecnico.subscription.domain.User;
import com.desafiotecnico.subscription.error.CodedException;
import com.desafiotecnico.subscription.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User createUser(UserCreationRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new CodedException("EMAIL_ALREADY_EXIST", "Já existe esse email cadastrado no sistema.");
        }
    }
}
