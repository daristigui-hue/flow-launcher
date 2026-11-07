package mx.kuoz.launcher;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

/** Las paletas fijas (Carmesí, Medianoche, Dorado, Sakura) y la que sale del wallpaper (Tu fondo). */
final class Skin {
    final String id, nombre;
    final int abismo, marino, azul, cian, hielo, blanco, rojo, fuerte;
    final float rayos;       // opacidad del sol de rayos del fondo
    final int particulas;    // 0 nada (Carmesí), 1 burbujas (Medianoche, Tu fondo), 2 pétalos (Dorado, Sakura)

    private Skin(String id, String nombre, int abismo, int marino, int azul, int cian, int hielo,
                 int blanco, int rojo, int fuerte, float rayos, int particulas) {
        this.id = id; this.nombre = nombre; this.abismo = abismo; this.marino = marino; this.azul = azul;
        this.cian = cian; this.hielo = hielo; this.blanco = blanco; this.rojo = rojo; this.fuerte = fuerte;
        this.rayos = rayos; this.particulas = particulas;
    }

    static final Skin[] TODAS = {
        new Skin("p5", "CARMESÍ", 0xff0a0000, 0xff120607, 0xffa8000e, 0xffff2d3f, 0xffffe5e7,
                 0xffffffff, 0xffe60012, 0xffe60012, .35f, 0),
        new Skin("p3", "MEDIANOCHE", 0xff020a2e, 0xff06216e, 0xff1546d6, 0xff3fd4ff, 0xffbff3ff,
                 0xfff4fbff, 0xffff3b5c, 0xff1546d6, .12f, 1),
        new Skin("oro", "DORADO", 0xff0b0908, 0xff1d1612, 0xffa3202c, 0xffd2a85a, 0xfff1d9a0,
                 0xffefe5d0, 0xffc8323a, 0xffa3202c, .12f, 2),
        new Skin("sakura", "SAKURA", 0xff1a0b12, 0xff2b1520, 0xffc2577f, 0xffffb3cd, 0xffffe4ee,
                 0xfffff6f9, 0xffff7fa8, 0xffe8679a, .12f, 2),
    };

    /** Id de la skin que toma sus colores del wallpaper. No vive en TODAS: se arma al vuelo. */
    static final String FONDO = "fondo";

    /** Android 12+ saca del wallpaper (también de los animados) la paleta de Material You. */
    static boolean hayFondo() { return Build.VERSION.SDK_INT >= 31; }

    static Skin por(Context c, String id) {
        if (FONDO.equals(id) && hayFondo()) return delFondo(c);
        for (Skin s : TODAS) if (s.id.equals(id)) return s;
        return TODAS[0];
    }

    /** Lee la paleta actual; volver a llamarla tras cambiar de wallpaper da los colores nuevos. */
    static Skin delFondo(Context c) {
        Resources r = c.getResources();
        return new Skin(FONDO, "TU FONDO",
                r.getColor(android.R.color.system_neutral1_900, null),
                r.getColor(android.R.color.system_accent1_900, null),
                r.getColor(android.R.color.system_accent1_700, null),
                r.getColor(android.R.color.system_accent1_200, null),
                r.getColor(android.R.color.system_accent1_50, null),
                r.getColor(android.R.color.system_neutral1_10, null),
                r.getColor(android.R.color.system_accent3_400, null),
                r.getColor(android.R.color.system_accent1_500, null),
                .12f, 1);
    }

    boolean igual(Skin o) {
        return abismo == o.abismo && marino == o.marino && azul == o.azul && cian == o.cian && hielo == o.hielo
                && blanco == o.blanco && rojo == o.rojo && fuerte == o.fuerte;
    }

    /** Orden del ciclo: las fijas y al final Tu fondo (si el teléfono lo soporta). */
    Skin siguiente(Context c) {
        for (int i = 0; i < TODAS.length; i++) {
            if (!TODAS[i].id.equals(id)) continue;
            if (i + 1 < TODAS.length) return TODAS[i + 1];
            return hayFondo() ? delFondo(c) : TODAS[0];
        }
        return TODAS[0];
    }
}
