# Practical 4 - Animated Login & Registration (Java version)

Same project as the Flask one, with the backend rewritten in Java.
The pages (HTML / CSS / JS) are the same, so the output looks and behaves the same.

| Python (Flask)          | Java                                         |
|-------------------------|----------------------------------------------|
| app.py (Flask)          | src/main/java/pr4/App.java (JDK HttpServer)  |
| mysql-connector-python  | mysql-connector-j (JDBC)                     |
| bcrypt                  | BouncyCastle bcrypt ($2b$, cost 12)          |
| templates/, static/     | src/main/resources/templates, static         |

Routes: /  /profile  /api/register  /api/login  /api/users  /api/test-db

## Setup
1. Install JDK 17+ and Maven, and have MySQL running.
2. Run schema.sql in MySQL (creates authentication_db and the users table).
   Users from the Python project keep working (same bcrypt format).
3. Open `src/main/resources/application.properties` and set your MySQL details (db.host, db.port, db.name, db.user, db.password) and the server port (server.port).
   Environment variables (DB_HOST, DB_USER, DB_PASSWORD, PORT ...) override the file if set.

## Run
    mvn compile exec:java

or build one jar:

    mvn package
    java -jar target/pr4-java.jar

Open http://127.0.0.1:5000  (http://127.0.0.1:5000/api/test-db checks MySQL).
To change the address, edit server.host / server.port in application.properties.
