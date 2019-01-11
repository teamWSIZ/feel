package pm.feel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Boot2Application {

	public static void main(String[] args) {
		SpringApplication.run(Boot2Application.class, args);
	}
}
