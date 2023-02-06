package ru.yandex.direct.core.entity.banner.type.flags;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerUserFlagsUpdatesRepository;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithFlags.FLAGS;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.age;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerUserFlagsUpdatesTest extends BannerClientInfoUpdateOperationTestBase {
    @Autowired
    private TestBannerUserFlagsUpdatesRepository testBannerUserFlagsUpdatesRepository;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void update() {
        NewTextBannerInfo bannerInfo = createBanner();

        int shard = bannerInfo.getShard();
        Long bannerId = bannerInfo.getBannerId();

        assertThat(testBannerUserFlagsUpdatesRepository.get(shard, bannerId), is(nullValue()));

        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(age(16), FLAGS);
        prepareAndApplyValid(modelChanges);

        var record = testBannerUserFlagsUpdatesRepository.get(shard, bannerId);
        assertThat(record, is(notNullValue()));
        assertThat(record.getLeft().getAdGroupId(), is(bannerInfo.getAdGroupId()));
        assertThat(record.getLeft().getCampaignId(), is(bannerInfo.getCampaignId()));

        LocalDateTime updateTime = record.getRight();

        modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(age(12), FLAGS);
        prepareAndApplyValid(modelChanges);

        record = testBannerUserFlagsUpdatesRepository.get(shard, bannerId);
        assertThat(record, is(notNullValue()));
        assertThat(record.getLeft().getAdGroupId(), is(bannerInfo.getAdGroupId()));
        assertThat(record.getLeft().getCampaignId(), is(bannerInfo.getCampaignId()));
        assertThat(record.getRight(), is(greaterThanOrEqualTo(updateTime)));
    }

    private NewTextBannerInfo createBanner() {
        TextBanner banner = fullTextBanner()
                .withFlags(age(18));

        return steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(banner)
                        .withClientInfo(clientInfo));
    }
}
