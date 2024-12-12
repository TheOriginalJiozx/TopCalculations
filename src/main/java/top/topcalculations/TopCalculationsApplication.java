package top.topcalculations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TopCalculationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TopCalculationsApplication.class, args);
    }

}
