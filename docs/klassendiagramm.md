# UML-Klassendiagramm der Entitäten (Aufgabe 3.3)

Zum Exportieren als PNG/PDF: Code unten auf <https://mermaid.live> einfügen → *Actions* → *PNG/SVG*.

```mermaid
classDiagram
    direction LR

    class AppUser {
        -Long id
        -String username
        -String passwordHash
        -String role
        +getId() Long
        +getUsername() String
        +getPasswordHash() String
        +getRole() String
        +setUsername(String) void
        +setPasswordHash(String) void
        +setRole(String) void
    }

    class Project {
        -Long id
        -String name
        -String description
        -List~Task~ tasks
        +getId() Long
        +getName() String
        +getDescription() String
        +getTasks() List~Task~
        +setName(String) void
        +setDescription(String) void
    }

    class Task {
        -Long id
        -String title
        -String description
        -String status
        -LocalDate dueDate
        -Project project
        -Set~Label~ labels
        -AppUser createdBy
        +getId() Long
        +getTitle() String
        +getStatus() String
        +getDueDate() LocalDate
        +getProject() Project
        +getLabels() Set~Label~
        +getCreatedBy() AppUser
        +setTitle(String) void
        +setStatus(String) void
        +setDueDate(LocalDate) void
        +setProject(Project) void
        +setLabels(Set~Label~) void
        +setCreatedBy(AppUser) void
    }

    class Label {
        -Long id
        -String name
        -String color
        -Set~Task~ tasks
        +getId() Long
        +getName() String
        +getColor() String
        +getTasks() Set~Task~
        +setName(String) void
        +setColor(String) void
    }

    Project "0..1" --> "0..*" Task : enthält
    Task "0..*" --> "0..*" Label : hat
    AppUser "0..1" <-- "0..*" Task : erstellt von
```

## Beziehungen

| Beziehung | Art | Umsetzung in der Datenbank |
|---|---|---|
| Project – Task | 1:n (eine Aufgabe gehört höchstens zu einem Projekt) | Fremdschlüssel `task.project_id` (nullable) |
| Task – Label | n:m | Join-Tabelle `task_label (task_id, label_id)` |
| Task – AppUser | n:1 (Ersteller der Aufgabe) | Fremdschlüssel `task.created_by_id` (nullable) |

Getter/Setter für `id` sind bei allen Klassen vorhanden und hier der Übersicht halber nur als `getId()` aufgeführt.
