# Flow Launcher

Launcher minimalista para Android: tus favoritas en una lista y una columna con el
alfabeto en la orilla para llegar a cualquier app con el pulgar. Animado, con resortes,
bloques ladeados y cinco skins.

*English below.*

## Qué hace
- **Favoritas** en la pantalla de inicio; mantén y arrastra para ordenarlas.
- **Columna del alfabeto**: desliza el pulgar y suelta en una letra; toca una app para abrirla
  (deslizar nunca abre nada por accidente).
- **Carpetas**, dentro de la columna o en el inicio.
- **Ocultar apps** que no usas: mantén la app › *OCULTAR APP*. Se recuperan en
  *Opciones › APPS OCULTAS*.
- **5 skins**: Carmesí, Medianoche, Dorado, Sakura y «Color del sistema» (la paleta
  Material You del teléfono, Android 12+); fondo animado o el wallpaper del teléfono
  (incluido Wallpaper Engine), íconos sí/no.
- Toca la hora para abrir el calendario. El ⚙ bajo la columna abre las *Opciones*.
- Desliza ↓ en cualquier parte para buscar; ↑ para ver tus apps más usadas.
- Sin internet, sin anuncios, sin permisos raros. Pesa ~55 KB.

## Instalar
1. Descarga [`releases/flow-launcher.apk`](releases/flow-launcher.apk) desde el teléfono.
2. Ábrelo y permite instalar desde el navegador si Android lo pide.
3. Botón de inicio → elige **Flow** (o *Opciones › PANTALLA DE INICIO*).

Requiere Android 11 o superior. Para actualizar, instala el APK nuevo encima: se conservan
favoritas, carpetas y apps ocultas.

## Compilar
Sin Gradle ni Android Studio. En Debian/Ubuntu:
```bash
sudo apt install openjdk-21-jdk-headless aapt zipalign apksigner
# en tools/: descomprimir platform-34-ext12_r01.zip y build-tools_r34-linux.zip (dl.google.com)
./build.sh
```
`build.sh` crea una llave de firma propia en `firma/` la primera vez. Un APK que compiles tú
lleva otra firma: para instalarlo encima del oficial hay que desinstalar el oficial antes.

## Apoya el proyecto
Si te sirve, puedes invitarme un café ☕ en **[Ko-fi](https://ko-fi.com/davida89953)**.

[![Invítame un café en Ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/davida89953)

## Licencia
MIT — ver [LICENSE](LICENSE).

---

# Flow Launcher (English)

A minimalist Android launcher: favorites in a list plus an alphabet column on the edge, so
any app is one thumb swipe away. Springy animations, tilted blocks and five skins.

- **Favorites** on the home screen; long-press and drag to reorder.
- **Alphabet column**: slide your thumb to a letter, tap an app to open it (swiping never opens anything).
- **Folders**, in the column or on the home screen.
- **Hide apps**: long-press › *OCULTAR APP*; bring them back in *Opciones › APPS OCULTAS*.
- Swipe ↓ anywhere to search, ↑ for your most used apps. The ⚙ under the column opens settings.
- **5 skins** (one follows the phone's Material You palette, Android 12+), animated background or your system wallpaper (Wallpaper Engine works), icons on/off.
- No internet permission, no ads. ~55 KB. Android 11+. UI is in Spanish.

**Install:** download [`releases/flow-launcher.apk`](releases/flow-launcher.apk), open it, then pick
**Flow** as your home app. **Build:** see the commands above. **Support:** [buy me a coffee on Ko-fi](https://ko-fi.com/davida89953) ☕.
**License:** MIT.
