package org.acme.controller;

import org.acme.dto.AuthResponseDto;
import org.acme.dto.CredentialsDto;
import org.acme.service.AuthService;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/auth")
@PermitAll
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Login und Registrierung")
public class AuthController {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public AuthResponseDto login(@NotNull @Valid CredentialsDto credentials) {
        return authService.login(credentials.getUsername(), credentials.getPassword());
    }

    @POST
    @Path("/register")
    public AuthResponseDto register(@NotNull @Valid CredentialsDto credentials) {
        return authService.register(credentials.getUsername(), credentials.getPassword());
    }
}
