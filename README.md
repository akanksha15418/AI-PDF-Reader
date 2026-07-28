# 📄 AI PDF Chat Assistant (RAG Based)

A full-stack, beginner-friendly Retrieval-Augmented Generation (RAG) web application built using **Java 21**, **Spring Boot 3**, **LangChain4j**, **InMemoryEmbeddingStore**, **Google Gemini API**, and **React (Vite)** with a modern **Green & White** theme.

---

## 🌟 Key Features

- **PDF Text Parsing**: Upload single PDF files and extract readable text using Apache PDFBox.
- **Document Chunking & Vector Ingestion**: Splits extracted text into chunks using LangChain4j and generates dense 384-dimensional vector embeddings locally via `AllMiniLmL6V2`.
- **In-Memory Embedding Store**: Uses `InMemoryEmbeddingStore` without requiring external vector databases like ChromaDB or Pinecone.
- **Semantic Search & Gemini Q&A**: Performs similarity retrieval to fetch top relevant PDF text chunks and sends context + user query to Google Gemini API (`gemini-1.5-flash`).
- **Real-Time Project Statistics**: Dashboard card tracking:
  - PDF Upload Status (Yes/No)
  - Total Questions Asked
  - Total Chunks Ingested
  - AI Model Used (`Google Gemini`)
- **Recent Questions Section**: Tracks the last 5 questions asked in the current session (no database needed; clicking a question re-asks it).
- **Clean Green & White UI**: Designed with an Emerald Green accent (`#22C55E`), responsive layout, drag-and-drop PDF upload, collapsible retrieved RAG context snippets, and loading animations.

---

## 🏗️ Architecture Overview

```
                     ┌──────────────────────────────────────────────┐
                     │          React (Vite) Frontend              │
                     │  - Green & White Clean Dashboard UI           │
                     │  - Drag & Drop PDF Upload                    │
                     │  - Real-time Session Stats & Chat           │
                     └──────────────────────┬───────────────────────┘
                                            │ HTTP (REST)
                                            ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                             Spring Boot 3 Backend                                │
│                                                                                  │
│  ┌───────────────────────┐   ┌──────────────────────┐   ┌─────────────────────┐  │
│  │ PdfChatController     │──▶│ PdfRagService        │──▶│ InMemoryEmbedding   │  │
│  │  - POST /api/pdf/upload  │   │  - LangChain4j RAG   │   │ Store               │  │
│  │  - POST /api/pdf/ask  │   │  - Document Splitter │   └─────────────────────┘  │
│  │  - GET  /api/pdf/stats│   │  - Embedding Model   │   ┌─────────────────────┐  │
│  └───────────────────────┘   │  - Gemini Chat Model │──▶│ Google Gemini API   │  │
│                              └──────────────────────┘   └─────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

- **Backend**: Java 21, Spring Boot 3.3, Maven, Apache PDFBox 3.0
- **RAG Framework**: LangChain4j 0.35.0
- **Embedding Model**: `AllMiniLmL6V2` (In-process local ONNX model)
- **Vector Store**: `InMemoryEmbeddingStore`
- **LLM**: Google Gemini API (`gemini-1.5-flash`)
- **Frontend**: React 18, Vite, Lucide-React Icons, Vanilla CSS Design System

---

## ⚡ Quick Start

### 1. Prerequisites
- Java 21+ JDK
- Node.js 18+ & npm
- Google Gemini API Key ([Get a free key here](https://aistudio.google.com/))

### 2. Run the Backend
```bash
cd backend
mvn spring-boot:run
```
*(Backend server runs at `http://localhost:8080`)*

### 3. Run the Frontend
```bash
cd frontend
npm install
npm run dev
```
*(Frontend server runs at `http://localhost:5173`)*

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/pdf/upload` | Upload single PDF file and ingest text chunks into `InMemoryEmbeddingStore`. |
| `POST` | `/api/pdf/ask` | Submit user question, retrieve matching PDF context, and get AI answer from Gemini. |
| `GET` | `/api/pdf/stats` | Fetch current session dashboard statistics and recent questions. |
| `POST` | `/api/pdf/reset` | Clear stored vector chunks, uploaded PDF state, and session statistics. |

---

## 📜 License

This project is open-source and licensed under the MIT License.
