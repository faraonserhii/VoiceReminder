param(
    [switch]$SkipAdb
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Host "[1/4] Build debug APK" -ForegroundColor Cyan
& .\gradlew.bat :app:assembleDebug

if (-not $SkipAdb) {
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $adb) {
        throw "adb not found. Run with -SkipAdb for build-only mode."
    }

    Write-Host "[2/4] Check connected devices" -ForegroundColor Cyan
    & adb devices

    Write-Host "[3/4] Install debug app" -ForegroundColor Cyan
    & .\gradlew.bat :app:installDebug

    Write-Host "[4/4] Launch app" -ForegroundColor Cyan
    & adb shell am start -n com.proapps.voiceremind/.MainActivity
}
else {
    Write-Host "[2/4] Skip adb checks" -ForegroundColor Yellow
    Write-Host "[3/4] Skip install" -ForegroundColor Yellow
    Write-Host "[4/4] Skip launch" -ForegroundColor Yellow
}

Write-Host "Done. Continue with manual checklist:" -ForegroundColor Green
Write-Host "docs/sauna_widget_smoke_checklist.md"

