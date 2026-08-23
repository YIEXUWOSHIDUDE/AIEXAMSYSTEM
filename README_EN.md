# AI Exam System

[中文说明](README.md) | English

An AI-assisted examination platform that adds document-based question ingestion, short-answer evaluation, and question-selection workflows to an existing exam-management foundation.

The system combines a Vue administration interface, a Spring Boot application, a Python document-processing service, MySQL persistence, MinIO object storage, and an OpenAI-compatible Qwen model endpoint.

> This repository is an engineering prototype. LLM-generated scores and feedback should support human review rather than serve as the sole basis for high-stakes assessment.

## What this version adds

- **Short-answer evaluation** — sends the question, reference answer, student answer, knowledge point, and maximum score to an LLM and returns structured scoring feedback.
- **Document-to-question ingestion** — parses uploaded DOCX files, extracts embedded images, preserves question-image relationships, and passes structured content into the question-import pipeline.
- **Question and answer matching** — checks the completeness of extracted questions and falls back to a simpler extraction path when validation fails.
- **AI-assisted question selection** — filters by knowledge point and difficulty, sends a reduced question representation to the model, and falls back to randomized selection when the AI service is unavailable.
- **Exam-management workflow** — supports question banks, papers, exams, users, roles, results, and administrative operations through the existing platform.

## Architecture

```text
Vue 2 + Element UI (8080)
            |
            | HTTP / JSON
            v
Spring Boot API (8101) ---------> MySQL
      |          |
      |          +--------------> OpenAI-compatible Qwen endpoint
      |
      +-------------------------> Python document service (8003)
                                      |
                                      +----> MinIO image storage
```

### Main request flows

```text
Document upload
  -> Python extracts text and embedded images
  -> images are stored in MinIO
  -> Spring Boot requests structured question extraction
  -> parsed questions and answers are validated and persisted

Short-answer submission
  -> Spring Boot builds a constrained evaluation prompt
  -> Qwen returns structured JSON
  -> the API validates and returns score, grade, and feedback
```

## Technology stack

| Layer | Technologies |
| --- | --- |
| Frontend | Vue 2, Vue Router, Vuex, Element UI, Axios |
| Backend | Java 8, Spring Boot, MyBatis-Plus, Apache Shiro, JWT, Maven |
| Data | MySQL, Druid connection pool |
| AI integration | Qwen3-32B through an OpenAI-compatible chat-completions API |
| Document processing | Python, FastAPI, python-docx, MinIO |
| Operations | Swagger, Quartz, Spring Boot Actuator |

## Repository layout

```text
AIEXAMSYSTEM/
├── exam-api/                         # Spring Boot API and business logic
├── exam-vue/                         # Vue 2 administration and exam UI
├── extract_questions_with_images.py  # DOCX text/image extraction service
└── docs/                             # Database and deployment references
```

## Prerequisites

- JDK 8 and Maven 3.6+
- MySQL 5.7+
- Node.js 14+ and npm
- Python 3.8+
- A MinIO-compatible object store
- An OpenAI-compatible Qwen model endpoint

The checked-in configuration contains development-specific values. Before running or deploying the project, replace database, object-storage, upload-path, and model-endpoint settings with values for your own environment. Do not reuse example credentials in production.

## Local setup

### 1. Initialize MySQL

Create a database and import the provided schema:

```bash
mysql -u <user> -p <database> < "docs/安装资源/数据库脚本.sql"
```

Then update the datasource settings in:

```text
exam-api/src/main/resources/application-dev.yml
```

### 2. Configure the AI and document services

Provide your model endpoint and model name for `AIProcessingService`. Configure the MinIO endpoint, access credentials, and bucket used by `extract_questions_with_images.py`.

For production use, move these values to environment variables or an external secrets manager instead of committing them to source control.

### 3. Start the Python document service

```bash
python -m venv .venv
source .venv/bin/activate
pip install fastapi uvicorn minio python-docx python-multipart
python extract_questions_with_images.py
```

The service listens on `http://localhost:8003`.

### 4. Start the Spring Boot API

```bash
cd exam-api
mvn spring-boot:run
```

The API listens on `http://localhost:8101`.

### 5. Start the Vue application

```bash
cd exam-vue
npm install
npm run dev
```

The development UI is available at `http://localhost:8080` by default.

## Key API endpoints

### Upload and split a document

```http
POST /exam/api/ai-upload/upload
Content-Type: multipart/form-data

file=<document>
subject=<optional subject>
grade=<optional grade>
```

### Evaluate a short answer

```http
POST /exam/api/judge/short-answer
Content-Type: application/json
```

```json
{
  "questionId": "question-001",
  "questionContent": "Explain the role of an operating-system scheduler.",
  "userAnswer": "It selects a ready process to run on the CPU.",
  "standardAnswer": "The scheduler selects work from the ready queue according to a scheduling policy.",
  "knowledgePoint": "CPU scheduling",
  "maxScore": 10
}
```

A successful model response is normalized to fields such as:

```json
{
  "score": 8.5,
  "percentage": 85,
  "grade": "B",
  "comment": "The main idea is correct; add how the scheduling policy affects the selection.",
  "maxScore": 10
}
```

### Analyze a completed test

```http
POST /exam/api/judge/overall-test
Content-Type: application/json
```

## Reliability and limitations

- Model output is parsed as structured JSON; malformed or unavailable responses are returned as service errors.
- Document extraction includes validation and fallback paths, but complex layouts, equations, and uncommon image formats can still require manual correction.
- AI-assisted question selection falls back to non-AI selection when the model call or response parsing fails.
- No benchmark in this repository establishes grading accuracy, latency, fairness, or repeatability. Evaluate these properties on a representative, human-scored dataset before real-world use.
- Protect student answers and assessment data according to the privacy and retention requirements of the deployment environment.

## Security checklist

Before exposing the system outside a local development environment:

- rotate any credentials that have appeared in source history;
- move database, MinIO, and model credentials out of the repository;
- change all default accounts and passwords;
- restrict CORS, file types, upload sizes, and administrative endpoints;
- avoid logging student answers or model prompts containing sensitive data;
- run dependency and secret scans as part of CI.

## License

This repository is distributed under the [MIT License](LICENSE).

