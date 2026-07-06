# 🤖 Nova — AI Chatbot with Memory

> A full-stack conversational AI chatbot built with **Java Spring Boot** + **Groq API (Free)**, featuring stateful session memory, sliding window context management, and a sleek dark UI.

![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green?style=flat-square&logo=springboot)
![Groq](https://img.shields.io/badge/Groq-Free%20API-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-purple?style=flat-square)

---

## 📸 Preview

```
┌─────────────────────────────────────────┐
│  ✦ Nova          🧠 4 msgs   🗑 Clear   │
├─────────────────────────────────────────┤
│                                         │
│  [You]  My name is Vipin                │
│                                         │
│  [✦]   Nice to meet you, Vipin!        │
│                                         │
│  [You]  Write a poem about technology  │
│                                         │
│  [✦]   In circuits deep and code...   │
│                                         │
│  [You]  What is my name?               │
│                                         │
│  [✦]   Your name is Vipin! ✅          │
│                                         │
├─────────────────────────────────────────┤
│  Message Nova...              [↑ Send] │
└─────────────────────────────────────────┘
```

---

## ✨ Features

- 🧠 **Stateful Memory** — Maintains full conversation history per session using an in-memory array
- 🪟 **Sliding Window Algorithm** — FIFO pruning keeps the last 10 turns to prevent token overflow
- 🔒 **Structural Validation Gate** — Blocks empty inputs before they hit the API (prevents 400 errors)
- 🆔 **Session Isolation** — Every browser tab gets a unique `UUID` session, histories never mix
- 🎨 **Nova UI** — Dark glassmorphism interface with typing indicators and suggestion chips
- ⚡ **Groq Powered** — Uses Llama 3.3 70B on Groq's free tier (no credit card required)
- 🔄 **Model Swappable** — Switch between Llama, Mixtral, Gemma by changing one config line

---

## 🏗️ Project Structure

```
gemini-chatbot/
├── src/main/java/com/chatbot/
│   ├── ChatbotApplication.java          # Spring Boot entry point
│   ├── controller/
│   │   └── ChatController.java          # REST endpoints (/api/chat, /api/clear)
│   ├── service/
│   │   └── GeminiService.java           # Memory loop + Groq API calls
│   └── model/
│       └── ChatMessage.java             # Message schema (role + parts)
├── src/main/resources/
│   ├── application.properties           # API key + config
│   └── static/
│       └── index.html                   # Nova frontend (HTML/CSS/JS)
└── pom.xml                              # Maven dependencies
```

---

## 🧠 How Memory Works

This project implements the **Artificial Memory Loop** pattern from the DecodeLabs Generative AI curriculum:

```
User Input (Mₜ)  +  History Array (Hₜ₋₁)
          │
          ▼
   Validation Gate ──► (empty?) → reject
          │
          ▼
  Append to History
          │
          ▼
  Sliding Window Check (FIFO prune if > 10 turns)
          │
          ▼
  POST full history → Groq API
          │
          ▼
  Response (Rₜ) → Append to History → Show in UI
```

**Key insight:** LLMs are stateless by nature. Memory is faked by sending the entire conversation history with every request. This project proves you can transform a stateless LLM into a fully contextual chatbot through pure session state logic.

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+
- Free Groq API key → [console.groq.com](https://console.groq.com)

### Installation

**1. Clone the repository**
```bash
git clone https://github.com/YOUR_USERNAME/nova-chatbot.git
cd nova-chatbot
```

**2. Add your Groq API key**

Open `src/main/resources/application.properties` and replace:
```properties
openai.api.key=YOUR_GROQ_API_KEY_HERE
```
with your actual key (starts with `gsk_...`)

**3. Run the application**
```bash
mvn spring-boot:run
```

**4. Open in browser**
```
http://localhost:8080
```

---

## ⚙️ Configuration

All settings are in `src/main/resources/application.properties`:

```properties
# Your Groq API key (free at console.groq.com)
openai.api.key=gsk_xxxxxxxxxxxxxxxxxxxx

# API endpoint
openai.api.url=https://api.groq.com/openai/v1/chat/completions

# Model selection (swap anytime, no code change needed)
openai.model=llama-3.3-70b-versatile

# Sliding window size (number of conversation turns to remember)
chatbot.max.history.turns=10
```

### Available Groq Models (all free)

| Model | Speed | Best For |
|---|---|---|
| `llama-3.3-70b-versatile` | Fast | Best quality, recommended |
| `llama-3.1-8b-instant` | Blazing fast | Quick responses |
| `mixtral-8x7b-32768` | Fast | Long conversations |
| `gemma2-9b-it` | Fast | Google's open model |

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/chat` | Send a message, get AI response |
| `POST` | `/api/clear` | Clear conversation history for a session |
| `GET` | `/api/session/new` | Generate a new unique session ID |

### Example Request

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "abc-123", "message": "What is my name?"}'
```

### Example Response

```json
{
  "response": "Your name is Vipin!",
  "historySize": 4
}
```

---

## 🧪 Testing Memory (The Memory Exam)

Run these 3 messages in order to verify memory works correctly:

| Step | Input | Expected Output |
|---|---|---|
| 1 | `My name is Vipin` | Acknowledgement |
| 2 | `Write a poem about technology` | Long poem (fills context) |
| 3 | `What is my name?` | **"Vipin"** ✅ |

If step 3 correctly returns "Vipin", your stateful memory loop is working perfectly.

---

## 📚 Concepts Covered

This project demonstrates core Generative AI Engineering concepts:

- **Stateless vs Stateful APIs** — Building memory loops on top of stateless REST endpoints
- **Session State Management** — Input validation guards, FIFO truncation, sliding window managers
- **Chat Session Schema** — Structured role-content message objects (`user` / `assistant`)
- **Token Budget Management** — Preventing context window overflow with proactive pruning
- **API Integration** — Connecting to frontier LLMs via official SDKs

---

## 🔁 Switching APIs

This project is designed to be API-agnostic. To switch providers, change only `application.properties`:

**OpenAI**
```properties
openai.api.url=https://api.openai.com/v1/chat/completions
openai.model=gpt-4o-mini
openai.api.key=sk-xxxx
```

**Ollama (100% local, no key needed)**
```properties
openai.api.url=http://localhost:11434/v1/chat/completions
openai.model=llama3.2
openai.api.key=ollama
```

**Anthropic Claude**
> Requires minor code changes to `GeminiService.java` (different auth headers)

---

## 🛠️ Built With

- [Spring Boot 3.2](https://spring.io/projects/spring-boot) — Backend framework
- [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html) — Reactive HTTP client
- [Jackson](https://github.com/FasterXML/jackson) — JSON processing
- [Groq API](https://console.groq.com) — Free LLM inference
- [Llama 3.3 70B](https://groq.com) — Language model by Meta, hosted on Groq

---

## 🙏 Acknowledgements

- Built as part of the **DecodeLabs Generative AI Industrial Training** — Batch 2026
- Inspired by the *Engineering Stateful Conversational Agents* curriculum
- Nova UI design inspired by modern AI chat interfaces

---

<p align="center">Made with ❤️ by a DecodeLabs AI Engineering Intern</p>
