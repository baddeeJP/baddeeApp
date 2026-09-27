package badee.updaterComponent.hub;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import badee.updaterComponent.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/** The admin trigger API, through the real security filter chain and (stub) token decoder. */
class AdminApiIT extends IntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rejectsRequestsWithoutAToken() throws Exception {
		mockMvc.perform(get("/admin/updates")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/admin/updates")).andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsAnUnknownToken() throws Exception {
		mockMvc.perform(get("/admin/updates").header("Authorization", "Bearer not-the-dev-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void devTokenListsEveryRegisteredSpoke() throws Exception {
		mockMvc.perform(get("/admin/updates").header("Authorization", "Bearer dev-token"))
				.andExpect(status().isOk())
				.andExpect(content().json("[\"jmdict\", \"jmnedict\", \"kanji\", \"tatoeba\"]"));
	}

	@Test
	void unknownSourceIsNotFoundOnceAuthorized() throws Exception {
		mockMvc.perform(post("/admin/updates/nope").header("Authorization", "Bearer dev-token"))
				.andExpect(status().isNotFound());
	}

	@Test
	void authenticatedCallerOutsideTheAdminGroupIsForbidden() throws Exception {
		mockMvc.perform(get("/admin/updates")
						.with(jwt().authorities(new SimpleGrantedAuthority("GROUP_learners"))))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/admin/updates/jmdict")
						.with(jwt().authorities(new SimpleGrantedAuthority("GROUP_learners"))))
				.andExpect(status().isForbidden());
	}
}
