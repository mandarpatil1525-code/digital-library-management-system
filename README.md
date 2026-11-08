# Digital Library Management System
Java 17 · Spring Boot 3 · Thymeleaf · Spring Data JPA · H2 (default) / MySQL · HTML/CSS

## Run in VS Code
1. Install **JDK 17+** and the VS Code extensions *Extension Pack for Java* and *Spring Boot Extension Pack*.
2. `File > Open Folder…` and choose this `library-system` folder (wait for Maven to download dependencies).
3. Press **F5** (uses `.vscode/launch.json`), or open `LibraryApplication.java` and click **Run**.
   Terminal alternative (needs Maven): `mvn spring-boot:run`
4. Open http://localhost:8080

## Logins
- Admin: `admin@library.com` / `admin123`  (auto-created on first run)
- Users: register from the Register page.

## Database
Default is an H2 file database stored in `./data` (no install needed). H2 console: http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:file:./data/librarydb`, user `sa`, empty password).
For MySQL, edit `src/main/resources/application.properties` (instructions inside). Tables are auto-created.

## Business rules
Loan 14 days · max 3 books per member · fine ₹5/day when overdue (set `Issue.FINE_PER_DAY`)
· members with unpaid fines can't issue · reserve only when all copies are out; reserved copies are held for those members.

## Structure
model/ entities · repository/ Spring Data · service/LibraryService (all rules) · controller/ Auth, User, Admin
· config/ auth interceptor + seed data · resources/templates Thymeleaf pages
