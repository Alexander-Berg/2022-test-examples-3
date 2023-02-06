package ru.yandex.mail.tests.mbody;

import io.restassured.builder.RequestSpecBuilder;
import ru.yandex.mail.common.api.Headers;
import ru.yandex.mail.tests.mbody.generated.ApiMbody;

import static ru.yandex.mail.common.api.CommonApiSettings.baseWmiSpec;

public class MbodyApi {
    public static ApiMbody apiMbody(String uri, String xRequestId, String v1Ticket) {
        return ApiMbody.mbody(ApiMbody.Config.mbodyConfig()
                .withReqSpecSupplier(() ->
                        new RequestSpecBuilder().addRequestSpecification(baseWmiSpec(uri, xRequestId))
                                .addHeader(Headers.TVM_TICKET, v1Ticket)));
    }
}
