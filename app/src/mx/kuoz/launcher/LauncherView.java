package mx.kuoz.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Toda la pantalla de inicio en una sola vista dibujada a mano (Canvas): así las
 * animaciones de Persona no pelean con el sistema de layouts. Es la maqueta
 * maqueta/launcher.html pasada a Android, con los mismos números.
 */
final class LauncherView extends View {

    interface Oyente {
        void abrir(AppEntry a);
        void fijar(AppEntry a);
        void abrirBusqueda();
        void cerrarBusqueda();
        void cambiarSkin();
        void reordenar(List<AppEntry> nuevoOrden);
        void alternarIconos();
        void ajustesInicio();
        void abrirCalendario();
        void alternarFondo();
        void cambiarWallpaper();
        void aCarpeta(AppEntry a, String carpetaId);
        void nuevaCarpeta(AppEntry conApp);          // conApp puede ser null
        void sacarDeCarpeta(AppEntry a, String carpetaId);
        void reordenarCarpeta(String carpetaId, List<AppEntry> orden);
        void renombrarCarpeta(String carpetaId);
        void borrarCarpeta(String carpetaId);
        void alternarCarpetaInicio(String carpetaId);
        void ocultar(AppEntry a);                   // fuera de la ola, búsqueda y favoritas
        void mostrar(String clave);
        void alternarFranja();
        void alternarSoltar();
    }

    static final String CARPETA = "carpeta:";   // prefijo del modo que muestra una carpeta

    static final String FAV = "fav", BUSCA = "busca", USADAS = "usadas";
    // Como Niagara: solo las letras que tienen apps, ★ arriba = favoritas, # al final.
    // Se recalcula en setApps(). Máximo ★ + 26 + # = 28.
    private static final char FAVS_LETRA = '★';
    private String letras = FAVS_LETRA + "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String[] DIAS = {"DOMINGO", "LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO"};
    private static final String[] MES = {"ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"};
    // Cada renglón con su propio ladeo y sangría, como el menú de P5
    private static final float[] ROT = {-4f, -2.5f, -5f, -3f};
    private static final float[] SANGRIA = {0f, 14f, 4f, 22f};

    private final float d;
    private final Oyente oyente;
    private Skin skin;
    private final Typeface negra;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final Random azar = new Random();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final boolean animaciones = ValueAnimator.areAnimatorsEnabled();

    // Datos
    private List<AppEntry> todas = new ArrayList<>();
    private List<AppEntry> favs = new ArrayList<>();
    private List<AppEntry> usadas = new ArrayList<>();
    private final Set<String> clavesFav = new HashSet<>();
    private final List<AppEntry> visibles = new ArrayList<>();
    private String modo = FAV;
    private int sel = 0;

    // Letras: una sola tira con todas las apps; la letra solo la desplaza (v0.9)
    private float scroll, scrollVel, scrollMeta = Float.NaN, scroll0;
    private boolean desliza;
    private boolean engranajeToque;   // el dedo bajó sobre el ⚙ de debajo de la ola
    private char letraVista;
    private android.view.VelocityTracker vt;

    // Geometría
    private int insetArriba, insetAbajo;
    private float filaAlto, listaArriba;

    // Animación
    private long ultimoFrame;
    // Consumo para Opciones: cuadros dibujados por segundo (el último segundo completo) y RAM
    private long tSegundo;
    private int cuadros, cuadrosPorSeg, cuadrosAntes, ramMb;
    private long tEntrada;
    private float duracionEntrada = 550f;
    private final OvershootInterpolator entra = new OvershootInterpolator(1.3f);
    private final PathInterpolator seco = new PathInterpolator(.7f, 0f, .2f, 1f);
    private final DecelerateInterpolator frena = new DecelerateInterpolator(2.2f);
    private final Resorte cursorY = new Resorte(520, .45f), cursorAncho = new Resorte(520, .5f);
    private final float[] dentado = new float[12], filo = new float[10];
    private long tDentado;

    // Ola del alfabeto
    private static final int MAX_CARPETAS_OLA = 12;
    private final Resorte[] olaEscala = new Resorte[28 + MAX_CARPETAS_OLA], olaX = new Resorte[28 + MAX_CARPETAS_OLA];
    private boolean olaActiva;
    private int letraAct = -1;
    private float dedoY;
    private final Resorte burbuja = new Resorte(420, .4f), letraFondo = new Resorte(300, .45f);

    // Corte diagonal al abrir app
    private static final int NADA = 0, VA = 1, CUBIERTO = 2, SALE = 3;
    private int corte = NADA;
    private long tCorte;
    private String corteNombre = "";
    private Runnable alCubrir;

    // Calendario del cambio de día
    private long tCal = -1;
    private final Resorte calHoy = new Resorte(260, .35f);

    // Aviso
    private String aviso;
    private long tAviso;
    private final Resorte avisoEscala = new Resorte(420, .45f);

    // Fondo
    private Bitmap puntos;
    private Path rayos;
    private final float[][] parts = new float[14][5]; // x, tamaño, periodo, fase, deriva

    // Toque
    private float x0, y0;
    private int pulso = -1;
    // Opción «abrir al soltar»: el dedo baja en un nombre, el cursor lo sigue y al levantar se abre
    private boolean abrirAlSoltar, eligiendo;
    private int eligeOrigen = -1, autoScroll;   // autoScroll: -1 sube, 1 baja (dedo en la orilla de la tira)
    private float eligeX, eligeY;
    private boolean arrastre, cambio, largoHecho, tiraToque;

    // Favorita "levantada" para reordenar (mantener + arrastrar)
    private int levantada = -1, levOrigen = -1;
    private float levY, levAgarre;
    private final Resorte levEscala = new Resorte(500, .5f);
    private final java.util.HashMap<String, Resorte> filaY = new java.util.HashMap<>();
    // Menú de acciones de una app (mantener): renglones {texto, acción}
    private AppEntry menuApp;
    private float menuY;
    private final List<String[]> menuOps = new ArrayList<>();
    private final List<RectF> menuRects = new ArrayList<>();
    private long tMenu;

    // Carpetas
    private List<Carpeta> carpetas = new ArrayList<>();
    private final java.util.HashMap<String, AppEntry> porClave = new java.util.HashMap<>();

    // Opciones
    private boolean opciones, iconos = true, wallpaper;
    private long tOpc;
    private String opcSkin = "", opcVersion = "";
    private final RectF[] opcRect = new RectF[9];
    private static final int MAX_OCULTAS = 6;   // + fila de regresar = 7 ≤ opcRect
    private int opcPagina;            // 0 principal · 1 carpetas · 2 una carpeta
    private String opcCarpeta;
    private boolean opcConfirmaBorrar;
    private final Runnable largoItem = () -> {
        if (pulso >= 0 && pulso < visibles.size()) {
            largoHecho = true;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            if (FAV.equals(modo) || modo.startsWith(CARPETA)) {
                // Se "levanta" para moverla; si se suelta sin mover, sale el menú
                levantada = levOrigen = pulso;
                levY = y0; levAgarre = y0 - centroFila(pulso);
                levEscala.pos = 1; levEscala.vel = 0; levEscala.objetivo = 1.1f;
                invalidate();
            } else {
                // Letra o búsqueda: el menú sale ya; la soltada de este dedo no cuenta (largoHecho)
                abrirMenu(visibles.get(pulso), centroFila(pulso));
            }
            pulso = -1;
        }
    };
    private final Runnable largoVacio = () -> {
        largoHecho = true;
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        abrirOpciones();
    };

    // Hora
    private String hora = "", fecha1 = "", fecha2 = "", fecha3 = "";
    private int minutoPintado = -1;

    LauncherView(Context c, Skin skin, Oyente oyente) {
        super(c);
        this.d = c.getResources().getDisplayMetrics().density;
        this.skin = skin;
        this.oyente = oyente;
        negra = Typeface.create(Typeface.create("sans-serif-black", Typeface.NORMAL), 900, true);
        t.setTypeface(negra);
        for (int i = 0; i < opcRect.length; i++) opcRect[i] = new RectF();
        for (int i = 0; i < olaEscala.length; i++) {
            olaEscala[i] = new Resorte(500, .42f); olaEscala[i].fija(1);
            olaX[i] = new Resorte(500, .42f); olaX[i].fija(0);
        }
        for (float[] q : parts) {
            q[0] = azar.nextFloat(); q[1] = 8 + azar.nextFloat() * 26; q[2] = 8 + azar.nextFloat() * 14;
            q[3] = azar.nextFloat(); q[4] = azar.nextFloat() * 80 - 40;
        }
        nuevoDentado();
        setHapticFeedbackEnabled(true);
    }

    // ───────────────────────── API para MainActivity ─────────────────────────

    void setInsets(int arriba, int abajo) { insetArriba = arriba; insetAbajo = abajo; invalidate(); }

    void setSkin(Skin s) { skin = s; puntos = null; invalidate(); }

    void setApps(List<AppEntry> todas, List<AppEntry> favs) {
        this.todas = todas;
        this.favs = favs;
        clavesFav.clear();
        for (AppEntry a : favs) clavesFav.add(a.clave);
        porClave.clear();
        for (AppEntry a : todas) porClave.put(a.clave, a);
        StringBuilder sb = new StringBuilder().append(FAVS_LETRA);
        boolean hayNumero = false;
        for (char l = 'A'; l <= 'Z'; l++) for (AppEntry a : todas) if (a.letra == l) { sb.append(l); break; }
        for (AppEntry a : todas) if (a.letra == '#') { hayNumero = true; break; }
        if (hayNumero) sb.append('#');
        if (!olaActiva) { letras = sb.toString(); letraAct = -1; }
        if (!BUSCA.equals(modo)) refresca();
        else invalidate();
    }

    private List<AppEntry> ocultas = new ArrayList<>();

    void setOcultas(List<AppEntry> l) { ocultas = l; invalidate(); }

    void setCarpetas(List<Carpeta> l) {
        carpetas = l;
        if (modo.startsWith(CARPETA) && carpeta(modo.substring(CARPETA.length())) == null) pinta(FAV, true);
        else if (modo.startsWith(CARPETA)) refresca();
        invalidate();
    }

    private Carpeta carpeta(String id) {
        for (Carpeta c : carpetas) if (c.id.equals(id)) return c;
        return null;
    }

    private List<AppEntry> appsDe(Carpeta c) {
        List<AppEntry> l = new ArrayList<>();
        for (String k : c.claves) { AppEntry a = porClave.get(k); if (a != null) l.add(a); }
        return l;
    }

    /** Rehace la lista visible sin reiniciar resortes: el reacomodo se ve en vez de saltar. */
    private void refresca() {
        if (FAV.equals(modo)) { visibles.clear(); visibles.addAll(favs); }
        else if (USADAS.equals(modo)) { visibles.clear(); visibles.addAll(usadas); }
        else if (modo.startsWith(CARPETA)) {
            Carpeta c = carpeta(modo.substring(CARPETA.length()));
            visibles.clear();
            if (c != null) visibles.addAll(appsDe(c));
        }
        else if (enLetras()) { visibles.clear(); visibles.addAll(todas); scroll = Math.min(scroll, scrollMax()); }
        sel = Math.min(sel, Math.max(0, visibles.size() - 1));
        invalidate();
    }

    /** Las más abiertas desde el launcher (deslizar ↑ en inicio). */
    void setUsadas(List<AppEntry> l) {
        usadas = l;
        if (USADAS.equals(modo)) refresca();
    }

    void setFavs(List<AppEntry> favs) {
        this.favs = favs;
        clavesFav.clear();
        for (AppEntry a : favs) clavesFav.add(a.clave);
        if (FAV.equals(modo)) {
            // Sin pinta(): conserva los resortes de cada fila para que el reacomodo se vea
            visibles.clear();
            visibles.addAll(favs);
            sel = Math.min(sel, Math.max(0, visibles.size() - 1));
        }
        invalidate();
    }

    void setIconos(boolean si) { iconos = si; invalidate(); }

    /** true = la app se abre cuando la franja ya cubrió la pantalla (el efecto completo, 0.4 s);
     *  false = se pide al instante y la franja corre encima. */
    private boolean franjaCompleta = true;

    void setFranjaCompleta(boolean si) { franjaCompleta = si; invalidate(); }

    void setAbrirAlSoltar(boolean si) { abrirAlSoltar = si; invalidate(); }

    boolean franjaCompleta() { return franjaCompleta; }

    /** true = se ve el wallpaper del sistema (p. ej. Wallpaper Engine) detrás de la lista. */
    void setWallpaper(boolean si) { wallpaper = si; invalidate(); }

    private String wallpaperActual = "";

    void setWallpaperActual(String nombre) { wallpaperActual = nombre; }

    void setOpciones(String skinNombre, String version) { opcSkin = skinNombre; opcVersion = version; invalidate(); }

    void abrirOpciones() {
        // Foto del consumo justo antes de abrir: el ritmo de redibujo en reposo y la RAM real
        // (PSS, lo mismo que mide Ajustes › Memoria)
        cuadrosAntes = cuadrosPorSeg;
        ramMb = (int) (android.os.Debug.getPss() / 1024);
        opciones = true;
        opcPagina = 0;
        opcConfirmaBorrar = false;
        tOpc = animaciones ? SystemClock.uptimeMillis() : 0;
        invalidate();
    }

    /** "Atrás": cierra opciones o el menú QUITAR. Devuelve true si cerró algo. */
    boolean cerrarCapas() {
        if (opciones) {
            if (opcPagina > 0) { opcPagina = opcPagina == 2 ? 1 : 0; opcConfirmaBorrar = false; tOpc = animaciones ? SystemClock.uptimeMillis() : 0; }
            else opciones = false;
            invalidate();
            return true;
        }
        if (menuApp != null) { menuApp = null; invalidate(); return true; }
        return false;
    }

    void irAFavoritas() { if (!FAV.equals(modo)) pinta(FAV, true); }

    boolean enFavoritas() { return FAV.equals(modo); }

    void filtrar(String q) {
        String plano = AppEntry.sinAcentos(q).toLowerCase().trim();
        if (enLetras()) { cursorY.pos = 0; scroll = 0; }   // venía de la tira: otras coordenadas
        visibles.clear();
        for (AppEntry a : todas) {
            if (a.plano.contains(plano)) visibles.add(a);
            if (visibles.size() == 8) break;
        }
        modo = BUSCA;
        filaY.clear();
        duracionEntrada = 300f;
        reiniciaEntrada();
    }

    AppEntry primero() { return visibles.isEmpty() ? null : visibles.get(0); }

    /** Modo letra: la lista es la tira completa y se desplaza. */
    private boolean enLetras() { return !FAV.equals(modo) && !BUSCA.equals(modo) && !USADAS.equals(modo) && !modo.startsWith(CARPETA); }

    private float desp() { return enLetras() ? scroll : 0; }

    private float listaAbajo() { return getHeight() - insetAbajo - 60 * d; }

    private float scrollMax() { return Math.max(0, visibles.size() * filaAlto - (listaAbajo() - listaArriba)); }

    private int indiceLetra(char l) {
        for (int i = 0; i < visibles.size(); i++) if (visibles.get(i).letra == l) return i;
        return 0;
    }

    /** Primer renglón (en todo o en parte) a la vista. */
    private int primeraVisible() { return Math.max(0, (int) Math.floor(desp() / filaAlto)); }

    /** Lleva la tira a donde empieza la letra; {@code suave} = se desliza, si no salta. */
    private void irALetra(char l, boolean suave) {
        int i = indiceLetra(l);
        float meta = Math.min(scrollMax(), i * filaAlto);
        scrollVel = 0;
        if (suave) scrollMeta = meta; else { scroll = meta; scrollMeta = Float.NaN; }
        sel = i;
        cursorY.fija(listaArriba + i * filaAlto + filaAlto / 2f);
        invalidate();
    }

    /** Barrido diagonal; cuando la pantalla queda cubierta corre {@code luego} (abrir la app). */
    void corte(String nombre, Runnable luego) {
        corteNombre = nombre;
        alCubrir = luego;
        if (!animaciones) { corte = CUBIERTO; if (luego != null) luego.run(); return; }
        corte = VA;
        tCorte = SystemClock.uptimeMillis();
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        invalidate();
    }

    /** Al volver a la pantalla de inicio: si quedó cubierta, se destapa. */
    void alVolver() {
        if (corte == CUBIERTO || corte == VA) {
            pinta(FAV, false);
            corte = animaciones ? SALE : NADA;
            tCorte = SystemClock.uptimeMillis();
        }
        ultimoFrame = 0;
        invalidate();
    }

    void calendario() {
        if (!animaciones) return;
        tCal = SystemClock.uptimeMillis();
        calHoy.fija(1);
        invalidate();
    }

    void aviso(String texto) {
        aviso = texto;
        tAviso = SystemClock.uptimeMillis();
        avisoEscala.pos = .4f; avisoEscala.vel = 0; avisoEscala.objetivo = 1;
        invalidate();
    }

    // ───────────────────────── Lista ─────────────────────────

    private void pinta(String nuevo, boolean conEntrada) {
        boolean eraLetras = enLetras();
        modo = nuevo;
        filaY.clear();
        menuApp = null;
        visibles.clear();
        if (FAV.equals(nuevo)) visibles.addAll(favs);
        else if (USADAS.equals(nuevo)) visibles.addAll(usadas);
        else if (nuevo.startsWith(CARPETA)) {
            Carpeta c = carpeta(nuevo.substring(CARPETA.length()));
            if (c != null) visibles.addAll(appsDe(c));
        }
        else if (!BUSCA.equals(nuevo)) visibles.addAll(todas);
        duracionEntrada = 550f;
        scrollVel = 0; scrollMeta = Float.NaN;
        if (!enLetras()) scroll = 0;
        if (FAV.equals(nuevo) || USADAS.equals(nuevo)) letraFondo.objetivo = 0;
        else if (!BUSCA.equals(nuevo)) { letraFondo.pos = .6f; letraFondo.objetivo = 1; }
        if (conEntrada) reiniciaEntrada();
        else { sel = Math.min(sel, Math.max(0, visibles.size() - 1)); invalidate(); }
        if (enLetras()) {
            geometria();
            irALetra(nuevo.charAt(0), false);
            letraVista = nuevo.charAt(0);
        } else if (eraLetras) cursorY.pos = 0;   // coordenadas de la tira ≠ de la lista: que no vuele
    }

    private void reiniciaEntrada() {
        sel = 0;
        tEntrada = animaciones ? SystemClock.uptimeMillis() : 0;
        invalidate();
    }

    private void geometria() {
        float h = getHeight();
        float arriba = BUSCA.equals(modo) ? insetArriba + 40 * d + 64 * d + 26 * d : insetArriba + 20 * d + 170 * d;
        float abajo = h - insetAbajo - 60 * d;
        int n = Math.max(1, visibles.size());
        filaAlto = Math.max(44 * d, Math.min(64 * d, (abajo - arriba) / n));
        listaArriba = arriba;
        if (enLetras()) filaAlto = Math.max(44 * d, Math.min(56 * d, (abajo - arriba) / Math.min(n, 7)));
        else if (!BUSCA.equals(modo)) {
            // Centrada en el espacio libre: con pocas apps (una letra) queda a media pantalla,
            // al alcance del pulgar, no pegada a la hora (v0.4)
            float bloque = Math.min(n, (int) ((abajo - arriba) / filaAlto)) * filaAlto;
            listaArriba = arriba + Math.max(0, (abajo - arriba - bloque) / 2f);
        }
    }

    private int filasQueCaben() {
        float abajo = listaAbajo();
        if (enLetras()) {
            // En la tira: hasta el último renglón que asoma por abajo
            return Math.min(visibles.size(), (int) Math.ceil((abajo - listaArriba + scroll) / filaAlto));
        }
        return Math.min(visibles.size(), (int) ((abajo - listaArriba) / filaAlto));
    }

    private int filaEn(float x, float y) {
        if (x > getWidth() - 72 * d) return -1;
        if (enLetras() && (y < listaArriba || y > listaAbajo())) return -1;
        int i = (int) Math.floor((y - listaArriba + desp()) / filaAlto);
        return (i >= 0 && i < filasQueCaben()) ? i : -1;
    }

    // ───────────────────────── Dibujo ─────────────────────────

    @Override
    protected void onDraw(Canvas c) {
        long ahora = SystemClock.uptimeMillis();
        cuadros++;
        if (ahora - tSegundo >= 1000) { cuadrosPorSeg = ahora - tSegundo < 2000 ? cuadros : 0; cuadros = 0; tSegundo = ahora; }
        float dt = ultimoFrame == 0 ? 0.016f : (ahora - ultimoFrame) / 1000f;
        ultimoFrame = ahora;
        boolean mueve = false;

        geometria();
        int w = getWidth(), h = getHeight();

        fondo(c, w, h, ahora);
        if (!BUSCA.equals(modo)) cabeza(c);

        // Letra gigante de fondo: la del renglón marcado (con Z la tira topa al final y
        // arriba queda otra letra; el marcado sí es la Z)
        if (enLetras() && !visibles.isEmpty()) {
            char l = visibles.get(Math.min(sel, visibles.size() - 1)).letra;
            if (l != letraVista) { letraVista = l; letraFondo.pos = .7f; letraFondo.objetivo = 1; }
        }
        mueve |= letraFondo.paso(dt, .002f);
        if (!FAV.equals(modo) && !BUSCA.equals(modo) && !USADAS.equals(modo) && letraFondo.pos > .01f) {
            c.save();
            c.translate(w - 40 * d, listaArriba - 30 * d);
            c.rotate(-10);
            c.scale(letraFondo.pos, letraFondo.pos);
            t.setTextAlign(Paint.Align.RIGHT);
            String gigante = enLetras() ? String.valueOf(letraVista) : modo;
            t.setTextSize(240 * d);
            if (modo.startsWith(CARPETA)) {
                Carpeta cp = carpeta(modo.substring(CARPETA.length()));
                gigante = cp == null ? "" : cp.nombre.toUpperCase();
                t.setTextSize(Math.min(120 * d, w * 1.5f / Math.max(3, gigante.length())));
            }
            t.setLetterSpacing(0);
            t.setColor(Color.BLACK); t.setAlpha(140);
            c.drawText(gigante, 10 * d, 190 * d + 10 * d, t);
            t.setColor(skin.fuerte); t.setAlpha(140);
            c.drawText(gigante, 0, 190 * d, t);
            t.setAlpha(255);
            c.restore();
        }

        // Título
        t.setTextAlign(Paint.Align.LEFT);
        t.setTextSize(15 * d);
        t.setLetterSpacing(.35f);
        t.setColor(skin.cian);
        c.save();
        c.translate(14 * d, listaArriba - 8 * d);
        c.rotate(-4);
        String titulo = FAV.equals(modo) ? "FAVORITAS" : USADAS.equals(modo) ? "MÁS USADAS" : BUSCA.equals(modo) ? visibles.size() + " RESULTADOS"
                : modo.startsWith(CARPETA) ? "CARPETA · " + visibles.size() + " APPS" : visibles.size() + " APPS";
        c.drawText(titulo, 0, 0, t);
        c.restore();

        mueve |= lista(c, ahora, dt);
        mueve |= ola(c, w, h, dt);
        mueve |= avisoDibuja(c, w, h, ahora, dt);
        if (opciones) mueve |= opcionesDibuja(c, w, h, ahora);
        mueve |= corteDibuja(c, w, h, ahora);
        mueve |= calendarioDibuja(c, w, h, ahora, dt);

        // Con algo moviéndose: a la tasa de la pantalla. Quieto: el fondo a ~20 fps
        // para no gastar batería (y nada si la ventana no se ve: onDraw ya no llega).
        if (mueve || olaActiva) postInvalidateOnAnimation();
        else postInvalidateDelayed(wallpaper ? 1000 : 50);   // con wallpaper solo cambia la hora
    }

    private void fondo(Canvas c, int w, int h, long ahora) {
        if (wallpaper) {
            // El wallpaper lo dibuja el sistema (Wallpaper Engine): aquí solo una sombra para
            // que la hora y los nombres se lean sobre cualquier imagen.
            c.drawColor(0x40000000);
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, 0, 0, insetArriba + 320 * d, 0x99000000, 0x00000000, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, insetArriba + 320 * d, p);
            p.setShader(new LinearGradient(w * .45f, 0, 0, 0, 0x00000000, 0x66000000, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w * .45f, h, p);
            p.setShader(null);
            return;
        }
        c.drawColor(skin.abismo);
        if (rayos == null) {
            rayos = new Path();
            float r = 1.4f * Math.max(w, h);
            for (int a = 0; a < 360; a += 14) {
                rayos.moveTo(0, 0);
                rayos.lineTo((float) (r * Math.cos(Math.toRadians(a))), (float) (r * Math.sin(Math.toRadians(a))));
                rayos.lineTo((float) (r * Math.cos(Math.toRadians(a + 7))), (float) (r * Math.sin(Math.toRadians(a + 7))));
                rayos.close();
            }
        }
        c.save();
        c.translate(w / 2f, h * .4f);
        c.rotate((ahora % 90000L) / 90000f * 360f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(skin.marino);
        p.setAlpha((int) (255 * skin.rayos));
        c.drawPath(rayos, p);
        c.restore();
        p.setAlpha(255);

        if (puntos == null) puntos = tramaDePuntos(w, h);
        c.save();
        c.scale(2, 2);
        c.drawBitmap(puntos, 0, 0, null);
        c.restore();

        // Marca: la "P" gigante de abajo
        t.setTextAlign(Paint.Align.RIGHT);
        t.setLetterSpacing(-.06f);
        t.setTextSize(w * .46f);
        t.setColor(skin.marino);
        c.save();
        c.translate(w + w * .06f, h + h * .04f - w * .08f);
        c.rotate(-8);
        c.drawText("P", 0, 0, t);
        c.restore();

        if (skin.particulas == 0) return;
        for (float[] q : parts) {
            float periodo = q[2] * 1000f;
            float f = ((ahora % (long) periodo) / periodo + q[3]) % 1f;
            float y = h + 40 * d - f * (h * 1.1f + 40 * d);
            float x = q[0] * w + q[4] * d * f;
            float tam = q[1] * d / 2f;
            c.save();
            c.translate(x, y);
            c.rotate(f * 220f);
            if (skin.particulas == 1) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2 * d);
                p.setColor(skin.cian); p.setAlpha(90);
                c.drawCircle(0, 0, tam, p);
            } else {
                p.setStyle(Paint.Style.FILL);
                p.setColor(skin.rojo); p.setAlpha(100);
                rect.set(-tam, -tam / 2, tam, tam / 2);
                c.drawOval(rect, p);
            }
            c.restore();
        }
        p.setAlpha(255);
        p.setStyle(Paint.Style.FILL);
    }

    /** Trama de puntos (a media resolución) que se desvanece hacia arriba a la izquierda. */
    private Bitmap tramaDePuntos(int w, int h) {
        int bw = Math.max(1, w / 2), bh = Math.max(1, h / 2);
        Bitmap b = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        Canvas k = new Canvas(b);
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        q.setColor(skin.azul);
        float paso = 7 * d, radio = .8f * d;
        for (float y = 0; y < bh; y += paso) for (float x = 0; x < bw; x += paso) k.drawCircle(x, y, radio, q);
        Paint m = new Paint();
        m.setShader(new LinearGradient(0, 0, bw * .35f, bh, 0x00000000, 0x2e000000, Shader.TileMode.CLAMP));
        m.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        k.drawRect(0, 0, bw, bh, m);
        return b;
    }

    private void cabeza(Canvas c) {
        Calendar k = Calendar.getInstance();
        int minuto = k.get(Calendar.HOUR_OF_DAY) * 60 + k.get(Calendar.MINUTE);
        if (minuto != minutoPintado) {
            minutoPintado = minuto;
            hora = String.format("%02d:%02d", k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE));
            fecha1 = DIAS[k.get(Calendar.DAY_OF_WEEK) - 1].substring(0, 3) + " · ";
            fecha2 = String.format("%02d", k.get(Calendar.DAY_OF_MONTH));
            fecha3 = " " + MES[k.get(Calendar.MONTH)];
        }
        c.save();
        c.translate(22 * d, insetArriba + 26 * d);
        c.rotate(-6);
        c.skew(-.14f, 0);
        t.setTextAlign(Paint.Align.LEFT);
        t.setLetterSpacing(-.04f);
        t.setTextSize(86 * d);
        float base = 72 * d;
        t.setColor(Color.BLACK);
        c.drawText(hora, 9 * d, base + 9 * d, t);
        t.setColor(skin.fuerte);
        c.drawText(hora, 6 * d, base + 6 * d, t);
        t.setColor(skin.blanco);
        c.drawText(hora, 0, base, t);

        // Etiqueta de fecha blanca ladeada
        t.setTextSize(17 * d);
        t.setLetterSpacing(.18f);
        float a1 = t.measureText(fecha1), a2 = t.measureText(fecha2), a3 = t.measureText(fecha3);
        float ancho = a1 + a2 + a3 + 36 * d, arriba = base + 18 * d, alto = 32 * d;
        poligono(c, 0, arriba, ancho, alto, new float[]{.04f, 0, 1, .08f, .95f, 1, 0, .88f}, skin.blanco);
        float y = arriba + 22 * d, x = 14 * d;
        t.setColor(Color.BLACK); c.drawText(fecha1, x, y, t); x += a1;
        t.setColor(skin.rojo); c.drawText(fecha2, x, y, t); x += a2;
        t.setColor(Color.BLACK); c.drawText(fecha3, x, y, t);
        c.restore();
    }

    private boolean lista(Canvas c, long ahora, float dt) {
        int n = filasQueCaben();
        boolean mueve = false;
        if (n == 0) {
            if (FAV.equals(modo)) {
                t.setTextAlign(Paint.Align.LEFT); t.setTextSize(14 * d); t.setLetterSpacing(.2f);
                t.setColor(skin.hielo); t.setAlpha(150);
                c.drawText("MANTÉN UNA APP PARA FIJARLA", 20 * d, listaArriba + 40 * d, t);
                t.setAlpha(255);
            } else if (USADAS.equals(modo)) {
                t.setTextAlign(Paint.Align.LEFT); t.setTextSize(14 * d); t.setLetterSpacing(.2f);
                t.setColor(skin.hielo); t.setAlpha(150);
                c.drawText("AÚN NO HAS ABIERTO APPS DESDE AQUÍ", 20 * d, listaArriba + 40 * d, t);
                t.setAlpha(255);
            }
            return false;
        }
        if (sel >= n) sel = n - 1;
        boolean tira = enLetras();
        int desde = 0;
        if (tira) {
            mueve |= pasoScroll(dt);
            desde = primeraVisible();
            n = filasQueCaben();
            if (sel < desde || sel >= n) sel = Math.max(desde, Math.min(n - 1, sel));
            // Los renglones que salen por arriba o abajo se cortan en la orilla de la lista
            c.save();
            c.clipRect(0, listaArriba - 4 * d, getWidth(), listaAbajo() + 4 * d);
        }

        t.setTextAlign(Paint.Align.LEFT);
        t.setLetterSpacing(-.01f);
        t.setTextSize(Math.min(30 * d, filaAlto * .47f));

        // Cursor dentado, detrás del renglón elegido
        AppEntry s = visibles.get(sel);
        float objY = listaArriba + sel * filaAlto + filaAlto / 2f;
        float objW = t.measureText(s.mayus) * 1.08f + SANGRIA[sel % 4] * d + (iconos ? 120 : 74) * d;
        if (cursorY.pos == 0) { cursorY.fija(objY); cursorAncho.fija(objW); }
        cursorY.objetivo = objY; cursorAncho.objetivo = objW;
        mueve |= cursorY.paso(dt, .5f);
        mueve |= cursorAncho.paso(dt, .5f);
        if (ahora - tDentado > 140) { nuevoDentado(); tDentado = ahora; }
        float cx = 4 * d, cw = cursorAncho.pos, ch = Math.min(70 * d, filaAlto + 6 * d), cy = cursorY.pos - desp() - ch / 2f;
        float ent0 = progresoEntrada(sel - desde, ahora);
        if (ent0 > .2f && levantada < 0) {
            c.save();
            c.translate(cx, cy);
            c.translate(9 * d, 7 * d); poligono(c, 0, 0, cw, ch, dentado, Color.BLACK); c.translate(-9 * d, -7 * d);
            poligono(c, 0, 0, cw, ch, dentado, skin.blanco);
            poligono(c, 0, 0, cw, ch, filo, skin.fuerte);
            c.restore();
        }

        for (int i = desde; i < n; i++) {
            AppEntry a = visibles.get(i);
            float prog = progresoEntrada(i - desde, ahora);
            if (prog < 1f || (ahora - tEntrada) < duracionEntrada + (i - desde) * 45 + 50) mueve = true;
            if (tira) {
                // En la tira cada renglón va pegado al desplazamiento, sin resorte propio
                fila(c, a, i, listaArriba + i * filaAlto + filaAlto / 2f - scroll, prog, i == sel, 0, 1);
                continue;
            }
            // Cada renglón va a su lugar con resorte: al reordenar, los demás se hacen a un lado con rebote
            Resorte r = filaY.get(a.clave);
            float objetivo = listaArriba + i * filaAlto + filaAlto / 2f;
            if (r == null) { r = new Resorte(420, .55f); r.fija(objetivo); filaY.put(a.clave, r); }
            r.objetivo = objetivo;
            mueve |= r.paso(dt, .5f);
            if (i == levantada) continue;
            fila(c, a, i, r.pos, prog, i == sel && levantada < 0, 0, 1);
        }
        if (levantada >= 0 && levantada < n) {
            mueve |= levEscala.paso(dt, .002f);
            fila(c, visibles.get(levantada), levantada, levY - levAgarre, 1, true, -3, levEscala.pos);
            mueve = true;
        }
        if (tira) c.restore();
        if (menuApp != null) { menuDibuja(c, ahora); mueve |= ahora - tMenu < 500; }
        return mueve;
    }

    /** Inercia del deslizamiento y viaje suave a una letra. true mientras se mueva. */
    private boolean pasoScroll(float dt) {
        if (desliza) return false;   // el dedo manda
        if (eligiendo && autoScroll != 0) {
            // Eligiendo con el dedo en la orilla: la tira avanza sola y el cursor sigue al dedo
            scroll = Math.max(0, Math.min(scrollMax(), scroll + autoScroll * 520 * d * Math.min(dt, 1f / 30f)));
            int j = filaEn(Math.min(eligeX, getWidth() - 80 * d), eligeY);
            if (j >= 0 && j != sel) { sel = j; cambio = true; performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); }
            return true;
        }
        float max = scrollMax();
        if (!Float.isNaN(scrollMeta)) {
            scroll += (scrollMeta - scroll) * Math.min(1, dt * 16);
            if (Math.abs(scrollMeta - scroll) < .5f) { scroll = scrollMeta; scrollMeta = Float.NaN; return false; }
            return true;
        }
        if (Math.abs(scrollVel) < 20 * d) { scrollVel = 0; return false; }
        scroll += scrollVel * Math.min(dt, 1f / 30f);
        scrollVel *= (float) Math.exp(-3.2f * dt);
        if (scroll < 0 || scroll > max) { scroll = Math.max(0, Math.min(max, scroll)); scrollVel = 0; }
        return true;
    }

    private float centroFila(int i) {
        if (enLetras()) return listaArriba + i * filaAlto + filaAlto / 2f - scroll;
        Resorte r = i < visibles.size() ? filaY.get(visibles.get(i).clave) : null;
        return r != null ? r.pos : listaArriba + i * filaAlto + filaAlto / 2f;
    }

    /** Un renglón: etiqueta con ícono (si están activos) y el nombre ladeado. */
    private void fila(Canvas c, AppEntry a, int i, float centro, float prog, boolean elegido, float giroExtra, float escala) {
        boolean levanta = escala > 1.001f;
        c.save();
        c.translate(18 * d + SANGRIA[i % 4] * d + (1 - prog) * -1.15f * getWidth() + (elegido ? 10 * d : 0), centro);
        c.rotate(ROT[i % 4] + giroExtra);
        c.skew((1 - prog) * -.58f, 0);
        if (elegido) c.scale(1.06f * escala, 1.06f * escala);

        if (levanta) {
            // Levantada: bloque con sombra dura, como carta de P5 en la mano
            float ancho = t.measureText(a.mayus) + (iconos ? 70 : 24) * d, alto = filaAlto * .9f;
            c.translate(8 * d, 8 * d);
            poligono(c, -6 * d, -alto / 2, ancho, alto, new float[]{0, .1f, 1, 0, .97f, 1, .02f, .92f}, Color.BLACK);
            c.translate(-8 * d, -8 * d);
            poligono(c, -6 * d, -alto / 2, ancho, alto, new float[]{0, .1f, 1, 0, .97f, 1, .02f, .92f}, skin.fuerte);
        }

        float tx = 6 * d;
        Carpeta cp = a.carpetaId != null ? carpeta(a.carpetaId) : null;
        if (cp != null) {
            // Carpeta: bloque del color fuerte con pestaña arriba y su inicial
            float lado = Math.min(34 * d, filaAlto * .55f);
            poligono(c, 6 * d, -lado / 2 - 5 * d, lado * .5f, 7 * d, new float[]{0, .2f, .8f, 0, 1, 1, 0, 1}, elegido && !levanta ? Color.BLACK : skin.hielo);
            poligono(c, 6 * d, -lado / 2, lado, lado, new float[]{.1f, 0, 1, .06f, .9f, 1, 0, .92f}, elegido && !levanta ? Color.BLACK : skin.fuerte);
            t.setTextAlign(Paint.Align.CENTER);
            float tam = t.getTextSize();
            t.setTextSize(lado * .5f);
            t.setColor(skin.blanco);
            c.drawText(String.valueOf(cp.inicial()), 6 * d + lado / 2, lado * .18f, t);
            t.setTextSize(tam);
            t.setTextAlign(Paint.Align.LEFT);
            tx = 6 * d + lado + 12 * d;
        } else if (iconos) {
            float lado = Math.min(34 * d, filaAlto * .55f);
            poligono(c, 6 * d, -lado / 2, lado, lado, new float[]{.1f, 0, 1, .06f, .9f, 1, 0, .92f},
                     elegido && !levanta ? Color.BLACK : skin.hielo);
            Bitmap ico = a.icono;
            if (ico != null) {
                float m = lado * .1f;
                rect.set(6 * d + m, -lado / 2 + m, 6 * d + lado - m, lado / 2 - m);
                c.drawBitmap(ico, null, rect, null);
            } else {
                t.setTextAlign(Paint.Align.CENTER);
                float tam = t.getTextSize();
                t.setTextSize(lado * .5f);
                t.setColor(elegido && !levanta ? skin.blanco : Color.BLACK);
                c.drawText(String.valueOf(a.letra), 6 * d + lado / 2, lado * .18f, t);
                t.setTextSize(tam);
                t.setTextAlign(Paint.Align.LEFT);
            }
            tx = 6 * d + lado + 12 * d;
        }

        float ty = t.getTextSize() * .36f;
        if (!elegido || levanta) {
            t.setColor(Color.BLACK); t.setAlpha(170);
            c.drawText(a.mayus, tx + 3 * d, ty + 3 * d, t);
            t.setAlpha(255);
        }
        t.setColor(elegido && !levanta ? Color.BLACK : skin.blanco);
        c.drawText(a.mayus, tx, ty, t);
        if (cp != null) {
            float tam = t.getTextSize(), esp = t.getLetterSpacing();
            t.setTextSize(10 * d); t.setLetterSpacing(.2f);
            t.setColor(elegido && !levanta ? Color.BLACK : skin.cian);
            c.drawText(appsDe(cp).size() + " APPS", tx + 2 * d, ty + 14 * d, t);
            t.setTextSize(tam); t.setLetterSpacing(esp);
        }
        if (!FAV.equals(modo) && clavesFav.contains(a.clave)) {
            float tam = t.getTextSize(), ancho = t.measureText(a.mayus);
            t.setTextSize(13 * d); t.setColor(skin.cian);
            c.drawText("★", tx + ancho + 4 * d, -tam * .3f, t);
            t.setTextSize(tam);
        }
        c.restore();
    }

    /** Arma el menú de una app según dónde está (favoritas, carpeta, letra/búsqueda). */
    private void abrirMenu(AppEntry a, float y) {
        menuApp = a;
        menuY = y;
        menuOps.clear();
        boolean fav = clavesFav.contains(a.clave);
        if (a.carpetaId != null) {
            menuOps.add(new String[]{"✕ QUITAR DEL INICIO", "F"});
            menuOps.add(new String[]{"✎ RENOMBRAR", "R"});
        } else if (FAV.equals(modo)) {
            menuOps.add(new String[]{"✕ QUITAR", "F"});
            menuOps.add(new String[]{"▣ A CARPETA ›", "C"});
            menuOps.add(new String[]{"⊘ OCULTAR APP", "O"});
        } else if (modo.startsWith(CARPETA)) {
            menuOps.add(new String[]{"↩ SACAR DE CARPETA", "S"});
            menuOps.add(new String[]{fav ? "✕ QUITAR DE FAVORITAS" : "★ FIJAR", "F"});
            menuOps.add(new String[]{"⊘ OCULTAR APP", "O"});
        } else {
            menuOps.add(new String[]{fav ? "✕ QUITAR DE FAVORITAS" : "★ FIJAR", "F"});
            menuOps.add(new String[]{"▣ A CARPETA ›", "C"});
            menuOps.add(new String[]{"⊘ OCULTAR APP", "O"});
        }
        tMenu = SystemClock.uptimeMillis();
        invalidate();
    }

    /** Segundo paso de "A CARPETA": lista de carpetas + nueva. */
    private void menuCarpetas() {
        menuOps.clear();
        for (Carpeta c : carpetas) if (!c.claves.contains(menuApp.clave)) menuOps.add(new String[]{"▣ " + c.nombre.toUpperCase(), "P" + c.id});
        menuOps.add(new String[]{"+ NUEVA CARPETA", "N"});
        tMenu = SystemClock.uptimeMillis();
        invalidate();
    }

    private void accionMenu(String acc) {
        AppEntry a = menuApp;
        menuApp = null;
        switch (acc.charAt(0)) {
            case 'F': oyente.fijar(a); break;
            case 'C': menuApp = a; menuCarpetas(); return;
            case 'P': oyente.aCarpeta(a, acc.substring(1)); break;
            case 'N': oyente.nuevaCarpeta(a); break;
            case 'S': oyente.sacarDeCarpeta(a, modo.substring(CARPETA.length())); break;
            case 'R': oyente.renombrarCarpeta(a.carpetaId); break;
            case 'O':
                oyente.ocultar(a);
                if (BUSCA.equals(modo)) { visibles.remove(a); sel = Math.min(sel, Math.max(0, visibles.size() - 1)); }
                else if (enLetras()) {
                    visibles.remove(a);
                    scroll = Math.min(scroll, scrollMax());
                    if (visibles.isEmpty()) pinta(FAV, true);   // era la última app visible
                }
                break;
        }
        invalidate();
    }

    /** Menú como pila de bloques P5 a la derecha del renglón, entrando uno tras otro. */
    private void menuDibuja(Canvas c, long ahora) {
        t.setTextAlign(Paint.Align.LEFT); t.setTextSize(16 * d); t.setLetterSpacing(.15f);
        float alto = 44 * d, hueco = 10 * d;
        float total = menuOps.size() * (alto + hueco) - hueco;
        float y = Math.max(insetArriba + 20 * d, Math.min(menuY - total / 2, getHeight() - insetAbajo - 20 * d - total));
        while (menuRects.size() < menuOps.size()) menuRects.add(new RectF());
        for (int i = 0; i < menuOps.size(); i++) {
            String txt = menuOps.get(i)[0];
            float f = Math.min(1, Math.max(0, (ahora - tMenu - i * 50) / 250f));
            float e = animaciones ? entra.getInterpolation(f) : 1;
            float ancho = t.measureText(txt) + 36 * d;
            float x = getWidth() - 80 * d - ancho - (i % 2) * 14 * d;
            menuRects.get(i).set(x - 10 * d, y - 4 * d, x + ancho + 10 * d, y + alto + 4 * d);
            c.save();
            c.translate(x + ancho / 2, y + alto / 2);
            c.rotate(i % 2 == 0 ? -5 : -3);
            c.scale(e, e);
            c.translate(-ancho / 2, -alto / 2);
            c.translate(5 * d, 5 * d); poligono(c, 0, 0, ancho, alto, new float[]{.04f, 0, 1, .12f, .95f, 1, 0, .88f}, Color.BLACK);
            c.translate(-5 * d, -5 * d); poligono(c, 0, 0, ancho, alto, new float[]{.04f, 0, 1, .12f, .95f, 1, 0, .88f}, i == 0 ? skin.fuerte : skin.blanco);
            t.setColor(i == 0 ? skin.blanco : Color.BLACK);
            c.drawText(txt, 18 * d, alto / 2 + 6 * d, t);
            c.restore();
            y += alto + hueco;
        }
        t.setLetterSpacing(-.01f);
    }

    private float progresoEntrada(int i, long ahora) {
        if (tEntrada == 0) return 1f;
        float f = (ahora - tEntrada - i * 45f) / duracionEntrada;
        if (f <= 0) return 0;
        if (f >= 1) return 1;
        return entra.getInterpolation(f);
    }

    private void nuevoDentado() {
        float[] base = {2, 10, 60, 2, 100, 14, 97, 62, 40, 68, 0, 58};
        for (int i = 0; i < base.length; i++) dentado[i] = (base[i] + (azar.nextFloat() * 2 - 1) * 3) / (i % 2 == 0 ? 100f : 70f);
        float[] f = {0, 56, 42, 66, 97, 60, 95, 70, -2, 70};
        for (int i = 0; i < f.length; i++) filo[i] = (f[i] + (azar.nextFloat() * 2 - 1) * 3) / (i % 2 == 0 ? 100f : 70f);
    }

    /** Polígono con puntos normalizados (0..1) dentro de un rectángulo. */
    private void poligono(Canvas c, float x, float y, float w, float h, float[] pts, int color) {
        path.reset();
        path.moveTo(x + pts[0] * w, y + pts[1] * h);
        for (int i = 2; i < pts.length; i += 2) path.lineTo(x + pts[i] * w, y + pts[i + 1] * h);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawPath(path, p);
    }

    // Columna compacta y recta a media pantalla, como Niagara: toda cabe bajo el pulgar.
    // (v0.5 la estiró a toda la altura en arco: con la A arriba y la Z abajo no se alcanzaba.)
    private static final float PASO_OLA = 19f;   // dp entre letras

    private float pasoOla() { return PASO_OLA * d; }

    // Debajo de las letras van las carpetas (por defecto viven aquí), separadas y más espaciadas
    private int nCarpetasOla() { return Math.min(carpetas.size(), MAX_CARPETAS_OLA); }

    private int totalOla() { return letras.length() + nCarpetasOla(); }

    private float yRel(int i) {
        int nl = letras.length();
        if (i < nl) return i * pasoOla();
        return (nl - 1) * pasoOla() + 24 * d + (i - nl) * 28 * d;
    }

    private float altoOla() { return yRel(totalOla() - 1); }

    private float olaArriba() {
        float centro = getHeight() * .56f;
        // Se centra contando el ⚙ de abajo, para que no caiga encima de la última carpeta
        return Math.max(insetArriba + 170 * d, centro - (altoOla() + 44 * d) / 2);
    }

    private float olaAbajo() { return olaArriba() + altoOla(); }

    private boolean ola(Canvas c, int w, int h, float dt) {
        boolean mueve = false;
        float arriba = olaArriba();
        t.setTextAlign(Paint.Align.RIGHT);
        t.setLetterSpacing(0);
        t.setTextSize(13 * d);
        int nl = letras.length();
        if (nCarpetasOla() > 0) {
            // Rayita que separa letras de carpetas
            p.setStyle(Paint.Style.FILL); p.setColor(skin.hielo); p.setAlpha(120);
            float ys = arriba + (nl - 1) * pasoOla() + 12 * d;
            c.drawRect(w - 26 * d, ys - 1 * d, w - 12 * d, ys + 1 * d, p);
            p.setAlpha(255);
        }
        for (int i = 0; i < totalOla(); i++) {
            mueve |= olaEscala[i].paso(dt, .002f);
            mueve |= olaX[i].paso(dt, .3f);
            if (i >= nl) {
                // Carpeta: cuadrito ladeado con su inicial
                Carpeta cp = carpetas.get(i - nl);
                float lado = 18 * d;
                c.save();
                c.translate(w - 14 * d - lado / 2 + olaX[i].pos, arriba + yRel(i));
                c.scale(olaEscala[i].pos, olaEscala[i].pos);
                c.rotate(-12);
                poligono(c, -lado / 2, -lado / 2, lado, lado, new float[]{.12f, 0, 1, .08f, .88f, 1, 0, .9f},
                         i == letraAct ? skin.blanco : skin.fuerte);
                t.setTextAlign(Paint.Align.CENTER); t.setTextSize(11 * d);
                t.setColor(i == letraAct ? Color.BLACK : skin.blanco);
                c.drawText(String.valueOf(cp.inicial()), 0, 4 * d, t);
                t.setTextAlign(Paint.Align.RIGHT); t.setTextSize(13 * d);
                c.restore();
                continue;
            }
            String l = String.valueOf(letras.charAt(i));
            c.save();
            c.translate(w - 14 * d + olaX[i].pos, arriba + yRel(i));
            c.scale(olaEscala[i].pos, olaEscala[i].pos);
            c.rotate(-12);
            c.skew(-.18f, 0);
            if (i == letraAct) {
                t.setColor(Color.BLACK); c.drawText(l, 5 * d / olaEscala[i].pos, 9 * d, t);
                t.setColor(skin.fuerte); c.drawText(l, 3 * d / olaEscala[i].pos, 7 * d, t);
                t.setColor(skin.blanco);
            } else {
                t.setColor(skin.hielo); t.setAlpha(180);
            }
            c.drawText(l, 0, 5 * d, t);
            t.setAlpha(255);
            c.restore();
        }

        if (!BUSCA.equals(modo)) engranaje(c, w - 20 * d, engranajeY(), 9 * d);

        mueve |= burbuja.paso(dt, .002f);
        if (burbuja.pos > .02f && letraAct >= 0) {
            float lado = 96 * d, e = burbuja.pos;
            c.save();
            c.translate(w - 150 * d - lado / 2, dedoY);
            c.rotate(-30 + 22 * Math.min(1, e));
            c.scale(e, e);
            poligono(c, -lado / 2, -lado / 2, lado, lado, new float[]{.14f, 0, 1, .1f, .86f, 1, 0, .86f}, skin.fuerte);
            t.setTextAlign(Paint.Align.CENTER);
            t.setTextSize(54 * d);
            String l = letraAct < letras.length() ? String.valueOf(letras.charAt(letraAct))
                    : letraAct - letras.length() < carpetas.size() ? String.valueOf(carpetas.get(letraAct - letras.length()).inicial()) : "";
            t.setColor(Color.BLACK); c.drawText(l, 4 * d, 22 * d, t);
            t.setColor(skin.blanco); c.drawText(l, 0, 18 * d, t);
            c.restore();
        }
        return mueve;
    }

    /** Debajo de la ola (y de sus carpetas), sin salirse por abajo de la pantalla. */
    private float engranajeY() { return Math.min(olaAbajo() + 44 * d, getHeight() - insetAbajo - 28 * d); }

    private Path engranajePath;   // centrado en (0,0); se arma una vez, no en cada cuadro

    /** ⚙ dibujado a mano (el carácter cambia de fuente/emoji según el teléfono): abre Opciones. */
    private void engranaje(Canvas c, float cx, float cy, float r) {
        if (engranajePath == null) {
            Path dientes = new Path();
            for (int k = 0; k < 8; k++) {
                double a = Math.toRadians(k * 45 + 10);
                float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
                float x1 = dx * r * .75f, y1 = dy * r * .75f, x2 = dx * r * 1.35f, y2 = dy * r * 1.35f;
                float ex = -dy * r * .26f, ey = dx * r * .26f;
                dientes.moveTo(x1 + ex, y1 + ey); dientes.lineTo(x2 + ex, y2 + ey);
                dientes.lineTo(x2 - ex, y2 - ey); dientes.lineTo(x1 - ex, y1 - ey); dientes.close();
            }
            // Cada figura en su propio Path: en uno solo los traslapes se anulan entre sí
            Path g = new Path();
            g.addCircle(0, 0, r, Path.Direction.CW);
            g.op(dientes, Path.Op.UNION);
            Path hoyo = new Path();
            hoyo.addCircle(0, 0, r * .42f, Path.Direction.CW);
            g.op(hoyo, Path.Op.DIFFERENCE);
            engranajePath = g;
        }
        p.setStyle(Paint.Style.FILL);
        c.save();
        c.translate(cx + 2 * d, cy + 2 * d); p.setColor(Color.BLACK); p.setAlpha(150); c.drawPath(engranajePath, p);
        c.translate(-2 * d, -2 * d); p.setColor(engranajeToque ? skin.fuerte : skin.blanco); p.setAlpha(230);
        c.drawPath(engranajePath, p);
        c.restore();
        p.setAlpha(255);
    }

    private boolean avisoDibuja(Canvas c, int w, int h, long ahora, float dt) {
        if (aviso == null) return false;
        long edad = ahora - tAviso;
        if (edad > 1300 && avisoEscala.objetivo != 0) avisoEscala.objetivo = 0;
        boolean mueve = avisoEscala.paso(dt, .002f);
        if (edad > 1300 && !mueve) { aviso = null; return false; }
        t.setTextAlign(Paint.Align.CENTER); t.setTextSize(15 * d); t.setLetterSpacing(.15f);
        float ancho = t.measureText(aviso) + 44 * d, alto = 40 * d;
        c.save();
        c.translate(w / 2f, h - insetAbajo - 110 * d);
        c.rotate(-4);
        c.scale(avisoEscala.pos, avisoEscala.pos);
        poligono(c, -ancho / 2, -alto / 2, ancho, alto, new float[]{.04f, 0, 1, .1f, .96f, 1, 0, .86f}, skin.blanco);
        t.setColor(Color.BLACK);
        c.drawText(aviso, 0, 5 * d, t);
        c.restore();
        return true;
    }

    /** Panel de opciones con entrada de menú de Persona: título gigante y bloques que llegan de lado. */
    private boolean opcionesDibuja(Canvas c, int w, int h, long ahora) {
        long edad = tOpc == 0 ? 10000 : ahora - tOpc;
        boolean mueve = edad < 900;
        float fondo = Math.min(1, edad / 200f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.BLACK); p.setAlpha((int) (235 * fondo));
        c.drawRect(0, 0, w, h, p);
        p.setAlpha(255);
        // Franja diagonal del color fuerte
        float fb = seco.getInterpolation(Math.min(1, edad / 350f));
        c.save();
        c.translate(0, insetArriba + 60 * d);
        c.rotate(-12);
        p.setColor(skin.fuerte);
        c.drawRect(-w + fb * w, 0, fb * w * 1.6f, 70 * d, p);
        c.restore();

        String tituloOpc = opcPagina == 0 ? "OPCIONES" : opcPagina == 1 ? "CARPETAS" : opcPagina == 3 ? "OCULTAS"
                : carpeta(opcCarpeta) != null ? carpeta(opcCarpeta).nombre.toUpperCase() : "CARPETA";
        float ft = entra.getInterpolation(Math.min(1, edad / 450f));
        c.save();
        c.translate(22 * d - (1 - ft) * w, insetArriba + 110 * d);
        c.rotate(-8);
        t.setTextAlign(Paint.Align.LEFT); t.setLetterSpacing(-.03f);
        t.setTextSize(Math.min(58 * d, (w - 60 * d) * 1.5f / Math.max(6, tituloOpc.length())));
        t.setColor(Color.BLACK); c.drawText(tituloOpc, 6 * d, 6 * d, t);
        t.setColor(skin.blanco); c.drawText(tituloOpc, 0, 0, t);
        c.restore();

        List<String[]> filas = filasOpciones();
        float y = insetArriba + 190 * d;
        // Con muchas filas se aprietan para no tapar el pie (RAM / versión)
        float paso = Math.min(78 * d, (h - insetAbajo - 64 * d - y) / Math.max(1, Math.min(filas.size(), opcRect.length)));
        float alto = Math.min(64 * d, paso - 8 * d);
        for (int i = 0; i < opcRect.length; i++) opcRect[i].setEmpty();
        for (int i = 0; i < filas.size() && i < opcRect.length; i++) {
            float f = (edad - 120 - i * 70) / 420f;
            float e = f <= 0 ? 0 : f >= 1 ? 1 : entra.getInterpolation(f);
            float x = 18 * d + (1 - e) * w * 1.2f + (i % 2) * 18 * d;
            float ancho = w - 60 * d - (i % 2) * 18 * d;
            opcRect[i].set(18 * d, y, w - 18 * d, y + alto);
            c.save();
            c.translate(x, y);
            c.rotate(i % 2 == 0 ? -3 : -1.5f);
            c.translate(7 * d, 7 * d); poligono(c, 0, 0, ancho, alto, new float[]{0, .08f, 1, 0, .98f, 1, .01f, .9f}, skin.fuerte);
            c.translate(-7 * d, -7 * d); poligono(c, 0, 0, ancho, alto, new float[]{0, .08f, 1, 0, .98f, 1, .01f, .9f}, skin.blanco);
            t.setColor(skin.fuerte); t.setTextSize(12 * d); t.setLetterSpacing(.3f);
            c.drawText(filas.get(i)[0], 18 * d, 25 * d, t);
            t.setColor(Color.BLACK); t.setTextSize(22 * d); t.setLetterSpacing(-.01f);
            c.drawText(filas.get(i)[1], 18 * d, 53 * d, t);
            c.restore();
            y += paso;
        }
        t.setColor(skin.hielo); t.setAlpha(140); t.setTextSize(11 * d); t.setLetterSpacing(.25f);
        c.drawText("RAM " + ramMb + " MB  ·  EN REPOSO " + cuadrosAntes + " CUADROS/S", 22 * d, h - insetAbajo - 44 * d, t);
        c.drawText("VERSIÓN " + opcVersion + "  ·  TOCA FUERA PARA CERRAR", 22 * d, h - insetAbajo - 26 * d, t);
        t.setAlpha(255);
        return mueve;
    }

    private boolean corteDibuja(Canvas c, int w, int h, long ahora) {
        if (corte == NADA) return false;
        long edad = ahora - tCorte;
        float b1, b2; // 0 = fuera por la izquierda, 1 = cubre, 2 = fuera por la derecha
        boolean mueve = true;
        if (corte == VA) {
            b1 = seco.getInterpolation(Math.min(1, edad / 320f));
            b2 = seco.getInterpolation(Math.max(0, Math.min(1, (edad - 70) / 320f)));
            if (edad > 400) {
                corte = CUBIERTO;
                if (alCubrir != null) { Runnable r = alCubrir; alCubrir = null; ui.post(r); }
            }
        } else if (corte == CUBIERTO) {
            b1 = b2 = 1; mueve = false;
        } else {
            b2 = 1 + seco.getInterpolation(Math.min(1, edad / 350f));
            b1 = 1 + seco.getInterpolation(Math.max(0, Math.min(1, (edad - 70) / 350f)));
            if (edad > 430) { corte = NADA; return false; }
        }
        banda(c, w, h, b1, Color.BLACK);
        banda(c, w, h, b2, skin.fuerte);
        if (corte == CUBIERTO || (corte == VA && b2 > .95f)) {
            c.save();
            c.translate(24 * d, h * .48f);
            c.rotate(-7);
            t.setTextAlign(Paint.Align.LEFT); t.setLetterSpacing(-.01f);
            t.setTextSize(Math.min(58 * d, (w - 48 * d) / Math.max(4, corteNombre.length()) * 1.6f));
            t.setColor(Color.BLACK); c.drawText(corteNombre, 6 * d, 6 * d, t);
            t.setColor(skin.blanco); c.drawText(corteNombre, 0, 0, t);
            c.restore();
        }
        return mueve;
    }

    /** Banda en diagonal (skewX -24°). f: 0 fuera izq · 1 cubre · 2 fuera der. */
    private void banda(Canvas c, int w, int h, float f, int color) {
        if (f <= 0 || f >= 2) return;
        float ancho = w * 1.4f, sesgo = (float) Math.tan(Math.toRadians(24)) * h;
        float x = (f - 1) * (ancho + sesgo);
        path.reset();
        path.moveTo(x - w * .2f + sesgo, -h * .2f);
        path.lineTo(x + ancho + sesgo, -h * .2f);
        path.lineTo(x + ancho, h * 1.2f);
        path.lineTo(x - w * .2f - sesgo, h * 1.2f);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawPath(path, p);
    }

    private boolean calendarioDibuja(Canvas c, int w, int h, long ahora, float dt) {
        if (tCal < 0) return false;
        long edad = ahora - tCal;
        if (edad > 2750) { tCal = -1; return false; }
        float alfa = edad > 2400 ? 1 - (edad - 2400) / 350f : 1;
        c.saveLayerAlpha(0, 0, w, h, (int) (255 * Math.max(0, alfa)));
        c.drawColor(Color.BLACK);

        float celda = 92 * d, hueco = 22 * d, centroY = h * .45f;
        float destino = w / 2f - (6 * (celda + hueco) + celda / 2);
        float corrido = destino * frena.getInterpolation(Math.min(1, edad / 1100f));
        if (edad > 1150 && calHoy.objetivo != 2.2f) {
            calHoy.objetivo = 2.2f;
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        calHoy.paso(dt, .002f);

        Calendar k = Calendar.getInstance();
        k.add(Calendar.DAY_OF_MONTH, -6);
        for (int i = 0; i < 8; i++) {
            boolean hoy = i == 6;
            float x = corrido + i * (celda + hueco) + celda / 2;
            c.save();
            c.translate(x, centroY);
            c.rotate(-8);
            float e = hoy ? calHoy.pos : 1;
            c.scale(e, e);
            t.setTextAlign(Paint.Align.CENTER); t.setLetterSpacing(0);
            t.setTextSize(54 * d);
            String num = String.valueOf(k.get(Calendar.DAY_OF_MONTH));
            if (hoy && edad > 1150) { t.setColor(skin.fuerte); c.drawText(num, 4 * d, 4 * d, t); t.setColor(skin.blanco); }
            else { t.setColor(skin.hielo); t.setAlpha(115); }
            c.drawText(num, 0, 0, t);
            t.setTextSize(12 * d); t.setLetterSpacing(.2f);
            c.drawText(DIAS[k.get(Calendar.DAY_OF_WEEK) - 1].substring(0, 3), 0, 18 * d, t);
            t.setAlpha(255);
            c.restore();
            k.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (edad > 1150) {
            float f = Math.min(1, (edad - 1150) / 400f);
            String hoyTxt = DIAS[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1];
            c.save();
            c.translate(w / 2f, centroY + 150 * d + (1 - entra.getInterpolation(f)) * 20 * d);
            c.rotate(-4);
            t.setTextSize(16 * d); t.setLetterSpacing(.3f);
            float a1 = t.measureText("HOY · "), a2 = t.measureText(hoyTxt);
            float x = -(a1 + a2 + 20 * d) / 2;
            t.setTextAlign(Paint.Align.LEFT);
            t.setColor(skin.blanco); t.setAlpha((int) (255 * f));
            c.drawText("HOY · ", x, 0, t);
            p.setColor(skin.fuerte); p.setAlpha((int) (255 * f));
            c.drawRect(x + a1, -18 * d, x + a1 + a2 + 20 * d, 8 * d, p);
            p.setAlpha(255);
            c.drawText(hoyTxt, x + a1 + 10 * d, 0, t);
            t.setAlpha(255);
            c.restore();
        }
        c.restore();
        return true;
    }

    // ───────────────────────── Toque ─────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        int accion = e.getActionMasked();

        // Capas encima de la lista: se llevan el toque completo
        if (opciones) {
            if (accion == MotionEvent.ACTION_UP) {
                // La soltada del mismo dedo que abrió Opciones (mantener) no cuenta como toque:
                // antes caía "fuera" de las filas y cerraba el menú al instante (v0.4)
                if (largoHecho) largoHecho = false;
                else tocaOpciones(x, y);
            }
            return true;
        }
        if (menuApp != null) {
            if (accion == MotionEvent.ACTION_UP) {
                if (largoHecho) { largoHecho = false; return true; }   // soltada del dedo que lo abrió
                for (int k = 0; k < menuOps.size() && k < menuRects.size(); k++) {
                    if (menuRects.get(k).contains(x, y)) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        accionMenu(menuOps.get(k)[1]);
                        return true;
                    }
                }
                menuApp = null;
                invalidate();
            }
            return true;
        }

        switch (accion) {
            case MotionEvent.ACTION_DOWN:
                x0 = x; y0 = y; arrastre = false; cambio = false; largoHecho = false; pulso = -1;
                desliza = false;
                tiraToque = enLetras() && x < getWidth() - 72 * d && y >= listaArriba && y <= listaAbajo();
                if (tiraToque) {
                    // Tocar la tira mientras corre solo la frena; no abre lo que pasaba debajo
                    boolean corria = Math.abs(scrollVel) > 300 * d || !Float.isNaN(scrollMeta);
                    scroll0 = scroll; scrollVel = 0; scrollMeta = Float.NaN;
                    if (vt == null) vt = android.view.VelocityTracker.obtain(); else vt.clear();
                    vt.addMovement(e);
                    if (corria) { largoHecho = true; return true; }
                    // Abrir al soltar: sobre un nombre el dedo elige en vez de desplazar
                    if (abrirAlSoltar && filaEn(x, y) >= 0) tiraToque = false;
                }
                if (tCal >= 0) { tCal = Math.min(tCal, SystemClock.uptimeMillis() - 2400); return true; }
                if (corte != NADA) return true;
                if (!BUSCA.equals(modo) && x > getWidth() - 56 * d && Math.abs(y - engranajeY()) < 24 * d) {
                    engranajeToque = true;
                    invalidate();
                    return true;
                }
                if (x > getWidth() - 72 * d && y > olaArriba() - 24 * d && y < olaAbajo() + 24 * d) {
                    olaActiva = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    olaMueve(y);
                    return true;
                }
                int i = filaEn(x, y);
                eligiendo = false; autoScroll = 0;
                if (i >= 0) {
                    pulso = i;
                    if (abrirAlSoltar) { eligiendo = true; eligeOrigen = i; eligeX = x; eligeY = y; }
                    if (sel != i) { sel = i; performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); }
                    ui.postDelayed(largoItem, 550);
                } else if (!BUSCA.equals(modo)) {
                    // Mantener en la hora o en cualquier espacio vacío → Opciones
                    ui.postDelayed(largoVacio, 600);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (engranajeToque) return true;
                if (olaActiva) { olaMueve(y); return true; }
                if (levantada >= 0) { mueveLevantada(y); return true; }
                if (Math.abs(y - y0) > 12 * d || Math.abs(x - x0) > 12 * d) {
                    if (!arrastre) { ui.removeCallbacks(largoItem); ui.removeCallbacks(largoVacio); }
                    arrastre = true;
                }
                if (tiraToque) {
                    // En la tira el dedo la desplaza (no mueve el cursor) y se puede pasar de letra
                    vt.addMovement(e);
                    if (!desliza && arrastre && Math.abs(y - y0) > Math.abs(x - x0)) { desliza = true; y0 = y; scroll0 = scroll; }
                    if (desliza) { scroll = Math.max(0, Math.min(scrollMax(), scroll0 - (y - y0))); invalidate(); }
                    return true;
                }
                if (eligiendo && enLetras()) {
                    eligeX = x; eligeY = y;
                    int nuevo = y < listaArriba + filaAlto * .6f ? -1 : y > listaAbajo() - filaAlto * .6f ? 1 : 0;
                    if (nuevo != autoScroll) { autoScroll = nuevo; invalidate(); }
                }
                if (pulso >= 0) {
                    // Pasar el dedo mueve el cursor; abre solo con toque, o al soltar si la opción está
                    int j = filaEn(Math.min(x, getWidth() - 80 * d), y);
                    if (j >= 0 && j != sel) {
                        sel = j; cambio = true;
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        invalidate();
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                ui.removeCallbacks(largoItem); ui.removeCallbacks(largoVacio);
                if (engranajeToque) {
                    engranajeToque = false;
                    if (x > getWidth() - 72 * d && Math.abs(y - engranajeY()) < 40 * d) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        abrirOpciones();
                    }
                    invalidate();
                    return true;
                }
                if (olaActiva) { olaSuelta(); return true; }
                if (levantada >= 0) { sueltaLevantada(); return true; }
                if (desliza && scroll0 <= 0 && y - y0 > 70 * d) {
                    // Ya estaba hasta arriba y se siguió jalando ↓: buscar, como en inicio
                    desliza = false; tiraToque = false; pulso = -1;
                    oyente.abrirBusqueda();
                    return true;
                }
                if (desliza) {
                    // Se suelta con impulso: la tira sigue de largo y frena sola
                    vt.addMovement(e);
                    vt.computeCurrentVelocity(1000);
                    scrollVel = -vt.getYVelocity();
                    desliza = false; tiraToque = false; pulso = -1;
                    invalidate();
                    return true;
                }
                tiraToque = false;
                autoScroll = 0;
                if (largoHecho) { eligiendo = false; return true; }
                if (eligiendo && arrastre) {
                    // Abrir al soltar: la app bajo el dedo. Fuera de la lista o de vuelta en la de
                    // salida = arrepentirse, no abre nada
                    eligiendo = false; pulso = -1;
                    int j = filaEn(Math.min(x, getWidth() - 80 * d), y);
                    if (j >= 0 && j != eligeOrigen && j < visibles.size()) {
                        AppEntry a = visibles.get(j);
                        sel = j;
                        if (a.carpetaId != null) pinta(CARPETA + a.carpetaId, true);
                        else oyente.abrir(a);
                    }
                    invalidate();
                    return true;
                }
                eligiendo = false;
                if (pulso >= 0 && !arrastre && sel < visibles.size()) {
                    pulso = -1;
                    AppEntry a = visibles.get(sel);
                    if (a.carpetaId != null) pinta(CARPETA + a.carpetaId, true);
                    else oyente.abrir(a);
                    return true;
                }
                boolean desdeFila = pulso >= 0;
                pulso = -1;
                if (!arrastre && !BUSCA.equals(modo) && y < insetArriba + 150 * d && x < getWidth() * .75f) {
                    oyente.abrirCalendario();   // tocar hora/fecha
                    return true;
                }
                if (wallpaper && !arrastre && !desdeFila) tocaWallpaper(x, y);
                if (BUSCA.equals(modo)) {
                    if (!arrastre) oyente.cerrarBusqueda();
                } else if (y - y0 > 70 * d && Math.abs(y - y0) > Math.abs(x - x0)) {
                    oyente.abrirBusqueda();   // ↓ desde cualquier parte (también sobre una app)
                } else if (y0 - y > 70 * d && Math.abs(y - y0) > Math.abs(x - x0)) {
                    if (!USADAS.equals(modo)) pinta(USADAS, true);   // ↑ = las más usadas
                } else if (!FAV.equals(modo) && !arrastre) {
                    pinta(FAV, true);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                ui.removeCallbacks(largoItem); ui.removeCallbacks(largoVacio);
                pulso = -1;
                desliza = false; tiraToque = false; engranajeToque = false; eligiendo = false; autoScroll = 0;
                if (olaActiva) olaSuelta();
                if (levantada >= 0) { levantada = -1; pinta(modo, false); }
                return true;
        }
        return super.onTouchEvent(e);
    }

    /** Los wallpapers interactivos (Wallpaper Engine) reaccionan al toque en espacio vacío. */
    private void tocaWallpaper(float x, float y) {
        try {
            int[] pos = new int[2];
            getLocationOnScreen(pos);
            android.app.WallpaperManager.getInstance(getContext()).sendWallpaperCommand(
                getWindowToken(), android.app.WallpaperManager.COMMAND_TAP, (int) x + pos[0], (int) y + pos[1], 0, null);
        } catch (Exception ignorado) { }
    }

    /** Renglones {etiqueta, valor} de la página actual de Opciones. */
    private List<String[]> filasOpciones() {
        List<String[]> l = new ArrayList<>();
        if (opcPagina == 0) {
            l.add(new String[]{"SKIN", opcSkin});
            l.add(new String[]{"ÍCONOS", iconos ? "SÍ" : "NO · SOLO TEXTO"});
            l.add(new String[]{"FONDO", wallpaper ? "WALLPAPER DEL TELÉFONO" : "ANIMADO"});
            l.add(new String[]{"WALLPAPER · AHORA: " + wallpaperActual, "CAMBIAR ›"});
            l.add(new String[]{"CARPETAS", carpetas.size() + " · ADMINISTRAR ›"});
            l.add(new String[]{"APPS OCULTAS", ocultas.size() + " · ADMINISTRAR ›"});
            l.add(new String[]{"PANTALLA DE INICIO", "ELEGIR LAUNCHER ›"});
            l.add(new String[]{"AL ABRIR UNA APP", franjaCompleta ? "FRANJA COMPLETA" : "AL INSTANTE"});
            l.add(new String[]{"ABRIR AL SOLTAR", abrirAlSoltar ? "SÍ · DESLIZA Y SUELTA" : "NO · SOLO CON TOQUE"});
        } else if (opcPagina == 3) {
            for (int i = 0; i < ocultas.size() && i < MAX_OCULTAS; i++)
                l.add(new String[]{"OCULTA · TOCA PARA MOSTRAR", ocultas.get(i).mayus});
            if (ocultas.isEmpty()) l.add(new String[]{"NINGUNA", "MANTÉN UNA APP › OCULTAR"});
            l.add(new String[]{ocultas.size() > MAX_OCULTAS ? "+" + (ocultas.size() - MAX_OCULTAS) + " MÁS" : "", "‹ REGRESAR"});
        } else if (opcPagina == 1) {
            for (int i = 0; i < carpetas.size() && i < 5; i++) {
                Carpeta c = carpetas.get(i);
                l.add(new String[]{"CARPETA" + (clavesFav.contains(c.clave()) ? " · EN INICIO" : ""),
                                   c.nombre.toUpperCase() + " · " + appsDe(c).size() + " ›"});
            }
            l.add(new String[]{"", "+ NUEVA CARPETA"});
            l.add(new String[]{"", "‹ REGRESAR"});
        } else {
            Carpeta c = carpeta(opcCarpeta);
            boolean inicio = c != null && clavesFav.contains(c.clave());
            l.add(new String[]{"EN INICIO", inicio ? "SÍ · TAMBIÉN EN FAVORITAS" : "NO · SOLO EN LA COLUMNA"});
            l.add(new String[]{"NOMBRE", (c != null ? c.nombre.toUpperCase() : "") + " ✎"});
            l.add(new String[]{"BORRAR", opcConfirmaBorrar ? "¿SEGURO? TOCA OTRA VEZ" : "BORRAR CARPETA"});
            l.add(new String[]{"", "‹ REGRESAR"});
        }
        return l;
    }

    private void tocaOpciones(float x, float y) {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        int i = -1;
        for (int k = 0; k < opcRect.length; k++) if (opcRect[k].contains(x, y)) { i = k; break; }
        if (i < 0) { opciones = false; invalidate(); return; }
        int pagAntes = opcPagina;
        if (opcPagina == 0) {
            if (i == 0) oyente.cambiarSkin();
            else if (i == 1) oyente.alternarIconos();
            else if (i == 2) oyente.alternarFondo();
            else if (i == 3) { opciones = false; oyente.cambiarWallpaper(); }
            else if (i == 4) opcPagina = 1;
            else if (i == 5) opcPagina = 3;
            else if (i == 6) { opciones = false; oyente.ajustesInicio(); }
            else if (i == 7) oyente.alternarFranja();
            else if (i == 8) oyente.alternarSoltar();
        } else if (opcPagina == 3) {
            int n = Math.min(ocultas.size(), MAX_OCULTAS);
            if (i < n) oyente.mostrar(ocultas.get(i).clave);   // la página se queda: sale la siguiente
            else if (ocultas.isEmpty() && i == 0) { }
            else opcPagina = 0;
        } else if (opcPagina == 1) {
            int n = Math.min(carpetas.size(), 5);
            if (i < n) { opcCarpeta = carpetas.get(i).id; opcPagina = 2; opcConfirmaBorrar = false; }
            else if (i == n) { opciones = false; oyente.nuevaCarpeta(null); }
            else opcPagina = 0;
        } else {
            if (i == 0) oyente.alternarCarpetaInicio(opcCarpeta);
            else if (i == 1) { opciones = false; oyente.renombrarCarpeta(opcCarpeta); }
            else if (i == 2) {
                if (opcConfirmaBorrar) { oyente.borrarCarpeta(opcCarpeta); opcPagina = 1; opcConfirmaBorrar = false; }
                else opcConfirmaBorrar = true;
            }
            else opcPagina = 1;
        }
        if (opcPagina != pagAntes) tOpc = animaciones ? SystemClock.uptimeMillis() : 0;
        invalidate();
    }

    private void mueveLevantada(float y) {
        levY = y;
        int n = filasQueCaben();
        int destino = (int) Math.floor((y - levAgarre - listaArriba) / filaAlto);
        destino = Math.max(0, Math.min(n - 1, destino));
        if (destino != levantada) {
            visibles.add(destino, visibles.remove(levantada));
            levantada = destino;
            sel = destino;
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        invalidate();
    }

    private void sueltaLevantada() {
        int fin = levantada;
        levantada = -1;
        // La fila suelta arranca donde quedó el dedo y "aterriza" con resorte
        Resorte r = filaY.get(visibles.get(fin).clave);
        if (r != null) r.pos = levY - levAgarre;
        largoHecho = false;   // esta soltada ya se usó; la siguiente es un toque normal
        if (fin != levOrigen) {
            if (modo.startsWith(CARPETA)) oyente.reordenarCarpeta(modo.substring(CARPETA.length()), new ArrayList<>(visibles));
            else oyente.reordenar(new ArrayList<>(visibles));
        } else {
            abrirMenu(visibles.get(fin), listaArriba + fin * filaAlto + filaAlto / 2f);
        }
        invalidate();
    }

    private void olaMueve(float y) {
        dedoY = y;
        float arriba = olaArriba();
        float sigma = 48 * d;
        int mejor = 0; float dmin = Float.MAX_VALUE;
        for (int i = 0; i < totalOla(); i++) {
            float dist = Math.abs(y - (arriba + yRel(i)));
            // Campana: las letras cerca del dedo se inflan y salen hacia la izquierda
            float g = (float) Math.exp(-(dist * dist) / (2 * sigma * sigma));
            olaEscala[i].objetivo = 1 + 1.9f * g;
            olaX[i].objetivo = -62 * d * g;
            if (dist < dmin) { dmin = dist; mejor = i; }
        }
        if (mejor != letraAct) {
            letraAct = mejor;
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            burbuja.pos = .3f; burbuja.vel = 0; burbuja.objetivo = 1;
            if (mejor >= letras.length()) {
                pinta(CARPETA + carpetas.get(mejor - letras.length()).id, true);
            } else {
                char l = letras.charAt(mejor);
                if (l == FAVS_LETRA) { if (!FAV.equals(modo)) pinta(FAV, true); }
                else if (enLetras()) { modo = String.valueOf(l); irALetra(l, true); }   // misma tira: solo se desliza
                else pinta(String.valueOf(l), true);   // solo hay letras con apps
            }
        }
        invalidate();
    }

    private void olaSuelta() {
        olaActiva = false;
        burbuja.objetivo = 0;
        for (int i = 0; i < totalOla(); i++) { olaEscala[i].objetivo = 1; olaX[i].objetivo = 0; }
        ui.postDelayed(() -> { if (!olaActiva) { letraAct = -1; invalidate(); } }, 300);
        invalidate();
    }

    /** Android pide memoria: se tira lo que se puede rehacer al vuelo (la trama de puntos ~4 MB). */
    void soltarMemoria() { puntos = null; rayos = null; engranajePath = null; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        rayos = null;
        puntos = null;
    }
}
