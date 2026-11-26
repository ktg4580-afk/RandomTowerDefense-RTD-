$ErrorActionPreference = "Stop"
Set-Location (Resolve-Path (Join-Path $PSScriptRoot ".."))
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) { throw "JDK javac 명령을 찾을 수 없습니다. JAVA_HOME/PATH를 확인하세요." }
New-Item -ItemType Directory -Force out | Out-Null
$files = Get-ChildItem -Path src/main/java -Filter *.java -Recurse | ForEach-Object FullName
& javac -encoding UTF-8 -d out @files
if ($LASTEXITCODE -ne 0) { throw "Java compile failed" }
& java -cp out client.StartScreen
