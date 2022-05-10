package de.ooch.micronaut.bugs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.Preconditions;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@MicronautTest(application = Application.class, environments = { "test", "debug" })
public class RunawayServerMetricsTest {
	@Inject
	@Client("/")
	private HttpClient client;
	
	@Test
	public void testCorsPreflightRequestsDontProduceRunawayServerMetrics() {
		final MutableHttpRequest<?> preflight = HttpRequest //
			.OPTIONS("/greetings/path-should-not-appear-in-server-metrics")
			.header(HttpHeaders.ORIGIN, "http://localhost")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization");
		
		final HttpResponse<?> response = this.client.toBlocking().exchange(preflight);
		Preconditions.condition(response.code() == 200, "CORS preflight should have succeeded");
		
		final HttpResponse<String> scrape = this.client.toBlocking().exchange("/endpoints/prometheus", String.class);
		
		RunawayServerMetricsTest.log.trace("prometheus scrape:\n{}", scrape.body());
		
		Assertions.assertFalse(
			scrape.body().replaceAll("^(?!http_server_requests_.*).*$", "").contains(preflight.getUri().getPath()),
			"input from user requests (including their URI) should not appear in http.server.requests metrics"
		);
	}
	
	@Client("/")
	public static interface GreetingsClient {
		@Get("/greetings/{name}")
		public String sayHallo(@PathVariable("name") String name);
	}
}
