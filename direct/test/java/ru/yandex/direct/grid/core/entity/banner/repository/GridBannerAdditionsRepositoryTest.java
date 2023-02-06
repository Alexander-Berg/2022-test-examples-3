package ru.yandex.direct.grid.core.entity.banner.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBanner;
import ru.yandex.direct.grid.core.entity.banner.model.GdiSitelink;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

@GridCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class GridBannerAdditionsRepositoryTest {

    private static final long NOT_EXIST_ID = Long.MAX_VALUE;

    @Autowired
    private Steps steps;

    @Autowired
    private GridBannerAdditionsRepository gridBannerAdditionsRepository;


    @Test
    public void checkGetSitelinks() {
        SitelinkSetInfo defaultSitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet();
        GdiBanner gdiBannerWithSitelinks = generateTestBanner(defaultSitelinkSet.getSitelinkSetId());
        List<GdiBanner> gdiBanners = Collections.singletonList(gdiBannerWithSitelinks);

        Map<Long, List<GdiSitelink>> sitelinks =
                gridBannerAdditionsRepository.getSitelinks(defaultSitelinkSet.getShard(), gdiBanners);

        assertThat(sitelinks)
                .hasSize(1)
                .containsKey(gdiBannerWithSitelinks.getId());
        assertThat(sitelinks.get(gdiBannerWithSitelinks.getId()))
                .hasSize(defaultSitelinkSet.getSitelinkSet().getSitelinks().size());
    }

    @Test
    public void checkGetSitelinks_WhenBannersWithoutSitelinksAndWithNotExistSitelinks() {
        int shard = 1;
        List<GdiBanner> gdiBanners = Arrays.asList(
                generateTestBanner(null),
                generateTestBanner(NOT_EXIST_ID)
        );

        Map<Long, List<GdiSitelink>> sitelinks = gridBannerAdditionsRepository.getSitelinks(shard, gdiBanners);
        assertThat(sitelinks)
                .isEmpty();
    }

    private static GdiBanner generateTestBanner(@Nullable Long sitelinksSetId) {
        return new GdiBanner()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withSitelinksSetId(sitelinksSetId);
    }

}
