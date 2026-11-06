package org.acme.controller;

import java.util.List;

import org.acme.dto.TaskDto;
import org.acme.service.TaskService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON)
public class TaskController {

    @Inject
    TaskService taskService;

    @GET
    public List<TaskDto> findAll() {
        return taskService.findAll();
    }

    @GET
    @Path("/{id}")
    public TaskDto findById(@PathParam("id") long id) {
        return taskService.findById(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public TaskDto create(TaskDto task) {
        return taskService.create(task);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public TaskDto update(@PathParam("id") long id, TaskDto task) {
        return taskService.update(id, task);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") long id) {
        taskService.delete(id);
    }
}
