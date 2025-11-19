# PortraitCamera_CI_Scaffold
Minimal Android project scaffold with a GitHub Actions workflow that installs Android command-line tools,
installs Gradle (via apt), and builds a debug APK. This is intended as a starting point — if your real
project has native code, custom modules, or different SDK versions you'll need to adapt the workflow.

How to use:
1. Unzip into a new GitHub repository, commit & push to GitHub.
2. On GitHub go to Actions → run the `Android CI - Build Debug APK` workflow (it triggers on push to main).
3. After the workflow succeeds, download the artifact `portraitcamera-apk` from the Actions run.
4. Install the APK with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Notes & troubleshooting:
- The workflow installs Android command-line tools and requested platform/build-tools (android-33 / 33.0.2).
  If your project uses a different compileSdkVersion change `.github/workflows/android-build.yml` accordingly.
- The workflow uses `gradle` installed with apt. If you prefer Gradle wrapper, add the wrapper and adjust the workflow.
- If the job fails with SDK or Gradle plugin errors, paste the Actions log here and I'll provide fixes.
