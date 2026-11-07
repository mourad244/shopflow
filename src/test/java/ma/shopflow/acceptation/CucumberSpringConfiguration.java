package ma.shopflow.acceptation;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

import io.cucumber.spring.CucumberContextConfiguration;
import ma.shopflow.TestcontainersConfiguration;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
public class CucumberSpringConfiguration {
}
