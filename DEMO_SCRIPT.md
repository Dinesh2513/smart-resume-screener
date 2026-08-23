# 2–3 Minute Demo Script

## 0:00–0:20 — Introduction

“This is Smart Resume Screener, a Java Spring Boot application that parses PDF and text resumes, compares them with a job description using a local LLM, and ranks candidates with transparent evidence.”

Show the dashboard and briefly point to the fairness note.

## 0:20–0:45 — Inputs

1. Enter `Java Backend Developer` as the job title.
2. Paste `sample-data/java-backend-job-description.txt`.
3. Select all three resumes from `sample-data/resumes`.

Say: “The system supports multiple PDF or TXT resumes, validates file type and size, and stores parsed information in an H2 database.”

## 0:45–1:30 — AI Processing

Click **Analyse and rank candidates**.

Say: “Each resume is parsed into skills, experience, and education. The Ollama service uses qwen2.5 with a strict JSON schema to generate a score from 1 to 10, matched skills, missing skills, justification, and recommendation. If Ollama is unavailable, a deterministic fallback keeps the workflow operational.”

## 1:30–2:10 — Results

Show:

- Ranked candidate order
- Total, shortlisted, and average-score cards
- Score and candidate status
- Matched and missing skill chips
- Expand **View scoring evidence**
- Analysis source (`OLLAMA_LLM` or `RULE_BASED_FALLBACK`)

Say: “A score of seven or above is shortlisted, five to six requires manual review, and below five is rejected. The final decision remains with the recruiter.”

## 2:10–2:35 — Engineering Quality

Briefly show the project folders and terminal test result.

Say: “The application uses layered architecture with controllers, services, repositories, DTOs, and centralized exception handling. Automated tests cover parsing, real PDF extraction, persistence, API behavior, and the complete upload workflow.”

## 2:35–2:50 — Close

Show the README.

“The repository includes architecture, setup commands, documented LLM prompts, security decisions, sample data, and complete run instructions. Thank you.”

## Before Recording

```powershell
ollama list
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Use a clean browser window at `http://localhost:8080`. Keep VS Code ready with the test result and README tabs open.
