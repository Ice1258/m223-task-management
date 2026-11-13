package org.acme.repository;

import org.acme.model.Label;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LabelRepository implements PanacheRepository<Label> {
}
