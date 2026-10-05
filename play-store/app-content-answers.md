# Play Console "App content" answers – Expenses Manager 1.0.0

Play Console → **Policy and programs → App content**. These answers match the app as built: no account,
no ads, no analytics, all expense data stored on the device. The app does have internet permission, which
Google Play's billing library adds and uses only to process optional tips. **If you later add Google
sign-in, sync, ads or analytics, these answers must change before that release.**

Wording in Play Console changes from time to time; pick the option that means the same thing.

## Privacy policy
Paste the public URL of `privacy-policy.html`.

## Ads
**No**, my app does not contain ads.

## App access
**All functionality is available without special access.** (There is no login.)

## Content rating (IARC questionnaire)
- Category: **Utility, productivity, communication or other** (not a game).
- Answer **No** to every question about violence, sexual content, language, drugs, gambling, user-to-user
  communication, sharing location, and digital purchases.
- Expected rating: Everyone / PEGI 3 or similar.

## Target audience and content
- Target age groups: **18 and over** (you may add 16–17). Do **not** include under 13.
- Appeals to children: **No**.

## Data safety
- Does your app collect or share any of the required user data types? **No.**
  - Reason: data the user enters stays on the device and is never sent to the developer or any third
    party. Data that never leaves the device doesn't count as "collected" under Play's definitions.
  - **Check one thing for tips:** the app includes the Google Play Billing Library. Google publishes
    data-safety guidance for its own libraries (search "Play Billing Library data safety" in the Android
    developer docs, or check Play Console's SDK index). If that guidance says purchase history has to be
    declared, add **Financial info → Purchase history**: collected, not shared, used for app
    functionality, and required only when the user chooses to tip. Otherwise keep "No".
  - Exports and backups are files the user saves wherever they choose. Android's own device backup is a
    system feature controlled by the user.
- Since nothing is collected, the questions about encryption in transit and deletion requests are skipped
  or answered "not applicable". If asked whether users can delete their data: **Yes**, via "Delete all my
  data" in the Profile tab, or by uninstalling.

## Financial features
The app only records the user's own spending. It doesn't move money, give loans, trade, offer
banking or payments, or connect to any financial account. Pick the option closest to
**personal finance management / budgeting with no money transfers**. If no option fits, choose
"My app doesn't provide any of these financial features".

## Government apps, News, Health, COVID-19, Data deletion for accounts
- Government app: **No**. News app: **No**. Health features: **No**.
- Account deletion URL: **not required**, because the app has no accounts.

## Tips ("Buy me a coffee") – one-time setup
The app looks for three in-app products with **exactly** these product IDs. Until they exist and are
active, the About page shows "Tips are available in the Google Play Store version of the app."

1. **Payments profile:** Play Console → Settings → **Payments profile**. Add bank and tax details. It's
   needed before you can sell anything, including tips. Free.
2. **Upload a build first:** Play only lets you create products after an `.aab` that includes billing
   has been uploaded to any track. Upload 1.2.0 to **Internal testing**.
3. **Create the products:** your app → **Monetize with Play → Products → One-time products → Create**:

   | Product ID | Name shown in Google Play | Suggested price |
   |---|---|---|
   | `tip_coffee` | Coffee ☕ | ₹49 |
   | `tip_coffee_cake` | Coffee & cake 🍰 | ₹99 |
   | `tip_big_thanks` | Big thanks 🎉 | ₹199 |

   Description for each: "An optional tip to support the developer. It doesn't unlock anything; every
   feature is free." Set the price in INR, let Play convert other countries, then **Activate** each one.
   Product IDs can never be reused, so type them carefully.
4. **Test without being charged:** Play Console → Settings → **License testing** → add your Gmail
   address. Install the app from the Internal testing link (not from an APK) and tip: you'll see
   "Test card, always approves" and won't be charged.

Google keeps 15% of each tip (for developers earning under US$1 million a year); the rest is paid to
your bank monthly. In the store listing, Play automatically shows "In-app purchases".

## Store settings
- App or game: **App**. Category: **Finance**.
- Contact email: pratulkhandelwal16@gmail.com (shown publicly on the listing).
