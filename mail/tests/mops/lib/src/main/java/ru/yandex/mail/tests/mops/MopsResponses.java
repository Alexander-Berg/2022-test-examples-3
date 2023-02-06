package ru.yandex.mail.tests.mops;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;


public class MopsResponses {
    public static ResponseSpecification ok(){
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    public static ResponseSpecification okSync() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("taskType", equalTo("sync"))
                .expectBody("taskGroupId", is(nullValue()))
                .build();
    }

        public static ResponseSpecification okAsync() {
            return new ResponseSpecBuilder()
                    .expectStatusCode(HttpStatus.SC_OK)
                    .expectBody("taskType", equalTo("async"))
                    .expectBody("taskGroupId", not(isEmptyString()))
                    .build();
        }

        public static ResponseSpecification invalidRequest() {
            return new ResponseSpecBuilder()
                    .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                    .expectBody("result", equalTo("invalid request"))
                    .build();
        }

        public static ResponseSpecification invalidRequest(Matcher<?> messageMatcher) {
            return new ResponseSpecBuilder()
                    .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                    .expectBody("result", equalTo("invalid request"))
                    .expectBody("error", messageMatcher)
                    .build();
        }

        public static ResponseSpecification internalError() {
            return new ResponseSpecBuilder()
                    .expectStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .expectBody("result", equalTo("internal error"))
                    .build();
        }

        public static ResponseSpecification internalError(Matcher<?> messageMatcher) {
            return new ResponseSpecBuilder()
                    .expectStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .expectBody("result", equalTo("internal error"))
                    .expectBody("error", messageMatcher)
                    .build();
        }
    }
