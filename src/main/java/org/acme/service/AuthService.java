package org.acme.service;

import java.time.Duration;
import java.util.Set;

import org.acme.dto.AuthResponseDto;
import org.acme.model.AppUser;
import org.acme.repository.AppUserRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.util.ModularCrypt;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

/**
 * Kleiner Identity Server: prueft Benutzerdaten und stellt signierte JWTs aus (Aufgabe 5.3).
 */
@ApplicationScoped
public class AuthService {

    public static final String ROLE_USER = "User";

    private static final WildFlyElytronPasswordProvider PASSWORD_PROVIDER = new WildFlyElytronPasswordProvider();

    @Inject
    AppUserRepository userRepository;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @Transactional
    public AuthResponseDto login(String username, String password) {
        AppUser user = userRepository.findByUsername(username.trim())
                .filter(u -> passwordMatches(password, u.getPasswordHash()))
                // Gleiche Meldung fuer "unbekannter Benutzer" und "falsches Passwort"
                .orElseThrow(() -> new NotAuthorizedException("Benutzername oder Passwort falsch", "Bearer"));
        return new AuthResponseDto(createToken(user), user.getUsername(), user.getRole());
    }

    /**
     * Registriert einen neuen Benutzer mit der Rolle "User" und meldet ihn direkt an.
     */
    @Transactional
    public AuthResponseDto register(String username, String password) {
        String name = username.trim();
        if (userRepository.findByUsername(name).isPresent()) {
            throw new ClientErrorException("Benutzername ist bereits vergeben", Response.Status.CONFLICT);
        }
        AppUser user = new AppUser();
        user.setUsername(name);
        user.setPasswordHash(BcryptUtil.bcryptHash(password));
        user.setRole(ROLE_USER);
        userRepository.persist(user);
        return new AuthResponseDto(createToken(user), user.getUsername(), user.getRole());
    }

    private String createToken(AppUser user) {
        return Jwt.upn(user.getUsername())
                .issuer(issuer)
                .groups(Set.of(user.getRole()))
                .expiresIn(Duration.ofHours(8))
                .sign();
    }

    private boolean passwordMatches(String plainPassword, String bcryptHash) {
        try {
            PasswordFactory factory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT, PASSWORD_PROVIDER);
            Password stored = factory.translate(ModularCrypt.decode(bcryptHash));
            return factory.verify(stored, plainPassword.toCharArray());
        } catch (Exception e) {
            return false;
        }
    }
}
