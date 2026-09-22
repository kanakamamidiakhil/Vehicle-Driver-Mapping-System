---
title: Vehicle Driver Mapping System
emoji: 🚗
colorFrom: blue
colorTo: indigo
sdk: docker
app_port: 7860
pinned: false
short_description: Fleet assignments with a local-LLM assistant
---

# Vehicle Driver Mapping System

Spring Boot + Angular + PostgreSQL, with an AI assistant powered by an open-source LLM
(Qwen 2.5 via Ollama) running inside this Space. No external AI API is used.

- Open the app, then **✨ Ask AI**: "Who is driving the Alto TS 09 AB 1234?"
- Demo driver logins: `ravi@fleet.com`, `priya@fleet.com`, `arjun@fleet.com`, `sneha@fleet.com`
  (password `driver123`).

The database is reset to the demo data whenever the Space restarts. Source code and full documentation:
https://github.com/kanakamamidiakhil/Vehicle-Driver-Mapping-System
