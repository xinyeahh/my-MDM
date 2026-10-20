$fingerprint = "sim-concurrent-" + [guid]::NewGuid().ToString()

$body = @{
    deviceName = "concurrent-simulator"
    model = "simulator-v1"
    os = "Android"
    osVersion = "15"
    fingerprint = $fingerprint
} | ConvertTo-Json -Compress

$jobs = 1..10 | ForEach-Object {
    Start-Job -ArgumentList $body -ScriptBlock {
        param($requestBody)

        try {
            $response = Invoke-WebRequest `
                -Uri "http://localhost:8080/device/register" `
                -Method Post `
                -ContentType "application/json" `
                -Body $requestBody `
                -UseBasicParsing

            [pscustomobject]@{
                Status = [int]$response.StatusCode
                DeviceId = ($response.Content | ConvertFrom-Json).deviceId
                Error = ""
            }
        } catch {
            [pscustomobject]@{
                Status = "ERROR"
                DeviceId = ""
                Error = $_.Exception.Message
            }
        }
    }
}

$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

$results | Format-Table -AutoSize
"本次 fingerprint：$fingerprint"