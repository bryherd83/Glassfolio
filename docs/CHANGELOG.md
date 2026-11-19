# Changelog

What changed for people using Glassfolio. Developer details are in the commit history.

## Unreleased

### Added
- Each suggestion and each left-out index has a "Why this pick?" link that opens a panel with the rule and how its score was worked out.
- The quiz ends with a result screen: which profile fits you and why, with "Use" and "Retake the quiz".
- Each suggestion says in plain words how far your budget fell in the year's worst slide, and what the year was worth after inflation.
- Not sure which profile fits? Answer 3 quick questions and Glassfolio picks one for you.
- Welcome back: tick "Remember me" and Glassfolio remembers your name and last top pick on this computer and shows how it moved since. "Not you?" forgets it.
- Glassfolio asks for your FRED API key on first launch, checks it, and can remember it. No environment variable needed. Change it any time with "Change API key" under the form.
- Each suggestion shows how it did against keeping the same money in safe savings (3-month Treasury bills).
- The results chart can show the past week, month, 3 months or year; shorter periods show the change since that period began.
- Point at the results chart to see the date and each index's change on that day.
- Glassfolio opens full screen (F11 or Esc for a window) and always in the light theme. Switch to dark any time with Switch theme.
- More motion: numbers count up, cards slide in and lift when you point at them, and buttons press in when clicked.

### Changed
- A pink-violet glass redesign: tinted callout boxes for the start blurb, form, quiz, disclaimer and API key screen; a violet-pink header band and tinted cards on the results page; gradient headlines; key words in the start blurb in bold violet. The dark theme is now teal and navy with a lime accent.
- The 3D chart can be split with the mouse from the moment it appears; the split only pauses while the chart is turned away.
- `User.setPassword` rejects null; removed the unused `SUBTITLE` style and `-sweep` color.
- `Growth` validates its goal and `Stock.fromPrices` rejects zero or negative prices; quiz result style no longer sticks; plainer key-screen and quiz-result copy.
- `Suggestion`, `Stock` and the chart math reject bad numbers instead of passing NaN or dividing by zero; docs say where the saved key is protected.
- Calmer, clearer results: the score says what it is ("Fit for your profile"), tiles name your own budget and show the worst drop, trend lines are neutral, and errors are in plain words.
- The API key screen sits in the same frame as the rest of the app.
- The background dots part around the mouse and drift back, and the 3D chart breaks apart like a hologram where the mouse passes through it, then reassembles.
- A slim modern scrollbar, and a clearer results header (a divider after the name, the profile as a small tag).
- Clicking no longer nudges the layout: buttons keep the same size when they get focus.
- Each suggestion card is a head row and up to four clear boxes: the last 12 months, after prices rose, your budget at its low point, and how it compares with safe savings.
- Glassfolio has its own icon: three rising bars, in the title bar, the taskbar and next to the name.
- Both screens sit in one rounded frame. The background moves: drifting color and a dot grid that parts around the mouse.
- Clearer text: one type scale for the whole app, bigger numbers and headlines, softer small print.
- Buttons change color under the mouse, and links look like links (accent color, underline on hover, arrows).
- A new start screen: one rounded frame with the form on the left and the live market picture on the right. Picking a profile explains it right under the choice.
- The background drifts slowly the whole time; the API key link moved to the top bar; clearer wording on the form.
- A calmer look: pill-shaped buttons and a new typeface, Poppins, and a pink-violet glass look.
- One typeface, Poppins, for all text; see-through cards over brighter background blobs; the top pick card is lifted and tinted, with bigger scores.

## 0.3.1 – 2026-11-18

### Added
- Glassfolio can be run from a single JAR file. See "Runnable JAR" in the README.
- The start screen shows unemployment next to the Fed rate and inflation.

### Changed
- The results chart labels the past year by month and draws the top pick's line thicker.
- Numbers use real minus signs, and an index that never fell shows a biggest drop of 0.0%.
- Small captions are easier to read.

## 0.3.0 – 2026-11-17

### Added
- A short startup animation with an original chime. Click or press any key to skip it.
- The start screen's 3D chart takes the shape of the S&P 500's last 12 months once data loads.
- The results show the other profile's picks with one click, no need to go back.
- Indexes that weren't suggested are listed with the reason why.
- Each suggestion now shows its biggest drop over the year and its year range.
- A desktop window replaces the text prompts. Fill in one form and click Get suggestions.
- The start screen shows a 3D bar chart built from shards that fly into place, turn once, then follow your mouse.
- Light and dark themes. Click Switch theme on either screen.
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
- Full screen now stays on when moving between screens.
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
