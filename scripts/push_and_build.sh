#!/bin/bash
# Push workflow to GitHub and trigger build
# Usage: bash scripts/push_and_build.sh [PAT_TOKEN]

cd "$(dirname "$0")/.."

echo "=== Mayara JNI Build: GitHub Actions Setup ==="
echo ""

# Check if PAT is provided
PAT_TOKEN="${1-}"

if [ -z "$PAT_TOKEN" ]; then
    echo "GitHub Actions workflow created: .github/workflows/build-jni.yml"
    echo ""
    echo "To trigger the build, provide a GitHub Personal Access Token:"
    echo ""
    echo "  bash scripts/push_and_build.sh <YOUR_GITHUB_PAT>"
    echo ""
    echo "OR manually:"
    echo "  1. Create PAT at: https://github.com/settings/tokens"
    echo "  2. Grant: repo scope"
    echo "  3. Run: bash scripts/push_and_build.sh <token>"
    echo ""
    echo "Current status:"
    git status --short .github/workflows/build-jni.yml
    exit 1
fi

# Configure git credential helper
echo "Setting up GitHub authentication..."
git config --local credential.helper store
echo "https://${PAT_TOKEN}@github.com" | git credential-store approve
echo "https://github.com/marcelrv/mayara-app" >> ~/.git-credentials

# Set remote to HTTPS (for token auth)
git remote set-url origin "https://github.com/marcelrv/mayara-app.git"

echo "Pushing to GitHub..."
git add .github/workflows/build-jni.yml
git commit -m "ci: add GitHub Actions workflow for JNI library compilation" || echo "Already committed"

# Push with auth
GIT_ASKPASS_OVERRIDE=true git push -u origin main 2>&1 | grep -E 'Everything|remote:|error|success'

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Workflow pushed successfully!"
    echo ""
    echo "Build will start automatically. Check progress at:"
    echo "  https://github.com/marcelrv/mayara-app/actions"
    echo ""
    echo "Expected file location after build:"
    echo "  app/src/main/jniLibs/arm64-v8a/libradar.so"
else
    echo "❌ Push failed. Check your PAT token scope and validity."
    exit 1
fi
