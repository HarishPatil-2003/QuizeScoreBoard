package com.example;

import com.sun.net.httpserver.*;
import com.sun.tools.javac.Main;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class main {
    static List<String> names = new ArrayList<>();
    
    public static void main(String[]args) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress(8080),0);

        s.createContext("/", e -> send(e, """
            <h2>Quiz Score Board<h2>
            <a href="/index.html">open Score Form</a>
            ""","text/html"));

            s.createContext("/style.css", e -> {
                try(var in = Main.class.getResourceAsStream("/style.css")){
                    if (in == null) {
                        send(e, "CSS file missing", "text/plain");
                    } else {
                        send(e, new String(in.readAllBytes(),
                    StandardCharsets.UTF_8), "text/css");
                    }
                }
            });

            s.createContext("/index.html", e -> {
                try(var in = Main.class.getResourceAsStream("/index.html")) {
                    if(in == null) {
                        send(e, "HTML file missing", "text/plain");
                    } else {
                        send(e, new String(in.readAllBytes(),
                        StandardCharsets.UTF_8), "text/html");
                    }
                }
            });

            s.createContext("/save", e -> {
                if(!e.getRequestMethod().equals("POST")) {
                    e.getResponseHeaders().set("Allow", "POST");
                    e.sendResponseHeaders(405, -1 0);
                    return;
                }

                String body = new String(egetRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);
                Map<String, String> form = new HashMap<>();

                for (String part : body.split("&")) {
                    String[] p = part.split("=", 2);
                    if(p.length == 2)
                        form.put(URLDecoder.decode(p[0], "UTF_8"),
                    URLDecoder.decode(p[1], "UTF_8"));
                }

                String name = form.getOrDefault(name, "").trim();
                String subject = form.getOrDefault(subject, "");
                int score;

                try {
                    score = integer.parseInt(form.getOrDefault(score, "-1"));
                } catch (NumberFormatException ex) {
                    score = -1;
                }

                if (name.isEmpty() || name.length() > 100 ||
                     !List.of("Core Java", "Python", "Mern", "JavaScript").contains(subject) ||
                    score < 0 || score > 100) {
                        send(e, "Invalid input. Go back and try again.", "text/plain");
                        return;
                    }
                    synchronized (scores) {
                        scores.add(new String[]{name, subject, "" + score});
                    }

                    send(e, "<h2>Score saved successfully!</h2>"
                        + "<a href='/index.html'>Add another score</a><br>"
                        + "<a href='/score'>View Score Board</a>", "text/html");
            });

            s.createContext("/scores", e -> {
                StringBuilder b = new StringBuilder(
                    "<h2>Score Board</h2><table border='1'><tr>"
                    + "<th>Name</th><th>Subject</th><th>Score</th></tr>");

                    synchronized (scores) {
                        for (String[] r : score) {
                            b.append("<tr>");
                            for (String v : r)
                            b.append("<td>").append(escape(v)).append("</td>");
                            b.append("</tr>");
                        }
                    }

                    b.append("</table><p><a href='/index.html'>Back</a></p>");
                    send(e, b.toString(), "text/html");
                
            });
            
            s.start();
            System.out.println("Open http://localhost:8080/index.html");
    }
    static String escape (String v) {
        return v.replace("&", "&amp").replace("<", "&quot;");
    }
    static void send(HttpExchange e, String text, String type)
    throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("content-Type", type + "; Charset=UTF_8");
        e.sendResponseHeaders(200, data.length );
        try (OutputStream out = e.getResponseBody()) {
            out.write(data);
        }
    }
}
