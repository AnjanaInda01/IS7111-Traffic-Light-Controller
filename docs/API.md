# REST API

Base URL: `http://localhost:8080/api/traffic`

All successful operations return HTTP 200 with a JSON controller snapshot:

```json
{
  "currentLight": "GREEN",
  "mode": "AUTOMATIC",
  "remainingSeconds": 7,
  "running": true,
  "emergency": false,
  "message": "Normal cycle"
}
```

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/status` | Current controller snapshot. |
| POST | `/start` | New automatic cycle at RED/10; blocked by emergency. |
| POST | `/pause` | AUTOMATIC -> PAUSED; keep phase and remaining duration. |
| POST | `/resume` | PAUSED -> AUTOMATIC; blocked otherwise. |
| POST | `/stop` | RED/STOPPED/0; preserve an existing emergency latch. |
| POST | `/reset` | RED/STOPPED/0; clear emergency/manual state. |
| POST | `/emergency` | RED/EMERGENCY/0; lock until Reset. |
| POST | `/manual/{light}` | RED, YELLOW or GREEN, from STOPPED/MANUAL without emergency. |

These commands take no request body. There is no database endpoint or persistence.

Modes: `STOPPED`, `AUTOMATIC`, `PAUSED`, `MANUAL`, `EMERGENCY`.

`remainingSeconds` is zero in STOPPED/MANUAL/EMERGENCY. PAUSED retains its countdown. `running` is true only in AUTOMATIC. The emergency boolean is authoritative for the Reset-required lock, including after Stop.

## Errors

An invalid light returns HTTP 400. A valid command that cannot run in the current mode returns HTTP 409. The traffic command error body is:

```json
{
  "message": "Resume requires a paused automatic cycle.",
  "state": {
    "currentLight": "RED",
    "mode": "STOPPED",
    "remainingSeconds": 0,
    "running": false,
    "emergency": false,
    "message": "Ready to start"
  }
}
```

Unknown routes and unsupported HTTP methods receive Spring's normal 404/405 error responses. The client also handles non-JSON errors from an intermediary by displaying the HTTP status.

## PowerShell examples

```powershell
Invoke-RestMethod http://localhost:8080/api/traffic/status
Invoke-RestMethod -Method Post http://localhost:8080/api/traffic/start
Invoke-RestMethod -Method Post http://localhost:8080/api/traffic/pause
Invoke-RestMethod -Method Post http://localhost:8080/api/traffic/resume
Invoke-RestMethod -Method Post http://localhost:8080/api/traffic/stop
Invoke-RestMethod -Method Post http://localhost:8080/api/traffic/manual/GREEN
Invoke-RestMethod -Method Post http://localhost:8080/api/traffic/emergency
Invoke-RestMethod -Method Post http://localhost:8080/api/traffic/reset
```
