const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

// Meal notification messages (same as Android app)
const breakfastMessages = [
  "Guess what's cooking this morning? 🍳👀",
  "Something hot and tasty is waiting… come check it out! 🥐🔥",
  "What's your guess: Paratha or Poha today? 🥘🤔",
  "A delicious surprise awaits you – don't miss breakfast! 🥞❓",
  "The aroma in the air isn't lying… breakfast's calling! ☕️👃"
];

const lunchMessages = [
  "Can you guess the main dish today? 🍛👀",
  "There's something special on your plate today! 🍲🎁",
  "Lunch just got interesting… any guesses? 🥙🔍",
  "A tasty twist awaits your lunchtime – curious? 😋❓",
  "Today's lunch might surprise you… check it out! 🍽️🕵️"
];

const snackMessages = [
  "It's that time… but what's the snack today? 🤤🧐",
  "Snack hour just dropped – something sweet or salty? 🍪🎯",
  "Bet you didn't expect *this* as today's snack! 🍩👀",
  "A mini treat is hiding in plain sight… go find it! 🧁🔍",
  "Something's waiting to crunch your cravings! 🍿❓"
];

const dinnerMessages = [
  "Dinner's on – but there's a twist! What could it be? 🥘😮",
  "A cozy meal is ready – want to know what's special tonight? 🌙🍛",
  "Could tonight be your favorite dish? Only one way to know… 🍽️🤫",
  "Your evening just got tastier… come find out how! 🕯️🍲",
  "Something comforting is waiting to end your day just right… 🛋️🍜"
];

/**
 * Helper function to get a random message from an array
 */
function getRandomMessage(messages) {
  return messages[Math.floor(Math.random() * messages.length)];
}

/**
 * Helper function to send notifications to users with enabled preferences
 */
async function sendMealNotification(mealName, messages) {
  try {
    console.log(`Starting ${mealName} notification send...`);

    // Get all users with notifications enabled
    const usersSnapshot = await admin.firestore()
      .collection('users')
      .where('notificationsEnabled', '==', true)
      .get();

    if (usersSnapshot.empty) {
      console.log('No users with notifications enabled');
      return { success: true, count: 0 };
    }

    console.log(`Found ${usersSnapshot.size} users with notifications enabled`);

    // Collect all FCM tokens
    const tokens = [];
    const tokenPromises = [];

    for (const userDoc of usersSnapshot.docs) {
      const userId = userDoc.id;
      tokenPromises.push(
        admin.firestore()
          .collection('users')
          .doc(userId)
          .collection('fcmTokens')
          .get()
          .then(tokensSnapshot => {
            tokensSnapshot.forEach(tokenDoc => {
              tokens.push(tokenDoc.id);
            });
          })
      );
    }

    await Promise.all(tokenPromises);

    if (tokens.length === 0) {
      console.log('No FCM tokens found');
      return { success: true, count: 0 };
    }

    console.log(`Sending to ${tokens.length} devices`);

    // Prepare notification message
    const messageText = getRandomMessage(messages);

    // Send to each token individually using FCM v1 API
    let successCount = 0;
    let failureCount = 0;
    const tokensToRemove = [];

    for (const token of tokens) {
      try {
        const message = {
          token: token,
          notification: {
            title: `${mealName} Time! 🍽️`,
            body: messageText
          },
          data: {
            type: 'meal_notification',
            mealName: mealName,
            message: messageText,
            timestamp: Date.now().toString()
          },
          android: {
            priority: 'high',
            notification: {
              sound: 'default',
              channelId: 'meal_reminder_channel'
            }
          }
        };

        await admin.messaging().send(message);
        successCount++;
      } catch (error) {
        console.error('Error sending to token:', error.code, error.message);
        failureCount++;

        // Mark invalid tokens for removal
        if (error.code === 'messaging/invalid-registration-token' ||
          error.code === 'messaging/registration-token-not-registered' ||
          error.code === 'messaging/invalid-argument') {
          tokensToRemove.push(token);
        }
      }
    }

    // Clean up invalid tokens from Firestore
    if (tokensToRemove.length > 0) {
      console.log(`Removing ${tokensToRemove.length} invalid tokens`);
      const removePromises = tokensToRemove.map(async (token) => {
        const usersWithToken = await admin.firestore()
          .collectionGroup('fcmTokens')
          .where(admin.firestore.FieldPath.documentId(), '==', token)
          .get();

        const deletePromises = usersWithToken.docs.map(doc => doc.ref.delete());
        return Promise.all(deletePromises);
      });
      await Promise.all(removePromises);
    }

    console.log(`${mealName} notifications sent: ${successCount} success, ${failureCount} failures`);

    return {
      success: true,
      successCount,
      failureCount,
      totalTokens: tokens.length
    };

  } catch (error) {
    console.error(`Error in ${mealName} notification:`, error);
    return { success: false, error: error.message };
  }
}

/**
 * Scheduled function for Breakfast notifications
 * Runs at 7:30 AM IST on weekdays, 8:00 AM IST on weekends
 */
exports.sendBreakfastNotifications = functions.pubsub
  .schedule('30 7 * * 1-5')  // Weekdays at 7:30 AM
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    return sendMealNotification('Breakfast', breakfastMessages);
  });

exports.sendBreakfastNotificationsWeekend = functions.pubsub
  .schedule('0 8 * * 0,6')  // Weekends at 8:00 AM
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    return sendMealNotification('Breakfast', breakfastMessages);
  });

/**
 * Scheduled function for Lunch notifications
 * Runs at 12:00 PM IST on weekdays, 12:30 PM IST on weekends
 */
exports.sendLunchNotifications = functions.pubsub
  .schedule('0 12 * * 1-5')  // Weekdays at 12:00 PM
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    return sendMealNotification('Lunch', lunchMessages);
  });

exports.sendLunchNotificationsWeekend = functions.pubsub
  .schedule('30 12 * * 0,6')  // Weekends at 12:30 PM
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    return sendMealNotification('Lunch', lunchMessages);
  });

/**
 * Scheduled function for Snacks notifications
 * Runs at 4:30 PM IST daily
 */
exports.sendSnackNotifications = functions.pubsub
  .schedule('30 16 * * *')  // Daily at 4:30 PM
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    return sendMealNotification('Snacks', snackMessages);
  });

/**
 * Scheduled function for Dinner notifications
 * Runs at 7:30 PM IST daily
 */
exports.sendDinnerNotifications = functions.pubsub
  .schedule('30 19 * * *')  // Daily at 7:30 PM
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    return sendMealNotification('Dinner', dinnerMessages);
  });

/**
 * HTTP function for testing notifications manually
 * Call with ?meal=breakfast|lunch|snacks|dinner
 */
exports.testMealNotification = functions.https.onRequest(async (req, res) => {
  const meal = req.query.meal || 'breakfast';

  let messages;
  let mealName;

  switch (meal.toLowerCase()) {
    case 'lunch':
      messages = lunchMessages;
      mealName = 'Lunch';
      break;
    case 'snacks':
      messages = snackMessages;
      mealName = 'Snacks';
      break;
    case 'dinner':
      messages = dinnerMessages;
      mealName = 'Dinner';
      break;
    default:
      messages = breakfastMessages;
      mealName = 'Breakfast';
  }

  const result = await sendMealNotification(mealName, messages);
  res.json(result);
});
