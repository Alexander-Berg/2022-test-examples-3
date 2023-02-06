package ru.yandex.direct.core.entity.banner.type.measurers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.CommonUtils.nvl;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithMeasurersUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBanner> {
    @Autowired
    public BannersAddOperationFactory addOperationFactory;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Set<OldBannerMeasurer> initialMeasurers;

    @Parameterized.Parameter(2)
    public Set<BannerMeasurer> newMeasurers;

    private Long bannerId;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "update empty -> empty",
                        emptySet(),
                        emptySet(),
                },
                {
                        "update empty -> one",
                        emptySet(),
                        Set.of(mes(BannerMeasurerSystem.SIZMEK)),
                },
                {
                        "update empty -> two",
                        emptySet(),
                        Set.of(mes(BannerMeasurerSystem.SIZMEK, true), mes(BannerMeasurerSystem.MOAT)),
                },
                {
                        "update one -> empty",
                        Set.of(mes(OldBannerMeasurerSystem.SIZMEK)),
                        emptySet(),
                },
                {
                        "update one -> the same",
                        Set.of(mes(OldBannerMeasurerSystem.SIZMEK)),
                        Set.of(mes(BannerMeasurerSystem.SIZMEK)),
                },
                {
                        "update one -> another",
                        Set.of(mes(OldBannerMeasurerSystem.SIZMEK)),
                        Set.of(mes(BannerMeasurerSystem.OMI, true)),
                },
                {
                        "update two -> empty",
                        Set.of(mes(OldBannerMeasurerSystem.SIZMEK), mes(OldBannerMeasurerSystem.MOAT)),
                        emptySet(),
                },
                {
                        "update two -> one of two",
                        Set.of(mes(OldBannerMeasurerSystem.SIZMEK), mes(OldBannerMeasurerSystem.MOAT)),
                        Set.of(mes(BannerMeasurerSystem.SIZMEK)),
                },
                {
                        "update two -> one another",
                        Set.of(mes(OldBannerMeasurerSystem.SIZMEK), mes(OldBannerMeasurerSystem.MOAT)),
                        Set.of(mes(BannerMeasurerSystem.OMI, true)),
                },
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        ClientInfo defaultClient = steps.clientSteps().createDefaultClient();
        CreativeInfo creativeInfo =
                steps.creativeSteps().addDefaultHtml5Creative(defaultClient, steps.creativeSteps().getNextCreativeId());

        bannerInfo = steps.bannerSteps().createBanner(
                activeCpmBanner(null, null, creativeInfo.getCreativeId())
                        .withMeasurers(new ArrayList<>(initialMeasurers)),
                defaultClient);

        bannerId = bannerInfo.getBannerId();
    }

    @Test
    public void updatedWell() {
        ModelChanges<CpmBanner> modelChanges = ModelChanges.build(bannerId, CpmBanner.class,
                CpmBanner.MEASURERS, ifNotNull(newMeasurers, ArrayList::new));

        prepareAndApplyValid(modelChanges);

        CpmBanner actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getMeasurers()).containsOnlyElementsOf(nvl(newMeasurers, emptySet()));
    }

    private static BannerMeasurer mes(BannerMeasurerSystem system) {
        return mes(system, false);
    }

    private static BannerMeasurer mes(BannerMeasurerSystem system, Boolean hasIntegration) {
        return new BannerMeasurer()
                .withBannerMeasurerSystem(system)
                .withParams("{\"json\": \"" + system.name() + "\"}")
                .withHasIntegration(hasIntegration);
    }

    private static OldBannerMeasurer mes(OldBannerMeasurerSystem system) {
        return new OldBannerMeasurer()
                .withBannerMeasurerSystem(system)
                .withParams("{\"json\": \"" + system.name() + "\"}")
                .withHasIntegration(false);
    }
}
