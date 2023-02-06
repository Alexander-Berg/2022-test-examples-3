package ru.yandex.mail.tests.sendbernar;



import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import static org.hamcrest.Matchers.*;


public class SendbernarResponses {
    public static ResponseSpecification maxEmailAddr400() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody("reason", is("max_email_addr_reached"))
                .expectBody("category", is("mail send"))
                .expectBody("message", is("compose error"))
                .build();
    }

    public static ResponseSpecification ok200() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    public static ResponseSpecification okEmptyBody() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    public static ResponseSpecification wrongUid400() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody("message", is("bb error"))
                .expectBody("reason", startsWith("No such account for uid"))
                .build();
    }

    public static ResponseSpecification noSuchParam400() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .build();
    }

    public static ResponseSpecification virus409() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_CONFLICT)
                .expectBody("category", is("mail send"))
                .expectBody("message", is("so error"))
                .expectBody("reason", is("virus_found"))
                .expectBody("mid", not(empty()))
                .expectBody("fid", not(empty()))
                .build();
    }

    public static ResponseSpecification lightSpam402() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_PAYMENT_REQUIRED)
                .expectBody("category", is("mail send"))
                .expectBody("message", is("captcha requested"))
                .expectBody("reason", is("captcha_request"))
                .expectBody("key", not(empty()))
                .expectBody("url", not(empty()))
                .expectBody("mid", not(empty()))
                .expectBody("fid", not(empty()))
                .build();
    }

    public static ResponseSpecification strongSpam409() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_CONFLICT)
                .expectBody("category", is("mail send"))
                .expectBody("message", is("so error"))
                .expectBody("reason", is("strongspam_found"))
                .expectBody("mid", not(empty()))
                .expectBody("fid", not(empty()))
                .build();
    }

    public static ResponseSpecification storageError400() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody("category", is("mail send"))
                .expectBody("message", is("compose error"))
                .expectBody("reason", is("storage_error"))
                .build();
    }

    public static ResponseSpecification attachOk200() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    public static ResponseSpecification emptyFilename400() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody("category", is("sendbernar"))
                .expectBody("message", is("invalid param"))
                .expectBody("reason", is("No such entry: filename"))
                .build();
    }

}
