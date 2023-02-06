package ru.yandex.mail.tests.hound;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.IsNot.not;

public class HoundResponses {
    public static ResponseSpecification ok200() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody(".", not(hasKey("error")))
                .build();
    }
}
