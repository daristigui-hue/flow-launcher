package mx.kuoz.launcher;

/**
 * Resorte amortiguado: lo que da el "rebote" de Persona. Con amortiguación < 1
 * se pasa del objetivo y regresa, como los menús de P5.
 */
final class Resorte {
    float pos, vel, objetivo;
    private final float k, c;

    Resorte(float rigidez, float amortiguacion) {
        k = rigidez;
        c = 2f * amortiguacion * (float) Math.sqrt(rigidez);
    }

    /** Avanza dt segundos. Devuelve true mientras siga moviéndose. */
    boolean paso(float dt, float quieto) {
        if (dt > 1f / 30f) dt = 1f / 30f;     // un frame tardío no lo dispara
        float a = -k * (pos - objetivo) - c * vel;
        vel += a * dt;
        pos += vel * dt;
        if (Math.abs(pos - objetivo) < quieto && Math.abs(vel) < quieto * 4) {
            pos = objetivo; vel = 0; return false;
        }
        return true;
    }

    void fija(float v) { pos = objetivo = v; vel = 0; }
}
