# Verdify 🌿

**Verdify** is a mobile application developed as a university project for the **Software Engineering** course. 
It is an AI-powered assistant designed to help users take better care of their plants by bridging the gap between botanical knowledge and modern technology.

## 📌 Project Overview
The main goal of this project was to build a functional prototype in **Kotlin** that solves real-world gardening challenges. By combining image processing and external APIs, Verdify provides users with data-driven advice to keep their plants healthy and thriving.

## ✨ Key Features
* **AI Plant Identification:** Simply take a photo or upload an image to identify your plant species instantly.
* **Smart Care Intelligence:** Once a plant is identified, the app provides tailored treatment guidelines:
    * **Watering:** Optimal frequency based on the specific species needs.
    * **Pruning:** Expert advice on when and how to trim your plants.
    * **Health Tips:** General actions to ensure long-term growth.
* **Weather Integration:** The app includes a built-in weather service that provides real-time updates and a **5-day forecast**.
* **Proactive Protection:** Verdify suggests specific actions (like moving a plant indoors) based on upcoming weather threats such as frost, heavy rain, or extreme heatwaves.

## 🛠 Tech Stack
* **Language:** Kotlin
* **Environment:** Android Studio
* **Core Concepts:** Software Engineering principles, API Integration (Weather & Plant ID), and UI/UX design.

---
*Developed for academic purposes.*

## ⚙️ Configuration & API Setup

This project relies on external services to provide AI and weather data. To run the app, you need to obtain API keys from the following providers:

* **PlantNet API:** Used for plant identification via image processing.
* **Google AI Studio (Gemini):** Used to generate smart care advice and processing data.
* **OpenWeatherMap API:** Used for real-time weather data and 5-day forecasts.

### Security Note
For security reasons, API keys are **not** stored in the source code. They are managed as environment variables.

To set up the project:
1. Create a `local.properties` file in your root directory (if not already present).
2. Add your keys as follows:
   ```properties
   PLANTNET_API_KEY=your_key_here
   GEMINI_API_KEY=your_key_here
   OPENWEATHER_API_KEY=your_key_here
