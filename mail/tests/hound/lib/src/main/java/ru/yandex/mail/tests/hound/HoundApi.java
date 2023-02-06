package ru.yandex.mail.tests.hound;

import ru.yandex.mail.common.api.CommonApiSettings;
import ru.yandex.mail.common.api.RequestTraits;
import ru.yandex.mail.common.properties.TvmProperties;
import ru.yandex.mail.tests.hound.generated.ApiHound;


public class HoundApi {
    public static ApiHound apiHound(String url, String xRequestId) {
        return ApiHound.hound(
                ApiHound.Config
                        .houndConfig()
                        .withReqSpecSupplier(
                                CommonApiSettings.requestSpecBuilder(
                                        new RequestTraits()
                                                .withServiceTicket(
                                                        TvmProperties.props().ticketFor("hound")
                                                )
                                                .withUrl(url)
                                                .withXRequestId(xRequestId)
                                )
                        )
        );
    }
}