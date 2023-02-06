package ru.yandex.direct.intapi.entity.moderation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerMulticard;
import ru.yandex.direct.core.entity.banner.model.BannerMulticardSetStatusModerate;
import ru.yandex.direct.core.entity.moderation.model.ModResyncQueueObj;
import ru.yandex.direct.core.entity.moderation.model.ModResyncQueueObjectType;
import ru.yandex.direct.core.testing.info.BannerImageFormatInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModResyncQueueRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.moderation.model.modresync.ImportToResyncQueueRequest;
import ru.yandex.direct.result.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@IntApiTest
@ParametersAreNonnullByDefault
@RunWith(SpringJUnit4ClassRunner.class)
public class ModResyncQueueImportServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private ModResyncQueueImportService modResyncQueueImportService;

    @Autowired
    private TestModResyncQueueRepository testModResyncQueueRepository;

    @Before
    public void before() {
        testModResyncQueueRepository.clear(1);
        testModResyncQueueRepository.clear(2);
    }

    @Test
    public void bannersInQueueAreValidWhenAdded() {
        Map<Integer, List<Long>> shardToBannerIds = addBannersToQueue();

        checkObjectsInShard(1, ModResyncQueueObjectType.BANNER, shardToBannerIds.get(1));
        checkObjectsInShard(2, ModResyncQueueObjectType.BANNER, shardToBannerIds.get(2));
    }

    @Test
    public void queueSizeIsValidWhenBannersAdded() {
        addBannersToQueue();

        int shard1QueueSize = testModResyncQueueRepository.getSize(1);
        int shard2QueueSize = testModResyncQueueRepository.getSize(2);
        assertThat(shard1QueueSize).isEqualTo(2);
        assertThat(shard2QueueSize).isEqualTo(2);
    }

    @Test
    public void testAddMultibanner() {
        NewTextBannerInfo bannerInfo = createMulticardBanner();
        ImportToResyncQueueRequest request = new ImportToResyncQueueRequest()
                .setIds(List.of(bannerInfo.getBannerId()))
                .setType(ModResyncQueueObjectType.BANNER_MULTICARD);
        Result<Integer> result = modResyncQueueImportService.importToModResyncQueue(request);
        assertThat(result.getErrors()).isEmpty();
        assertThat(testModResyncQueueRepository.getSize(1)).isEqualTo(1);
        var resyncQueueObj = testModResyncQueueRepository.get(bannerInfo.getClientInfo().getShard(),
                ModResyncQueueObjectType.BANNER_MULTICARD, bannerInfo.getBannerId());
        assertThat(resyncQueueObj.getObjectType()).isEqualTo(ModResyncQueueObjectType.BANNER_MULTICARD);
    }

    private Map<Integer, List<Long>> addBannersToQueue() {
        Map<Integer, List<Long>> shardToBannerIds = createBanners();
        addObjectsToQueue(ModResyncQueueObjectType.BANNER, shardToBannerIds);
        return shardToBannerIds;
    }

    private Map<Integer, List<Long>> createBanners() {
        CpmBannerInfo shard1banner1 = steps.bannerSteps().createActiveCpmBanner();
        CpmBannerInfo shard1banner2 = steps.bannerSteps().createActiveCpmBanner();

        TextBannerInfo shard2banner1 = new TextBannerInfo().withBanner(activeTextBanner());
        shard2banner1.getClientInfo().withShard(2);
        TextBannerInfo shard2banner2 = new TextBannerInfo().withBanner(activeTextBanner());
        shard2banner2.getClientInfo().withShard(2);

        steps.bannerSteps().createBanner(shard2banner1);
        steps.bannerSteps().createBanner(shard2banner2);

        return ImmutableMap.of(
                1, List.of(shard1banner1.getBannerId(), shard1banner2.getBannerId()),
                2, List.of(shard2banner1.getBannerId(), shard2banner2.getBannerId()));
    }

    private void addObjectsToQueue(ModResyncQueueObjectType objectType, Map<Integer, List<Long>> shardToObjectIds) {
        List<Long> allIds = new ArrayList<>();
        allIds.addAll(shardToObjectIds.get(1));
        allIds.addAll(shardToObjectIds.get(2));

        ImportToResyncQueueRequest req = new ImportToResyncQueueRequest()
                .setIds(allIds)
                .setType(objectType);

        Result<Integer> result = modResyncQueueImportService.importToModResyncQueue(req);

        System.out.println(result.getErrors());
        assertThat(result.getErrors()).isEmpty();
    }

    private void checkObjectsInShard(int shard, ModResyncQueueObjectType type, List<Long> ids) {
        Map<Long, ModResyncQueueObj> shard1Records = testModResyncQueueRepository.get(shard, type, ids);
        assertThat(shard1Records.values()).hasSize(ids.size());

        shard1Records.values().forEach(this::checkObject);
    }

    private void checkObject(ModResyncQueueObj obj) {
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(obj).isNotNull();
            assertions.assertThat(obj.getPriority()).isEqualTo(110L);
            assertions.assertThat(obj.getRemoderate()).isEqualTo(false);
        });
    }

    private NewTextBannerInfo createMulticardBanner() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        List<BannerMulticard> multicards = List.of(
                new BannerMulticard()
                        .withImageHash(steps.bannerSteps().createRegularImageFormat(clientInfo).getImageHash())
                        .withText("Текст первой карточки"),
                new BannerMulticard()
                        .withImageHash(steps.bannerSteps().createRegularImageFormat(clientInfo).getImageHash())
                        .withText("Текст второй карточки"));
        return steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(fullTextBanner()
                        .withMulticardSetStatusModerate(BannerMulticardSetStatusModerate.NO)
                        .withMulticards(multicards))
                .withBannerImageFormatInfo(new BannerImageFormatInfo()));
    }
}
