# Vehicle Driver Mapping System

A web application for managing which driver drives which vehicle and when. Admins register vehicles and
drivers and send assignment requests. Drivers log in to accept or reject them. An **AI assistant** answers
plain-English questions such as *"Who is driving the Alto TS 09 AB 1234?"* or *"What is the time assigned
to Priya?"*.

| Layer    | Technology |
|----------|------------|
| Frontend | Angular 21 (standalone components, signals) |
| Backend  | Spring Boot 4 (Java 21, Spring MVC, Spring Data JPA, Bean Validation, Flyway) |
| Database | PostgreSQL 16 |
| AI       | Self-hosted open-source LLM (Llama 3.2 by default) served by [Ollama](https://ollama.com), with tool calling |

```
Angular UI  ──/api──►  Spring Boot REST API  ──JPA──►  PostgreSQL
                             │
                             └─ /api/ai/ask ──►  Ollama (local LLM)
                                                   │ calls tools
                                                   ▼
                                  FleetTools: read-only DB lookups
```

## Features

**Admin**
- Add, list and delete drivers and vehicles
- Assign a vehicle to a driver for a time window. This sends a request with status `SENT`.
- View all assignments and filter them by status
- Search drivers by name or phone
- Search a driver's assignments by status, and unassign
- Nearby drivers: on-duty drivers within a range of an (x, y) point at a given time, nearest first

**Driver portal**
- Register and log in. Passwords are BCrypt-hashed.
- See pending and accepted assignments, and accept or reject requests

**Scheduling rules** (kept from the original project)
- A vehicle cannot have two overlapping `ACCEPTED` assignments.
- A driver cannot have two overlapping `ACCEPTED` assignments.
- The end time must be after the start time.

**AI assistant** (`✨ Ask AI` in the UI)
- Examples: *"Who is driving the alto of number TS09AB1234?"*, *"What time is Ravi assigned tomorrow?"*,
  *"Which vehicles are free right now?"*, *"Show pending requests"*, *"Give me a fleet summary"*.
- Plates are matched regardless of spacing or case. A model name alone ("alto") lists every matching vehicle.
- Follow-up questions work, because the recent conversation is sent along.

## How the AI layer works

The assistant does not use a hosted chatbot API. It runs an open-source LLM on your own machine through
Ollama, and uses the model's **tool (function) calling**:

1. The question goes to the LLM with a system prompt (including the current date and time) and five tool
   definitions:
   - `find_vehicle`: look up by plate or make/model; returns who is driving it now or at a given time, plus its schedule
   - `find_driver`: look up by name, phone or email; returns contact details, current vehicle and assignment times
   - `list_assignments`: filter by status and/or day
   - `check_availability`: free vehicles and drivers in a time window
   - `fleet_summary`: counts and who is on duty now
2. The model chooses a tool. The backend runs it as a **read-only** JPA query and sends the JSON result back.
   The model never writes SQL and cannot change data.
3. The model writes the answer from those results. The UI shows which tools were used.

If Ollama is not running, or the model fails, a built-in **rule-based fallback** answers the common
questions from the same tools. The UI shows a notice when this happens. Set `AI_ENABLED=false` to always
use the fallback.

Code: `backend/src/main/java/com/vdms/ai/`. The main files are `FleetAssistant` (the LLM loop),
`FleetTools` (the lookups), `ToolRegistry` (the tool schemas) and `RuleBasedAssistant` (the fallback).

## Running with Docker (everything in one command)

```sh
docker compose up --build
```

This starts PostgreSQL, Ollama and a one-off job that downloads `llama3.2` (about 2 GB, first run only),
then the backend and the frontend.

- UI: http://localhost:4200
- API: http://localhost:8080/api

To use another model, run for example `OLLAMA_MODEL=qwen2.5:7b docker compose up --build`. Any Ollama model
that supports tools will work. Bigger models such as `qwen2.5:7b` or `llama3.1:8b` follow the tools more
reliably than the 3B default.

## Free online deployment (Oracle Cloud Always Free)

The whole stack, including the local LLM, runs on a free Oracle Cloud VM (Ampere A1, 4 CPU / 24 GB
RAM). After creating the VM, one command installs and starts everything:

```sh
curl -fsSL https://raw.githubusercontent.com/kanakamamidiakhil/Vehicle-Driver-Mapping-System/main/deploy/oracle/setup.sh | bash
```

Full step-by-step guide (account, VM, firewall, SSH):
**[deploy/oracle/README.md](deploy/oracle/README.md)**.

A single-container build for Hugging Face Spaces is also in `deploy/huggingface/`. Hugging Face now
requires a PRO subscription for Docker Spaces, so its workflow only runs when started manually.

## Running locally (for development)

Prerequisites: Java 21, Node.js 22.12 or newer, PostgreSQL, and optionally [Ollama](https://ollama.com/download).

1. **Database**
   ```sh
   createdb vehicle_mapping
   ```
   Then tell the backend your PostgreSQL password (the user defaults to `postgres`):
   ```sh
   export DB_PASSWORD=your-postgres-password      # Windows: set DB_PASSWORD=your-postgres-password
   ```
2. **LLM (optional but recommended)**
   ```sh
   ollama pull llama3.2
   ollama serve                    # listens on http://localhost:11434
   ```
3. **Backend** (Flyway creates the tables, and demo data is loaded into an empty database)
   ```sh
   cd backend
   ./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
   ```
4. **Frontend** (the dev server proxies `/api` to `localhost:8080`)
   ```sh
   cd frontend
   npm install
   npm start                       # http://localhost:4200
   ```

Demo driver logins (password `driver123`): `ravi@fleet.com`, `priya@fleet.com`, `arjun@fleet.com`,
`sneha@fleet.com`.

### Configuration (environment variables)

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/vehicle_mapping` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | *(empty)* | DB password. Set it unless your PostgreSQL uses trust auth. |
| `SEED_DEMO_DATA` | `true` | Load demo data when the DB is empty |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server |
| `OLLAMA_MODEL` | `llama3.2` | Model used by the assistant |
| `AI_ENABLED` | `true` | `false` = rule-based answers only |
| `AI_TIMEOUT` | `120s` | Max wait for one LLM response |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Allowed browser origins |

### Tests

```sh
cd backend && ./mvnw test
```

The tests run against an in-memory H2 database in PostgreSQL mode with a fixed clock. They cover the
scheduling rules, the REST API, the rule-based answers, and the LLM tool-calling loop (using a scripted
model).

## REST API

| Method | Path | Description |
|--------|------|-------------|
| GET/POST | `/api/drivers` | List / register drivers |
| GET/PUT/DELETE | `/api/drivers/{id}` | Get / update / delete a driver |
| GET | `/api/drivers/search?type=NAME\|PHONE&term=` | Search drivers |
| GET | `/api/drivers/nearby?x=&y=&range=&at=` | On-duty drivers near a point |
| GET | `/api/drivers/{id}/assignments?status=` | A driver's assignments |
| POST | `/api/auth/driver/login` | Driver login `{email, password}` |
| GET/POST | `/api/vehicles` | List / add vehicles |
| GET/PUT/DELETE | `/api/vehicles/{id}` | Get / update / delete a vehicle |
| GET/POST | `/api/assignments` | List / create assignment requests |
| GET | `/api/assignments/search?type=&term=&status=` | Search by driver and status |
| POST | `/api/assignments/{id}/accept` / `reject` | Driver response |
| DELETE | `/api/assignments/{id}` | Unassign |
| POST | `/api/ai/ask` | Ask the assistant `{question, history?}` |
| GET | `/api/ai/status` | Model name and whether Ollama is reachable |

## Note on authentication

As in the original project, the admin pages are not protected by a login. The driver portal keeps the
logged-in driver in the browser session. Add Spring Security (for example JWT or session login) before
exposing this outside a trusted network.

## Demo videos of the original (Django) version

Level 0 - https://drive.google.com/file/d/15-heQzK37nL6aiG6RQ64TnVwFA0aqhf_/view?usp=sharing

Level 1 - https://drive.google.com/file/d/1cmvoZSBh9TIbn5EI2SOVVAqu-Ftt17EG/view?usp=sharing

Level 2 - https://drive.google.com/file/d/1zKXoIXFBubY9VtEV6Fq3lMtv93L7iO1e/view?usp=sharing

Level 3 - https://drive.google.com/file/d/1vAJN8pWsa4NJfvfcntkclDaBRkwBbZjU/view?usp=sharing
