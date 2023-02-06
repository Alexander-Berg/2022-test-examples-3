package ru.yandex.mail.common.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static java.lang.String.format;


public class RestAssuredLoggingFilter implements Filter {
    private Logger logger;

    private Level priority;

    private RequestLogger requestLogger;


    public RestAssuredLoggingFilter(Logger logger, boolean isLocalDebug) {
        this.priority = Level.INFO;
        this.logger = logger;
        this.requestLogger = isLocalDebug ? new LocalLogger() : new HtmlLogger();
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec, FilterContext ctx) {
        Response response = null;
        try {
            response = ctx.next(requestSpec, responseSpec);
            String body = response.asString();

            if (response.contentType().equals("application/json")) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                body = gson.toJson(new JsonParser().parse(body));
            }

            logger.log(priority, requestLogger.log(requestSpec, response, body));

            return response;
        } catch (Exception e) {
            logger.error(format("FAILED TO LOG: %s: %s\n%s", requestSpec.getMethod(), requestSpec.getURI(), e.toString()));
        }

        return response;
    }
}
