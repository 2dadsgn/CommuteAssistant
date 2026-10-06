# Commute Alert (Chrome + Firefox) – no API key

Watches your regular routes (e.g. work to home) using the travel time Google Maps shows,
and tells you the latest time you can leave to arrive by a set time.

## How it works
Around each trip, the extension opens your route on google.com/maps in a **hidden tab (Firefox) or minimized window (Chrome)**
for a few seconds, reads the travel time and traffic colour, then closes it.
No API key, no account, no cost.

Leave-by time = arrival time − safety margin − current travel time.
It re-checks every 10 minutes (every 5 in the last half hour), so the time follows traffic as it changes.

## Two kinds of routes
**Tell me when to leave**: set an arrival time and get leave-by alerts (below).

**Just send traffic updates**: no arrival time. Every X minutes (5 or more) you get the current
travel time, traffic level and when you'd arrive if you left now. You can limit it to a time
range (e.g. 17:00–19:00) and choose to be notified only when the travel time changes.
Use **Pause / Resume** in the popup to turn a route's alerts on and off quickly.

## Address suggestions
From and To suggest places as you type, using Photon (free, OpenStreetMap-based, no key). In Firefox, click **Allow** in the suggestion box the first time. If nothing matches, press Enter to search OpenStreetMap more widely.
Picking a suggestion saves its exact coordinates (a green tick appears), and those are sent to
Google Maps, so the right place is always measured. If you type an address without picking
a suggestion, Google Maps interprets the text itself.

## Leave-by alerts
- **Leave by** heads-up before you need to go (default 15 min before).
- **Traffic got worse / eased** when the leave time moves by 5+ min.
- **Leave now**, including how late you'll be if you're already behind.
- **Toolbar badge** counting down minutes, coloured by traffic.
- Click a notification to open the route in Google Maps.

"+X min" is compared with the fastest time the extension has seen for that route,
so it gets more accurate after a few days of use.

## Install
**Chrome / Edge / Brave**: `chrome://extensions` → Developer mode on → **Load unpacked** → pick this folder.

**Firefox (121+)**: `about:debugging#/runtime/this-firefox` → **Load Temporary Add-on** → pick `manifest.json`.
Temporary add-ons are removed when Firefox restarts; to keep it, sign it for free (unlisted)
at addons.mozilla.org/developers and set your own `gecko.id` in manifest.json.
When you first save, Firefox asks to allow access to google.com — accept it.

## Set up
1. Open google.com/maps once in this browser and answer the cookie prompt if one appears
   (otherwise the hidden check gets stuck on that page).
2. Settings opens on install (or toolbar icon → Settings). Enter From, To, arrival time and days.
3. Click **Save and test**. Use **Add return trip** for the morning route.

## Limits to know
- **Current traffic only.** It can't predict traffic for a future departure, so the leave-by
  time is most accurate within the last hour before you go (that's when it checks most often).
  Raise the safety margin if your route gets suddenly worse at rush hour.
- **Driving only.**
- **It can break when Google changes its page.** The reading code is the
  `readTripFromPage` function in `background.js`, which is the only part you'd need to update.
- **Google's terms don't allow automated reading of Maps.** It's light use (a page load
  every 5–10 minutes around your trip), but it's not an officially supported method.
- Your browser must be running. A minimized window may briefly appear in your taskbar during a check.
