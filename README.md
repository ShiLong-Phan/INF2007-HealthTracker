# Health Tracker

An intelligent fitness companion that helps you monitor daily activity, track nutrition, and maintain a healthy lifestyle.

## Features

### User Profile & Authentication
- Secure login and registration system
- Personalized user profiles with health metrics
- Customizable fitness goals

### Dashboard
- Daily summary of health activities
- Calorie intake and burn tracking
- Step counter with daily goals
- Hydration monitoring
- Weekly statistics with visual representations

### Food Tracking
- Calorie tracking for meals
- Barcode scanner for packaged foods
- AI-powered food recognition from images
- Detailed nutritional information

### AI Meal Planning
- Personalized meal recommendations based on:
  - Age, gender, weight, and height
  - Activity level and dietary preferences
  - Daily calorie targets
- Nearby restaurant recommendations based on your meal plan and real-time location

### Comprehensive History
- Detailed history of food entries
- Step count records
- Meal plans archive
- Filtered search by date or date range

## Technologies

- **Frontend**: Kotlin with Jetpack Compose
- **Backend**: Firebase (Authentication, Firestore)
- **AI Features**:
  - Gemini AI for meal recommendations and food recognition
  - AI-powered calorie estimation
- **APIs**:
  - Yelp API for restaurant recommendations
  - OpenFoodFacts for barcode scanning and nutrition data
- **Location Services**: Google Maps integration

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or newer
- Minimum SDK: Android 6.0 (API level 23)
- Gradle 7.0+
- Kotlin 1.5+

### Installation

1. Clone the repository:
   ```
   https://github.com/ShiLong-Phan/INF2007-HealthTracker.git
   ```

2. Open the project in Android Studio.

3. Set up Firebase:
   - Create a Firebase project
   - Add your Android app to Firebase
   - Download the google-services.json file and place it in the app directory
   - Enable Firebase Authentication and Firestore

4. Configure API keys:
   - Create a keys.properties file in the project root
   - Add your API keys:
     ```
     GEMINI_API_KEY=your_gemini_api_key_here
     YELP_API_KEY=your_yelp_api_key_here
     ```
   - **Important**: Never share your API keys publicly or commit them to version control

5. Build and run the app.

## Usage

### Dashboard
The dashboard provides a quick overview of your daily health metrics including:
- Step count with progress towards your daily goal
- Calorie intake and calories burned
- Hydration tracking with quick-add buttons
- Weekly trends for steps and calorie intake

### Food Tracking
Track your meals with multiple options:
- Scan barcodes of packaged food
- Take photos for AI-powered recognition
- Manual entry with calorie estimation assistance

### Meal Planning
Get AI-generated meal plans:
- Tailored to your dietary preferences and calorie goals
- Complete with restaurant recommendations near you
- Save meal plans for future reference

### History
View your complete health history:
- Filter by date or date range
- Swipe to delete entries
- View detailed statistics for any time period

## Architecture
The app follows a clean architecture approach with UI built using Jetpack Compose:
- Screens/ - UI components and screens
- utilities/ - Helper classes and utility functions
- Firebase integration for remote data storage
- ViewModel pattern for managing UI-related data

## License
This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgements
- [Firebase](https://firebase.google.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [OpenFoodFacts](https://world.openfoodfacts.org/)
- [Yelp Fusion API](https://www.yelp.com/developers/documentation/v3)
- [Gemini AI](https://ai.google.dev/)
