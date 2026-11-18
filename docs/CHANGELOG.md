# Changelog

What changed for people using Glassfolio. Developer details are in the commit history.

## 0.3.0 – 2026-11-17

### Added
- A desktop window replaces the text prompts. Fill in one form and click Get suggestions.
- The start screen shows a 3D bar chart built from shards that fly into place, turn once, then follow your mouse.
- Light and dark themes. Click Switch theme on either screen; Glassfolio remembers your choice.
- Works at any window size. Press F11 for full screen; both screens fill it.
- The start screen shows the S&P 500, NASDAQ and Dow (latest value, 1-year change, trend) plus the Fed rate and inflation.
- A short guide under the form explains which risk profile fits you.
- Each suggestion is a card with its full reason, price, 1-year change, volatility, and what your budget would be worth if invested a year ago.
- The best pick is highlighted at the top of the list.
- A small trend line on each card, green if the index rose over the year and red if it fell.
- A chart comparing all three indexes over the past year.
- Current economy numbers at the top: the Fed interest rate, inflation over the last 12 months, and unemployment.
- A Back button to change your answers and try again.

### Changed
- New name: Glassfolio.
- New typeface, IBM Plex Sans, chosen for clear numbers and a trustworthy look.
- The form no longer asks for a password. There is no sign-in yet, so it never used one.
- Data loads in the background, so the window never freezes.

### Fixed
- The full reason for each suggestion is always visible instead of being cut off.
- Inflation is measured against the same month a year earlier, even when a month of data is missing.
- When you're offline, the error message now says to check your internet connection.

## 0.2.0 – 2026-11-17

### Added
- Suggestions now use real market data from FRED (Federal Reserve Economic Data).
- The app suggests one of three market indexes: the S&P 500, NASDAQ Composite, or Dow Jones Industrial Average.
- Stability picks the index with the steadiest prices. Growth picks the one that rose the most over the last year.

### Fixed
- Typing letters or a negative number for the budget now asks again instead of crashing.

## 0.1.0 – 2026-11-03

### Added
- First version: enter a username, budget, and risk profile (Stability or Growth) in the console.
