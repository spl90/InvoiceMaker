# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android tablet application for generating contractor proposal/invoice PDFs. The app allows users to input job details, generate PDFs matching the visual layout of `invoice.jpg` (a contractor proposal/work order form), and email or print them directly.

The visual template (`invoice.jpg`) is a contractor proposal form with:
- Business logo/header with email and phone
- Contract/Proposal checkbox selection
- "Proposal Submitted To" and "Work To Be Performed At" sections
- Line items area for materials and labor descriptions
- Payment terms with signature/date/deposit/balance fields
- 15% late fee clause, valid for 30 days

## Architecture

**Pattern:** MVVM with Jetpack Compose UI layer
**Language:** Kotlin
**Min SDK:** 24 (Android 7.0)
**Target:** Tablet-optimized layouts

### Module Structure
```
app/
├── data/
│   ├── db/          # Room database, entities, DAOs
│   ├── model/       # Domain models (Invoice, LineItem, BusinessInfo)
│   └── repository/  # Repository pattern over Room
├── ui/
│   ├── input/       # Invoice input form composables
│   ├── history/     # Invoice history list/detail
│   ├── preview/     # PDF preview screen
│   └── theme/       # Material3 theme, dark mode support
├── viewmodel/       # ViewModels for each screen
├── pdf/             # PdfDocument-based PDF generator
├── util/            # Email intent builder, print adapter, FileProvider helpers
└── di/              # Hilt dependency injection modules
```

### Key Architectural Decisions

- **Room** for persistence of business info and invoice history. Business info is a singleton row; invoices have a one-to-many relationship with line items.
- **Android PdfDocument API** (not third-party) for PDF generation. The PDF layout must replicate `invoice.jpg` using Canvas drawing operations — header logo, form fields, table grid, footer terms.
- **FileProvider** required for sharing PDFs via email intents. Declared in AndroidManifest with a `file_provider_paths.xml` resource pointing to the app's files directory.
- **Android Print Framework** (`PrintManager` + `PrintDocumentAdapter`) for printing. Reuses the generated PDF file.
- **Hilt** for dependency injection across ViewModels, repositories, and the PDF generator.
- **Coroutines + Flow** for reactive data from Room and async PDF generation.

### Data Models

**InvoiceEntity:** id (auto-gen), businessInfoId, clientName, clientAddress, proposalDate, jobAddress, datePlans, architect, workDescription, contractOrProposal (enum), subtotal, taxPercent, total, notes, pdfPath, createdAt
**LineItemEntity:** id, invoiceId (FK), description, quantity, unitPrice, lineTotal
**BusinessInfoEntity:** id, businessName, address, phone, email, logoUri

### PDF Generation

The PDF generator draws directly on `PdfDocument.Page` canvases to match the proposal form layout:
1. Header with business logo/name/contact
2. Contract/Proposal checkboxes
3. Date field
4. Two-column section: "Proposal Submitted To" (name, address, phone) and "Work To Be Performed At" (address, date of plans, architect)
5. Work description paragraph
6. Line items table with gridlines
7. Dollar amount and payment terms
8. Signature/date/deposit/balance line
9. Terms and conditions footer (late fee notice, 30-day validity)

Multi-page support: track Y position and create new pages when content exceeds page height minus footer reserved space.

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected tablet
./gradlew installDebug

# Run all unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew testDebugUnitTest --tests "com.example.invoicegen.ExampleTest"

# Run instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Lint check
./gradlew lintDebug

# Clean build
./gradlew clean
```

## Key Dependencies

- Jetpack Compose BOM + Material3
- Room (runtime, compiler, ktx)
- Hilt (android, compiler)
- Navigation Compose
- Kotlin Coroutines + Flow
- AndroidX Core KTX, Lifecycle ViewModel Compose

## AndroidManifest Requirements

- `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />` (for SDK < 29 fallback)
- `<provider>` block for FileProvider with `android:authorities="${applicationId}.fileprovider"`
- `<supports-screens>` with `android:largeScreens="true"` and `android:xlargeScreens="true"`

## Tablet Layout Considerations

- Use `WindowSizeClass` to adapt layouts for tablet widths (expanded width class)
- Input form should use two-column layout on tablets matching the proposal form's "Submitted To" / "Work Performed At" side-by-side sections
- Touch targets minimum 48dp
- PDF preview alongside edit form in landscape (list-detail pattern)

## Currency & Calculation Rules

- All monetary values stored as `Long` (cents) to avoid floating-point errors
- Display formatted with `NumberFormat.getCurrencyInstance(Locale.US)`
- Line total = quantity × unit price (auto-calculated, not editable)
- Subtotal = sum of line totals
- Tax = subtotal × (taxPercent / 100)
- Total = subtotal + tax
- All totals update reactively via StateFlow in the ViewModel
