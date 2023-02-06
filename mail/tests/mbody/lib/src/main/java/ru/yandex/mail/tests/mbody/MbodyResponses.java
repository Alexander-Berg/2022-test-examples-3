package ru.yandex.mail.tests.mbody;

import io.restassured.builder.ResponseSpecBuilder;
import org.apache.http.HttpStatus;


public class MbodyResponses {
    public static io.restassured.specification.ResponseSpecification ok200() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }
}
