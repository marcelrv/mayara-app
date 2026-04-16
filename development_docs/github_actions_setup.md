# GitHub Actions: Automated JNI Library Build

## Setup Complete ✅

The GitHub Actions workflow is ready to build `libradar.so` automatically. Here's how to use it:

### Option 1: Push Changes to Trigger Build

The workflow file has been created at `.github/workflows/build-jni.yml`. When you push changes to GitHub, it will automatically:

1. ✅ Check out the code (including mayara-server submodule)
2. ✅ Install Rust + Android target (aarch64-linux-android)
3. ✅ Install cargo-ndk
4. ✅ Download Android NDK r30 automatically
5. ✅ Compile libradar.so for ARM64
6. ✅ Verify the artifact
7. ✅ Upload as GitHub Actions artifact
8. ✅ Auto-commit the .so file back to main branch

```bash
# Commit the workflow file:
git add .github/workflows/build-jni.yml
git commit -m "ci: add GitHub Actions workflow for JNI library compilation"
git push origin main
```

This triggers **`build-jni.yml`** automatically on every push to `main` or `develop` branches.

### Option 2: Manual Trigger

Go to your GitHub repository:
1. Click **Actions** tab
2. Select **Build JNI Library (libradar.so)** workflow
3. Click **Run workflow** button
4. Choose branch (main/develop)
5. Click green **Run workflow** button

The build will start immediately and complete in ~5-10 minutes.

### What the Workflow Does

**Triggers**:
- Push to `main` or `develop`
- Changes to `mayara-jni/`, `mayara-server/`, `scripts/build_jni.sh`
- Manual dispatch (click "Run workflow")

**Build Steps**:
```yaml
1. Checkout with submodules
2. Install Rust + aarch64-linux-android target
3. Install cargo-ndk (cross-compilation tool)
4. Setup Android NDK r30 via nttld/setup-ndk@v1
5. Run: bash scripts/build_jni.sh
6. Verify: app/src/main/jniLibs/arm64-v8a/libradar.so
7. Upload artifact (90-day retention)
8. Auto-commit .so to main branch
```

### Artifacts

After the workflow completes:
- ✅ **Artifact Upload**: downloadable from GitHub Actions UI
- ✅ **Auto-Commit**: `.so` file committed to `main` branch automatically
- ✅ **Ready for APK**: `app/src/main/jniLibs/arm64-v8a/libradar.so` in repo

### Event Log

Check build status at: `github.com/marcelrv/mayara-app/actions`

Each workflow run shows:
- Build logs (compile output)
- Artifact downloads
- Commit history

---

## Current Status

**All Phase 1 Code**: ✅ COMPLETE  
**JNI Bridge**: ✅ 3 functions + 4 tests  
**Kotlin Wrapper**: ✅ RadarJni.kt with suspend functions  
**Build System**: ✅ GitHub Actions ready  

**Next**: Push to GitHub to trigger automatic compilation!
