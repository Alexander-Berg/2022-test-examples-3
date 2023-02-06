package ru.yandex.mail.tests.akita;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.Cookies;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import ru.yandex.mail.common.api.CommonApiSettings;
import ru.yandex.mail.common.api.RequestTraits;
import ru.yandex.mail.common.properties.TvmProperties;
import ru.yandex.mail.tests.akita.generated.ApiAkita;

import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.mail.common.properties.CoreProperties.props;

public class AkitaApi {
    public static ResponseSpecification okAuth() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("account_information", notNullValue())
                .build();
    }

    public static ApiAkita apiAkita(Cookies cookies) {
        return ApiAkita.akita(ApiAkita.Config.akitaConfig()
                .withReqSpecSupplier(
                        CommonApiSettings.requestSpecBuilder(
                                new RequestTraits()
                                        .withUrl(AkitaProperties.properties().akitaUri())
                                        .withXRequestId(props().getCurrentRequestId())
                                        .withServiceTicket(
                                                TvmProperties.props().ticketFor("akita")
                                        ),
                                cookies
                        )
                )
        );
    }

    public static ApiAkita apiAkitaWithoutAuth() {
        return ApiAkita.akita(ApiAkita.Config.akitaConfig()
                .withReqSpecSupplier(
                        CommonApiSettings.requestSpecBuilder(
                                new RequestTraits()
                                        .withUrl(AkitaProperties.properties().akitaUri())
                                        .withXRequestId(props().getCurrentRequestId())
                                        .withServiceTicket(
                                                TvmProperties.props().ticketFor("akita")
                                        )
                        )
                )
        );
    }
}
