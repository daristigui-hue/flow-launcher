package mx.kuoz.launcher;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements LauncherView.Oyente {

    // Favoritas de arranque (si están instaladas). Luego mandan las que el usuario fije.
    private static final String[] FAV_INICIALES = {
        "org.telegram.messenger", "com.whatsapp", "com.microsoft.office.outlook",
        "com.anthropic.claude", "com.termux", "com.google.android.GoogleCamera", "com.spotify.music",
    };

    private LauncherView vista;
    private EditText busca;
    private FrameLayout raiz;
    private SharedPreferences prefs;
    private LauncherApps launcherApps;
    private UserManager usuarios;
    private Skin skin;
    private final ExecutorService fondo = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private List<AppEntry> todas = new ArrayList<>();      // todas las del teléfono, ocultas incluidas
    private List<AppEntry> mostradas = new ArrayList<>();  // las que ve la vista (sin ocultas)
    private final Set<String> ocultas = new HashSet<>();
    private List<Carpeta> carpetas = new ArrayList<>();

    // Campo para escribir el nombre de una carpeta (con velo oscuro detrás)
    interface AlTexto { void listo(String texto); }
    private EditText campo;
    private View velo;
    private AlTexto alTexto;

    // Clases internas SIEMPRE static (y anónimas, ninguna): el javac 21 de Debian les pone
    // al constructor un parámetro "sin nombre" (MethodParameters) y el d8 de build-tools 34
    // truena al leerlo (NPE en String.length). Las lambdas no tienen el problema.
    private static final class CambiosDeApps extends LauncherApps.Callback {
        private final MainActivity a;
        CambiosDeApps(MainActivity a) { this.a = a; }
        @Override public void onPackageRemoved(String p, UserHandle u) { a.cargarApps(); }
        @Override public void onPackageAdded(String p, UserHandle u) { a.cargarApps(); }
        @Override public void onPackageChanged(String p, UserHandle u) { a.cargarApps(); }
        @Override public void onPackagesAvailable(String[] p, UserHandle u, boolean r) { a.cargarApps(); }
        @Override public void onPackagesUnavailable(String[] p, UserHandle u, boolean r) { a.cargarApps(); }
    }

    private static final class AlEscribir implements TextWatcher {
        private final MainActivity a;
        AlEscribir(MainActivity a) { this.a = a; }
        @Override public void beforeTextChanged(CharSequence s, int x, int y, int z) { }
        @Override public void onTextChanged(CharSequence s, int x, int y, int z) { }
        @Override public void afterTextChanged(Editable s) {
            if (a.busca.getVisibility() == View.VISIBLE) a.vista.filtrar(s.toString());
        }
    }

    private final CambiosDeApps cambiosDeApps = new CambiosDeApps(this);

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        Crash.instalar(this);
        getWindow().setDecorFitsSystemWindows(false);

        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        launcherApps = getSystemService(LauncherApps.class);
        usuarios = getSystemService(UserManager.class);
        skin = Skin.por(prefs.getString("skin", "p5"));

        raiz = new FrameLayout(this);
        vista = new LauncherView(this, skin, this);
        vista.setIconos(prefs.getBoolean("iconos", true));
        aplicarFondo();
        vista.setOpciones(skin.nombre, version());
        raiz.addView(vista, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        busca = new EditText(this);
        final float d = getResources().getDisplayMetrics().density;
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (64 * d));
        lp.leftMargin = lp.rightMargin = (int) (18 * d);
        lp.topMargin = (int) (40 * d);
        estiloBusca();
        busca.setVisibility(View.GONE);
        raiz.addView(busca, lp);

        String ocu = prefs.getString("ocultas", "");
        if (!ocu.isEmpty()) Collections.addAll(ocultas, ocu.split("\n"));
        carpetas = Carpeta.deJson(prefs.getString("carpetas", null));
        vista.setCarpetas(carpetas);
        velo = new View(this);
        velo.setBackgroundColor(0xd0000000);
        velo.setVisibility(View.GONE);
        velo.setOnClickListener(v -> cierraCampo());
        raiz.addView(velo, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        campo = new EditText(this);
        final FrameLayout.LayoutParams lpCampo = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (64 * d));
        lpCampo.leftMargin = lpCampo.rightMargin = (int) (18 * d);
        lpCampo.topMargin = (int) (120 * d);
        estiloCampo(campo, "NOMBRE DE LA CARPETA", EditorInfo.IME_ACTION_DONE);
        campo.setVisibility(View.GONE);
        campo.setOnEditorActionListener((v, accion, ev) -> {
            String txt = campo.getText().toString().trim();
            AlTexto cb = alTexto;
            cierraCampo();
            if (!txt.isEmpty() && cb != null) cb.listo(txt);
            return true;
        });
        raiz.addView(campo, lpCampo);
        setContentView(raiz);

        raiz.setOnApplyWindowInsetsListener((v, ins) -> {
            Insets s = ins.getInsets(WindowInsets.Type.systemBars());
            vista.setInsets(s.top, s.bottom);
            lp.topMargin = s.top + (int) (40 * d);
            busca.setLayoutParams(lp);
            lpCampo.topMargin = s.top + (int) (120 * d);
            campo.setLayoutParams(lpCampo);
            return ins;
        });

        busca.addTextChangedListener(new AlEscribir(this));
        busca.setOnEditorActionListener((v, accion, ev) -> {
            AppEntry f = vista.primero();
            if (f != null) abrir(f);
            return true;
        });

        launcherApps.registerCallback(cambiosDeApps, ui);
        cargarApps();
        mostrarErrorSiHay();
    }

    @Override
    protected void onDestroy() {
        launcherApps.unregisterCallback(cambiosDeApps);
        fondo.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        vista.alVolver();
        vista.setWallpaperActual(wallpaperActual());
        // El calendario de Persona sale la primera vez que se ve el inicio en el día
        String hoy = LocalDate.now().toString();
        if (!hoy.equals(prefs.getString("ultimo_dia", ""))) {
            prefs.edit().putString("ultimo_dia", hoy).apply();
            vista.calendario();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Botón/gesto de inicio estando ya en el inicio → regresa a favoritas
        vista.cerrarCapas();
        if (busca.getVisibility() == View.VISIBLE) cerrarBusqueda();
        else vista.irAFavoritas();
    }

    @Override
    public void onBackPressed() {
        // Un launcher no se cierra con "atrás"
        if (campo.getVisibility() == View.VISIBLE) { cierraCampo(); return; }
        if (vista.cerrarCapas()) return;
        if (busca.getVisibility() == View.VISIBLE) cerrarBusqueda();
        else vista.irAFavoritas();
    }

    // ───────────────────────── Apps ─────────────────────────

    private void cargarApps() {
        fondo.execute(() -> {
            List<AppEntry> lista = new ArrayList<>();
            for (UserHandle u : usuarios.getUserProfiles()) {
                long serie = usuarios.getSerialNumberForUser(u);
                for (LauncherActivityInfo info : launcherApps.getActivityList(null, u)) {
                    if (info.getComponentName().getPackageName().equals(getPackageName())) continue;
                    String clave = info.getComponentName().flattenToString() + "|" + serie;
                    lista.add(new AppEntry(info, clave));
                }
            }
            final Collator col = Collator.getInstance(new Locale("es", "MX"));
            col.setStrength(Collator.PRIMARY);
            Collections.sort(lista, (a, b) -> col.compare(a.nombre, b.nombre));

            ui.post(() -> {
                todas = lista;
                publicaApps();
            });

            // Íconos después, sin trabar la pantalla
            int lado = (int) (34 * getResources().getDisplayMetrics().density);
            int dens = getResources().getDisplayMetrics().densityDpi;
            int n = 0;
            for (AppEntry a : lista) {
                if (a.info == null || a.icono != null) continue;
                try {
                    Drawable dr = a.info.getBadgedIcon(dens);
                    Bitmap b = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(b);
                    dr.setBounds(0, 0, lado, lado);
                    dr.draw(c);
                    a.icono = b;
                } catch (Exception ignorado) { }
                if (++n % 8 == 0) vista.postInvalidate();
            }
            vista.postInvalidate();
        });
    }

    /** Manda a la vista las apps sin las ocultas; las ocultas van aparte para Opciones. */
    private void publicaApps() {
        mostradas = new ArrayList<>();
        List<AppEntry> escondidas = new ArrayList<>();
        for (AppEntry a : todas) (ocultas.contains(a.clave) ? escondidas : mostradas).add(a);
        vista.setOcultas(escondidas);
        vista.setApps(mostradas, favoritas());
    }

    @Override
    public void ocultar(AppEntry a) {
        if (a.carpetaId != null) return;
        ocultas.add(a.clave);
        guardaOcultas();
        List<AppEntry> favs = favoritas();
        for (int i = 0; i < favs.size(); i++) if (favs.get(i).clave.equals(a.clave)) { favs.remove(i); guardaFavs(favs); break; }
        publicaApps();
        vista.aviso("OCULTA ⊘");
    }

    @Override
    public void mostrar(String clave) {
        if (!ocultas.remove(clave)) return;
        guardaOcultas();
        publicaApps();
        vista.aviso("DE VUELTA ✔");
    }

    private void guardaOcultas() {
        prefs.edit().putString("ocultas", String.join("\n", ocultas)).apply();
    }

    private List<AppEntry> favoritas() {
        List<AppEntry> res = new ArrayList<>();
        String guardadas = prefs.getString("favs", null);
        if (guardadas == null) {
            // Primera vez: las iniciales que existan (la 1ª actividad de cada paquete)
            for (String pkg : FAV_INICIALES) {
                for (AppEntry a : mostradas) {
                    boolean es = a.clave.equals(pkg) || (a.comp != null && a.comp.getPackageName().equals(pkg));
                    if (es) { res.add(a); break; }
                }
            }
            guardaFavs(res);
            return res;
        }
        for (String clave : guardadas.split("\n")) {
            if (clave.startsWith(LauncherView.CARPETA)) {
                Carpeta c = carpeta(clave.substring(LauncherView.CARPETA.length()));
                if (c != null) res.add(AppEntry.deCarpeta(c));
                continue;
            }
            for (AppEntry a : mostradas) if (a.clave.equals(clave)) { res.add(a); break; }
        }
        return res;
    }

    private void guardaFavs(List<AppEntry> l) {
        StringBuilder sb = new StringBuilder();
        for (AppEntry a : l) { if (sb.length() > 0) sb.append('\n'); sb.append(a.clave); }
        prefs.edit().putString("favs", sb.toString()).apply();
    }

    // ───────────────────────── Oyente de la vista ─────────────────────────

    @Override
    public void abrir(final AppEntry a) {
        if (busca.getVisibility() == View.VISIBLE) ocultaBusca();
        vista.corte(a.mayus, () -> lanzar(a));
    }

    private void lanzar(AppEntry a) {
        Bundle opc = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        try {
            if (a.intent != null) startActivity(new Intent(a.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), opc);
            else launcherApps.startMainActivity(a.comp, a.user, new Rect(), opc);
        } catch (Exception e) {
            vista.alVolver();
            vista.aviso("NO ABRE");
        }
    }

    @Override
    public void fijar(AppEntry a) {
        List<AppEntry> favs = favoritas();
        boolean estaba = false;
        for (int i = 0; i < favs.size(); i++) if (favs.get(i).clave.equals(a.clave)) { favs.remove(i); estaba = true; break; }
        if (!estaba) favs.add(a);
        guardaFavs(favs);
        vista.setFavs(favs);
        if (a.carpetaId != null) vista.aviso(estaba ? "FUERA DEL INICIO" : "▣ EN INICIO");
        else vista.aviso(estaba ? "QUITADA ✕" : "★ FIJADA");
    }

    @Override
    public void abrirBusqueda() {
        busca.setText("");
        busca.setVisibility(View.VISIBLE);
        busca.requestFocus();
        vista.filtrar("");
        busca.post(() -> busca.getWindowInsetsController().show(WindowInsets.Type.ime()));
    }

    @Override
    public void cerrarBusqueda() {
        ocultaBusca();
        vista.irAFavoritas();
    }

    private void ocultaBusca() {
        busca.getWindowInsetsController().hide(WindowInsets.Type.ime());
        busca.clearFocus();
        busca.setVisibility(View.GONE);
    }

    @Override
    public void cambiarSkin() {
        skin = skin.siguiente();
        prefs.edit().putString("skin", skin.id).apply();
        vista.setSkin(skin);
        vista.setOpciones(skin.nombre, version());
        estiloBusca();
        aplicarFondo();
        vista.aviso(skin.nombre);
    }

    @Override
    public void reordenar(List<AppEntry> nuevoOrden) {
        guardaFavs(nuevoOrden);
        vista.setFavs(nuevoOrden);
    }

    @Override
    public void alternarIconos() {
        boolean si = !prefs.getBoolean("iconos", true);
        prefs.edit().putBoolean("iconos", si).apply();
        vista.setIconos(si);
    }

    @Override
    public void ajustesInicio() {
        try { startActivity(new Intent(android.provider.Settings.ACTION_HOME_SETTINGS)); }
        catch (Exception e) { vista.aviso("NO DISPONIBLE"); }
    }

    @Override
    public void abrirCalendario() {
        final Intent cal = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        vista.corte("CALENDARIO", () -> {
            try { startActivity(cal); }
            catch (Exception e) {
                // Sin app con la categoría: el visor de Google Calendar por fecha
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis()))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (Exception e2) { vista.alVolver(); vista.aviso("SIN CALENDARIO"); }
            }
        });
    }

    @Override
    public void alternarFondo() {
        boolean si = !prefs.getBoolean("wallpaper", false);
        prefs.edit().putBoolean("wallpaper", si).apply();
        aplicarFondo();
    }

    @Override
    public void cambiarWallpaper() {
        try {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"));
        } catch (Exception e) { vista.aviso("NO DISPONIBLE"); }
    }

    /** Fondo Persona (ventana opaca) o el wallpaper del sistema visto a través de la ventana. */
    private void aplicarFondo() {
        boolean si = prefs.getBoolean("wallpaper", false);
        // Con fondo Persona se apaga el wallpaper: tapado por la vista opaca, un wallpaper
        // animado (Wallpaper Engine) seguiría gastando batería sin verse.
        if (si) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        vista.setWallpaper(si);
        vista.setWallpaperActual(wallpaperActual());
    }

    /** Qué wallpaper tiene puesto el sistema: el nombre del animado (p. ej. Wallpaper Engine) o imagen fija. */
    private String wallpaperActual() {
        try {
            android.app.WallpaperInfo wi = android.app.WallpaperManager.getInstance(this).getWallpaperInfo();
            if (wi == null) return "IMAGEN FIJA";
            return wi.loadLabel(getPackageManager()).toString().toUpperCase();
        } catch (Exception e) { return "?"; }
    }

    // ───────────────────────── Carpetas ─────────────────────────

    private Carpeta carpeta(String id) {
        for (Carpeta c : carpetas) if (c.id.equals(id)) return c;
        return null;
    }

    private void guardaCarpetas() {
        prefs.edit().putString("carpetas", Carpeta.aJson(carpetas)).apply();
        vista.setCarpetas(carpetas);
        vista.setFavs(favoritas());   // el renglón de una carpeta en inicio cambia de nombre/conteo
    }

    @Override
    public void aCarpeta(AppEntry a, String carpetaId) {
        Carpeta c = carpeta(carpetaId);
        if (c == null || a.carpetaId != null) return;
        if (!c.claves.contains(a.clave)) c.claves.add(a.clave);
        guardaCarpetas();
        vista.aviso("▣ EN " + c.nombre.toUpperCase());
    }

    @Override
    public void nuevaCarpeta(final AppEntry conApp) {
        pideTexto("", nombre -> {
            Carpeta c = new Carpeta("c" + System.currentTimeMillis(), nombre);
            if (conApp != null && conApp.carpetaId == null) c.claves.add(conApp.clave);
            carpetas.add(c);
            guardaCarpetas();
            vista.aviso("▣ " + nombre.toUpperCase());
        });
    }

    @Override
    public void sacarDeCarpeta(AppEntry a, String carpetaId) {
        Carpeta c = carpeta(carpetaId);
        if (c == null) return;
        c.claves.remove(a.clave);
        guardaCarpetas();
        vista.aviso("↩ FUERA DE " + c.nombre.toUpperCase());
    }

    @Override
    public void reordenarCarpeta(String carpetaId, List<AppEntry> orden) {
        Carpeta c = carpeta(carpetaId);
        if (c == null) return;
        // Las claves de apps desinstaladas (no visibles) se quedan al final, por si regresan
        List<String> resto = new ArrayList<>(c.claves);
        c.claves.clear();
        for (AppEntry a : orden) { c.claves.add(a.clave); resto.remove(a.clave); }
        c.claves.addAll(resto);
        guardaCarpetas();
    }

    @Override
    public void renombrarCarpeta(String carpetaId) {
        final Carpeta c = carpeta(carpetaId);
        if (c == null) return;
        pideTexto(c.nombre, nombre -> { c.nombre = nombre; guardaCarpetas(); });
    }

    @Override
    public void borrarCarpeta(String carpetaId) {
        Carpeta c = carpeta(carpetaId);
        if (c == null) return;
        carpetas.remove(c);
        List<AppEntry> favs = favoritas();
        for (int i = favs.size() - 1; i >= 0; i--) if (favs.get(i).clave.equals(c.clave())) favs.remove(i);
        guardaFavs(favs);
        guardaCarpetas();
        vista.aviso("CARPETA BORRADA");
    }

    @Override
    public void alternarCarpetaInicio(String carpetaId) {
        Carpeta c = carpeta(carpetaId);
        if (c != null) fijar(AppEntry.deCarpeta(c));
    }

    private void pideTexto(String inicial, AlTexto cb) {
        alTexto = cb;
        campo.setText(inicial);
        campo.setSelection(campo.getText().length());
        velo.setVisibility(View.VISIBLE);
        campo.setVisibility(View.VISIBLE);
        campo.requestFocus();
        campo.post(() -> campo.getWindowInsetsController().show(WindowInsets.Type.ime()));
    }

    private void cierraCampo() {
        alTexto = null;
        campo.getWindowInsetsController().hide(WindowInsets.Type.ime());
        campo.clearFocus();
        campo.setVisibility(View.GONE);
        velo.setVisibility(View.GONE);
    }

    private String version() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception e) { return "?"; }
    }

    // ───────────────────────── Detalles ─────────────────────────

    private void estiloBusca() {
        estiloCampo(busca, "¿QUÉ ABRES?", EditorInfo.IME_ACTION_GO);
        if (campo != null) estiloCampo(campo, "NOMBRE DE LA CARPETA", EditorInfo.IME_ACTION_DONE);
    }

    private void estiloCampo(EditText e, String pista, int accionIme) {
        float d = getResources().getDisplayMetrics().density;
        e.setTypeface(Typeface.create(Typeface.create("sans-serif-black", Typeface.NORMAL), 900, true));
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        e.setTextColor(0xff000000);
        e.setHintTextColor(0x66000000);
        e.setHint(pista);
        e.setSingleLine(true);
        e.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(24)});
        e.setImeOptions(accionIme | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        e.setPadding((int) (18 * d), 0, (int) (18 * d), 0);
        e.setGravity(Gravity.CENTER_VERTICAL);
        e.setBackground(new Poligono(skin.blanco, new float[]{.02f, 0, 1, .06f, .98f, 1, 0, .9f}));
        e.setRotation(-2);
    }

    /** Si la última vez tronó, enseñar el error para poder mandar captura. Tocar lo cierra. */
    private void mostrarErrorSiHay() {
        String error = Crash.leer(this);
        if (error == null) return;
        final ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText("EL LAUNCHER SE CERRÓ POR UN ERROR.\nMANDA CAPTURA DE ESTO Y TOCA PARA CERRAR.\n\n" + error);
        tv.setTextColor(0xffffffff);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.MONOSPACE);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad * 3, pad, pad * 3);
        sv.setBackgroundColor(0xf0000000);
        sv.addView(tv);
        View.OnClickListener cerrar = v -> { Crash.borrar(this); raiz.removeView(sv); };
        tv.setOnClickListener(cerrar);
        raiz.addView(sv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** Fondo blanco ladeado para el buscador (puntos normalizados 0..1). */
    static final class Poligono extends Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float[] pts;

        Poligono(int color, float[] pts) { p.setColor(color); this.pts = pts; }

        @Override public void draw(Canvas c) {
            Rect b = getBounds();
            path.reset();
            path.moveTo(b.left + pts[0] * b.width(), b.top + pts[1] * b.height());
            for (int i = 2; i < pts.length; i += 2) path.lineTo(b.left + pts[i] * b.width(), b.top + pts[i + 1] * b.height());
            path.close();
            c.drawPath(path, p);
        }

        @Override public void setAlpha(int a) { p.setAlpha(a); }
        @Override public void setColorFilter(ColorFilter cf) { p.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
