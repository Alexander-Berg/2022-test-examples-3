package ru.yandex.direct.core.entity.banner.type.titleextension;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.direct.core.entity.banner.model.TextBanner.TITLE;
import static ru.yandex.direct.core.entity.banner.model.TextBanner.TITLE_EXTENSION;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTitleExtensionRepositoryTest {

    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private BannerTypedRepository typedRepository;
    @Autowired
    private BannerModifyRepository modifyRepository;

    private int shard;
    private AdGroupInfo adGroupInfo;
    private BannerRepositoryContainer repositoryContainer;

    @Before
    public void setUp() {
        adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup());
        shard = adGroupInfo.getShard();
        repositoryContainer = new BannerRepositoryContainer(shard);
    }

    @Test
    public void testReadWrite() {
        var banners = List.of(getBanner("title 1", null), getBanner("title 2", ""), getBanner("title 3", "title extension"));

        var addedBannersIds = modifyRepository.add(dslContextProvider.ppc(shard), repositoryContainer, banners);
        var addedBanners = typedRepository.getTyped(shard, addedBannersIds);

        assertThat(addedBanners, containsInAnyOrder(
                allOf(hasProperty(TITLE.name(), is("title 1")),
                        hasProperty(TITLE_EXTENSION.name(), nullValue())),
                allOf(hasProperty(TITLE.name(), is("title 2")),
                        hasProperty(TITLE_EXTENSION.name(), nullValue())),
                allOf(hasProperty(TITLE.name(), is("title 3")),
                        hasProperty(TITLE_EXTENSION.name(), is("title extension")))
        ));
    }

    private TextBanner getBanner(String title, String titleExtension) {
        return fullTextBanner()
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTitle(title)
                .withTitleExtension(titleExtension);
    }
}
