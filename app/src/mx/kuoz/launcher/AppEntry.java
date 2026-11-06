package mx.kuoz.launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.graphics.Bitmap;
import android.os.UserHandle;

import java.text.Normalizer;

/** Una app del teléfono, un atajo (intent), o una carpeta (en favoritas). */
final class AppEntry {
    final String nombre, mayus, plano, clave;
    final char letra;
    final ComponentName comp;
    final UserHandle user;
    final LauncherActivityInfo info;
    final Intent intent;
    final String carpetaId;   // no nulo = este renglón es una carpeta
    volatile Bitmap icono;

    AppEntry(LauncherActivityInfo info, String clave) {
        this(info.getLabel().toString(), info.getComponentName(), info.getUser(), info, null, clave, null);
    }

    AppEntry(String nombre, Intent intent, String clave) {
        this(nombre, null, null, null, intent, clave, null);
    }

    static AppEntry deCarpeta(Carpeta c) {
        return new AppEntry(c.nombre, null, null, null, null, c.clave(), c.id);
    }

    private AppEntry(String nombre, ComponentName comp, UserHandle user, LauncherActivityInfo info,
                     Intent intent, String clave, String carpetaId) {
        this.nombre = nombre;
        this.mayus = nombre.toUpperCase();
        this.plano = sinAcentos(nombre).toLowerCase();
        this.comp = comp; this.user = user; this.info = info; this.intent = intent; this.clave = clave;
        this.carpetaId = carpetaId;
        char c = plano.isEmpty() ? '#' : Character.toUpperCase(plano.charAt(0));
        this.letra = (c >= 'A' && c <= 'Z') ? c : '#';
    }

    static String sinAcentos(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
