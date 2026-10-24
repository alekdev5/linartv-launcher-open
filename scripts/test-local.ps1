$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$javaBin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { '' }
$compiler = if ($javaBin) { Join-Path $javaBin 'javac.exe' } else { 'javac' }
$runtime = if ($javaBin) { Join-Path $javaBin 'java.exe' } else { 'java' }
$outputPath = Join-Path $projectRoot 'tmp\local-tests'
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
& $compiler -encoding UTF-8 -d $outputPath (Join-Path $projectRoot 'app\src\main\java\com\linart\tvpreview\LauncherTiming.java') (Join-Path $projectRoot 'tests\LauncherTimingCheck.java')
if ($LASTEXITCODE -ne 0) { throw 'Compilation of local checks failed.' }
& $runtime -cp $outputPath com.linart.tvpreview.LauncherTimingCheck
if ($LASTEXITCODE -ne 0) { throw 'Local checks failed.' }
& $compiler -encoding UTF-8 -d $outputPath (Join-Path $projectRoot 'app\src\main\java\com\linart\tvpreview\BlurPixels.java') (Join-Path $projectRoot 'tests\BlurPixelsCheck.java')
if ($LASTEXITCODE -ne 0) { throw 'Compilation of blur checks failed.' }
& $runtime -cp $outputPath com.linart.tvpreview.BlurPixelsCheck
if ($LASTEXITCODE -ne 0) { throw 'Blur checks failed.' }
