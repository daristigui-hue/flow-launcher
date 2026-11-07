package mx.kuoz.launcher;

import android.content.pm.ApplicationInfo;

import java.util.Random;

/**
 * Tipo de cada app (redes, juegos, trabajo…) para que la última nube de la pausa hable de eso.
 * Primero manda lo que la persona eligió; si no, lo que la app declara a Android; si no, una lista
 * corta de paquetes conocidos; si no, «otra».
 */
final class Tipos {
    static final String[] IDS = {"social", "mensajes", "juegos", "video", "trabajo", "compras", "noticias", "otra"};
    static final String[] NOMBRES = {"REDES SOCIALES", "MENSAJERÍA", "VIDEOJUEGOS", "VIDEO", "TRABAJO", "COMPRAS", "NOTICIAS", "OTRA"};

    // Todas en tercera persona y sin género: hablan de quien abre la app, no le hablan a esa persona
    private static final String[][] FRASES = {
        {"Solo un reel más, dice…", "¿Sigue esperando un mensaje?", "Otra vez a ver la vida de los demás…",
         "¿Qué cree que se perdió en 10 min?", "Va a deslizar hasta que se le acabe el dedo",
         "Los likes no se van a ir a ningún lado", "¿Y si mejor le escribe a alguien de verdad?"},
        {"¿Le contestaron o solo está revisando?", "Nadie le ha escrito… otra vez", "Ya leyó todo. Tres veces.",
         "Si fuera urgente, le llamarían"},
        {"¿No tiene nada mejor que hacer que jugar?", "Una partida más… como siempre", "¿Y lo que tenía pendiente?",
         "«Nomás 5 minutos»… hace una hora", "La partida puede esperar"},
        {"«Nomás un video», dice…", "¿Cuántos van ya?", "El algoritmo ya se sabe sus gustos",
         "Otro episodio y ya… seguro", "¿Lo está viendo o solo lo tiene de fondo?"},
        {"Debería aprender a relajarse", "¿El trabajo no puede esperar?", "Ni en su descanso lo suelta",
         "El correo va a seguir ahí en la mañana"},
        {"¿De verdad lo necesita?", "Otra vez viendo cosas que no va a comprar", "El carrito ya está lleno… otra vez",
         "Mañana lo va a querer igual. Si no, no lo necesitaba"},
        {"Las noticias van a seguir ahí mañana", "¿Otra vez a preocuparse por lo que no controla?",
         "Ya sabe todo lo que pasó hace 20 min"},
        {"¿A qué irá?", "¿Se acuerda para qué la iba a abrir?", "Seguro ni sabe por qué la abrió"},
    };

    // Paquetes comunes que no declaran su tipo (o lo declaran como otra cosa)
    private static final String[][] CONOCIDAS = {
        {"com.instagram.android", "social"}, {"com.facebook.katana", "social"}, {"com.facebook.lite", "social"},
        {"com.twitter.android", "social"}, {"com.reddit.frontpage", "social"}, {"com.snapchat.android", "social"},
        {"com.pinterest", "social"}, {"com.threads", "social"}, {"com.instagram.barcelona", "social"},
        {"com.whatsapp", "mensajes"}, {"com.whatsapp.w4b", "mensajes"}, {"org.telegram.messenger", "mensajes"},
        {"org.thoughtcrime.securesms", "mensajes"}, {"com.facebook.orca", "mensajes"}, {"com.discord", "mensajes"},
        {"com.google.android.youtube", "video"}, {"com.zhiliaoapp.musically", "video"}, {"com.ss.android.ugc.trill", "video"},
        {"com.netflix.mediaclient", "video"}, {"com.disney.disneyplus", "video"}, {"tv.twitch.android.app", "video"},
        {"com.amazon.avod.thirdpartyclient", "video"}, {"com.hbo.hbonow", "video"}, {"com.wbd.stream", "video"},
        {"com.microsoft.office.outlook", "trabajo"}, {"com.microsoft.teams", "trabajo"}, {"com.Slack", "trabajo"},
        {"com.google.android.gm", "trabajo"}, {"com.microsoft.office.officehubrow", "trabajo"}, {"com.linkedin.android", "trabajo"},
        {"com.amazon.mShop.android.shopping", "compras"}, {"com.mercadolibre", "compras"}, {"com.shein.android", "compras"},
        {"com.einnovation.temu", "compras"}, {"com.alibaba.aliexpresshd", "compras"},
    };

    private Tipos() { }

    static int indice(String id) {
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return IDS.length - 1;
    }

    static String nombre(String id) { return NOMBRES[indice(id)]; }

    /** Lo que se puede saber sin preguntar: la categoría que declara la app y los paquetes conocidos. */
    static String detecta(AppEntry a, String paquete) {
        if (paquete != null) for (String[] k : CONOCIDAS) if (k[0].equals(paquete)) return k[1];
        ApplicationInfo ai = a.info != null ? a.info.getApplicationInfo() : null;
        if (ai == null) return "otra";
        switch (ai.category) {
            case ApplicationInfo.CATEGORY_GAME: return "juegos";
            case ApplicationInfo.CATEGORY_SOCIAL: return "social";
            case ApplicationInfo.CATEGORY_VIDEO: return "video";
            case ApplicationInfo.CATEGORY_NEWS: return "noticias";
            case ApplicationInfo.CATEGORY_PRODUCTIVITY: return "trabajo";
            default: return "otra";
        }
    }

    /** Una frase al azar del tipo, distinta de la última que salió (anterior = -1 si no hay). */
    static int elige(String id, int anterior, Random azar) {
        int n = FRASES[indice(id)].length;
        if (n == 1) return 0;
        int k = azar.nextInt(n - (anterior >= 0 && anterior < n ? 1 : 0));
        if (anterior >= 0 && anterior < n && k >= anterior) k++;
        return k;
    }

    static String frase(String id, int k) {
        String[] l = FRASES[indice(id)];
        return l[Math.max(0, Math.min(l.length - 1, k))];
    }
}
