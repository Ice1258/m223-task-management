package org.acme.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.acme.dto.TaskDto;
import org.acme.model.Label;
import org.acme.model.Project;
import org.acme.model.Task;
import org.acme.repository.AppUserRepository;
import org.acme.repository.LabelRepository;
import org.acme.repository.ProjectRepository;
import org.acme.repository.TaskRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

/**
 * Fachlogik fuer Aufgaben. Abhaengigkeiten (Repositories, Mapper) kommen per Dependency Injection.
 */
@ApplicationScoped
public class TaskService {

    public static final String STATUS_OPEN = "offen";
    public static final String STATUS_DONE = "erledigt";

    @Inject
    TaskRepository taskRepository;

    @Inject
    ProjectRepository projectRepository;

    @Inject
    LabelRepository labelRepository;

    @Inject
    AppUserRepository userRepository;

    @Inject
    DtoMapper mapper;

    /**
     * Alle Aufgaben, optional gefiltert. Offene Aufgaben zuerst, dann nach Faelligkeit.
     */
    @Transactional
    public List<TaskDto> findAll(String status, Long projectId, Long labelId) {
        return taskRepository.listAll().stream()
                .filter(t -> status == null || status.equals(t.getStatus()))
                .filter(t -> projectId == null
                        || (t.getProject() != null && projectId.equals(t.getProject().getId())))
                .filter(t -> labelId == null
                        || t.getLabels().stream().anyMatch(l -> labelId.equals(l.getId())))
                .sorted(Comparator
                        .comparing((Task t) -> STATUS_DONE.equals(t.getStatus()))
                        .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Task::getId))
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public TaskDto findById(long id) {
        return mapper.toDto(getTask(id));
    }

    /**
     * Erstellt eine Aufgabe. Ohne Status wird sie als "offen" angelegt.
     * Ist ein Benutzername angegeben (aus dem JWT), wird er als Ersteller gespeichert.
     */
    @Transactional
    public TaskDto create(TaskDto dto, String username) {
        validate(dto, false);
        Task task = new Task();
        apply(dto, task);
        if (task.getStatus() == null) {
            task.setStatus(STATUS_OPEN);
        }
        if (username != null) {
            userRepository.findByUsername(username).ifPresent(task::setCreatedBy);
        }
        taskRepository.persist(task);
        return mapper.toDto(task);
    }

    /**
     * Aktualisiert eine Aufgabe. Zuerst wird validiert, danach geprueft, ob die Aufgabe existiert.
     */
    @Transactional
    public TaskDto update(long id, TaskDto dto) {
        validate(dto, true);
        Task task = getTask(id);
        apply(dto, task);
        return mapper.toDto(task);
    }

    @Transactional
    public void delete(long id) {
        if (!taskRepository.deleteById(id)) {
            throw new NotFoundException("Aufgabe nicht gefunden");
        }
    }

    private Task getTask(long id) {
        Task task = taskRepository.findById(id);
        if (task == null) {
            throw new NotFoundException("Aufgabe nicht gefunden");
        }
        return task;
    }

    /** Uebertraegt die Werte aus dem DTO auf die Entitaet (inkl. Projekt und Labels). */
    private void apply(TaskDto dto, Task task) {
        task.setTitle(dto.getTitle().trim());
        task.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }
        task.setDueDate(dto.getDueDate());
        task.setProject(resolveProject(dto.getProjectId()));
        task.setLabels(resolveLabels(dto.getLabelIds()));
    }

    private Project resolveProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        Project project = projectRepository.findById(projectId);
        if (project == null) {
            throw new BadRequestException("Projekt existiert nicht");
        }
        return project;
    }

    private Set<Label> resolveLabels(List<Long> labelIds) {
        Set<Label> labels = new HashSet<>();
        if (labelIds == null) {
            return labels;
        }
        for (Long labelId : labelIds) {
            Label label = labelRepository.findById(labelId);
            if (label == null) {
                throw new BadRequestException("Label existiert nicht");
            }
            labels.add(label);
        }
        return labels;
    }

    /**
     * Prueft die Eingaben. Wird zusaetzlich zur Bean Validation im Controller ausgefuehrt,
     * damit der Service auch ohne REST-Schicht (z.B. in Unit-Tests) korrekt reagiert.
     */
    private void validate(TaskDto task, boolean requireStatus) {
        if (task == null || task.getTitle() == null || task.getTitle().isBlank()) {
            throw new BadRequestException("Titel ist ein Pflichtfeld");
        }
        boolean hasValidStatus = STATUS_OPEN.equals(task.getStatus())
                || STATUS_DONE.equals(task.getStatus());
        if (requireStatus && !hasValidStatus) {
            throw new BadRequestException("Status muss offen oder erledigt sein");
        }
        if (!requireStatus && task.getStatus() != null && !hasValidStatus) {
            throw new BadRequestException("Status muss offen oder erledigt sein");
        }
    }
}
