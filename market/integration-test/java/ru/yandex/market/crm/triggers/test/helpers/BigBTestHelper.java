package ru.yandex.market.crm.triggers.test.helpers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import yabs.proto.Profile;

import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.HttpRequest;
import ru.yandex.market.mcrm.http.ResponseBuilder;

import static ru.yandex.market.mcrm.http.HttpRequest.get;

/**
 * @author apershukov
 */
@Component
public class BigBTestHelper {

    public static Profile profile(long cryptaId) {
        return Profile.newBuilder()
                .setUserIdentifiers(
                        Profile.TUserIdentifiers.newBuilder()
                                .setCryptaId(cryptaId)
                )
                .build();
    }

    @NotNull
    private static HttpRequest createRequest(Uid id) {
        HttpRequest request = get("http://bigb-fast.yandex.ru/bigb")
                .param("client", "market-mcrm")
                .param("format", "protobuf");

        switch (id.getType()) {
            case PUID -> request.param("puid", id.getValue());
            case UUID -> request.param("uuid", id.getValue());
            case EMAIL -> request.param("email", id.getValue());
            default -> throw new IllegalArgumentException("Id of type " + id.getType() + " is now supported yet. " +
                    "See https://wiki.yandex-team.ru/bigb/how-to-realtime/");
        }
        return request;
    }

    private final HttpEnvironment httpEnvironment;

    public BigBTestHelper(HttpEnvironment httpEnvironment) {
        this.httpEnvironment = httpEnvironment;
    }

    public void prepareProfile(Uid id, Profile profile) {
        HttpRequest request = createRequest(id);

        httpEnvironment.when(request)
                .then(
                        ResponseBuilder.newBuilder()
                                .body(profile.toByteArray())
                                .build()
                );
    }

    public void prepareNotFound(Uid id) {
        prepareProfile(id, Profile.getDefaultInstance());
    }
}
