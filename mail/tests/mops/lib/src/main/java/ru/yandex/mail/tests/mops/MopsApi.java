package ru.yandex.mail.tests.mops;


import io.restassured.builder.RequestSpecBuilder;
import ru.yandex.mail.common.api.Headers;
import ru.yandex.mail.tests.mops.generated.ApiMops;

import static ru.yandex.mail.common.api.CommonApiSettings.baseWmiSpec;
import static ru.yandex.mail.common.properties.CoreProperties.props;

public class MopsApi {
    public static final String clientType = "type";
    public static final String clientVersion = "version";

    public static ApiMops apiMops(String url, String ticket) {
        return ApiMops.mops(ApiMops.Config.mopsConfig()
                .withReqSpecSupplier(() ->
                        new RequestSpecBuilder().addRequestSpecification(baseWmiSpec(url, props().getCurrentRequestId()))
                                .setBaseUri(url)
                                .addHeader(Headers.CLIENT_TYPE, clientType)
                                .addHeader(Headers.CLIENT_VERSION, clientVersion)
                                .addHeader(Headers.TVM_TICKET, ticket)));
    }
}
