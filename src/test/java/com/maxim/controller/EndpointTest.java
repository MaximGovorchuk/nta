package com.maxim.controller;

import com.maxim.TestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WebAppConfiguration
public class EndpointTest {
	@Autowired
	private WebApplicationContext ctx;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
	}

	@Test
	public void shouldReturnStatistics() throws Exception {
		mockMvc.perform(get("/statistics"))
				.andExpect(status().isOk())
				.andExpect(content().string("{\"sum\":0.0,\"avg\":0.0,\"max\":0.0,\"min\":0.0,\"count\":0}"));
	}

	@Test
	public void shouldPostNewTransaction() throws Exception {
		mockMvc.perform(
				post("/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 100.0,\"timestamp\": " + System.currentTimeMillis() + "}")
		)
				.andExpect(status().isCreated())
				.andExpect(content().string(""));
	}

	@Test
	public void shouldReturnNoContentIfTransactionIsOlderThan60Seconds() throws Exception {
		final long moreThanSixtySecondsAgo = Instant.now().minusSeconds(60).minusMillis(1).toEpochMilli();
		mockMvc.perform(
				post("/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 100.0,\"timestamp\": " + moreThanSixtySecondsAgo + "}")
		)
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}
}
