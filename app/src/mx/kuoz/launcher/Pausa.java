package mx.kuoz.launcher;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Pausa consciente: 5 s antes de abrir una app marcada. Van saliendo nubes de pensamiento con lo
 * que «otros» piensan de quien la abre (en tercera persona, nunca «tú») y al final se elige
 * MEJOR NO o ABRIR. No se puede saltar. MEJOR NO cierra con las bandas de abrir, en espejo.
 */
final class Pausa {
    static final long DURA = 5000, BOTONES = 5200, NO_DURA = 1950;
    private static final long PRIMERA = 500;

    final AppEntry app;
    private final List<String> frases;
    private final int resisteAntes;
    private final View vista;
    private final float d;
    private final long t0;
    private long tNo = -1, tBotones = -1, tSi = -1;
    private boolean avisado;

    private final List<Nube> nubes = new ArrayList<>();
    private final RectF rNo = new RectF(), rSi = new RectF();

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), t = new Paint(Paint.ANTI_ALIAS_FLAG),
            borroso = new Paint(Paint.ANTI_ALIAS_FLAG), rastro = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final PathInterpolator seco = new PathInterpolator(.7f, 0f, .2f, 1f);
    private final DecelerateInterpolator suave = new DecelerateInterpolator(2f);
    private final OvershootInterpolator entra = new OvershootInterpolator(1.4f);

    private static final class Nube {
        final String texto;
        final boolean der, ultima;
        final Resorte y = new Resorte(70, .95f), alfa = new Resorte(70, .95f);
        final float[][] bolas = new float[7][4];   // x, y, ancho, alto (fracciones de la nube)
        long nace = -1;
        Nube(String texto, boolean der, boolean ultima) { this.texto = texto; this.der = der; this.ultima = ultima; }
    }

    // Bolas de cada nube (como la maqueta): x, y, tamaño
    private static final float[][] FORMA = {{0, .28f, .34f}, {.16f, .05f, .40f}, {.38f, 0, .36f}, {.58f, .08f, .34f},
                                            {.72f, .28f, .28f}, {.12f, .38f, .46f}, {.44f, .36f, .48f}};

    Pausa(AppEntry app, List<String> frases, int resisteAntes, View vista, float d, Typeface negra, long ahora) {
        this.app = app; this.frases = frases; this.resisteAntes = resisteAntes; this.vista = vista; this.d = d; this.t0 = ahora;
        t.setTypeface(negra);
        borroso.setMaskFilter(new BlurMaskFilter(4 * d, BlurMaskFilter.Blur.NORMAL));
        rastro.setMaskFilter(new BlurMaskFilter(1.5f * d, BlurMaskFilter.Blur.NORMAL));
        for (int i = 0; i < frases.size(); i++) {
            Nube n = new Nube(frases.get(i), i % 2 == 1, i == frases.size() - 1);
            for (int b = 0; b < FORMA.length; b++) {
                float s = FORMA[b][2] + azar(i + 1, b) * .08f;
                n.bolas[b][0] = FORMA[b][0] + azar(i + 1, b + 9) * .04f;
                n.bolas[b][1] = FORMA[b][1] + azar(i + 1, b + 3) * .08f;
                n.bolas[b][2] = s;
                n.bolas[b][3] = s * .62f + .08f;
            }
            nubes.add(n);
        }
    }

    private static float azar(int semilla, int n) { return (float) (Math.sin(semilla * 97 + n * 13.7) + 1) / 2; }

    boolean diciendoNo() { return tNo >= 0; }

    /** Atrás / inicio durante la pausa = MEJOR NO (nunca abre la app). */
    void mejorNo(long ahora) {
        if (tNo >= 0 || tSi >= 0) return;
        tNo = ahora;
        vista.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }

    /** 1 = eligió ABRIR, 0 = MEJOR NO, -1 = nada (el toque se lo queda la pausa de todos modos). */
    int toca(float x, float y, long ahora) {
        if (tNo >= 0 || tSi >= 0 || tBotones < 0) return -1;
        if (rNo.contains(x, y)) { mejorNo(ahora); return 0; }
        if (rSi.contains(x, y)) { vista.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); return 1; }
        return -1;
    }

    /** Eligió ABRIR: la pausa se sigue pintando mientras entra la franja, ya sin responder. */
    void abriendo(long ahora) { tSi = ahora; }

    /** true cuando la franja de abrir ya cubrió la pantalla (entra en 0.4 s). */
    boolean tapada(long ahora) { return tSi >= 0 && ahora - tSi > 420; }

    /** true cuando la animación de MEJOR NO terminó (hay que quitar la pausa). */
    boolean termino(long ahora) { return tNo >= 0 && ahora - tNo > NO_DURA; }

    // ───────────────────────── Dibujo ─────────────────────────

    void dibuja(Canvas c, int w, int h, int arriba, int abajo, Skin skin, long ahora, float dt) {
        long edad = ahora - t0;
        long eNo = tNo < 0 ? -1 : ahora - tNo;
        if (eNo < 0 || eNo < 330) escena(c, w, h, arriba, abajo, skin, ahora, edad, dt, eNo);
        if (eNo >= 0) despedida(c, w, h, skin, eNo);
    }

    private void escena(Canvas c, int w, int h, int arriba, int abajo, Skin skin, long ahora, long edad, float dt, long eNo) {
        float entrada = Math.min(1, edad / 300f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(skin.abismo); p.setAlpha((int) (245 * entrada));
        c.drawRect(0, 0, w, h, p);
        p.setAlpha(255);

        // Nombre de la app con el trazo de la hora del inicio + etiqueta PAUSA
        float fe = suave.getInterpolation(entrada);
        c.save();
        c.translate(22 * d - (1 - fe) * 40 * d, arriba + 26 * d);
        c.rotate(-6);
        c.skew(-.14f, 0);
        t.setTextAlign(Paint.Align.LEFT); t.setLetterSpacing(-.04f);
        t.setTextSize(Math.min(64 * d, (w - 60 * d) * 1.55f / Math.max(5, app.mayus.length())));
        float base = 58 * d;
        t.setColor(Color.BLACK); c.drawText(app.mayus, 8 * d, base + 8 * d, t);
        t.setColor(skin.fuerte); c.drawText(app.mayus, 5 * d, base + 5 * d, t);
        t.setColor(skin.blanco); c.drawText(app.mayus, 0, base, t);
        t.setTextSize(13 * d); t.setLetterSpacing(.3f);
        float tw = t.measureText("PAUSA") + 32 * d, ty = base + 16 * d;
        poligono(c, 0, ty, tw, 26 * d, new float[]{.04f, 0, 1, .08f, .95f, 1, 0, .88f}, skin.blanco);
        t.setColor(Color.BLACK); c.drawText("PAUSA", 12 * d, ty + 18 * d, t);
        c.restore();

        // Nubes: la nueva sale abajo y empuja a las anteriores, que suben y se desvanecen
        float zonaAbajo = h - abajo - h * .27f, alto = 78 * d, cw = w * .66f;
        int vivas = 0;
        long paso = (DURA - 600) / Math.max(1, nubes.size());
        for (int i = 0; i < nubes.size(); i++) {
            if (edad >= PRIMERA + i * paso) vivas = i + 1;
        }
        float salida = eNo < 0 ? 0 : suave.getInterpolation(Math.min(1, eNo / 330f));
        for (int i = 0; i < vivas; i++) {
            Nube n = nubes.get(i);
            int atras = vivas - 1 - i;
            if (n.nace < 0) {
                n.nace = ahora;
                n.y.fija(zonaAbajo - alto + 30 * d);
                n.alfa.fija(0);
                vista.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
            n.y.objetivo = zonaAbajo - alto - atras * 86 * d;
            n.alfa.objetivo = atras > 3 ? 0 : 1 - atras * .22f;
            n.y.paso(dt, .5f);
            n.alfa.paso(dt, .002f);
            float a = Math.max(0, Math.min(1, n.alfa.pos)) * (1 - salida);
            if (a <= .01f) continue;
            float flota = (float) Math.sin((ahora - n.nace) / 800.0 + i) * 3 * d;
            float x = n.der ? w - w * .06f - cw : w * .06f;
            x += (n.der ? 1 : -1) * salida * w * 1.2f;
            float escala = 1 - atras * .05f;
            c.save();
            c.translate(x + cw / 2, n.y.pos + flota + alto / 2);
            c.scale(escala, escala);
            c.translate(-cw / 2, -alto / 2);
            nube(c, n, cw, alto, skin, a);
            c.restore();
        }

        // Barra fina de los 5 s
        float by = h - abajo - h * .23f, bx = 22 * d, bw = w - 44 * d;
        p.setColor(skin.marino); p.setAlpha((int) (255 * entrada * (1 - salida)));
        c.drawRect(bx, by, bx + bw, by + 3 * d, p);
        p.setColor(skin.cian); p.setAlpha((int) (255 * (1 - salida)));
        c.drawRect(bx, by, bx + bw * Math.min(1, edad / (float) DURA), by + 3 * d, p);
        p.setAlpha(255);

        // MEJOR NO (la fila elegida del inicio) y ABRIR, chico
        if (edad >= BOTONES) {
            if (tBotones < 0) tBotones = ahora;
            if (!avisado) { avisado = true; vista.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); }
            float f1 = entra.getInterpolation(Math.min(1, (ahora - tBotones) / 550f));
            float f2 = entra.getInterpolation(Math.max(0, Math.min(1, (ahora - tBotones - 100) / 550f)));
            int alfa = (int) (255 * (1 - salida));
            float yNo = h - abajo - h * .07f - 96 * d;
            t.setTextAlign(Paint.Align.LEFT); t.setLetterSpacing(-.01f); t.setTextSize(30 * d);
            float anchoNo = t.measureText("MEJOR NO") + 44 * d;
            c.save();
            c.translate(22 * d - (1 - f1) * w, yNo);
            c.rotate(-3);
            p.setAlpha(alfa);
            poligono(c, 0, 0, anchoNo, 50 * d, new float[]{.02f, 0, 1, .06f, .97f, 1, 0, .92f}, skin.blanco);
            t.setColor(Color.BLACK); t.setAlpha(alfa); c.drawText("MEJOR NO", 16 * d, 36 * d, t);
            c.restore();
            rNo.set(10 * d, yNo - 12 * d, 22 * d + anchoNo + 20 * d, yNo + 58 * d);

            t.setTextSize(20 * d);
            float anchoSi = t.measureText("ABRIR");
            c.save();
            c.translate(40 * d - (1 - f2) * w, yNo + 92 * d);
            c.rotate(-3);
            t.setColor(skin.blanco); t.setAlpha((int) (190 * (1 - salida)));
            c.drawText("ABRIR", 0, 0, t);
            c.restore();
            rSi.set(20 * d, yNo + 60 * d, 40 * d + anchoSi + 30 * d, yNo + 110 * d);
            t.setAlpha(255); p.setAlpha(255);
        }
    }

    /** Una nube: bolas borrosas encimadas + rastro de burbujitas hacia la orilla + texto nítido. */
    private void nube(Canvas c, Nube n, float cw, float alto, Skin skin, float a) {
        int color = n.ultima ? skin.fuerte : skin.hielo;
        borroso.setColor(color); borroso.setAlpha((int) (237 * a));
        for (float[] b : n.bolas) {
            float bw = b[2] * cw, bh = b[3] * cw;
            float bx = b[0] * cw, by = b[1] * alto - bh * .15f;
            oval.set(bx, by, bx + bw, by + bh);
            c.drawOval(oval, borroso);
        }
        rastro.setColor(color); rastro.setAlpha((int) (205 * a));
        float ox = n.der ? cw + 6 * d : -6 * d, dir = n.der ? 1 : -1;
        c.drawCircle(ox, alto + 2 * d, 7 * d, rastro);
        c.drawCircle(ox + dir * 13 * d, alto + 16 * d, 4 * d, rastro);

        // Texto: renglones con \n; lo que va entre *asteriscos* se resalta
        String[] lineas = n.texto.split("\n");
        float normal = (n.ultima ? 20 : 13) * d, grande = 17 * d;
        int tinta = n.ultima ? skin.blanco : 0xff1a1214, realce = n.ultima ? skin.blanco : skin.azul;
        float altoTotal = 0;
        float[] altos = new float[lineas.length];
        for (int i = 0; i < lineas.length; i++) { altos[i] = (lineas[i].contains("*") ? grande : normal) * 1.18f; altoTotal += altos[i]; }
        float y = (alto - altoTotal) / 2 + 4 * d;
        t.setTextAlign(Paint.Align.LEFT); t.setLetterSpacing(.01f);
        for (int i = 0; i < lineas.length; i++) {
            String[] trozos = lineas[i].split("\\*", -1);
            float ancho = 0;
            for (int k = 0; k < trozos.length; k++) { t.setTextSize(k % 2 == 1 ? grande : normal); ancho += t.measureText(trozos[k]); }
            float x = (cw - ancho) / 2;
            y += altos[i];
            for (int k = 0; k < trozos.length; k++) {
                boolean em = k % 2 == 1;
                t.setTextSize(em ? grande : normal);
                t.setColor(em ? realce : tinta); t.setAlpha((int) (255 * a));
                c.drawText(trozos[k], x, y - altos[i] * .22f, t);
                x += t.measureText(trozos[k]);
            }
        }
        t.setAlpha(255);
    }

    /** MEJOR NO: entran bandas claras por la derecha, «Esta vez no» con chispas, y salen por la izquierda. */
    private void despedida(Canvas c, int w, int h, Skin skin, long e) {
        float b1 = e < 1550 ? 2 - seco.getInterpolation(Math.min(1, e / 320f)) : 1 - seco.getInterpolation(Math.min(1, (e - 1550) / 350f));
        float b2 = e < 1480 ? 2 - seco.getInterpolation(Math.max(0, Math.min(1, (e - 70) / 320f)))
                            : 1 - seco.getInterpolation(Math.min(1, (e - 1480) / 350f));
        // Con la pantalla alta la banda deja destapada la esquina de arriba a la izquierda: mientras
        // las dos están dentro se rellena todo (la banda es la misma de abrir y no se toca)
        if (e >= 320 && e < 1550) c.drawColor(skin.blanco);
        else banda(c, path, p, w, h, b1, skin.blanco);
        if (e >= 390 && e < 1480) c.drawColor(skin.hielo);
        else banda(c, path, p, w, h, b2, skin.hielo);

        // Texto: entra, se queda y se va hacia la izquierda junto con las bandas
        float f = (e - 300) / 1500f;
        if (f > 0 && f < 1) {
            float alfa, dx;
            if (f < .18f) { float g = suave.getInterpolation(f / .18f); alfa = g; dx = 40 * (1 - g); }
            else if (f < .85f) { alfa = 1; dx = -8 * (f - .18f) / .67f; }
            else { float g = (f - .85f) / .15f; alfa = 1 - g; dx = -8 - 52 * g; }
            c.save();
            c.translate(24 * d + dx * d, h * .46f);
            c.rotate(-7);
            t.setTextAlign(Paint.Align.LEFT); t.setLetterSpacing(-.03f);
            t.setTextSize(Math.min(48 * d, (w - 48 * d) / 6.2f));
            t.setColor(skin.cian); t.setAlpha((int) (255 * alfa)); c.drawText("Esta vez no", 5 * d, 5 * d, t);
            t.setColor(Color.BLACK); t.setAlpha((int) (255 * alfa)); c.drawText("Esta vez no", 0, 0, t);
            int n = resisteAntes + 1;
            String tag = n == 1 ? "LA PRIMERA DE HOY" : n + "ª VEZ QUE LO RESISTE HOY";
            t.setTextSize(11 * d); t.setLetterSpacing(.22f);
            float tw = t.measureText(tag) + 32 * d;
            p.setAlpha((int) (255 * alfa));
            poligono(c, 0, 14 * d, tw, 24 * d, new float[]{.04f, 0, 1, .08f, .95f, 1, 0, .88f}, Color.BLACK);
            t.setColor(skin.blanco); t.setAlpha((int) (255 * alfa)); c.drawText(tag, 12 * d, 30 * d, t);
            c.restore();
            t.setAlpha(255); p.setAlpha(255);
        }

        // Chispas de cuatro puntas que salen del texto
        for (int k = 0; k < 9; k++) {
            float g = (e - 350 - k * 40) / 900f;
            if (g <= 0 || g >= 1) continue;
            float ang = k / 9f * 6.28f, r = (70 + (k % 3) * 30) * d;
            float sx = w * (.30f + ((k * 37) % 50) / 100f), sy = h * (.36f + ((k * 23) % 22) / 100f);
            float esc, alfa, mx = 0, my = 0, giro;
            if (g < .3f) { float q = g / .3f; esc = .2f + 1.1f * q; alfa = q; giro = 45 * q; }
            else { float q = suave.getInterpolation((g - .3f) / .7f); esc = 1.3f - .9f * q; alfa = 1 - q;
                   mx = (float) Math.cos(ang) * r * q; my = (float) Math.sin(ang) * r * q; giro = 45 + 95 * q; }
            c.save();
            c.translate(sx + mx, sy + my);
            c.rotate(giro);
            c.scale(esc, esc);
            estrella(c, 8 * d, skin.fuerte, (int) (255 * alfa));
            c.restore();
        }
    }

    private void estrella(Canvas c, float r, int color, int alfa) {
        path.reset();
        path.moveTo(0, -r); path.lineTo(r * .24f, -r * .24f); path.lineTo(r, 0); path.lineTo(r * .24f, r * .24f);
        path.lineTo(0, r); path.lineTo(-r * .24f, r * .24f); path.lineTo(-r, 0); path.lineTo(-r * .24f, -r * .24f);
        path.close();
        p.setStyle(Paint.Style.FILL); p.setColor(color); p.setAlpha(alfa);
        c.drawPath(path, p);
        p.setAlpha(255);
    }

    private void poligono(Canvas c, float x, float y, float w, float h, float[] pts, int color) {
        int alfa = p.getAlpha();
        path.reset();
        path.moveTo(x + pts[0] * w, y + pts[1] * h);
        for (int i = 2; i < pts.length; i += 2) path.lineTo(x + pts[i] * w, y + pts[i + 1] * h);
        path.close();
        p.setStyle(Paint.Style.FILL); p.setColor(color); p.setAlpha(alfa);
        c.drawPath(path, p);
    }

    /** Banda en diagonal (skewX -24°), la misma de abrir una app. f: 0 fuera izq · 1 cubre · 2 fuera der. */
    static void banda(Canvas c, Path path, Paint p, int w, int h, float f, int color) {
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

    // ───────────────────────── Lo que «dicen» ─────────────────────────

    /**
     * Las frases de las nubes, en orden. uso = null sin el permiso «Uso de apps»: entonces solo
     * salen las veces (contadas por el launcher), la hora y la pregunta.
     */
    static List<String> frases(UsoApps.Dias uso, int vecesPropias, Calendar ahora) {
        List<String> l = new ArrayList<>();
        int veces = (uso != null ? uso.veces[0] : vecesPropias) + 1;
        l.add(veces == 1 ? "Es la *primera vez*\nque la abre hoy" : "Ya es la *" + veces + "ª vez*\nque la abre hoy…");
        if (uso != null) {
            long hoy = uso.ms[0], ayer = uso.ms[1];
            l.add(hoy < 60_000 ? "Hoy apenas\nla ha usado" : "Lleva *" + duracion(hoy) + "*\nahí hoy");
            if (ayer >= 60_000) {
                long pct = Math.round((hoy - ayer) * 100.0 / ayer);
                l.add(pct == 0 ? "Hoy la usa *igual*\nque ayer" : "Hoy la usa *" + Math.abs(pct) + " % " + (pct > 0 ? "más" : "menos") + "*\nque ayer");
            }
            long total = 0;
            for (int i = 1; i < UsoApps.DIAS; i++) total += uso.ms[i];
            long prom = total / (UsoApps.DIAS - 1);
            if (prom >= 60_000) {
                if (hoy > prom) l.add("Ya pasó su promedio\nde *" + duracion(prom) + "*");
                else {
                    int racha = 0;
                    for (int i = 1; i < UsoApps.DIAS && uso.ms[i] < prom; i++) racha++;
                    l.add(racha >= 2 ? "*" + racha + " días* seguidos\nbajo su promedio" : "Va bajo su promedio\nde *" + duracion(prom) + "*");
                }
            }
        }
        int h24 = ahora.get(Calendar.HOUR_OF_DAY), m = ahora.get(Calendar.MINUTE);
        int h12 = h24 % 12 == 0 ? 12 : h24 % 12;
        String hora = h12 + ":" + (m < 10 ? "0" : "") + m + (h24 < 12 ? " a.m." : " p.m.");
        String art = h12 == 1 ? "la" : "las";
        l.add(h24 >= 23 || h24 < 5 ? "¿A " + art + " *" + hora + "*?\n¿No duerme?" : "Son " + art + " *" + hora + "*…");
        l.add("¿A qué irá?");
        return l;
    }

    static String duracion(long ms) {
        long min = Math.round(ms / 60_000.0);
        if (min < 60) return min + " min";
        return (min / 60) + " h" + (min % 60 == 0 ? "" : " " + (min % 60) + " min");
    }
}
