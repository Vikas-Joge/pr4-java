# Practical 4 - Animated Login & Registration (Java Version)

Same project as the Flask version, with the backend rewritten in Java.

The pages (HTML / CSS / JS) are the same, so the output looks and behaves the same.

| Python (Flask) | Java |
|---|---|
| app.py (Flask) | src/main/java/pr4/App.java (JDK HttpServer) |
| mysql-connector-python | mysql-connector-j (JDBC) |
| bcrypt | BouncyCastle bcrypt |
| templates/, static/ | src/main/resources/templates, static |

**Routes:** `/`, `/profile`, `/api/register`, `/api/login`, `/api/users`, `/api/test-db`

## Setup

1. Install JDK 17+ and Maven, and have MySQL running.
2. Run `schema.sql` in MySQL.
3. Open `src/main/resources/application.properties` and configure your MySQL details and server port.
4. Environment variables override the configuration file if set.

## Run

```bash
mvn compile exec:java
```

Or build a JAR:

```bash
mvn package
java -jar target/pr4-java.jar
```

Open `http://127.0.0.1:5000`.

Check MySQL using `http://127.0.0.1:5000/api/test-db`.
