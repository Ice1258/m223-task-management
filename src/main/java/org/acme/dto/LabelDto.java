package org.acme.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LabelDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(readOnly = true)
    private Long id;

    @NotBlank(message = "Name ist ein Pflichtfeld")
    @Size(max = 50)
    private String name;

    @NotBlank(message = "Farbe ist ein Pflichtfeld")
    @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Farbe muss ein Hex-Wert wie #1f6feb sein")
    private String color;

    public LabelDto() {
    }

    public LabelDto(Long id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
