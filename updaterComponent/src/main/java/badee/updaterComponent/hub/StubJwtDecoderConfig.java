package badee.updaterComponent.hub;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Temporary stand-in for the real (Cognito) JWT verification, active only while
 * {@code updater.admin.stub-auth=true} (the default). It accepts a single
 * configured dev bearer token ({@code updater.admin.dev-token}) and mints a
 * minimal {@link Jwt}; any other token is rejected.
 *
 * <p>To switch to Cognito: set {@code updater.admin.stub-auth=false} and provide
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}. This bean then
 * backs off and Spring Boot's auto-configured, signature-validating decoder
 * takes over. This class can be deleted entirely once Cognito is live.
 */
@Configuration
@ConditionalOnProperty(name = "updater.admin.stub-auth", havingValue = "true", matchIfMissing = true)
public class StubJwtDecoderConfig {

	@Bean
	JwtDecoder jwtDecoder(@Value("${updater.admin.dev-token:dev-token}") String devToken) {
		return token -> {
			if (!devToken.equals(token)) {
				throw new BadJwtException("Invalid dev token (stub auth)");
			}
			Instant now = Instant.now();
			return Jwt.withTokenValue(token)
					.header("alg", "none")
					.subject("dev-admin")
					.claim("scope", "admin")
					.issuedAt(now)
					.expiresAt(now.plusSeconds(3600))
					.build();
		};
	}
}
