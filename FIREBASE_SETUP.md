# Firebase Setup Instructions

## Step 1: Add Sample Dishes to Firestore

1. **Open Firebase Console**
   - Go to https://console.firebase.google.com
   - Select your CU-Eats project

2. **Navigate to Firestore Database**
   - Click on "Firestore Database" in the left sidebar
   - If you haven't created a database yet, click "Create database"
   - Choose "Start in test mode" for now (we'll add security rules later)

3. **Create the "dishes" Collection**
   - Click "Start collection"
   - Collection ID: `dishes`
   - Click "Next"

4. **Add Sample Dishes**
   
   For each dish in `sample_dishes.json`, create a document:

   **Example: First Dish (Dhaba Chutney)**
   - Document ID: `dhaba_chutney`
   - Add fields:
     - `dishId` (string): `dhaba_chutney`
     - `name` (string): `Dhaba Chutney`
     - `calories` (number): `45`
     - `rating` (number): `4.2`
     - `ratingCount` (number): `156`
     - `isVeg` (boolean): `true`
     - `isHot` (boolean): `false`
     - `nutrition` (map):
       - `protein` (number): `2`
       - `carbs` (number): `8`
       - `fat` (number): `1`

   **Repeat for other dishes** (or use the import feature if available)

## Step 2: Update Your Meal Data (Optional)

To link dishes with meals, you need to add dish IDs to your existing meal data in Firebase Realtime Database.

**Example: Breakfast meal**
```json
{
  "type": "Breakfast",
  "time": "7:30 AM - 9:00 AM",
  "items": [
    "Dhaba Chutney",
    "Masala Aloo Sandwich",
    "Milk",
    "Tea"
  ],
  "dishIds": [
    "dhaba_chutney",
    "masala_aloo_sandwich",
    "milk",
    "tea"
  ]
}
```

**Note**: For now, we'll match dishes by name. Later we can add explicit dishIds.

## Step 3: Test the Connection

Once you've added the sample dishes, we'll test if the app can fetch them!
