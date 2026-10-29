package com.example;

import java.io.InputStream;
import java.net.URI;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseManager {
    private static DatabaseManager instance;

    private String jdbcUrl;
    private String user;
    private String password;
    private boolean isConnected = false;
    private String statusMessage = "Not initialized";
    private String detectedHost = "";

    // In-memory fallback if database credentials are not configured or unreachable
    private final List<QuizScore> inMemoryScores = new CopyOnWriteArrayList<>();
    private final AtomicInteger inMemoryIdGenerator = new AtomicInteger(1);

    private DatabaseManager() {
        loadConfiguration();
        testAndInitializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try loading from application.properties in classpath
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("[DB] Could not load application.properties from classpath: " + e.getMessage());
        }

        // 2. Check Environment Variables (Supabase / Heroku / Render standard variables)
        String envUrl = System.getenv("SUPABASE_DB_URL");
        if (envUrl == null || envUrl.isBlank()) {
            envUrl = System.getenv("DATABASE_URL");
        }
        if (envUrl == null || envUrl.isBlank()) {
            envUrl = System.getenv("DB_URL");
        }
        if (envUrl == null || envUrl.isBlank()) {
            envUrl = props.getProperty("supabase.db.url", props.getProperty("db.url", ""));
        }

        String envUser = System.getenv("DB_USER");
        if (envUser == null || envUser.isBlank()) {
            envUser = props.getProperty("supabase.db.user", props.getProperty("db.user", "postgres"));
        }

        String envPass = System.getenv("DB_PASSWORD");
        if (envPass == null || envPass.isBlank()) {
            envPass = props.getProperty("supabase.db.password", props.getProperty("db.password", ""));
        }

        // Parse connection URL
        if (envUrl != null && !envUrl.isBlank()) {
            parseAndSetConnectionDetails(envUrl.trim(), envUser, envPass);
        } else {
            // Default individual properties
            String host = props.getProperty("db.host", "localhost");
            String port = props.getProperty("db.port", "5432");
            String name = props.getProperty("db.name", "postgres");
            this.user = envUser;
            this.password = envPass;
            this.jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + name + "?sslmode=require";
            this.detectedHost = host;
        }
    }

    private void parseAndSetConnectionDetails(String rawUrl, String defaultUser, String defaultPassword) {
        try {
            // Handle postgres:// or postgresql:// format from Supabase dashboard
            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                URI uri = new URI(rawUrl);
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String dbName = (path != null && path.length() > 1) ? path.substring(1) : "postgres";

                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    this.user = parts[0];
                    this.password = parts[1];
                } else if (userInfo != null) {
                    this.user = userInfo;
                    this.password = defaultPassword;
                } else {
                    this.user = defaultUser;
                    this.password = defaultPassword;
                }

                this.jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName + "?sslmode=require";
                this.detectedHost = host + ":" + port;
            } else if (rawUrl.startsWith("jdbc:postgresql://")) {
                this.jdbcUrl = rawUrl;
                this.user = defaultUser;
                this.password = defaultPassword;
                this.detectedHost = rawUrl.substring("jdbc:postgresql://".length()).split("/")[0];
            } else {
                this.jdbcUrl = "jdbc:postgresql://" + rawUrl;
                this.user = defaultUser;
                this.password = defaultPassword;
                this.detectedHost = rawUrl;
            }
        } catch (Exception e) {
            System.err.println("[DB] Error parsing connection URL: " + e.getMessage());
            this.jdbcUrl = rawUrl;
            this.user = defaultUser;
            this.password = defaultPassword;
        }
    }

    public synchronized boolean testAndInitializeDatabase() {
        if (password == null || password.isBlank() || password.equals("your_supabase_password_here")) {
            this.isConnected = false;
            this.statusMessage = "Database credentials not configured. Running in in-memory mode. Please configure application.properties or SUPABASE_DB_URL.";
            System.out.println("[DB] Notice: " + statusMessage);
            return false;
        }

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    createTablesIfNotExist(conn);
                    this.isConnected = true;
                    this.statusMessage = "Connected successfully to Supabase PostgreSQL at " + detectedHost;
                    System.out.println("[DB] SUCCESS: " + statusMessage);
                    return true;
                }
            }
        } catch (Exception e) {
            this.isConnected = false;
            this.statusMessage = "Connection failed: " + e.getMessage() + ". Operating with in-memory storage fallback.";
            System.err.println("[DB] WARNING: " + statusMessage);
        }
        return false;
    }

    private Connection getConnection() throws SQLException {
        if (user != null && !user.isBlank()) {
            return DriverManager.getConnection(jdbcUrl, user, password);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    private void createTablesIfNotExist(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS quiz_scores (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                subject VARCHAR(50) NOT NULL,
                score INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[DB] Table 'quiz_scores' verified/created in database.");
        }
    }

    public boolean saveScore(QuizScore score) {
        if (isConnected) {
            String sql = "INSERT INTO quiz_scores (name, subject, score) VALUES (?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, score.getName());
                pstmt.setString(2, score.getSubject());
                pstmt.setInt(3, score.getScore());
                int affected = pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        score.setId(rs.getInt(1));
                    }
                }
                return affected > 0;
            } catch (SQLException e) {
                System.err.println("[DB] Error inserting score into PostgreSQL: " + e.getMessage());
                // Fallback to in-memory on error
            }
        }

        // In-memory fallback
        score.setId(inMemoryIdGenerator.getAndIncrement());
        if (score.getCreatedAt() == null || score.getCreatedAt().isBlank()) {
            score.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        inMemoryScores.add(score);
        return true;
    }

    public List<QuizScore> getScores(String subjectFilter) {
        List<QuizScore> list = new ArrayList<>();

        if (isConnected) {
            boolean hasFilter = subjectFilter != null && !subjectFilter.isBlank() && !subjectFilter.equalsIgnoreCase("All");
            String sql = "SELECT id, name, subject, score, TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at "
                       + "FROM quiz_scores "
                       + (hasFilter ? "WHERE LOWER(subject) = LOWER(?) " : "")
                       + "ORDER BY score DESC, id ASC";

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                if (hasFilter) {
                    pstmt.setString(1, subjectFilter);
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        QuizScore qs = new QuizScore();
                        qs.setId(rs.getInt("id"));
                        qs.setName(rs.getString("name"));
                        qs.setSubject(rs.getString("subject"));
                        qs.setScore(rs.getInt("score"));
                        qs.setCreatedAt(rs.getString("created_at"));
                        list.add(qs);
                    }
                    return list;
                }
            } catch (SQLException e) {
                System.err.println("[DB] Error fetching scores from PostgreSQL: " + e.getMessage());
                // Fallback to in-memory
            }
        }

        // Return from in-memory fallback
        List<QuizScore> memList = new ArrayList<>(inMemoryScores);
        if (subjectFilter != null && !subjectFilter.isBlank() && !subjectFilter.equalsIgnoreCase("All")) {
            memList = memList.stream()
                .filter(s -> s.getSubject() != null && s.getSubject().equalsIgnoreCase(subjectFilter))
                .toList();
        }
        // Sort descending by score
        memList = new ArrayList<>(memList);
        memList.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return memList;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("connected", isConnected);
        map.put("message", statusMessage);
        map.put("host", detectedHost);
        map.put("mode", isConnected ? "Supabase PostgreSQL" : "In-Memory Fallback");
        return map;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}
