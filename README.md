# Invoice Maker

An Android app for generating contractor proposal and invoice PDFs. Fill out the job details, hit generate, and send it straight to the client.

---

## Install

Open this link on your Android phone or tablet:

**[Download latest APK](https://github.com/spl90/InvoiceMaker/releases/latest/download/app-debug.apk)**

When Chrome asks if you want to allow installs from unknown sources, say yes. One-time thing.

---

## What it does

- Generates a PDF proposal or invoice that matches a standard contractor work order form
- Fills in all client info, job address, line items, subtotal, tax, and total
- Writes out the dollar amount in words (like a check) so clients just sign
- Email or print the PDF directly from the app
- Saves all your invoices so you can pull them up later
- Stores your business info and logo so you only enter it once

---

## Updating

When a new version drops, open the app, go to **Settings**, and tap **Update App**. It downloads and installs automatically.

---

## For developers

Built with Kotlin, Jetpack Compose, Room, and Hilt. PDF generation uses the Android `PdfDocument` API — no third-party PDF libraries.

```bash
# Build
./gradlew assembleDebug

# Deploy to connected device and publish release
./deploy.sh "your change description"
```

Pushing to `main` triggers a GitHub Actions build automatically.
