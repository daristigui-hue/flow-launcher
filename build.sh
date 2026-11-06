#!/usr/bin/env bash
# Arma flow-launcher.apk en la Pi, sin Gradle ni Android Studio.
# Requiere (una vez, con sudo): openjdk-21-jdk-headless aapt zipalign apksigner
# y en tools/: android.jar (platform-34) y d8.jar (build-tools 34), ambos de dl.google.com.
set -euo pipefail
cd "$(dirname "$0")"

JAR=$(find tools -path '*android-34*' -name android.jar | head -1)
D8=$(find tools -name d8.jar | head -1)
STUBS=$(find tools -name core-lambda-stubs.jar | head -1)   # lambdas: android.jar no trae LambdaMetafactory
[ -n "$JAR" ] || { echo "falta tools/…/android.jar (platform-34)"; exit 1; }
[ -n "$D8" ]  || { echo "falta tools/…/lib/d8.jar (build-tools 34)"; exit 1; }
for b in java javac keytool aapt zipalign apksigner; do
  command -v $b >/dev/null || { echo "falta $b → sudo apt install openjdk-21-jdk-headless aapt zipalign apksigner"; exit 1; }
done

rm -rf build && mkdir -p build/gen build/obj build/dex

# 1) Recursos + manifiesto → R.java y APK base (resources.arsc sin comprimir: obligatorio en targetSdk 30+)
aapt package -f -m -0 arsc -J build/gen -M app/AndroidManifest.xml -S app/res -I "$JAR" -F build/base.apk

# 2) Java → .class contra android.jar
javac -nowarn -Xlint:-options -source 8 -target 8 -encoding UTF-8 -bootclasspath "$JAR:$STUBS" -d build/obj \
  $(find app/src build/gen -name '*.java')

# 3) .class → classes.dex
java -cp "$D8" com.android.tools.r8.D8 --release --min-api 30 --lib "$JAR" --output build/dex \
  $(find build/obj -name '*.class')

# 4) Meter el dex, alinear (antes de firmar, nunca después) y firmar v2/v3
cp build/base.apk build/sin_firma.apk
(cd build/dex && aapt add ../sin_firma.apk classes.dex >/dev/null)
zipalign -f -p 4 build/sin_firma.apk build/alineado.apk

# La llave vive fuera de git (firma/). Si se pierde, cada actualización obliga a desinstalar.
mkdir -p firma
if [ ! -f firma/launcher.keystore ]; then
  head -c 24 /dev/urandom | base64 | tr -d '/+=' > firma/clave.txt
  chmod 600 firma/clave.txt
  keytool -genkeypair -keystore firma/launcher.keystore -alias launcher -keyalg RSA -keysize 4096 \
    -validity 36500 -storepass:file firma/clave.txt -keypass:file firma/clave.txt \
    -dname "CN=Flow Launcher, O=Flow Launcher" >/dev/null 2>&1
  chmod 600 firma/launcher.keystore
fi
apksigner sign --ks firma/launcher.keystore --ks-key-alias launcher \
  --ks-pass file:firma/clave.txt \
  --out build/flow-launcher.apk build/alineado.apk

# 5) Autorrevisión: sin adb, cada instalación fallida es un viaje al teléfono
zipalign -c -p 4 build/flow-launcher.apk >/dev/null && echo "✔ alineado"
apksigner verify -v build/flow-launcher.apk | grep -E 'v2 scheme|v3 scheme'
unzip -v build/flow-launcher.apk resources.arsc | grep -q ' Stored ' && echo "✔ resources.arsc sin comprimir"
aapt dump badging build/flow-launcher.apk | grep -E "^(package|sdkVersion|targetSdkVersion|launchable-activity)"
aapt dump xmltree build/flow-launcher.apk AndroidManifest.xml | grep -q 'android.intent.category.HOME' && echo "✔ categoría HOME"
ls -la build/flow-launcher.apk

# 6) Copia para GitHub: se sube como asset del Release (releases/flow-launcher.apk)
mkdir -p releases
cp build/flow-launcher.apk releases/flow-launcher.apk
aapt dump badging build/flow-launcher.apk | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -1 > releases/VERSION
echo "→ releases/flow-launcher.apk v$(cat releases/VERSION)"
