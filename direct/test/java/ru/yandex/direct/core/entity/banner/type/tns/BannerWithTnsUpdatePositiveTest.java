package ru.yandex.direct.core.entity.banner.type.tns;

import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerWithTns;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTns;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithTnsUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTns> {

    private static final String OLD_TNS_ID = "oldTns";
    private static final String NEW_TNS_ID = "newTns";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public String name;
    @Parameterized.Parameter(1)
    public String oldTnsId;
    @Parameterized.Parameter(2)
    public String newTnsId;

    private ClientInfo defaultClient;
    private CreativeInfo defaultCreativeInfo;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "add tnsId",
                        null,
                        NEW_TNS_ID
                },
                {
                        "delete tnsId",
                        OLD_TNS_ID,
                        null
                },
                {
                        "update tnsId",
                        OLD_TNS_ID,
                        NEW_TNS_ID
                },
        });
    }

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();
        defaultCreativeInfo = steps.creativeSteps().addDefaultCanvasCreative(defaultClient);
    }

    @Test
    public void updateTest() {
        bannerInfo = createCpmBanner(oldTnsId);
        Long bannerId = bannerInfo.getBannerId();

        ModelChanges<CpmBanner> modelChanges = createModelChanges(bannerId, newTnsId);

        prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getTnsId(), equalTo(newTnsId));
    }

    private ModelChanges<CpmBanner> createModelChanges(Long bannerId, String newTnsId) {
        return new ModelChanges<>(bannerId, CpmBanner.class)
                .process(newTnsId, BannerWithTns.TNS_ID);
    }

    private CpmBannerInfo createCpmBanner(String tns) {
        return steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(null, null, defaultCreativeInfo.getCreativeId())
                        .withTnsId(tns),
                defaultClient);
    }

}
