package org.acme.controller;

import java.util.List;

import org.acme.dto.TaskDto;
import org.acme.service.TaskService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Tasks", description = "Aufgaben verwalten")
@SecurityScheme(securitySchemeName = "BearerAuth", type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT")
@SecurityRequirement(name = "BearerAuth")
public class TaskController {

    @Inject
    TaskService taskService;

    @Inject
    JsonWebToken jwt;

    /**
     * Alle Aufgaben. Optional filtern mit ?status=offen, ?projectId=1 oder ?labelId=2.
     */
    @GET
    public List<TaskDto> findAll(@QueryParam("status") String status,
            @QueryParam("projectId") Long projectId,
            @QueryParam("labelId") Long labelId) {
        return taskService.findAll(status, projectId, labelId);
    }

    @GET
    @Path("/{id}")
    public TaskDto findById(@PathParam("id") long id) {
        return taskService.findById(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public TaskDto create(@NotNull @Valid TaskDto task) {
        // Der angemeldete Benutzer (upn-Claim aus dem JWT) wird als Ersteller gespeichert
        return taskService.create(task, jwt.getName());
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public TaskDto update(@PathParam("id") long id, @NotNull @Valid TaskDto task) {
        return taskService.update(id, task);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") long id) {
        taskService.delete(id);
    }
}
