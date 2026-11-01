package pr4;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Practical 4 - Animated Login & Registration (Java version of the Flask app).
 * Same pages, same /api/* endpoints, same JSON responses, same MySQL table.
 */
public class App {

    // ===================== CONFIGURATION (application.properties) =====================
    static final Properties PROPS = loadProperties();

    static final String DB_HOST = cfg(new String[]{"DB_HOST", "MYSQLHOST"}, new String[]{"db.host"}, "localhost");
    static final String DB_PORT = cfg(new String[]{"DB_PORT", "MYSQLPORT"}, new String[]{"db.port"}, "3306");
    static final String DB_USER = cfg(new String[]{"DB_USER", "MYSQLUSER"}, new String[]{"db.user", "spring.datasource.username"}, "personal");
    static final String DB_PASSWORD = cfg(new String[]{"DB_PASSWORD", "MYSQLPASSWORD"}, new String[]{"db.password", "spring.datasource.password"}, "MyNewPassword123!");
    static final String DB_NAME = cfg(new String[]{"DB_NAME", "MYSQLDATABASE"}, new String[]{"db.name"}, "authentication_db");
    static final String DB_URL = initDbUrl();

    static String initDbUrl() {
        String railwayUrl = System.getenv("MYSQL_URL");
        if (railwayUrl != null && !railwayUrl.trim().isEmpty()) {
            if (!railwayUrl.startsWith("jdbc:")) railwayUrl = "jdbc:" + railwayUrl;
            if (!railwayUrl.contains("?")) railwayUrl += "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            return railwayUrl.trim();
        }
        String customUrl = PROPS.getProperty("spring.datasource.url");
        if (customUrl != null && !customUrl.trim().isEmpty()) {
            if (!customUrl.contains("?")) {
                customUrl += "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            }
            return customUrl.trim();
        }
        return "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    static final SecureRandom RANDOM = new SecureRandom();

    /** Reads src/main/resources/application.properties from the classpath. */
    static Properties loadProperties() {
        Properties p = new Properties();
        try (InputStream in = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) p.load(in);
            else System.out.println("application.properties not found - using defaults");
        } catch (IOException e) {
            System.out.println("Could not read application.properties: " + e.getMessage());
        }
        return p;
    }

    /** Priority: environment variable > application.properties > default. */
    static String cfg(String envKey, String propKey, String def) {
        return cfg(new String[]{envKey}, new String[]{propKey}, def);
    }

    static String cfg(String envKey, String[] propKeys, String def) {
        return cfg(new String[]{envKey}, propKeys, def);
    }

    static String cfg(String[] envKeys, String[] propKeys, String def) {
        for (String envKey : envKeys) {
            String v = System.getenv(envKey);
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        for (String key : propKeys) {
            String val = PROPS.getProperty(key);
            if (val != null && !val.trim().isEmpty()) return val.trim();
        }
        return def;
    }

    // ===================== DATABASE CONNECTION =====================
    static Connection getDbConnection() {
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
            return null;
        }
    }

    // ===================== MAIN =====================
    public static void main(String[] args) throws IOException {
        String host = cfg("HOST", "server.host", "127.0.0.1");
        int port = Integer.parseInt(cfg("PORT", "server.port", "5000"));

        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", App::route);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println(" * Java server running on http://" + host + ":" + port);
    }

    // ===================== ROUTER =====================
    static void route(HttpExchange ex) throws IOException {
        try {
            // CORS (same as Flask-CORS)
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            if (method.equals("OPTIONS")) { send(ex, 204, "text/plain", new byte[0]); return; }

            if (path.equals("/") && method.equals("GET"))               page(ex, "templates/index.html");
            else if (path.equals("/profile") && method.equals("GET"))   page(ex, "templates/profile.html");
            else if (path.startsWith("/static/") && method.equals("GET")) staticFile(ex, path.substring(8));
            else if (path.equals("/api/test-db") && method.equals("GET")) testDb(ex);
            else if (path.equals("/api/register") && method.equals("POST")) register(ex);
            else if (path.equals("/api/login") && method.equals("POST"))    login(ex);
            else if (path.equals("/api/users") && method.equals("GET"))     getUsers(ex);
            else send(ex, 404, "text/plain; charset=utf-8", "404 Not Found".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            json(ex, 500, fail("Internal server error"));
        } finally {
            ex.close();
        }
    }

    // ===================== PAGES & STATIC FILES =====================
    static byte[] resource(String name) throws IOException {
        try (InputStream in = App.class.getClassLoader().getResourceAsStream(name)) {
            return in == null ? null : in.readAllBytes();
        }
    }

    static void page(HttpExchange ex, String name) throws IOException {
        byte[] data = resource(name);
        if (data == null) send(ex, 404, "text/plain", "Not found".getBytes());
        else send(ex, 200, "text/html; charset=utf-8", data);
    }

    static void staticFile(HttpExchange ex, String file) throws IOException {
        byte[] data = file.contains("..") ? null : resource("static/" + file);
        if (data == null) { send(ex, 404, "text/plain", "Not found".getBytes()); return; }
        String type = file.endsWith(".css") ? "text/css; charset=utf-8"
                : file.endsWith(".js") ? "application/javascript; charset=utf-8"
                : "application/octet-stream";
        send(ex, 200, type, data);
    }

    // ===================== TEST DATABASE =====================
    static void testDb(HttpExchange ex) throws IOException {
        Connection con = getDbConnection();
        if (con == null) { json(ex, 500, fail("Could not connect to MySQL")); return; }
        try (con; Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
            rs.next();
            Map<String, Object> r = ok("MySQL connection successful");
            r.put("database", rs.getString(1));
            json(ex, 200, r);
        } catch (SQLException e) {
            json(ex, 500, fail(e.getMessage()));
        }
    }

    // ===================== REGISTER =====================
    static void register(HttpExchange ex) throws IOException {
        Map<String, Object> data = body(ex);
        if (data == null || data.isEmpty()) { json(ex, 400, fail("No data received")); return; }

        String name = str(data, "name").trim();
        String email = str(data, "email").trim().toLowerCase();
        String password = str(data, "password");

        if (name.isEmpty())     { json(ex, 400, fail("Name is required")); return; }
        if (email.isEmpty())    { json(ex, 400, fail("Email is required")); return; }
        if (password.isEmpty()) { json(ex, 400, fail("Password is required")); return; }
        if (password.length() < 6) { json(ex, 400, fail("Password must be at least 6 characters")); return; }

        Connection con = getDbConnection();
        if (con == null) { json(ex, 500, fail("Database connection failed")); return; }

        try (con) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { json(ex, 409, fail("Email is already registered")); return; }
                }
            }

            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);
            String hash = OpenBSDBCrypt.generate("2b", password.toCharArray(), salt, 12);

            long userId;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users (name, email, password) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, hash);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); userId = keys.getLong(1); }
            }

            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", userId);
            user.put("name", name);
            user.put("email", email);
            Map<String, Object> r = ok("Registration successful");
            r.put("user", user);
            json(ex, 201, r);
        } catch (SQLException e) {
            System.out.println("Registration error: " + e.getMessage());
            json(ex, 500, fail("Registration failed"));
        }
    }

    // ===================== LOGIN =====================
    static void login(HttpExchange ex) throws IOException {
        Map<String, Object> data = body(ex);
        if (data == null || data.isEmpty()) { json(ex, 400, fail("No data received")); return; }

        String email = str(data, "email").trim().toLowerCase();
        String password = str(data, "password");

        if (email.isEmpty())    { json(ex, 400, fail("Email is required")); return; }
        if (password.isEmpty()) { json(ex, 400, fail("Password is required")); return; }

        Connection con = getDbConnection();
        if (con == null) { json(ex, 500, fail("Database connection failed")); return; }

        try (con; PreparedStatement ps = con.prepareStatement(
                "SELECT id, name, email, password FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { json(ex, 401, fail("Invalid email or password")); return; }

                boolean valid;
                try {
                    valid = OpenBSDBCrypt.checkPassword(rs.getString("password"), password.toCharArray());
                } catch (IllegalArgumentException bad) {
                    valid = false;
                }
                if (!valid) { json(ex, 401, fail("Invalid email or password")); return; }

                Map<String, Object> user = new LinkedHashMap<>();
                user.put("id", rs.getLong("id"));
                user.put("name", rs.getString("name"));
                user.put("email", rs.getString("email"));
                Map<String, Object> r = ok("Login successful");
                r.put("user", user);
                json(ex, 200, r);
            }
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
            json(ex, 500, fail("Login failed"));
        }
    }

    // ===================== GET ALL USERS =====================
    static void getUsers(HttpExchange ex) throws IOException {
        Connection con = getDbConnection();
        if (con == null) { json(ex, 500, fail("Database connection failed")); return; }

        try (con; Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, email, created_at FROM users ORDER BY id DESC")) {
            List<Object> users = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> u = new LinkedHashMap<>();
                u.put("id", rs.getLong("id"));
                u.put("name", rs.getString("name"));
                u.put("email", rs.getString("email"));
                Timestamp t = rs.getTimestamp("created_at");
                u.put("created_at", t == null ? null : t.toString());
                users.add(u);
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", true);
            r.put("users", users);
            json(ex, 200, r);
        } catch (SQLException e) {
            System.out.println("Get users error: " + e.getMessage());
            json(ex, 500, fail("Could not fetch users"));
        }
    }

    // ===================== HELPERS =====================
    static Map<String, Object> ok(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("message", msg);
        return m;
    }

    static Map<String, Object> fail(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("message", msg);
        return m;
    }

    static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : v.toString();
    }

    static Map<String, Object> body(HttpExchange ex) throws IOException {
        String text = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return new Json(text).parseObject();
        } catch (RuntimeException e) {
            return null;
        }
    }

    static void json(HttpExchange ex, int code, Object obj) throws IOException {
        StringBuilder sb = new StringBuilder();
        Json.write(sb, obj);
        send(ex, code, "application/json; charset=utf-8", sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    static void send(HttpExchange ex, int code, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        if (code == 204) { ex.sendResponseHeaders(204, -1); return; }
        ex.sendResponseHeaders(code, data.length);
        ex.getResponseBody().write(data);
    }

    // ===================== TINY JSON READER / WRITER =====================
    static class Json {
        private final String s;
        private int i = 0;

        Json(String s) { this.s = s; }

        Map<String, Object> parseObject() {
            ws();
            Object v = value();
            ws();
            if (!(v instanceof Map) || i != s.length()) throw new IllegalArgumentException("bad json");
            @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) v;
            return m;
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        private Object value() {
            if (i >= s.length()) throw new IllegalArgumentException("eof");
            char c = s.charAt(i);
            if (c == '{') {
                Map<String, Object> m = new LinkedHashMap<>();
                i++; ws();
                if (s.charAt(i) == '}') { i++; return m; }
                while (true) {
                    ws();
                    String k = string();
                    ws(); expect(':'); ws();
                    m.put(k, value());
                    ws();
                    if (s.charAt(i) == ',') { i++; continue; }
                    expect('}');
                    return m;
                }
            }
            if (c == '[') {
                List<Object> l = new ArrayList<>();
                i++; ws();
                if (s.charAt(i) == ']') { i++; return l; }
                while (true) {
                    ws(); l.add(value()); ws();
                    if (s.charAt(i) == ',') { i++; continue; }
                    expect(']');
                    return l;
                }
            }
            if (c == '"') return string();
            if (s.startsWith("true", i))  { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i))  { i += 4; return null; }
            int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (st == i) throw new IllegalArgumentException("bad token");
            return s.substring(st, i);
        }

        private void expect(char c) {
            if (i >= s.length() || s.charAt(i) != c) throw new IllegalArgumentException("expected " + c);
            i++;
        }

        private String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (i >= s.length()) throw new IllegalArgumentException("unterminated");
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char n = s.charAt(i++);
                    switch (n) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u': sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                        default: sb.append(n);
                    }
                } else sb.append(c);
            }
        }

        static void write(StringBuilder sb, Object o) {
            if (o == null) sb.append("null");
            else if (o instanceof Boolean || o instanceof Number) sb.append(o);
            else if (o instanceof Map) {
                sb.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                    if (!first) sb.append(',');
                    first = false;
                    quote(sb, String.valueOf(e.getKey()));
                    sb.append(':');
                    write(sb, e.getValue());
                }
                sb.append('}');
            } else if (o instanceof List) {
                sb.append('[');
                boolean first = true;
                for (Object x : (List<?>) o) {
                    if (!first) sb.append(',');
                    first = false;
                    write(sb, x);
                }
                sb.append(']');
            } else quote(sb, o.toString());
        }

        static void quote(StringBuilder sb, String t) {
            sb.append('"');
            for (char c : t.toCharArray()) {
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default:
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                }
            }
            sb.append('"');
        }
    }
}
