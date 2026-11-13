package org.acme.controller;

import java.util.List;

import org.acme.dto.ProjectDto;
import org.acme.service.ProjectService;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
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
import jakarta.ws.rs.core.MediaType;

@Path("/projects")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Projects", description = "Projekte verwalten")
@SecurityRequirement(name = "BearerAuth")
public class ProjectController {

    @Inject
    ProjectService projectService;

    @GET
    public List<ProjectDto> findAll() {
        return projectService.findAll();
    }

    @GET
    @Path("/{id}")
    public ProjectDto findById(@PathParam("id") long id) {
        return projectService.findById(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ProjectDto create(@NotNull @Valid ProjectDto project) {
        return projectService.create(project);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProjectDto update(@PathParam("id") long id, @NotNull @Valid ProjectDto project) {
        return projectService.update(id, project);
    }

    /** Nur Admins duerfen Projekte loeschen. */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("Admin")
    public void delete(@PathParam("id") long id) {
        projectService.delete(id);
    }
}
