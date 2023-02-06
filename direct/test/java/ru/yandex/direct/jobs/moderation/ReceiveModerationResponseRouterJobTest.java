package ru.yandex.direct.jobs.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.moderation.model.AbstractModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.bannerstorage.BannerstorageCreativeModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.bannerstorage.BannerstorageCreativeModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.displayhrefs.DisplayHrefsModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.displayhrefs.DisplayHrefsModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.ess.common.logbroker.LogbrokerClientFactoryFacade;
import ru.yandex.direct.jobs.moderation.config.DestConfig;
import ru.yandex.direct.jobs.moderation.config.ResponseModerationParameters;
import ru.yandex.direct.jobs.moderation.config.ResponseRoutingLogbrokerConsumerPropertiesHolder;
import ru.yandex.direct.jobs.moderation.config.ResponseRoutingParameters;
import ru.yandex.direct.jobs.moderation.config.TopicWithGroup;
import ru.yandex.direct.scheduler.hourglass.implementations.TaskParametersMapImpl;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiveModerationResponseRouterJobTest {
    @Mock
    private ResponseRoutingLogbrokerConsumerPropertiesHolder consumerPropertiesHolder;
    @Mock
    private ReceiveModerationService receiveModerationService;
    @Mock
    private ShardHelper shardHelper;
    @Mock
    private PpcPropertiesSupport propertiesSupport;
    @Mock
    private LogbrokerClientFactoryFacade logbrokerClientFactory;

    private DestConfig defaultTopic;
    private DestConfig bannerstorageTopic;
    private DestConfig adImageTopic;
    private DestConfig html5Topic;

    private ReceiveModerationResponseRouterJob job;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        //
        var consumerProperties =
                new ResponseRoutingLogbrokerConsumerPropertiesHolder(
                        "localhost", "consumer", 60, 15, 10, null, "read-topic", List.of(1, 2)
                );
        var producerProperties = new ResponseRoutingParameters.ProducerProperties(
                "localhost", 60, 10, CompressionCodec.GZIP
        );
        //
        defaultTopic = new DestConfig("default-write-topic", 2);
        bannerstorageTopic = new DestConfig("bannerstorage-creatives-topic", 1);
        adImageTopic = new DestConfig("ad_images_and_html5-topic", 1);
        html5Topic = new DestConfig("ad_images_and_html5-topic", 3);
        //
        var routingParameters = new ResponseRoutingParameters(
                consumerProperties,
                producerProperties,
                defaultTopic,
                Map.of(
                        ModerationObjectType.BANNERSTORAGE_CREATIVES,
                        bannerstorageTopic,
                        ModerationObjectType.AD_IMAGE,
                        adImageTopic,
                        ModerationObjectType.HTML5,
                        html5Topic
                )
        );
        //
        job = new ReceiveModerationResponseRouterJob(
                new ResponseModerationParameters.Builder().setLogbrokerNoCommit(true).build(),
                consumerPropertiesHolder,
                receiveModerationService,
                emptyList(),
                shardHelper,
                propertiesSupport,
                routingParameters,
                logbrokerClientFactory
        ) {
            @SuppressWarnings("rawtypes")
            @Override
            Map<Integer, List<AbstractModerationResponse>> groupByShard(@NotNull List<AbstractModerationResponse> responses) {
                return StreamEx.of(responses)
                        // для теста сделаем номер шарда численно равным clientId
                        .groupingBy(r -> (int) r.getMeta().getClientId());
            }
        };
        job.initialize(new TaskParametersMapImpl(Map.of("param", "read-topic__1")));
    }

    @Test
    public void testCreateLogbrokerWriters() {
        var logbrokerWriters = job.createLogbrokerWriters();

        assertThat(logbrokerWriters).containsKeys(
                new TopicWithGroup("ad_images_and_html5-topic", 1),
                new TopicWithGroup("ad_images_and_html5-topic", 2),
                new TopicWithGroup("ad_images_and_html5-topic", 3),
                new TopicWithGroup("bannerstorage-creatives-topic", 1),
                new TopicWithGroup("default-write-topic", 1),
                new TopicWithGroup("default-write-topic", 2)
        );
    }

    @Test
    public void testGetDestinationTopic() {
        var bannerLogoReponse = new BannerAssetModerationResponse();
        bannerLogoReponse.setType(ModerationObjectType.BANNER_LOGOS);

        var bannerstorageCreativeResponse = new BannerstorageCreativeModerationResponse();
        bannerstorageCreativeResponse.setType(ModerationObjectType.BANNERSTORAGE_CREATIVES);

        var adImageResponse = new BannerModerationResponse();
        adImageResponse.setType(ModerationObjectType.AD_IMAGE);

        var html5Response = new BannerModerationResponse();
        html5Response.setType(ModerationObjectType.HTML5);

        var emptyTypeResponse = new BannerModerationResponse();

        assertThat(job.getDestinationTopic(bannerLogoReponse)).isEqualTo(defaultTopic);
        assertThat(job.getDestinationTopic(bannerstorageCreativeResponse)).isEqualTo(bannerstorageTopic);
        assertThat(job.getDestinationTopic(adImageResponse)).isEqualTo(adImageTopic);
        assertThat(job.getDestinationTopic(html5Response)).isEqualTo(html5Topic);
        assertThat(job.getDestinationTopic(emptyTypeResponse)).isEqualTo(defaultTopic);
    }

    @Test
    public void testGroupByWriteGroup() {
        //noinspection rawtypes
        List<AbstractModerationResponse> responses = new ArrayList<>();
        {
            var r = new BannerAssetModerationResponse();
            r.setType(ModerationObjectType.BANNER_LOGOS);
            r.setMeta(new BannerAssetModerationMeta());
            r.getMeta().setClientId(1L);
            responses.add(r);
        }
        {
            var r = new BannerAssetModerationResponse();
            r.setType(ModerationObjectType.BANNER_BUTTONS);
            r.setMeta(new BannerAssetModerationMeta());
            r.getMeta().setClientId(2L);
            responses.add(r);
        }
        {
            var r = new BannerstorageCreativeModerationResponse();
            r.setType(ModerationObjectType.BANNERSTORAGE_CREATIVES);
            r.setMeta(new BannerstorageCreativeModerationMeta());
            r.getMeta().setClientId(3L);
            responses.add(r);
        }
        {
            var r = new BannerstorageCreativeModerationResponse();
            r.setType(ModerationObjectType.BANNERSTORAGE_CREATIVES);
            r.setMeta(new BannerstorageCreativeModerationMeta());
            r.getMeta().setClientId(4L);
            responses.add(r);
        }
        {
            var r = new DisplayHrefsModerationResponse();
            r.setType(ModerationObjectType.DISPLAYHREFS);
            r.setMeta(new DisplayHrefsModerationMeta());
            responses.add(r);
        }
        var grouped = job.groupByWriteGroup(responses, new DestConfig("default-write-topic", 2));

        assertThat(grouped.get(1)).allMatch(t -> t.getMeta().getClientId() % 2 == 1 || t.getMeta().getClientId() == 0);
        assertThat(grouped.get(2)).allMatch(t -> t.getMeta().getClientId() % 2 == 0 && t.getMeta().getClientId() != 0);
    }
}
