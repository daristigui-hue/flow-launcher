package mx.kuoz.launcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Carpeta de apps. Por defecto vive al final de la ola (columna derecha); con
 * su clave() en la lista de favoritas también sale como renglón en el inicio. Las apps se guardan por su
 * clave (componente|usuario), igual que las favoritas.
 */
final class Carpeta {
    final String id;
    String nombre;
    final List<String> claves = new ArrayList<>();

    Carpeta(String id, String nombre) { this.id = id; this.nombre = nombre; }

    /** Clave con la que la carpeta entra a la lista de favoritas. */
    String clave() { return "carpeta:" + id; }

    char inicial() {
        String p = AppEntry.sinAcentos(nombre).trim().toUpperCase();
        return p.isEmpty() ? '?' : p.charAt(0);
    }

    static String aJson(List<Carpeta> l) {
        JSONArray a = new JSONArray();
        try {
            for (Carpeta c : l) {
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("nombre", c.nombre);
                o.put("apps", new JSONArray(c.claves));
                a.put(o);
            }
        } catch (Exception ignorado) { }
        return a.toString();
    }

    static List<Carpeta> deJson(String s) {
        List<Carpeta> l = new ArrayList<>();
        if (s == null) return l;
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Carpeta c = new Carpeta(o.getString("id"), o.getString("nombre"));
                JSONArray apps = o.getJSONArray("apps");
                for (int j = 0; j < apps.length(); j++) c.claves.add(apps.getString(j));
                l.add(c);
            }
        } catch (Exception ignorado) { }
        return l;
    }
}
