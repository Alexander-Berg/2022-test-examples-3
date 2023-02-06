package ru.yandex.market.crm.triggers.test.helpers;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.core.util.UserEmail;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.ResponseBuilder;

import static ru.yandex.market.mcrm.http.HttpRequest.get;

/**
 * @author apershukov
 */
@Component
public class MarketUtilsHelper {

    private static final String ADDRESSES_TEMPLATE =
            """
                    {
                      "data": {
                        "address": {
                          "mail": "%s"
                        }
                      }
                    }""";

    private final JsonSerializer jsonSerializer;
    private final HttpEnvironment httpEnvironment;

    private final String baseUrl;

    public MarketUtilsHelper(JsonSerializer jsonSerializer,
                             HttpEnvironment httpEnvironment,
                             @Value("${external.persMarketUtils.url}") String baseUrl) {
        this.jsonSerializer = jsonSerializer;
        this.httpEnvironment = httpEnvironment;
        this.baseUrl = baseUrl;
    }

    public void setEmailsForNotification(long puid, Collection<UserEmail> emails) {
        httpEnvironment.when(get(baseUrl + "/api/settings/UID/" + puid + "/emails"))
                .then(
                        ResponseBuilder.newBuilder()
                                .body(jsonSerializer.writeObjectAsBytes(emails))
                                .build()
                );
    }

    public void setUserEmail(long puid, String email) {
        httpEnvironment.when(
                        get(baseUrl + "/getAddress.json")
                                .param("_user_id", String.valueOf(puid))
                )
                .then(
                        ResponseBuilder.newBuilder()
                                .body(String.format(ADDRESSES_TEMPLATE, email))
                                .build()
                );
    }
}
