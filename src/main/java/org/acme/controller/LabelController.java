package org.acme.controller;

import java.util.List;

import org.acme.dto.LabelDto;
import org.acme.service.LabelService;
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

@Path("/labels")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Labels", description = "Labels verwalten")
@SecurityRequirement(name = "BearerAuth")
public class LabelController {

    @Inject
    LabelService labelService;

    @GET
    public List<LabelDto> findAll() {
        return labelService.findAll();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public LabelDto create(@NotNull @Valid LabelDto label) {
        return labelService.create(label);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public LabelDto update(@PathParam("id") long id, @NotNull @Valid LabelDto label) {
        return labelService.update(id, label);
    }

    /** Nur Admins duerfen Labels loeschen. */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("Admin")
    public void delete(@PathParam("id") long id) {
        labelService.delete(id);
    }
}
