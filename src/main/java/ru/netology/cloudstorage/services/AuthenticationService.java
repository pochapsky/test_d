package ru.netology.cloudstorage.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import ru.netology.cloudstorage.exceptions.BadCredentialsExceptionError;
import ru.netology.cloudstorage.models.User;
import ru.netology.cloudstorage.repositories.AuthRepository;
import ru.netology.cloudstorage.repositories.UserRepository;
import ru.netology.cloudstorage.request.RequestAuth;

import ru.netology.cloudstorage.response.JwtTokenResponse;
import ru.netology.cloudstorage.security.JWTUtils;


@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthenticationService(UserRepository userRepository, AuthRepository authRepository,
                                 JWTUtils jwtUtils, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.authRepository = authRepository;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
    }

    public JwtTokenResponse login(RequestAuth requestAuth) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(requestAuth.getLogin(),
                    requestAuth.getPassword()));

            System.out.println("getLogin: " + requestAuth.getLogin());
            System.out.println("getPassword: " + requestAuth.getPassword());
        } catch (BadCredentialsExceptionError e) {
            throw new BadCredentialsExceptionError();
        }
        User user = userRepository.findUserByLogin(requestAuth.getLogin());
        String token = jwtUtils.generateToken(user);
        System.out.println("token: " + token);
        authRepository.saveAuthenticationUser(token, user);

        System.out.println("getUserPassword " + user.getUserPassword());
        return new JwtTokenResponse(token);
    }

    public void logout(String authToken) {
        String jwt = authToken.substring(7);
        authRepository.deleteAuthenticationUserByToken(jwt);
    }
}