package org.acme.service;

import java.util.Comparator;
import java.util.List;

import org.acme.dto.ProjectDto;
import org.acme.model.Project;
import org.acme.repository.ProjectRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class ProjectService {

    @Inject
    ProjectRepository projectRepository;

    @Inject
    DtoMapper mapper;

    @Transactional
    public List<ProjectDto> findAll() {
        return projectRepository.listAll().stream()
                .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public ProjectDto findById(long id) {
        return mapper.toDto(getProject(id));
    }

    @Transactional
    public ProjectDto create(ProjectDto dto) {
        String name = dto.getName().trim();
        ensureNameIsFree(name, null);
        Project project = new Project();
        project.setName(name);
        project.setDescription(dto.getDescription());
        projectRepository.persist(project);
        return mapper.toDto(project);
    }

    @Transactional
    public ProjectDto update(long id, ProjectDto dto) {
        Project project = getProject(id);
        String name = dto.getName().trim();
        ensureNameIsFree(name, id);
        project.setName(name);
        project.setDescription(dto.getDescription());
        return mapper.toDto(project);
    }

    /**
     * Loescht ein Projekt. Die Aufgaben bleiben erhalten, sind danach aber keinem Projekt zugeordnet.
     */
    @Transactional
    public void delete(long id) {
        Project project = getProject(id);
        project.getTasks().forEach(task -> task.setProject(null));
        projectRepository.delete(project);
    }

    private Project getProject(long id) {
        Project project = projectRepository.findById(id);
        if (project == null) {
            throw new NotFoundException("Projekt nicht gefunden");
        }
        return project;
    }

    private void ensureNameIsFree(String name, Long ownId) {
        projectRepository.find("lower(name)", name.toLowerCase()).firstResultOptional()
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new ClientErrorException("Ein Projekt mit diesem Namen existiert bereits",
                            Response.Status.CONFLICT);
                });
    }
}
