package org.acme.repository;

import java.util.Optional;

import org.acme.model.AppUser;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppUserRepository implements PanacheRepository<AppUser> {

    public Optional<AppUser> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }
}
