package ma.shopflow.infra;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HorlogeConfiguration {

    @Bean
    Clock horloge() {
        return Clock.system(ZoneId.of("Africa/Casablanca"));
    }
}
