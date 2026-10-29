package com.example;

public class QuizScore {
    private int id;
    private String name;
    private String subject;
    private int score;
    private String createdAt;

    public QuizScore() {}

    public QuizScore(int id, String name, String subject, int score, String createdAt) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.score = score;
        this.createdAt = createdAt;
    }

    public QuizScore(String name, String subject, int score) {
        this.name = name;
        this.subject = subject;
        this.score = score;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
