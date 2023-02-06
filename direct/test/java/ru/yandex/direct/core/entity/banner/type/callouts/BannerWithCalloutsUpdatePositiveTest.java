package ru.yandex.direct.core.entity.banner.type.callouts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerWithCallouts;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithCallouts;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithCalloutsUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithCallouts> {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public List<Long> calloutsIndexesBefore;

    @Parameterized.Parameter(2)
    public List<Long> calloutsIndexesAfter;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "[0, 1, 2] -> []",
                        List.of(0L, 1L, 2L),
                        List.of(),
                },
                {
                        "[] -> [0]",
                        List.of(),
                        List.of(0L),
                },
                {
                        "[0] -> [0, 1L]",
                        List.of(0L),
                        List.of(0L, 1L),
                },
                {
                        "[0] -> [1, 0]",
                        List.of(0L),
                        List.of(1L, 0L),
                },
                {
                        "[0, 1, 2] -> [2, 1]",
                        List.of(0L, 1L, 2L),
                        List.of(2L, 1L)
                }
        });
    }

    @Test
    public void test() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        List<Long> calloutIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            calloutIds.add(steps.calloutSteps().createDefaultCallout(clientInfo).getId());
        }
        List<Long> calloutIdsBefore = getCallouts(calloutIds, calloutsIndexesBefore);
        List<Long> calloutsIdsAfter = getCallouts(calloutIds, calloutsIndexesAfter);
        bannerInfo = createBanner(clientInfo, calloutIdsBefore);

        ModelChanges<TextBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                        .process(calloutsIdsAfter, BannerWithCallouts.CALLOUT_IDS);
        prepareAndApplyValid(modelChanges);

        BannerWithCallouts actualBanner = getBanner(bannerInfo.getBannerId(), TextBanner.class);
        assertThat(actualBanner.getCalloutIds(), equalTo(calloutsIdsAfter));
    }

    private List<Long> getCallouts(List<Long> callouts, List<Long> calloutIndexes) {
        return calloutIndexes.stream()
                .map(index -> callouts.get(index.intValue()))
                .collect(Collectors.toList());
    }

    private TextBannerInfo createBanner(ClientInfo clientInfo, List<Long> callouts) {
        // степы кривоваты, покороче не получилось написать. кажется не критично в свете скорого перехода
        // на новые степы
        OldTextBanner banner = activeTextBanner().withCalloutIds(callouts);
        return steps.bannerSteps().createBanner(banner, clientInfo);
    }

}
