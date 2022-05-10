package de.ooch.micronaut.bugs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.Preconditions;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@MicronautTest(application = Application.class, environments = { "test", "debug" })
public class RunawayClientMetricsTest {
	@Inject
	@Client("/")
	private HttpClient rawClient;
	
	@Inject
	private GreetingsClient templatedClient;
	
	@Test
	public void testRawClientDoesNotProduceRunawayMetrics() {
		final MutableHttpRequest<?> greetings = HttpRequest.GET("/greetings/path-should-not-appear-in-client-metrics");
		
		final HttpResponse<?> response = this.rawClient.toBlocking().exchange(greetings);
		Preconditions.condition(response.status().getCode() == 200, "test request should have worked");
		
		final HttpResponse<String> scrape = this.rawClient.toBlocking().exchange("/endpoints/prometheus", String.class);
		RunawayClientMetricsTest.log.trace("prometheus scrape:\n{}", scrape.body());
		
		Assertions.assertFalse(
			scrape.body().replaceAll("^(?!http_client_requests_.*).*$", "").contains(greetings.getPath()),
			"manually defined URI paths should not appear in http.client.requests metrics"
		);
	}
	
	@Test
	public void testTemplatedClientDoesNotProduceRunawayMetrics() {
		final String parameter = "parameter-value-should-not-appear-in-client-metrics";
		
		final HttpResponse<String> response = this.templatedClient.getGreetings(parameter);
		Preconditions.condition(response.status().getCode() == 200, "test request should have worked");
		
		final HttpResponse<String> scrape = this.rawClient.toBlocking().exchange("/endpoints/prometheus", String.class);
		RunawayClientMetricsTest.log.trace("prometheus scrape:\n{}", scrape.body());
		
		Assertions.assertFalse(
			scrape.body().replaceAll("^(?!http_client_requests_.*).*$", "").contains(parameter),
			"templated parameter value should not appear in http.client.requests metrics"
		);
	}
	
	@Client("/")
	public static interface GreetingsClient {
		@Get("/greetings/{name}")
		@Consumes("text/plain")
		public HttpResponse<String> getGreetings(@PathVariable("name") String name);
	}
}
