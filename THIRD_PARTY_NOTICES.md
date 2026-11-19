# Third-party notices

## marked

`app/assets/ai.html` embeds **marked v4.3.0** inline.

- Project: https://github.com/markedjs/marked
- License: MIT
- Copyright: 2011-2023, Christopher Jeffrey

The inlined script retains its upstream license header.

## wttr.in

The optional weather home widget reads the keyless JSON endpoint at https://wttr.in/ .

- Service: wttr.in (weather data provided through WorldWeatherOnline)
- No API key or account is required; the request contains only the city name.
- Results are cached locally for 30 minutes to stay well inside the service's rate
  limits.

## Hitokoto（一言）

The optional quote home widget reads https://v1.hitokoto.cn/ (literature, poetry and
philosophy categories).

- Service: Hitokoto (https://hitokoto.cn/)
- No API key is required. Requests only ask for one random sentence; there is no
  account, location or device data in the request.
- The most recent 24 quotes are cached locally, and the app keeps a small built-in
  fallback list for offline use.
