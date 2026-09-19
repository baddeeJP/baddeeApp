package badee.updaterComponent;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires Postgres + Elasticsearch running (docker compose up). "
		+ "Enable when running an integration environment.")
class UpdaterComponentApplicationTests {

	@Test
	void contextLoads() {
	}

}
