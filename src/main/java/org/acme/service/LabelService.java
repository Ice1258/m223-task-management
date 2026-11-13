package org.acme.service;

import java.util.Comparator;
import java.util.List;

import org.acme.dto.LabelDto;
import org.acme.model.Label;
import org.acme.repository.LabelRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class LabelService {

    @Inject
    LabelRepository labelRepository;

    @Inject
    DtoMapper mapper;

    @Transactional
    public List<LabelDto> findAll() {
        return labelRepository.listAll().stream()
                .sorted(Comparator.comparing(Label::getName, String.CASE_INSENSITIVE_ORDER))
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public LabelDto create(LabelDto dto) {
        String name = dto.getName().trim();
        ensureNameIsFree(name, null);
        Label label = new Label();
        label.setName(name);
        label.setColor(dto.getColor());
        labelRepository.persist(label);
        return mapper.toDto(label);
    }

    @Transactional
    public LabelDto update(long id, LabelDto dto) {
        Label label = getLabel(id);
        String name = dto.getName().trim();
        ensureNameIsFree(name, id);
        label.setName(name);
        label.setColor(dto.getColor());
        return mapper.toDto(label);
    }

    /**
     * Loescht ein Label und entfernt es vorher von allen Aufgaben (Eintraege in der Join-Tabelle).
     */
    @Transactional
    public void delete(long id) {
        Label label = getLabel(id);
        label.getTasks().forEach(task -> task.getLabels().remove(label));
        labelRepository.delete(label);
    }

    private Label getLabel(long id) {
        Label label = labelRepository.findById(id);
        if (label == null) {
            throw new NotFoundException("Label nicht gefunden");
        }
        return label;
    }

    private void ensureNameIsFree(String name, Long ownId) {
        labelRepository.find("lower(name)", name.toLowerCase()).firstResultOptional()
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new ClientErrorException("Ein Label mit diesem Namen existiert bereits",
                            Response.Status.CONFLICT);
                });
    }
}
