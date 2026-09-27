package badee.updaterComponent.hub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Secures the manual-trigger admin API with token-based (OAuth2 Bearer / JWT)
 * authentication via a Spring Security filter chain.
 *
 * <p>The production identity provider is AWS Cognito ({@link
 * CognitoJwtDecoderConfig}, enabled with {@code updater.admin.stub-auth=false}).
 * Until a user pool is wired up, {@link StubJwtDecoderConfig} supplies a stub
 * decoder that accepts a single configured dev token, so the chain is already
 * token-based and swapping in the real IdP requires no code change here.
 *
 * <p>Authentication alone isn't enough: the user pool may also hold ordinary
 * app users, so the caller must be in the Cognito group named by {@code
 * updater.admin.required-group} (read from the {@code cognito:groups} claim).
 * Authenticated callers without it get 403.
 */
@Configuration
public class SecurityConfig {

	static final String GROUP_AUTHORITY_PREFIX = "GROUP_";

	@Bean
	SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
			@Value("${updater.admin.required-group:updater-admin}") String requiredGroup) throws Exception {
		http
				.securityMatcher("/admin/**")
				.authorizeHttpRequests(auth -> auth.anyRequest()
						.hasAuthority(GROUP_AUTHORITY_PREFIX + requiredGroup))
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
						.jwtAuthenticationConverter(cognitoGroupsAuthenticationConverter())))
				// Stateless token-authenticated API: no session, no CSRF.
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.csrf(csrf -> csrf.disable());
		return http.build();
	}

	/** Maps each entry of the {@code cognito:groups} claim to a {@code GROUP_<name>} authority. */
	static JwtAuthenticationConverter cognitoGroupsAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter groups = new JwtGrantedAuthoritiesConverter();
		groups.setAuthoritiesClaimName("cognito:groups");
		groups.setAuthorityPrefix(GROUP_AUTHORITY_PREFIX);
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(groups);
		return converter;
	}
}
