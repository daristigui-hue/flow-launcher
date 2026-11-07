# Pendientes / ideas

## App
1. **Widgets** en la pantalla de inicio (a futuro).
2. Avisos bajo el nombre de la app (NotificationListener), como Niagara.
3. **Inglés:** textos según el idioma del teléfono (hoy solo español).
4. **Contador de aperturas** con el efecto del calendario (pospuesto).
5. Pausa: límite diario propio por app (hoy compara contra el promedio).
6. Pausa: frases en inglés junto con el resto de la app.

## Hecho
- v0.18: Opciones › APPS EN EL LISTADO › ELEGIR abre la lista completa con todas las apps, ocultas
  incluidas (tenues). ✓ = sale en el listado, ⊘ = oculta; tocar una la cambia sin abrirla. Se desliza y
  la ola salta por letra como siempre. LISTO (o atrás) regresa a Opciones. Las ocultas tampoco salen en
  la búsqueda ni en favoritas.
- v0.17: las nubes con datos ya no suenan a reporte: las hace alguien que observa. Lo bueno sale con
  decepción («Qué mal… hoy iba 35 % mejor que ayer», «Llevaba 4 días bajo su promedio… ¿y ahora?») y lo
  malo con burla («40 % más que ayer… ¿y todavía quiere más?»). Varias variantes que se turnan.
- v0.16: la pausa dura 6, 10 (de fábrica) o 15 s, o **crece** (6 s las primeras 4 veces del día, 10 s
  hasta la 9ª, 15 s después). Cada app tiene un **tipo** (redes, mensajería, juegos, video, trabajo,
  compras, noticias u otra; se detecta solo o se elige en mantener › TIPO) y la última nube es una frase
  de ese tipo, sin repetir la anterior. «Esta vez no» con sol de rayos, 22 estrellas y destellos. Si
  Android bloquea el permiso de uso, Flow lleva a Info de la app para liberarlo.
- v0.15: **pausa antes de abrir** (mantener una app › ◷ PAUSA ANTES DE ABRIR; lista en Opciones › PAUSA).
  Al abrirla salen durante 5 s, sin poder saltarse, nubes de pensamiento en tercera persona: veces que
  se abrió hoy, tiempo de hoy, % contra ayer, comparación con el promedio de los 7 días anteriores, la
  hora y «¿A qué irá?». Luego MEJOR NO (no cuenta como apertura; cierra con «Esta vez no») o ABRIR.
  El tiempo y el promedio piden el acceso «Uso de apps» de Android; los datos no salen del teléfono.
- v0.14: Opciones › ABRIR AL SOLTAR (apagada por defecto): el dedo baja en un nombre, el cursor lo
  sigue y al levantar se abre esa app. En la lista completa, arrastrar sobre los nombres elige en vez
  de desplazar; con el dedo en la orilla la lista avanza sola. Soltar fuera o en la app de salida no abre.
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
