package ru.yandex.mail.tests.sendbernar;


import ru.yandex.mail.common.api.CommonApiSettings;
import ru.yandex.mail.common.api.RequestTraits;
import ru.yandex.mail.common.properties.TvmProperties;
import ru.yandex.mail.tests.sendbernar.generated.ApiSendbernar;

public class SendbernarApi {
    public static ApiSendbernar apiSendbernar(RequestTraits traits) {
        return ApiSendbernar.sendbernar(
                ApiSendbernar.Config
                        .sendbernarConfig()
                        .withReqSpecSupplier(
                                CommonApiSettings.requestSpecBuilder(
                                        traits.withServiceTicket(
                                                TvmProperties.props().ticketFor("sendbernar")
                                        )
                                )
                        )
        );
    }
}
