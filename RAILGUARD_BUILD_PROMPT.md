# RailGuard full-build prompt

Build a production-quality mobile application called **RailGuard**, an AI-powered Railway Crack Detection and Safety Analysis platform for railway inspectors, maintenance engineers, and safety leads.

The app must feel like a trusted field engineering instrument used beside live track: precise, calm under pressure, fast to scan, and defensible in an audit. It must not look like a generic student dashboard, a generic SaaS admin template, or a collection of disconnected mockups.

## Design system

Use one shared design system across every screen:

- Light mode is the default product experience: cool-white background, white cards, graphite text, navy-blue actions, and restrained 1px borders. Include a fully supported dark mode as an explicit in-app setting.
- Strong white primary text, muted gray secondary text, and compact information-dense layouts.
- Semantic states must include a label and icon, not color alone:
  - Healthy: green + check/verified icon
  - Warning: amber + alert icon
  - Critical: red + critical alert icon
  - Unknown: neutral gray + help/info icon
- Use Inter for typography. Use consistent weights: 400 body, 500 supporting labels, 600 titles, 700 values and headings.
- Use a small spacing scale, consistent touch targets of at least 44px, and restrained corner radii. Avoid excessive pills, gradients, glass effects, oversized empty areas, and decorative illustrations.
- Use Lucide, Feather, or another single professional icon family. Do not mix icon styles and do not use emojis.
- Define reusable tokens for colors, typography, spacing, radius, borders, shadows, elevation, animation timing, and responsive breakpoints.
- Build reusable components: AppShell, TopBar, BottomNavigation, PageHeader, MetricCard, StatusBadge, SeverityBadge, AlertCard, DefectCard, InspectionCard, TrackHealthCard, GPSCard, MapCard, ChartCard, Timeline, EvidenceGallery, ImageComparison, CrackOverlay, DetectionBoundingBox, ProgressIndicator, SearchBar, FilterBar, DataTable, Modal, Drawer, BottomSheet, Toast, NotificationItem, EmptyState, LoadingState, ErrorState, ConfirmationDialog, ReportPreview, and form fields.

## Complete screen set

Implement every screen below as a real route. Do not skip, merge, or leave any route as an empty placeholder. Every screen must have purposeful content, a back path, and at least one useful next action.

### Authentication

Splash, Onboarding, Login, Registration, OTP, Email Verification, Forgot Password, Reset Password.

### Main

Home, Notifications, Profile.

### Inspection

Inspection Setup, Live Inspection, Camera, Detection Result, Crack Details, Crack Measurement, Object Detection, Alignment Analysis, Vibration Analysis, GPS, Inspection Summary, Save Inspection.

### Defects

Defects, Defect List, Defect Details, Crack History, Image Comparison, Growth Analysis.

### Evidence

Evidence, Comments, Engineer Verification.

### Map

Map, Railway Map, Defect Map, Track Map, Location Details, Risk Heatmap.

### Maintenance

Maintenance, Maintenance Dashboard, Maintenance Task, Task Details, Before/After, Maintenance Verification.

### Reports

Reports, Report Details, Evidence Package, PDF Preview, Share Report.

### Analytics

Analytics, Track Health, Health History, Crack Analytics, Risk Analytics, Maintenance Analytics.

### Scheduling

Scheduling, Inspection Schedule, Create Inspection, Assigned Inspections, Inspection Calendar.

### Settings

Settings, Account, App Settings, Notification Settings, Security, Language, Help Center, About.

## Required working flows

Make the important actions work end to end with local demo persistence when a backend is not available:

1. Authentication: splash → onboarding → login/registration → OTP/email verification → home.
2. Inspection: inspection setup → live inspection → camera/GPS/sensor modules → detection result → crack details → measurement → engineer verification → inspection summary → save inspection.
3. Defect review: defect list → defect details → image comparison → crack history → growth analysis → maintenance task.
4. Maintenance: maintenance dashboard → task details → before/after evidence → maintenance verification → updated task state.
5. Reports: reports → report details → evidence package → PDF preview → share report.
6. Analytics: analytics → track health → health history → crack/risk/maintenance analytics.
7. Scheduling: inspection schedule → create inspection → assigned inspections → inspection calendar.
8. Settings: account, app settings, notification settings, security, language, help center, and about must all open and save any editable state.

Every prominent button must either navigate to a real next screen, update local state, open a real sheet/modal, or show a useful confirmation/toast. No dead buttons, fake loading loops, or “coming soon” placeholders.

## Product behavior

- Use realistic railway data: corridor names, chainage positions, defect IDs, confidence scores, crack length, risk score, track side, timestamps, assigned engineers, due windows, sensor readings, and report metadata.
- Include loading, empty, error, success, verification, and offline states where relevant.
- Make every route discoverable from navigation, contextual cards, or a screen directory.
- Keep the UI responsive for small mobile screens and Expo web preview.
- Use native-safe areas and keyboard-safe forms.
- Use AsyncStorage for local settings, saved inspections, task status, and onboarding state when no server is connected.
- Use camera and location capabilities when the platform supports them, with clear permission-denied states and a safe demo fallback for web preview.
- Review all routes for consistent spacing, typography, iconography, button behavior, status semantics, and back navigation before finishing.