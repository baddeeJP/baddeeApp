package badee.updaterComponent.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class CognitoJwtDecoderConfigTest {

	private static final String ISSUER = "https://cognito-idp.ap-northeast-1.amazonaws.com/ap-northeast-1_Example";
	private static final String CLIENT_ID = "updater-admin-client";

	private final OAuth2TokenValidator<Jwt> validator =
			CognitoJwtDecoderConfig.accessTokenValidator(ISSUER, List.of(CLIENT_ID));

	@Test
	void acceptsAccessTokenFromTheIssuerForAnAllowedClient() {
		assertFalse(validator.validate(token(jwt -> { })).hasErrors());
	}

	@Test
	void rejectsTokenFromAnotherIssuer() {
		assertTrue(validator.validate(token(jwt -> jwt.issuer("https://evil.example.com"))).hasErrors());
	}

	@Test
	void rejectsIdTokens() {
		assertTrue(validator.validate(token(jwt -> jwt.claim("token_use", "id"))).hasErrors());
	}

	@Test
	void rejectsTokenForAnotherAppClient() {
		assertTrue(validator.validate(token(jwt -> jwt.claim("client_id", "end-user-app"))).hasErrors());
	}

	@Test
	void rejectsTokenWithoutClientId() {
		assertTrue(validator.validate(token(jwt -> jwt.claims(claims -> claims.remove("client_id")))).hasErrors());
	}

	@Test
	void rejectsExpiredToken() {
		Instant past = Instant.now().minusSeconds(7200);
		assertTrue(validator.validate(token(jwt -> jwt.issuedAt(past).expiresAt(past.plusSeconds(60)))).hasErrors());
	}

	@Test
	void refusesToStartWithoutIssuerOrClientIds() {
		CognitoJwtDecoderConfig config = new CognitoJwtDecoderConfig();
		assertThrows(IllegalStateException.class, () -> config.jwtDecoder("", List.of(CLIENT_ID)));
		assertThrows(IllegalStateException.class, () -> config.jwtDecoder(ISSUER, List.of()));
	}

	@Test
	void mapsCognitoGroupsToGroupAuthorities() {
		Jwt jwt = token(builder -> builder.claim("cognito:groups", List.of("updater-admin", "beta")));

		List<String> authorities = SecurityConfig.cognitoGroupsAuthenticationConverter().convert(jwt)
				.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				// Spring Security 7 also adds a FACTOR_BEARER authority; only the groups matter here.
				.filter(authority -> authority.startsWith("GROUP_")).toList();

		assertEquals(List.of("GROUP_updater-admin", "GROUP_beta"), authorities);
	}

	@Test
	void stubRefusesToStartWhenARealIssuerIsConfigured() {
		assertThrows(IllegalStateException.class,
				() -> new StubJwtDecoderConfig().jwtDecoder("dev-token", "updater-admin", ISSUER));
	}

	/** A valid Cognito-shaped access token, customised per test. */
	private static Jwt token(Consumer<Jwt.Builder> customizer) {
		Instant now = Instant.now();
		Jwt.Builder builder = Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.issuer(ISSUER)
				.subject("admin-user")
				.claim("token_use", "access")
				.claim("client_id", CLIENT_ID)
				.issuedAt(now)
				.expiresAt(now.plusSeconds(3600));
		customizer.accept(builder);
		return builder.build();
	}
}
