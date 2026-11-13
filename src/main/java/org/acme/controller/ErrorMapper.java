package org.acme.controller;

import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Gibt Fehler (400, 404, 409 ...) als JSON {"message": "..."} zurueck,
 * damit das Frontend eine verstaendliche Meldung anzeigen kann.
 */
@Provider
public class ErrorMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse().getStatus();
        String message = exception.getMessage() != null ? exception.getMessage() : "Fehler";
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message))
                .build();
    }
}
