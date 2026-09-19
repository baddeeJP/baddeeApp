package badee.updaterComponent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UpdaterComponentApplication {

	public static void main(String[] args) {
		SpringApplication.run(UpdaterComponentApplication.class, args);
	}

}
