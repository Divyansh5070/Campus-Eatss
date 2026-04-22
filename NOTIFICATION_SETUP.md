# Firebase Cloud Messaging Notification Setup Guide

This guide will help you set up Firebase Cloud Functions for reliable meal notifications in the CUEats app.

## Prerequisites

- Firebase project already set up (you have this)
- Firebase Blaze Plan (pay-as-you-go) - **Required for Cloud Functions**
- Node.js 18+ installed on your machine
- Firebase CLI installed

## Step 1: Install Firebase CLI

If you haven't already, install the Firebase CLI:

```bash
npm install -g firebase-tools
```

## Step 2: Login to Firebase

```bash
firebase login
```

This will open a browser window for you to authenticate with your Google account.

## Step 3: Initialize Firebase Project

Navigate to your project directory and link it to your Firebase project:

```bash
cd /Users/divyanshsharma/AndroidStudioProjects/CUEats1
firebase use --add
```

Select your Firebase project from the list (likely "cueats" or similar).

## Step 4: Install Cloud Functions Dependencies

```bash
cd functions
npm install
```

This will install all the required packages (`firebase-admin` and `firebase-functions`).

## Step 5: Deploy Cloud Functions

Deploy the notification functions to Firebase:

```bash
cd /Users/divyanshsharma/AndroidStudioProjects/CUEats1
firebase deploy --only functions
```

This will deploy 7 functions:
- `sendBreakfastNotifications` - Weekday breakfast (7:30 AM IST)
- `sendBreakfastNotificationsWeekend` - Weekend breakfast (8:00 AM IST)
- `sendLunchNotifications` - Weekday lunch (12:00 PM IST)
- `sendLunchNotificationsWeekend` - Weekend lunch (12:30 PM IST)
- `sendSnackNotifications` - Daily snacks (4:30 PM IST)
- `sendDinnerNotifications` - Daily dinner (7:30 PM IST)
- `testMealNotification` - HTTP function for testing

## Step 6: Verify Deployment

After deployment, you should see URLs for your functions in the terminal. The scheduled functions will automatically run at their designated times.

To verify in Firebase Console:
1. Go to https://console.firebase.google.com
2. Select your project
3. Navigate to **Functions** in the left sidebar
4. You should see all 7 functions listed

## Step 7: Test Notifications

### Option 1: Test via HTTP Function

You can test notifications immediately using the HTTP test function:

```bash
# Test breakfast notification
curl "https://YOUR-REGION-YOUR-PROJECT.cloudfunctions.net/testMealNotification?meal=breakfast"

# Test lunch notification
curl "https://YOUR-REGION-YOUR-PROJECT.cloudfunctions.net/testMealNotification?meal=lunch"

# Test snacks notification
curl "https://YOUR-REGION-YOUR-PROJECT.cloudfunctions.net/testMealNotification?meal=snacks"

# Test dinner notification
curl "https://YOUR-REGION-YOUR-PROJECT.cloudfunctions.net/testMealNotification?meal=dinner"
```

Replace `YOUR-REGION-YOUR-PROJECT` with your actual function URL (shown after deployment).

### Option 2: Test via Firebase Console

1. Go to Firebase Console → Functions
2. Click on `testMealNotification`
3. Go to the **Logs** tab
4. Trigger the function manually

### Option 3: Wait for Scheduled Time

The functions will automatically run at their scheduled times. Check the logs to verify:

```bash
firebase functions:log
```

## Step 8: Enable Notifications in the App

1. Open the CUEats app
2. Navigate to **Profile** screen
3. Toggle **"Meal Notifications"** to ON
4. Your device will now receive notifications at meal times!

## Troubleshooting

### No notifications received?

1. **Check notification preference**: Make sure the toggle is ON in the profile screen
2. **Check FCM token**: Verify your device has registered an FCM token in Firestore (`users/{userId}/fcmTokens`)
3. **Check function logs**: 
   ```bash
   firebase functions:log
   ```
4. **Check app permissions**: Ensure notification permissions are granted on your device

### Functions not deploying?

1. **Check Firebase plan**: Cloud Functions require the Blaze (pay-as-you-go) plan
2. **Check Node.js version**: Must be Node.js 18+
   ```bash
   node --version
   ```
3. **Check Firebase CLI**: Update to latest version
   ```bash
   npm install -g firebase-tools@latest
   ```

### Notifications sent to wrong time?

The functions use `Asia/Kolkata` timezone (IST). If you need a different timezone, edit `functions/index.js` and change the `.timeZone()` parameter.

## Monitoring

### View Function Execution Logs

```bash
firebase functions:log
```

### View Function Usage and Costs

1. Go to Firebase Console → Functions
2. Click on **Usage** tab
3. Monitor invocations, execution time, and costs

### Expected Costs

For a typical usage pattern:
- ~6 function invocations per day (breakfast, lunch, snacks, dinner + weekend variants)
- Each invocation processes all users with notifications enabled
- **Estimated cost**: $0.01 - $0.10 per month for 100-1000 users

Firebase provides a generous free tier:
- 2 million invocations/month free
- 400,000 GB-seconds/month free
- 200,000 CPU-seconds/month free

## Updating Notification Messages

To change the notification messages:

1. Edit `functions/index.js`
2. Modify the message arrays at the top of the file
3. Redeploy:
   ```bash
   firebase deploy --only functions
   ```

## Changing Notification Times

To change when notifications are sent:

1. Edit `functions/index.js`
2. Find the `.schedule()` calls (e.g., `'30 7 * * 1-5'` for 7:30 AM weekdays)
3. Update the cron expression
4. Redeploy:
   ```bash
   firebase deploy --only functions
   ```

### Cron Expression Format

```
* * * * *
│ │ │ │ │
│ │ │ │ └─── Day of week (0-6, Sunday = 0)
│ │ │ └───── Month (1-12)
│ │ └─────── Day of month (1-31)
│ └───────── Hour (0-23)
└─────────── Minute (0-59)
```

Examples:
- `30 7 * * 1-5` = 7:30 AM Monday-Friday
- `0 12 * * *` = 12:00 PM every day
- `30 19 * * 0,6` = 7:30 PM Saturday and Sunday

## Support

If you encounter any issues:
1. Check the Firebase Console logs
2. Review the function execution history
3. Verify Firestore data structure matches expectations
4. Test with the HTTP test function first

---

**Note**: The old WorkManager notification system has been completely removed. All notifications are now handled by Firebase Cloud Functions for better reliability.
