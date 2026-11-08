package mx.kuoz.launcher;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Cuánto se usa una app, día por día, con el permiso «Uso de apps» (lo da la persona en Ajustes).
 * Todo se lee y se queda en el teléfono: nada sale de aquí.
 */
final class UsoApps {
    static final int DIAS = 8;   // hoy + los 7 anteriores (el promedio sale de esos 7)

    /** Índice 0 = hoy, 1 = ayer… */
    static final class Dias {
        final long[] ms = new long[DIAS];
        final int[] veces = new int[DIAS];
        long ultima = -1;   // cuándo empezó la última apertura (-1 = ninguna en estos días)
    }

    private UsoApps() { }

    static boolean permiso(Context c) {
        AppOpsManager ops = c.getSystemService(AppOpsManager.class);
        return ops != null && ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), c.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    /** null si no hay permiso o Android no contesta. Lento (recorre los eventos): fuera del hilo de la vista. */
    static Dias leer(Context c, String paquete) {
        if (paquete == null || !permiso(c)) return null;
        UsageStatsManager um = c.getSystemService(UsageStatsManager.class);
        if (um == null) return null;
        long[] inicio = new long[DIAS];   // inicio[i] = medianoche del día i
        Calendar k = Calendar.getInstance();
        k.set(Calendar.HOUR_OF_DAY, 0); k.set(Calendar.MINUTE, 0); k.set(Calendar.SECOND, 0); k.set(Calendar.MILLISECOND, 0);
        for (int i = 0; i < DIAS; i++) { inicio[i] = k.getTimeInMillis(); k.add(Calendar.DAY_OF_MONTH, -1); }
        long ahora = System.currentTimeMillis();

        Dias r = new Dias();
        UsageEvents ev = um.queryEvents(inicio[DIAS - 1], ahora);
        if (ev == null) return null;
        UsageEvents.Event e = new UsageEvents.Event();
        // Una app puede tener varias pantallas al frente seguidas: la sesión dura mientras quede alguna,
        // y pasar de una a otra (o volver en menos de 3 s) no cuenta como otra apertura
        Set<String> alFrente = new HashSet<>();
        long desde = -1, ultimaSalida = -1;
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e);
            if (!paquete.equals(e.getPackageName())) continue;
            int tipo = e.getEventType();
            long ts = e.getTimeStamp();
            if (tipo == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (alFrente.isEmpty()) {
                    desde = ts;
                    if (ultimaSalida < 0 || ts - ultimaSalida > 3000) { r.veces[dia(inicio, ts)]++; r.ultima = ts; }
                }
                alFrente.add(e.getClassName());
            } else if (tipo == UsageEvents.Event.ACTIVITY_PAUSED || tipo == UsageEvents.Event.ACTIVITY_STOPPED) {
                if (alFrente.remove(e.getClassName()) && alFrente.isEmpty() && desde >= 0) {
                    suma(r, inicio, desde, ts, ahora);
                    desde = -1;
                    ultimaSalida = ts;
                }
            }
        }
        if (!alFrente.isEmpty() && desde >= 0) suma(r, inicio, desde, ahora, ahora);
        return r;
    }

    private static int dia(long[] inicio, long ts) {
        for (int i = 0; i < DIAS; i++) if (ts >= inicio[i]) return i;
        return DIAS - 1;
    }

    /** Reparte la sesión [a, b] entre los días que toca (una sesión puede cruzar la medianoche). */
    private static void suma(Dias r, long[] inicio, long a, long b, long ahora) {
        for (int i = 0; i < DIAS; i++) {
            long fin = i == 0 ? ahora : inicio[i - 1];
            long x = Math.max(a, inicio[i]), y = Math.min(b, fin);
            if (y > x) r.ms[i] += y - x;
        }
    }
}
