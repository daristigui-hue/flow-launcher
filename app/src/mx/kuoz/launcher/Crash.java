package mx.kuoz.launcher;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;

/**
 * Sin adb no hay logcat: si la app truena, el error se guarda y se enseña al
 * abrirla otra vez para poder mandar captura.
 */
final class Crash {
    private Crash() {}

    static File archivo(Context c) { return new File(c.getFilesDir(), "crash.txt"); }

    static void instalar(Context c) {
        final File f = archivo(c);
        final Thread.UncaughtExceptionHandler previo = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try (PrintWriter w = new PrintWriter(new FileWriter(f))) {
                w.println(new Date());
                e.printStackTrace(w);
            } catch (Exception ignorado) { }
            if (previo != null) previo.uncaughtException(t, e);
        });
    }

    static String leer(Context c) {
        File f = archivo(c);
        if (!f.exists()) return null;
        try { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }
        catch (Exception e) { return null; }
    }

    static void borrar(Context c) { archivo(c).delete(); }
}
