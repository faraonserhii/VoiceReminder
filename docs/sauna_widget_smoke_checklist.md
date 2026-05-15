# Sauna + Voice Widget Smoke Checklist

Quick manual validation for:
- `Sauna-Timer` phrases
- 1-tap voice home-screen widget (compact and expanded)

## Preconditions

- Debug app installed
- Microphone permission granted
- Notifications allowed
- At least one home screen with free space for widget

## 1) Add widget (compact)

1. Long press home screen -> Widgets.
2. Add `VoiceRemind` voice widget.
3. Keep default small size (1x1).
4. Tap widget.

Expected:
- App opens to `MainActivity`.
- Voice recognition prompt starts automatically.

## 2) Resize widget (expanded)

1. Long press the added widget.
2. Resize to larger size (about 2x2).
3. Tap widget again.

Expected:
- Widget still opens app.
- Voice recognition starts.
- Expanded layout shows mic + text label.

## 3) Sauna voice commands

Speak each command through the widget-triggered voice input:

- `Сауна через 30 минут`
- `Сауна через полчаса`
- `Сауна через 1 час`
- `Sauna in 45 minutes`
- `Sauna after 30 min`
- `Sauna in 2 hours`

Expected:
- Preview text reflects parsed delay.
- Toast confirms timer is scheduled.
- Input field is cleared after successful schedule.

## 4) Negative checks

Try:

- `Через 30 минут напомни`
- `Сауна через 721 минуту`

Expected:
- Sauna flow is NOT scheduled.
- App falls back to regular reminder parsing behavior.

## 5) Notification check

Wait for a short timer command (`через 1 минуту`, if supported in your recognizer output) or test with any small minute value that parser accepts.

Expected:
- `Sauna-Timer` notification appears.
- Notification text includes chosen minutes.

## Optional capture for PR

Take 3 screenshots:
1. Compact widget (1x1).
2. Expanded widget (2x2).
3. Sauna notification.

