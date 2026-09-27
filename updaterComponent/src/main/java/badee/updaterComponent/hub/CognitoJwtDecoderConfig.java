package badee.updaterComponent.hub;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Real JWT verification against an AWS Cognito user pool, active when
 * {@code updater.admin.stub-auth=false}. Required configuration:
 *
 * <pre>
 * spring.security.oauth2.resourceserver.jwt.issuer-uri=https://cognito-idp.&lt;region&gt;.amazonaws.com/&lt;userPoolId&gt;
 * updater.admin.cognito.client-ids=&lt;app client id&gt;[,&lt;another&gt;]
 * </pre>
 *
 * <p>Signatures are checked against the pool's JWKS (discovered from the
 * issuer on first use, so start-up doesn't depend on Cognito being reachable).
 * On top of the default expiry/issuer checks, only Cognito <em>access</em>
 * tokens issued to an allow-listed app client are accepted. Cognito access
 * tokens carry no {@code aud}, so {@code client_id} is the claim that says
 * which app the token was minted for; ID tokens are rejected because they are
 * meant for the client, not as API credentials. Which users may call the admin
 * API is decided separately by group membership, see {@link SecurityConfig}.
 */
@Configuration
@ConditionalOnProperty(name = "updater.admin.stub-auth", havingValue = "false")
public class CognitoJwtDecoderConfig {

	@Bean
	JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
			@Value("${updater.admin.cognito.client-ids:}") List<String> clientIds) {
		if (issuerUri.isBlank()) {
			throw new IllegalStateException("updater.admin.stub-auth=false requires "
					+ "spring.security.oauth2.resourceserver.jwt.issuer-uri (the Cognito user-pool issuer)");
		}
		if (clientIds.isEmpty()) {
			throw new IllegalStateException("updater.admin.stub-auth=false requires "
					+ "updater.admin.cognito.client-ids (the Cognito app client ids allowed to call the admin API)");
		}
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
		decoder.setJwtValidator(accessTokenValidator(issuerUri, clientIds));
		return decoder;
	}

	/** Expiry + issuer, plus Cognito's {@code token_use=access} and an allow-listed {@code client_id}. */
	static OAuth2TokenValidator<Jwt> accessTokenValidator(String issuerUri, Collection<String> clientIds) {
		Set<String> allowedClientIds = Set.copyOf(clientIds);
		return new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(issuerUri),
				new JwtClaimValidator<String>("token_use", "access"::equals),
				new JwtClaimValidator<String>("client_id",
						clientId -> clientId != null && allowedClientIds.contains(clientId)));
	}
}
