#!/bin/bash

# Firebase Cloud Functions Deployment Script
# Run this script to deploy meal notification functions

echo "🚀 CUEats Firebase Functions Deployment"
echo "========================================"
echo ""

# Step 1: Install Firebase CLI (if not already installed)
echo "Step 1: Installing Firebase CLI..."
if ! command -v firebase &> /dev/null; then
    echo "Firebase CLI not found. Installing..."
    sudo npm install -g firebase-tools
    echo "✅ Firebase CLI installed"
else
    echo "✅ Firebase CLI already installed"
fi

echo ""

# Step 2: Login to Firebase
echo "Step 2: Logging into Firebase..."
firebase login
echo ""

# Step 3: Select Firebase project
echo "Step 3: Linking to Firebase project..."
firebase use --add
echo ""

# Step 4: Install function dependencies
echo "Step 4: Installing Cloud Functions dependencies..."
cd functions
npm install
cd ..
echo "✅ Dependencies installed"
echo ""

# Step 5: Deploy functions
echo "Step 5: Deploying Cloud Functions..."
firebase deploy --only functions
echo ""

echo "🎉 Deployment complete!"
echo ""
echo "Next steps:"
echo "1. Check the deployment output for function URLs"
echo "2. Test notifications using the testMealNotification URL"
echo "3. Enable notifications in the app's Profile screen"
echo ""
