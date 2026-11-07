package ma.shopflow.acceptation;

import java.time.Clock;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDate;
import java.time.ZoneId;

/** Permet à un scénario de fixer la date vue par l'application. */
public class HorlogeReglable extends Clock {

    private static final ZoneId CASABLANCA = ZoneId.of("Africa/Casablanca");
    private volatile Instant maintenant = Instant.now();

    public void regler(LocalDate date) {
        maintenant = date.atTime(10, 0).atZone(CASABLANCA).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return CASABLANCA;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        InstantSource source = this::instant;
        return source.withZone(zone);
    }

    @Override
    public Instant instant() {
        return maintenant;
    }
}
