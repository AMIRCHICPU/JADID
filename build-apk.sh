#!/bin/bash
# ==============================================================================
# اسکریپت خودکار ساخت APK - برای مک و لینوکس
# این اسکریپت را داخل پوشه اصلی پروژه (کنار فایل gradlew) اجرا کن:
#   chmod +x build-apk.sh
#   ./build-apk.sh
# ==============================================================================

set -e  # اگر هر دستوری خطا داد، اسکریپت متوقف شود

echo "=========================================="
echo "  ساخت خودکار APK - SMS Location Finder"
echo "=========================================="
echo ""

# --- مرحله ۱: چک کردن Java ---
if ! command -v java &> /dev/null; then
    echo "❌ Java پیدا نشد."
    echo ""
    echo "لطفا اول Java (JDK 17) را نصب کن:"
    echo "  - macOS:  brew install openjdk@17"
    echo "  - Ubuntu/Debian: sudo apt install openjdk-17-jdk"
    echo "  - یا از https://adoptium.net دانلود کن"
    echo ""
    exit 1
fi
echo "✅ Java پیدا شد: $(java -version 2>&1 | head -1)"
echo ""

# --- مرحله ۲: تعیین مسیر نصب Android SDK ---
SDK_DIR="$HOME/android-sdk-minimal"
CMDLINE_TOOLS_VERSION="11076708"  # نسخه پایدار command-line tools
OS_TYPE="$(uname -s)"

if [ "$OS_TYPE" = "Darwin" ]; then
    CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-mac-${CMDLINE_TOOLS_VERSION}_latest.zip"
else
    CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
fi

echo "📁 مسیر نصب Android SDK: $SDK_DIR"
echo ""

# --- مرحله ۳: دانلود و نصب Command Line Tools (اگر قبلا نصب نشده) ---
if [ ! -d "$SDK_DIR/cmdline-tools/latest" ]; then
    echo "⬇️  در حال دانلود Android Command Line Tools..."
    mkdir -p "$SDK_DIR/cmdline-tools"
    TMP_ZIP=$(mktemp).zip
    curl -L -o "$TMP_ZIP" "$CMDLINE_TOOLS_URL"

    echo "📦 در حال استخراج..."
    unzip -q "$TMP_ZIP" -d "$SDK_DIR/cmdline-tools"
    mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm "$TMP_ZIP"
    echo "✅ Command Line Tools نصب شد."
else
    echo "✅ Android Command Line Tools از قبل نصب است."
fi
echo ""

export ANDROID_SDK_ROOT="$SDK_DIR"
export ANDROID_HOME="$SDK_DIR"
SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"

# --- مرحله ۴: پذیرش لایسنس‌ها ---
echo "📜 در حال پذیرش لایسنس‌های Android SDK..."
yes | "$SDKMANAGER" --licenses > /dev/null 2>&1 || true
echo "✅ لایسنس‌ها پذیرفته شد."
echo ""

# --- مرحله ۵: نصب بسته‌های لازم ---
echo "⬇️  در حال نصب platform-tools, build-tools و platform-34 (ممکن است چند دقیقه طول بکشد)..."
"$SDKMANAGER" "platform-tools" "platforms;android-34" "build-tools;34.0.0" > /dev/null
echo "✅ بسته‌های SDK نصب شدند."
echo ""

# --- مرحله ۶: ساخت فایل local.properties برای گریدل ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "sdk.dir=$SDK_DIR" > "$SCRIPT_DIR/local.properties"
echo "✅ فایل local.properties ساخته شد."
echo ""

# --- مرحله ۷: اجرای Gradle build ---
echo "🔨 در حال ساخت APK (ممکن است چند دقیقه طول بکشد)..."
cd "$SCRIPT_DIR"
chmod +x ./gradlew
./gradlew assembleDebug --no-daemon

echo ""
APK_PATH="$SCRIPT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "=========================================="
    echo "✅ ساخت APK با موفقیت تمام شد!"
    echo "📍 مسیر فایل:"
    echo "   $APK_PATH"
    echo "=========================================="
    echo ""
    echo "حالا این فایل را با کابل یا هر روش دیگری به گوشی‌ات منتقل کن و نصبش کن."
else
    echo "❌ چیزی اشتباه پیش رفت. فایل APK ساخته نشد."
    exit 1
fi
