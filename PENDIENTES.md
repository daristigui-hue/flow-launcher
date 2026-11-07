# Pendientes / ideas

## App
1. **Widgets** en la pantalla de inicio (a futuro).
2. Avisos bajo el nombre de la app (NotificationListener), como Niagara.
3. **Inglés:** textos según el idioma del teléfono (hoy solo español).

## Hecho
- v0.13: Opciones › AL ABRIR UNA APP: «franja completa» (el efecto entero; la app se pide cuando la
  franja cubre la pantalla, como hasta v0.11) o «al instante» (se pide ya y la franja corre encima).
- v0.12: tocar una app la pide al instante (antes esperaba 0.4 s a que terminara la franja). Opciones
  muestra la RAM que usa el launcher y cuántas veces se redibuja por segundo en reposo. Suelta la trama
  del fondo cuando Android pide memoria; con íconos apagados ya no se cargan. «Tu fondo» pasa a llamarse
  «Color del sistema»: sigue la paleta de Android, y con wallpapers animados que no reportan colores
  (Wallpaper Engine) Android no la cambia.
- v0.11: ⚙ bajo la columna para Opciones (adiós al texto de ayuda de abajo); deslizar ↓ busca desde
  cualquier parte (en la lista completa, jalando estando hasta arriba); deslizar ↑ muestra las más
  usadas (se cuentan las aperturas desde el launcher, sin permisos extra). Menos trabajo al cambiar
  de wallpaper: los avisos de paquetes se juntan en una sola recarga y los íconos ya dibujados se
  reutilizan.
- v0.10: skin **Sakura** (rosa) y skin **Tu fondo**, que usa la paleta de Material You que Android
  saca del wallpaper (sirve también con fondos animados). Se recalcula al volver al inicio.
- v0.9: la ola ya no encasilla en una letra. Lleva a donde empieza esa letra dentro de una sola
  lista con todas las apps, que se desliza con el dedo y sigue de largo con inercia.

## Darlo a conocer
1. Capturas y un video corto en el README.
2. GitHub Releases → instalable con Obtainium y candidato a IzzyOnDroid.
3. Publicar en Reddit (r/androidapps, r/androidthemes, r/fossdroid) y XDA, leyendo sus reglas.
4. Play Store cuando haya respuesta: $25 USD, 12 testers por 14 días, formato AAB,
   targetSdk actual y política de privacidad. Sin enlaces de donativo dentro de la app.
