# 🌿 Verdify - Installation and Configuration Guide
This guide explains how to set up the development environment, generate the necessary API keys, and launch the Verdify application on Android Studio.


# 📋 Prerequisites
Android Studio.

JDK 8.

An Android device or a configured emulator.



# 🔑 API Key Generation

Verdify uses three external services to function. You must obtain a free key for each of them.


## 1. PlantNet API (Plant Identification)

Used to identify species from photos.


Go to my.plantnet.org.

Create an account and log in.

Go to Settings, then API key.

Click on Generate new API key.

Click on Expose my API key and enter your IP address under Authorized IPs.

Click on Update key settings.

Once created, copy the displayed "API Key" string.



## 2. Google Gemini API (Artificial Intelligence)

Used to generate personalized care advice.


Go to Google AI Studio.

Create an account and log in.

Click on "Get API key".

Select "Create API key".

Give the token a name and create a new project.

Copy the generated key.


## 3. OpenWeatherMap API (Weather)

Used for weather forecasting and climate analysis.


Go to openweathermap.org/api.

Create an account and log in.

Go to the "My API keys" section of your profile.

Name the key and click Generate.

Copy the "Key".


# ⚙️ Project Configuration

For security reasons, the API keys are not included in the source code; they must be saved in a local file that is not shared on Git.


Open the project in Android Studio.

In the project's root folder, look for the local.properties file.

If the file does not exist, create it manually in the root folder (where build.gradle.kts and settings.gradle.kts are located), under Gradle Scripts.

Open local.properties and add the three keys obtained previously, using these exact variable names:


## Properties

sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk

### Add your keys below:

PLANTNET_API_KEY=paste_your_plantnet_key_here

GEMINI_API_KEY=paste_your_gemini_key_here

OPENWEATHER_API_KEY=paste_your_openweather_key_here


Warning: Do not insert spaces before or after the equals sign and do not use quotation marks


# 🚀 Building and Running

After modifying local.properties, click on "Sync Project with Gradle Files" so that Android Studio can read the new keys.

Select your device or emulator.

Press the Run button.

The app will be installed and, upon the first launch, it will request camera and location permissions, which are necessary for plant recognition and weather features.


# 📌 Project Overview

The main goal of this project was to build a functional prototype in **Kotlin** that solves real-world gardening challenges. By combining image processing and external APIs, 

Verdify provides users with data-driven advice to keep their plants healthy and thriving.



# ✨ Key Features

* **AI Plant Identification:** Simply take a photo or upload an image to identify your plant species instantly.
  
* **Smart Care Intelligence:** Once a plant is identified, the app provides tailored treatment guidelines:

    * **Watering:** Optimal frequency based on the specific species needs.
      
    * **Pruning:** Expert advice on when and how to trim your plants.
      
    * **Health Tips:** General actions to ensure long-term growth.
      
* **Weather Integration:** The app includes a built-in weather service that provides real-time updates and a **5-day forecast**.
  
* **Proactive Protection:** Verdify suggests specific actions (like moving a plant indoors) based on upcoming weather threats such as frost, heavy rain, or extreme heatwaves.


# 🛠 Tech Stack

* **Language:** Kotlin
  
* **Environment:** Android Studio
  
* **Core Concepts:** Software Engineering principles, API Integration (Weather & Plant ID), and UI/UX design.


---
*Developed for academic purposes.*
