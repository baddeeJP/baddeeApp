package badee.updaterComponent.hub;

import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Local-development stand-in for the real (Cognito) JWT verification, active
 * only while {@code updater.admin.stub-auth=true} (the default). It accepts a
 * single configured dev bearer token ({@code updater.admin.dev-token}) and
 * mints a minimal Cognito-shaped {@link Jwt} carrying the required admin group;
 * any other token is rejected.
 *
 * <p>To switch to Cognito: set {@code updater.admin.stub-auth=false} and
 * configure {@link CognitoJwtDecoderConfig}; this bean then backs off. As a
 * guard against shipping the dev token to an environment that has Cognito
 * configured, start-up fails if the stub is on while an issuer is set.
 */
@Configuration
@ConditionalOnProperty(name = "updater.admin.stub-auth", havingValue = "true", matchIfMissing = true)
public class StubJwtDecoderConfig {

	@Bean
	JwtDecoder jwtDecoder(@Value("${updater.admin.dev-token:dev-token}") String devToken,
			@Value("${updater.admin.required-group:updater-admin}") String requiredGroup,
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri) {
		if (!issuerUri.isBlank()) {
			throw new IllegalStateException("A JWT issuer-uri is configured but updater.admin.stub-auth is still "
					+ "on, which would accept the dev token; set updater.admin.stub-auth=false to use the issuer");
		}
		return token -> {
			if (!devToken.equals(token)) {
				throw new BadJwtException("Invalid dev token (stub auth)");
			}
			Instant now = Instant.now();
			return Jwt.withTokenValue(token)
					.header("alg", "none")
					.subject("dev-admin")
					.claim("token_use", "access")
					.claim("cognito:groups", List.of(requiredGroup))
					.issuedAt(now)
					.expiresAt(now.plusSeconds(3600))
					.build();
		};
	}
}
