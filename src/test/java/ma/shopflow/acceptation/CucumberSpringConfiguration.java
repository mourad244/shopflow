package ma.shopflow.acceptation;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import io.cucumber.spring.CucumberContextConfiguration;
import ma.shopflow.TestcontainersConfiguration;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, CucumberSpringConfiguration.Horloge.class})
public class CucumberSpringConfiguration {

    @TestConfiguration(proxyBeanMethods = false)
    static class Horloge {

        @Bean
        @Primary
        HorlogeReglable horlogeReglable() {
            return new HorlogeReglable();
        }
    }
}
