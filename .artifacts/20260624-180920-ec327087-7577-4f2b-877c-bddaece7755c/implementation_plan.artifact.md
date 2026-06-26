# Implementation Plan - Entry Button Redesign

This plan outlines the redesign of the "Add Income" and "Add Spending" entry buttons to a slick, edgy, and minimalistic style that fits the app's professional architecture.

## User Review Required

- **Placement**: I will add these redesigned buttons as floating elements on the `DashboardScreen` for quick access, and replace the standard FABs in `EarningsScreen` and `NotebookScreen`.
- **Style**: Pill-shaped with sharp borders, high-contrast colors (Black/Primary), and minimal iconography.

## Proposed Changes

### UI Components

#### [NEW] [ActionPill.kt](file:///C:/Users/dylan/AndroidStudioProjects/BudgetHero/app/src/main/java/com/example/budgethero/ui/components/ActionPill.kt)

- Create a reusable `ActionPill` composable.
- Properties:
    - Rounded corners (fully pill-shaped).
    - Border stroke (1-2dp) for "edgy" look.
    - Minimalist text + icon.
    - Subtle shadow or flat design depending on the vibe.

### Screens

#### [DashboardScreen.kt](file:///C:/Users/dylan/AndroidStudioProjects/BudgetHero/app/src/main/java/com/example/budgethero/ui/dashboard/DashboardScreen.kt)

- Add two `ActionPills` at the bottom right/center:
    - **+ Income** (Green-themed/Black)
    - **+ Expense** (Red-themed/Black)
- These will trigger the `BudgetEntryModal`.

#### [EarningsScreen.kt](file:///C:/Users/dylan/AndroidStudioProjects/BudgetHero/app/src/main/java/com/example/budgethero/ui/earnings/EarningsScreen.kt)

- Replace `LargeFloatingActionButton` with the new `ActionPill`.

#### [NotebookScreen.kt](file:///C:/Users/dylan/AndroidStudioProjects/BudgetHero/app/src/main/java/com/example/budgethero/ui/notebook/NotebookScreen.kt)

- Replace `LargeFloatingActionButton` with the new `ActionPill`.

---

## Verification Plan

### Automated Tests
- No automated tests required for this UI refactor.

### Manual Verification
1. **Visual Check**: Open the app and verify the new buttons look "slick, edgy, sharp, and clean".
2. **Dashboard**: Ensure both Income and Spending buttons are accessible and trigger the correct modal.
3. **Consistency**: Verify the same style is used across Earnings and Notebook screens.
4. **Interaction**: Confirm the buttons have clear click feedback (ripples/scaling).
