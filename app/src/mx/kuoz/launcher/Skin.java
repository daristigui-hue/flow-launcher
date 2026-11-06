package mx.kuoz.launcher;

/** Las 3 paletas: Carmesí, Medianoche y Dorado. */
final class Skin {
    final String id, nombre;
    final int abismo, marino, azul, cian, hielo, blanco, rojo, fuerte;
    final float rayos;       // opacidad del sol de rayos del fondo
    final int particulas;    // 0 nada (Carmesí), 1 burbujas (Medianoche), 2 pétalos (Dorado)

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
    };

    static Skin por(String id) {
        for (Skin s : TODAS) if (s.id.equals(id)) return s;
        return TODAS[0];
    }

    Skin siguiente() {
        for (int i = 0; i < TODAS.length; i++) if (TODAS[i] == this) return TODAS[(i + 1) % TODAS.length];
        return TODAS[0];
    }
}
