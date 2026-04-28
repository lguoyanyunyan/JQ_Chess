param(
    [switch]$Installer,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$targetDir = Join-Path $projectRoot "target"
$inputDir = Join-Path $targetDir "jpackage-input"
$distDir = Join-Path $targetDir "dist\windows"
$appName = "ZangJiuQi-Java"
$mainJar = "jqjava-0.1.0-SNAPSHOT.jar"
$mainClass = "com.zangjiuqi.app.JqJavaLauncher"

function Resolve-JPackage {
    $command = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    $defaultCandidate = "C:\Program Files\Java\jdk-17\bin\jpackage.exe"
    if (Test-Path -LiteralPath $defaultCandidate) {
        return $defaultCandidate
    }

    throw "jpackage was not found. Install JDK 17 and make jpackage available on PATH or JAVA_HOME."
}

function Invoke-CommandLine($command, [string[]]$arguments) {
    & $command @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $command $($arguments -join ' ')"
    }
}

function Assert-MavenUsesJava17 {
    $versionText = (& mvn -version) -join "`n"
    if ($versionText -notmatch "Java version:\s*17\.") {
        throw "Maven must run on Java 17. Current mvn -version output:`n$versionText"
    }
}

function Reset-Directory($path) {
    $resolvedTarget = [System.IO.Path]::GetFullPath($path)
    $resolvedProject = [System.IO.Path]::GetFullPath($projectRoot)
    if (-not $resolvedTarget.StartsWith($resolvedProject, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to delete outside project root: $resolvedTarget"
    }
    if (Test-Path -LiteralPath $resolvedTarget) {
        Remove-Item -LiteralPath $resolvedTarget -Recurse -Force
    }
    New-Item -ItemType Directory -Path $resolvedTarget | Out-Null
}

Push-Location $projectRoot
try {
    $jpackage = Resolve-JPackage
    Assert-MavenUsesJava17

    if (-not $SkipTests) {
        Invoke-CommandLine "mvn" @("-q", "test")
    }
    Invoke-CommandLine "mvn" @("-q", "-DskipTests", "package")

    Reset-Directory $inputDir
    New-Item -ItemType Directory -Path $distDir -Force | Out-Null

    Copy-Item -LiteralPath (Join-Path $targetDir $mainJar) -Destination (Join-Path $inputDir $mainJar)
    Get-ChildItem -LiteralPath (Join-Path $targetDir "runtime-libs") -Filter "*.jar" |
            ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $inputDir }

    $existingImage = Join-Path $distDir $appName
    if (Test-Path -LiteralPath $existingImage) {
        Remove-Item -LiteralPath $existingImage -Recurse -Force
    }
    Get-ChildItem -LiteralPath $distDir -Filter "$appName-0.1.0.exe*" -File -ErrorAction SilentlyContinue |
            ForEach-Object {
                $oldInstaller = $_.FullName
                try {
                    Remove-Item -LiteralPath $oldInstaller -Force
                } catch {
                    Write-Warning "Could not remove old installer '$oldInstaller'. Close any running installer/app process if you need a fresh installer file."
                }
            }

    $type = if ($Installer) { "exe" } else { "app-image" }
    $arguments = @(
        "--type", $type,
        "--name", $appName,
        "--app-version", "0.1.0",
        "--vendor", "JQJava",
        "--dest", $distDir,
        "--input", $inputDir,
        "--main-jar", $mainJar,
        "--main-class", $mainClass,
        "--java-options", "-Dfile.encoding=UTF-8"
    )
    if ($Installer) {
        $arguments += @("--win-menu", "--win-shortcut")
    }

    try {
        Invoke-CommandLine $jpackage $arguments
    } catch {
        if (-not $Installer) {
            throw
        }
        Write-Warning "Installer packaging failed. Falling back to app-image. Details: $($_.Exception.Message)"
        if (Test-Path -LiteralPath $existingImage) {
            Remove-Item -LiteralPath $existingImage -Recurse -Force
        }
        $fallbackArguments = @(
            "--type", "app-image",
            "--name", $appName,
            "--app-version", "0.1.0",
            "--vendor", "JQJava",
            "--dest", $distDir,
            "--input", $inputDir,
            "--main-jar", $mainJar,
            "--main-class", $mainClass,
            "--java-options", "-Dfile.encoding=UTF-8"
        )
        Invoke-CommandLine $jpackage $fallbackArguments
    }

    $readme = Get-ChildItem -LiteralPath $projectRoot -Filter "README-*.md" | Select-Object -First 1
    if ($readme) {
        Copy-Item -LiteralPath $readme.FullName -Destination (Join-Path $distDir $readme.Name) -Force
    }
    Write-Host "Windows package output: $distDir"
} finally {
    Pop-Location
}
