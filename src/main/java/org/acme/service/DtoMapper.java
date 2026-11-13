package org.acme.service;

import java.util.Comparator;

import org.acme.dto.LabelDto;
import org.acme.dto.ProjectDto;
import org.acme.dto.TaskDto;
import org.acme.model.Label;
import org.acme.model.Project;
import org.acme.model.Task;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Wandelt Entitaeten in DTOs um. Wird per Dependency Injection in die Services injiziert.
 */
@ApplicationScoped
public class DtoMapper {

    public TaskDto toDto(Task task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setDueDate(task.getDueDate());
        if (task.getProject() != null) {
            dto.setProjectId(task.getProject().getId());
            dto.setProjectName(task.getProject().getName());
        }
        dto.setLabels(task.getLabels().stream()
                .sorted(Comparator.comparing(Label::getName))
                .map(this::toDto)
                .toList());
        dto.setLabelIds(dto.getLabels().stream().map(LabelDto::getId).toList());
        if (task.getCreatedBy() != null) {
            dto.setCreatedBy(task.getCreatedBy().getUsername());
        }
        return dto;
    }

    public ProjectDto toDto(Project project) {
        ProjectDto dto = new ProjectDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setTaskCount(project.getTasks().size());
        dto.setOpenTaskCount((int) project.getTasks().stream()
                .filter(t -> "offen".equals(t.getStatus()))
                .count());
        return dto;
    }

    public LabelDto toDto(Label label) {
        return new LabelDto(label.getId(), label.getName(), label.getColor());
    }
}
