# Push workflow to GitHub and trigger build (PowerShell version)
# Usage: .\scripts\push_and_build.ps1 -Token <YOUR_GITHUB_PAT>

param(
    [string]$Token = ""
)

$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not $Token) {
    Write-Host "GitHub Actions workflow created: .github/workflows/build-jni.yml" -ForegroundColor Green
    Write-Host ""
    Write-Host "To trigger the build, provide a GitHub Personal Access Token:"
    Write-Host ""
    Write-Host "  .\scripts\push_and_build.ps1 -Token '<YOUR_GITHUB_PAT>'" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "OR manually:"
    Write-Host "  1. Create PAT at: https://github.com/settings/tokens"
    Write-Host "     - Grant 'repo' and 'workflow' scopes"
    Write-Host "  2. Run: .\scripts\push_and_build.ps1 -Token <token>"
    Write-Host ""
    Write-Host "Current status:"
    git status --short .github/workflows/build-jni.yml
    exit 1
}

Write-Host "Setting up GitHub authentication..." -ForegroundColor Yellow
# Use token for HTTPS authentication
$remoteUrl = "https://${Token}@github.com/marcelrv/mayara-app.git"
git remote set-url origin $remoteUrl

Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
Push-Location $repoRoot
git add .github/workflows/build-jni.yml
git commit -m "ci: add GitHub Actions workflow for JNI library compilation" -EA SilentlyContinue
$result = git push -u origin main 2>&1

Pop-Location

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Workflow pushed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Build will start automatically. Check progress at:" -ForegroundColor Cyan
    Write-Host "  https://github.com/marcelrv/mayara-app/actions"
    Write-Host ""
    Write-Host "Expected file location after build:" -ForegroundColor Cyan
    Write-Host "  app/src/main/jniLibs/arm64-v8a/libradar.so"
    Write-Host ""
    Write-Host "Estimated build time: 5-10 minutes"
} else {
    Write-Host "❌ Push failed. Check your PAT token scope and validity." -ForegroundColor Red
    Write-Host $result
    exit 1
}
