param(
    [Parameter(Mandatory=$true)][string]$Adb,
    [Parameter(Mandatory=$true)][string]$Serial,
    [int]$Port = 5037,
    [int]$Repeats = 3
)
$ErrorActionPreference = 'Stop'
function Send-Adb {
    & $Adb -P $Port -s $Serial @args | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'ADB measurement command failed.' }
}
# Start from the normal home screen; this script never switches an HDMI input.
Send-Adb shell am force-stop io.github.domlin92.linartv.open
Send-Adb shell am start -n io.github.domlin92.linartv.open/com.linart.tvpreview.DashboardActivity
Start-Sleep -Seconds 3
for ($iteration = 0; $iteration -lt $Repeats; $iteration++) {
    Send-Adb shell input tap 1808 96
    Start-Sleep -Milliseconds 500
    Send-Adb shell input keyevent 22
    Send-Adb shell input keyevent 20
    Send-Adb shell input keyevent 23
    Start-Sleep -Milliseconds 500
    Send-Adb shell input keyevent 4
    Send-Adb shell input keyevent 4
    Start-Sleep -Milliseconds 500
    Send-Adb shell input tap 350 520
    Start-Sleep -Milliseconds 500
    Send-Adb shell input keyevent 4
}
$appProcessId = (& $Adb -P $Port -s $Serial shell pidof io.github.domlin92.linartv.open).Trim()
& $Adb -P $Port -s $Serial logcat -d -v brief --pid=$appProcessId -s LinarTVPerf:D '*:S'
