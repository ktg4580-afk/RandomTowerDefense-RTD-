$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
New-Item -ItemType Directory -Force -Path out | Out-Null
$sourceFiles = @(Get-ChildItem -Path 'src/main/java', 'tests/server' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 -d out @sourceFiles
if ($LASTEXITCODE -ne 0) { throw 'Java compile failed' }
& java -cp out server.DiceGameTest
if ($LASTEXITCODE -ne 0) { throw 'DiceGame unit test failed' }
& python tests/integration/test_room.py
if ($LASTEXITCODE -ne 0) { throw 'Room integration test failed' }
& python tests/integration/test_dice.py
if ($LASTEXITCODE -ne 0) { throw 'Dice integration test failed' }
Write-Host 'PASS: all server tests'
