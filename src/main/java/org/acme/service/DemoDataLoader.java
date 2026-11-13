package org.acme.service;

import java.time.LocalDate;
import java.util.Set;

import org.acme.model.AppUser;
import org.acme.model.Label;
import org.acme.model.Project;
import org.acme.model.Task;
import org.acme.repository.AppUserRepository;
import org.acme.repository.LabelRepository;
import org.acme.repository.ProjectRepository;
import org.acme.repository.TaskRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Laedt beim Start Testdaten (Aufgabe 3.5): mindestens zwei Datensaetze pro Entitaet.
 * Aktiv nur, wenn taskmanagement.demo-data.enabled=true (Dev- und Test-Profil).
 */
@ApplicationScoped
public class DemoDataLoader {

    @ConfigProperty(name = "taskmanagement.demo-data.enabled", defaultValue = "false")
    boolean enabled;

    @Inject
    AppUserRepository userRepository;

    @Inject
    ProjectRepository projectRepository;

    @Inject
    LabelRepository labelRepository;

    @Inject
    TaskRepository taskRepository;

    void onStart(@Observes StartupEvent event) {
        if (enabled) {
            seed();
        }
    }

    @Transactional
    public void seed() {
        // Bereits gefuellte Datenbank nicht nochmals befuellen
        if (userRepository.count() > 0 || taskRepository.count() > 0) {
            return;
        }

        AppUser admin = user("admin", "admin123", "Admin");
        AppUser anna = user("anna", "anna1234", "User");

        Project school = project("Modul 223", "Aufgaben und Abgaben fuer das Modul 223");
        Project home = project("Haushalt", "Alles rund um die Wohnung");
        Project holiday = project("Ferien", "Planung der Sommerferien");

        Label urgent = label("Dringend", "#e5534b");
        Label schoolLabel = label("Schule", "#1f6feb");
        Label privateLabel = label("Privat", "#2da44e");
        Label later = label("Spaeter", "#8b949e");

        LocalDate today = LocalDate.now();
        task("Prüfung LB C vorbereiten", "Block 1 bis 8 repetieren, Cheat-Sheet anschauen",
                "offen", today.plusDays(3), school, Set.of(urgent, schoolLabel), anna);
        task("Frontend umsetzen", "Aufgabe 8.1: Frontend zur Projektarbeit",
                "offen", today.plusDays(7), school, Set.of(schoolLabel), admin);
        task("REST-API dokumentieren", "Endpunkte im README beschreiben",
                "erledigt", today.minusDays(2), school, Set.of(schoolLabel), admin);
        task("Einkaufen", "Milch, Brot, Kaffee",
                "offen", today, home, Set.of(privateLabel, urgent), anna);
        task("Wohnung putzen", null,
                "offen", today.plusDays(2), home, Set.of(privateLabel), anna);
        task("Unterkunft buchen", "Camping oder Hotel am See?",
                "offen", today.plusDays(30), holiday, Set.of(privateLabel, later), anna);
        task("Bücher zurückbringen", null,
                "erledigt", today.minusDays(5), null, Set.of(), admin);
    }

    private AppUser user(String username, String password, String role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(BcryptUtil.bcryptHash(password));
        user.setRole(role);
        userRepository.persist(user);
        return user;
    }

    private Project project(String name, String description) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        projectRepository.persist(project);
        return project;
    }

    private Label label(String name, String color) {
        Label label = new Label();
        label.setName(name);
        label.setColor(color);
        labelRepository.persist(label);
        return label;
    }

    private void task(String title, String description, String status, LocalDate dueDate,
            Project project, Set<Label> labels, AppUser createdBy) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setDueDate(dueDate);
        task.setProject(project);
        task.getLabels().addAll(labels);
        task.setCreatedBy(createdBy);
        taskRepository.persist(task);
    }
}
