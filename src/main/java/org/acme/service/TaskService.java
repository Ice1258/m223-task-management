package org.acme.service;

import java.util.List;

import org.acme.dto.TaskDto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class TaskService {

    public List<TaskDto> findAll() {
        return List.of(exampleTask());
    }

    public TaskDto findById(long id) {
        if (id != 1L) {
            throw new NotFoundException("Aufgabe nicht gefunden");
        }
        return exampleTask();
    }

    public TaskDto create(TaskDto task) {
        validate(task, false);
        task.setId(2L);
        task.setStatus("offen");
        return task;
    }

    public TaskDto update(long id, TaskDto task) {
        validate(task, true);
        findById(id);
        task.setId(id);
        return task;
    }

    public void delete(long id) {
        findById(id);
    }

    private void validate(TaskDto task, boolean requireStatus) {
        if (task == null || task.getTitle() == null || task.getTitle().isBlank()) {
            throw new BadRequestException("Titel ist ein Pflichtfeld");
        }
        if (requireStatus && !"offen".equals(task.getStatus())
                && !"erledigt".equals(task.getStatus())) {
            throw new BadRequestException("Status muss offen oder erledigt sein");
        }
    }

    private TaskDto exampleTask() {
        TaskDto task = new TaskDto();
        task.setId(1L);
        task.setTitle("REST-API umsetzen");
        task.setDescription("Aufgabe 2.4 bearbeiten");
        task.setStatus("offen");

        return task;
    }
}
