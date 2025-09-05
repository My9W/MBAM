# MBAM - Water Meter Billing App

Mobile application for **automatic water meter reading and billing** for **PAM Desa Modangan Kidul**, developed as part of a university project at Universitas Kristen Satya Wacana.  
The app replaces manual recording with **AI-powered detection** and **OCR (Optical Character Recognition)** to improve accuracy and efficiency.

## 🚀 Features
- **AI Validation** – Detects whether the captured image is a valid water meter using a TensorFlow Lite model.  
- **OCR Integration** – Extracts numbers from the water meter using **Google ML Kit OCR**.  
- **Admin Role**  
  - Manage users (add/update/delete)  
  - Record and update water usage  
  - Generate and save billing notes  
  - Adjust AI detection accuracy  
- **User Role**  
  - Login with NIK  
  - View water usage history and bills  
  - Save/print billing notes  
- **Offline-First Architecture**  
  - Uses **Room Database** for local storage  
  - Syncs automatically with **Firebase Firestore** when online  

## 🛠️ Tech Stack
- **Kotlin (Android)**  
- **TensorFlow Lite** (AI image validation)  
- **Google ML Kit OCR** (digit recognition)  
- **Room Database** (local storage)  
- **Firebase Firestore** (cloud sync)  

## 📲 Installation
https://drive.google.com/drive/folders/1KrLQfEH-q9h8JlWWUizVuyfvnch5Kv-6
