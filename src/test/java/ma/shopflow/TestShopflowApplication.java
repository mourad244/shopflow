package ma.shopflow;

import org.springframework.boot.SpringApplication;

/** Lance l'application avec une base PostgreSQL fournie par Testcontainers. */
public class TestShopflowApplication {

    public static void main(String[] args) {
        SpringApplication.from(ShopflowApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
