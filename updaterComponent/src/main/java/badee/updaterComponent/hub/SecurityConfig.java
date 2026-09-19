package badee.updaterComponent.hub;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Secures the manual-trigger admin API with token-based (OAuth2 Bearer / JWT)
 * authentication via a Spring Security filter chain.
 *
 * <p>The intended production identity provider is AWS Cognito: set
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} to the Cognito
 * user-pool issuer and Spring Boot auto-configures a validating {@link
 * org.springframework.security.oauth2.jwt.JwtDecoder}. Until Cognito is wired
 * up, {@link StubJwtDecoderConfig} supplies a stub decoder that accepts a single
 * configured dev token, so the chain is already token-based and swapping in the
 * real IdP requires no code change here.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/admin/**")
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
				// Stateless token-authenticated API: no session, no CSRF.
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.csrf(csrf -> csrf.disable());
		return http.build();
	}
}
