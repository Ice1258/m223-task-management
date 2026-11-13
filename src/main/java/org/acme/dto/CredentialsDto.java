package org.acme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Benutzername und Passwort fuer Login und Registrierung.
 */
public class CredentialsDto {

    @NotBlank(message = "Benutzername ist ein Pflichtfeld")
    @Size(min = 3, max = 50, message = "Benutzername muss 3 bis 50 Zeichen lang sein")
    private String username;

    @NotBlank(message = "Passwort ist ein Pflichtfeld")
    @Size(min = 6, max = 100, message = "Passwort muss mindestens 6 Zeichen lang sein")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
