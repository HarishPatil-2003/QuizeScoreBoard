# Quiz Score Board

A lightweight Java application and web dashboard for managing and displaying student quiz scores in real time, featuring Supabase (PostgreSQL) persistence.

## Features

- **Live Leaderboard**: Real-time score board and student performance tracking.
- **Score Submission**: Form to submit student names, subjects (Java, MERN, Python, JavaScript), and scores (0-100).
- **Supabase Integration**: PostgreSQL connection with fallback and automated table creation.
- **RESTful API**: Endpoints for saving scores, checking database connectivity, and retrieving top scores.

## Project Structure

```
Quize Score Board/
├── Quizwscoreboard/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── main.java            # HTTP server & routing
│   │   │   │   ├── DatabaseManager.java # Supabase connection & queries
│   │   │   │   └── QuizScore.java       # Model class
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── index.html           # Live UI
│   │   └── resources/
│   │       ├── style.css
│   │       └── script.js
│   └── pom.xml
└── README.md
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8+

### Configuration

Edit `Quizwscoreboard/src/main/resources/application.properties` or provide environment variables:

```properties
supabase.db.url=jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require
supabase.db.user=postgres
supabase.db.password=<YOUR_PASSWORD>
server.port=8080
```

Alternatively, set the environment variable:
```bash
export DATABASE_URL="jdbc:postgresql://..."
```

### Running the Application

```bash
cd Quizwscoreboard
mvn clean compile exec:java
```

Once running, navigate to `http://localhost:8080` in your browser.
