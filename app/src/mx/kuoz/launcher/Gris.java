package mx.kuoz.launcher;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;

/**
 * Blanco y negro al abrir una app con pausa: la corrección de color de Android en modo escala de
 * grises (la del sistema, no un filtro propio: no gasta batería). Vuelve el color al regresar al inicio.
 * Necesita WRITE_SECURE_SETTINGS, que solo se da por shell, una vez (Shizuku + aShell, o adb).
 * Guarda cómo estaba el ajuste antes y lo devuelve igual: si la persona ya usaba corrección de color, se respeta.
 */
final class Gris {
    private static final String ON = "accessibility_display_daltonizer_enabled";
    private static final String MODO = "accessibility_display_daltonizer";
    private static final int MONOCROMO = 0, DEUTERANOMALIA = 12;   // 12 = el modo de fábrica de Android

    /** El 1º da el gris; el 2º, de paso, el permiso de uso sin pasar por «Allow restricted settings». */
    static final String COMANDOS = "pm grant mx.kuoz.launcher android.permission.WRITE_SECURE_SETTINGS\n"
            + "appops set mx.kuoz.launcher GET_USAGE_STATS allow";

    private Gris() {}

    static boolean permiso(Context c) {
        return c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    static void poner(Context c, SharedPreferences prefs) {
        if (!prefs.getBoolean("gris", true) || prefs.getBoolean("gris_puesto", false) || !permiso(c)) return;
        try {
            ContentResolver r = c.getContentResolver();
            // commit, no apply: si truena estando en gris, al volver hay que saber que lo puso Flow
            prefs.edit().putBoolean("gris_puesto", true)
                    .putInt("gris_antes_on", Settings.Secure.getInt(r, ON, 0))
                    .putInt("gris_antes_modo", Settings.Secure.getInt(r, MODO, DEUTERANOMALIA)).commit();
            Settings.Secure.putInt(r, MODO, MONOCROMO);
            Settings.Secure.putInt(r, ON, 1);
        } catch (Exception e) { }
    }

    /** Al volver al inicio. Solo deshace lo que puso Flow. */
    static void quitar(Context c, SharedPreferences prefs) {
        if (!prefs.getBoolean("gris_puesto", false)) return;
        try {
            ContentResolver r = c.getContentResolver();
            Settings.Secure.putInt(r, ON, prefs.getInt("gris_antes_on", 0));
            Settings.Secure.putInt(r, MODO, prefs.getInt("gris_antes_modo", DEUTERANOMALIA));
            prefs.edit().putBoolean("gris_puesto", false).apply();
        } catch (Exception e) { }
    }
}
