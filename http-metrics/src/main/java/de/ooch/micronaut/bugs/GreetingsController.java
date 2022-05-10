package de.ooch.micronaut.bugs;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Produces;

@Controller
public class GreetingsController {
	@Get("/greetings/{name}")
	@Produces("text/plain")
	public HttpResponse<String> sayIt(@PathVariable("name") final String name) {
		return HttpResponse.ok(String.format("Hallo, %s!", name));
	}
}
