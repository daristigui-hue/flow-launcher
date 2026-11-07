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
- Opcional: *abrir al soltar* — desliza el dedo por la lista y suelta sobre la app.
- **Pausa antes de abrir** las apps que te comen el día: mantén la app › *PAUSA ANTES DE ABRIR*.
  Antes de abrirla salen unas nubes con cuántas veces la abriste hoy, cuánto tiempo llevas, cómo vas
  contra ayer y contra tu promedio, y un remate según el tipo de app (redes, juegos, trabajo…).
  Luego eliges *MEJOR NO* o *ABRIR*. Dura 6, 10 (de fábrica) o 15 s, o crece con cada apertura del día.
- Sin internet ni anuncios. Pesa ~70 KB. El único permiso especial es opcional (ver abajo).

## Instalar
1. Descarga [`releases/flow-launcher.apk`](releases/flow-launcher.apk) desde el teléfono.
2. Ábrelo y permite instalar desde el navegador si Android lo pide.
3. Botón de inicio → elige **Flow** (o *Opciones › PANTALLA DE INICIO*).

Requiere Android 11 o superior. Para actualizar, instala el APK nuevo encima: se conservan
favoritas, carpetas y apps ocultas.

### Permiso «Uso de apps» (opcional, para la pausa)
Sirve para que la pausa sepa cuánto tiempo usas cada app. Los datos se quedan en el teléfono: Flow no
tiene permiso de internet. Como el APK no viene de Play Store, Android lo bloquea la primera vez
(«ajuste restringido»). Para liberarlo:
1. *Ajustes › Apps › Flow* → **⋮** arriba a la derecha → **Permitir ajustes restringidos**.
2. Vuelve a Flow: *Opciones › PAUSA › PERMISO* y activa el interruptor.

Sin el permiso la pausa funciona igual, solo con las veces, la hora y el remate.

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
- **Mindful pause** before opening the apps you pick: clouds show how often and how long you used it
  today, versus yesterday and your average, then you choose *MEJOR NO* (not now) or *ABRIR* (open).
  Optional usage-access permission; sideloaded apps must first enable *App info › ⋮ › Allow restricted settings*.
- No internet permission, no ads. ~70 KB. Android 11+. UI is in Spanish.

**Install:** download [`releases/flow-launcher.apk`](releases/flow-launcher.apk), open it, then pick
**Flow** as your home app. **Build:** see the commands above. **Support:** [buy me a coffee on Ko-fi](https://ko-fi.com/davida89953) ☕.
**License:** MIT.
