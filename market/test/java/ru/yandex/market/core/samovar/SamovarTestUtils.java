package ru.yandex.market.core.samovar;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

import NCrawl.Feeds;
import org.assertj.core.api.Assertions;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.core.feed.validation.model.ValidationInfoWrapper;
import ru.yandex.market.core.logbroker.samovar.SamovarEvent;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.core.misc.resource.ResourceAccessCredentials;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

/**
 * Класс утилита для тестирования интеграции с самоваром
 * Date: 27.10.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class SamovarTestUtils {

    private SamovarTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static void assertSamovarEvent(SamovarEvent samovarEvent,
                                          ValidationInfoWrapper<?, ?> viw) throws IOException {
        Assertions.assertThat(samovarEvent)
                .isNotNull();

        RemoteResource remoteResource = viw.getRemoteResource();

        Feeds.TFeedExt payload = samovarEvent.getPayload();
        Assertions.assertThat(payload.getUrl())
                .isEqualTo(remoteResource.url());
        Assertions.assertThat(payload.getFeedName())
                .isEqualTo("test-feed");

        Feeds.TFeedContext feedContext = payload.getFeedContext();
        Feeds.TFeedContext.TAuthData authData = feedContext.getAuthData();
        SamovarContextOuterClass.SamovarContext samovarContext = SamovarContextOuterClass.SamovarContext
                .parseFrom(new ByteArrayInputStream(feedContext.getBytesValue().toByteArray()));

        Assertions.assertThat(samovarContext.getEnvironment())
                .isEqualTo(EnvironmentType.DEVELOPMENT.getValue());
        Assertions.assertThat(samovarContext.getRequestId())
                .isEqualTo(RequestContextHolder.getContext().getRequestId());

        SamovarContextOuterClass.ValidationFeedInfo validationFeed = samovarContext.getValidationFeeds(0);
        Assertions.assertThat(validationFeed.getValidationId())
                .isEqualTo(viw.getId());
        Assertions.assertThat(viw.getPartnerId())
                .isEqualTo(viw.getPartnerId());

        Assertions.assertThat(validationFeed.getCampaignType())
                .isEqualTo(viw.getCampaignType().getId());

        if (remoteResource.credentials().isEmpty()) {
            Assertions.assertThat(authData.hasLogin())
                    .isFalse();
            Assertions.assertThat(authData.hasPassword())
                    .isFalse();
        } else {
            ResourceAccessCredentials credentials = remoteResource.credentials()
                    .get();
            Assertions.assertThat(authData.getLogin())
                    .isEqualTo(credentials.login());
            Assertions.assertThat(authData.getPassword())
                    .isEqualTo(credentials.password());
        }
    }
}
