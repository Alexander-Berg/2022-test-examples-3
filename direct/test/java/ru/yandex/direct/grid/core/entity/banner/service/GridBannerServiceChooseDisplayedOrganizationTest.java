package ru.yandex.direct.grid.core.entity.banner.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.organization.model.BannerPermalink;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.AUTO;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.MANUAL;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class GridBannerServiceChooseDisplayedOrganizationTest {

    private static final BannerPermalink MANUAL_PERMALINK = new BannerPermalink()
            .withPermalinkId(1L)
            .withPermalinkAssignType(MANUAL)
            .withIsChangeToManualRejected(false);

    private static final BannerPermalink REJECTED_AUTO_PERMALINK = new BannerPermalink()
            .withPermalinkId(2L)
            .withPermalinkAssignType(AUTO)
            .withIsChangeToManualRejected(true);

    private static final BannerPermalink AUTO_PERMALINK = new BannerPermalink()
            .withPermalinkId(3L)
            .withPermalinkAssignType(AUTO)
            .withIsChangeToManualRejected(false);

    private static final BannerPermalink AUTO_PERMALINK_2 = new BannerPermalink()
            .withPermalinkId(4L)
            .withPermalinkAssignType(AUTO)
            .withIsChangeToManualRejected(false);

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Collection<BannerPermalink> permalinks;

    @Parameterized.Parameter(2)
    public Set<BannerPermalink> availablePermalinks;

    @Parameterized.Parameter(3)
    public BannerPermalink resultPermalink;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"нету пермалинков",
                        emptyList(), emptySet(), null},
                {"один ручной пермалинк",
                        List.of(MANUAL_PERMALINK), emptySet(), MANUAL_PERMALINK},
                {"ручной и авто-привязанный пермалинк",
                        List.of(MANUAL_PERMALINK, AUTO_PERMALINK), emptySet(), MANUAL_PERMALINK},
                {"ручной и опубликованный авто-привязанный пермалинк",
                        List.of(MANUAL_PERMALINK, AUTO_PERMALINK), Set.of(AUTO_PERMALINK), MANUAL_PERMALINK},
                {"неопубликованный авто-привязанный пермалинк",
                        List.of(AUTO_PERMALINK), emptySet(), null},
                {"опубликованный авто-привязанный пермалинк",
                        List.of(AUTO_PERMALINK), Set.of(AUTO_PERMALINK), AUTO_PERMALINK},
                {"отклоненный неопубликованный пермалинк",
                        List.of(REJECTED_AUTO_PERMALINK), emptySet(), null},
                {"отклоненный опубликованный пермалинк",
                        List.of(REJECTED_AUTO_PERMALINK), Set.of(REJECTED_AUTO_PERMALINK), null},
                {"отклоненный и неопубликованный авто-привязанный пермалинк",
                        List.of(REJECTED_AUTO_PERMALINK, AUTO_PERMALINK), Set.of(), null},
                {"отклоненный и авто-привязанный пермалинк",
                        List.of(REJECTED_AUTO_PERMALINK, AUTO_PERMALINK), Set.of(AUTO_PERMALINK), AUTO_PERMALINK},
                {"второй авто-привязанный пермалинк",
                        List.of(AUTO_PERMALINK_2), Set.of(AUTO_PERMALINK_2), AUTO_PERMALINK_2},
                {"опубликованный и неопубликованный пермалинки",
                        List.of(AUTO_PERMALINK, AUTO_PERMALINK_2), Set.of(AUTO_PERMALINK_2), AUTO_PERMALINK_2},
                {"два опубликованных авто-привязанных пермалинка с разными id",
                        List.of(AUTO_PERMALINK, AUTO_PERMALINK_2), Set.of(AUTO_PERMALINK, AUTO_PERMALINK_2), AUTO_PERMALINK},
                {"два опубликованных авто-привязанных пермалинка с разными id в другом порядке",
                        List.of(AUTO_PERMALINK_2, AUTO_PERMALINK), Set.of(AUTO_PERMALINK, AUTO_PERMALINK_2), AUTO_PERMALINK},
        });
    }

    @Test
    public void testChooseDisplayedBannerPermalink() {
        var availablePermalinkIds = listToSet(availablePermalinks, BannerPermalink::getPermalinkId);
        BannerPermalink result = GridBannerService.chooseDisplayedBannerPermalink(permalinks, availablePermalinkIds);
        Assertions.assertThat(result).isEqualTo(resultPermalink);
    }
}
