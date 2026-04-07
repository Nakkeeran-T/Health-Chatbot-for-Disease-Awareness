# AI-Driven Public Health Chatbot for Disease Awareness

A full-stack application designed to provide health information, disease awareness, and symptom-based predictions for rural and semi-urban populations.

## Project Structure

- **`backend/`**: Spring Boot application providing the REST API, security (JWT), and database integration (PostgreSQL).
- **`health-chatbot-frontend/`**: React application (Vite) for the user interface, supporting multilingual features.
- **`mlmodels/`**: Python Flask API for machine learning model inference (Intent Classification, Disease Prediction, Language Detection).

## Prerequisites

- **Java 17+**
- **Maven**
- **PostgreSQL**
- **Node.js & npm**
- **Python 3.9+**

## Setup and Running

### 1. ML API (Python)
```bash
cd mlmodels
pip install -r requirements.txt
python train_models.py  # Only once to generate models
python ml_api_server.py
```
- API runs on: `http://localhost:5001`

### 2. Backend (Spring Boot)
1. Configure `backend/src/main/resources/application.properties` (use `application.properties.example` as a template).
2. Run the application:
```bash
cd backend
mvn spring-boot:run
```
- API runs on: `http://localhost:8080`

### 3. Frontend (React)
```bash
cd health-chatbot-frontend
npm install
npm run dev
```
- App runs on: `http://localhost:5173`

## Features

- **Multilingual Support**: English, Hindi, and Odia.
- **AI Symptom Checker**: Predicts potential diseases based on user-provided symptoms.
- **Outbreak Alerts**: Real-time alerts for local disease outbreaks.
- **Vaccination Schedules**: Information on essential vaccinations.
- **Secure Authentication**: JWT-based login and registration.

## License
MIT
