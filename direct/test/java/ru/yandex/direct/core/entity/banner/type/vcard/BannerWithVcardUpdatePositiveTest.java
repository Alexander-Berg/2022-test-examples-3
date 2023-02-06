package ru.yandex.direct.core.entity.banner.type.vcard;

import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.BannerWithVcard;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithVcardUpdatePositiveTest extends BannerNewBannerInfoUpdateOperationTestBase {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BannerProvider bannerProvider;

    @Parameterized.Parameter(2)
    public BannerModelChangesProvider modelChangesProvider;

    @Parameterized.Parameter(3)
    public ExpectedVcardIdProvider expectedVcardIdProvider;

    @Parameterized.Parameter(4)
    public StatusBsSynced expectedStatusBsSynced;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "баннер с визиткой, нет изменений",
                        // исходный баннер
                        bannerWithVcard(fullTextBanner()
                                .withStatusBsSynced(StatusBsSynced.YES)),
                        //modelchange
                        emptyModelChanges(),
                        // ожидаемые значения
                        vcardFromOriginalBanner(),
                        StatusBsSynced.YES
                },
                {
                        "баннер без визитки, нет изменений",
                        // исходный баннер
                        bannerWithoutVcard(fullTextBanner()
                                .withStatusBsSynced(StatusBsSynced.YES)),
                        //modelchange
                        emptyModelChanges(),
                        // ожидаемые значения
                        vcardNull(),
                        StatusBsSynced.YES
                },
                {
                        "добавили визитку",
                        // исходный баннер
                        bannerWithoutVcard(fullTextBanner()
                                .withStatusBsSynced(StatusBsSynced.YES)),
                        //modelchange
                        newVcardModelChanges(),
                        // ожидаемые значения
                        vcardFromModelChanges(),
                        StatusBsSynced.NO
                },
                {
                        "удалили визитку",
                        // исходный баннер
                        bannerWithVcard(fullTextBanner()
                                .withStatusBsSynced(StatusBsSynced.YES)),
                        //modelchange
                        deleteVcardModelChanges(),
                        // ожидаемые значения
                        vcardNull(),
                        StatusBsSynced.NO
                },
                {
                        "изменили визитку",
                        // исходный баннер
                        bannerWithVcard(fullTextBanner()
                                .withStatusBsSynced(StatusBsSynced.YES)),
                        //modelchange
                        newVcardModelChanges(),
                        // ожидаемые значения
                        vcardFromModelChanges(),
                        StatusBsSynced.NO
                },
        });
    }

    @Test
    public void test() {
        NewTextBannerInfo banner = bannerProvider.createBanner(steps);
        bannerInfo = banner;

        ModelChanges<BannerWithSystemFields> modelChanges = modelChangesProvider.getModelChanges(steps, banner);
        prepareAndApplyValid(modelChanges);

        TextBanner actualBanner = getBanner(banner.getBannerId());
        Long expectedVcardId = expectedVcardIdProvider.getExpectedVcardId(banner, modelChanges);
        assertThat(actualBanner.getVcardId()).isEqualTo(expectedVcardId);
        assertThat(actualBanner.getStatusBsSynced()).isEqualTo(expectedStatusBsSynced);
    }

    private static BannerProvider bannerWithoutVcard(TextBanner banner) {
        return steps -> {
            ClientInfo client = steps.clientSteps().createDefaultClient();
            return steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                    .withClientInfo(client)
                    .withBanner(banner));
        };
    }

    private static BannerProvider bannerWithVcard(TextBanner banner) {
        return steps -> {
            ClientInfo client = steps.clientSteps().createDefaultClient();
            CampaignInfo campaign = steps.campaignSteps().createActiveTextCampaign(client);
            VcardInfo vcard = steps.vcardSteps().createVcard(fullVcard().withContactPerson("Old Person"), campaign);
            return steps.textBannerSteps().createBanner(
                    new NewTextBannerInfo()
                            .withCampaignInfo(campaign)
                            .withClientInfo(client)
                            .withVcardInfo(vcard)
                            .withBanner(banner));
        };
    }

    private static BannerModelChangesProvider emptyModelChanges() {
        return (steps, banner) -> new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                .castModelUp(BannerWithSystemFields.class);
    }

    private static BannerModelChangesProvider newVcardModelChanges() {
        return (steps, banner) -> {
            VcardInfo newVcard = steps.vcardSteps().createVcard(fullVcard().withContactPerson("New Person"),
                    banner.getCampaignInfo());
            return new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                    .process(newVcard.getVcardId(), BannerWithVcard.VCARD_ID)
                    .castModelUp(BannerWithSystemFields.class);
        };
    }

    private static BannerModelChangesProvider deleteVcardModelChanges() {
        return (steps, banner) -> new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                .process(null, BannerWithVcard.VCARD_ID)
                .castModelUp(BannerWithSystemFields.class);
    }

    private static ExpectedVcardIdProvider vcardFromOriginalBanner() {
        return (originalBanner, modelChanges) -> ((TextBanner) originalBanner.getBanner()).getVcardId();
    }

    private static ExpectedVcardIdProvider vcardFromModelChanges() {
        return (originalBanner, modelChanges) -> modelChanges
                .castModelUp(BannerWithVcard.class)
                .getChangedProp(BannerWithVcard.VCARD_ID);
    }

    private static ExpectedVcardIdProvider vcardNull() {
        return (originalBanner, modelChanges) -> null;
    }

    interface BannerProvider {
        NewTextBannerInfo createBanner(Steps steps);
    }

    interface BannerModelChangesProvider {
        ModelChanges<BannerWithSystemFields> getModelChanges(Steps steps, NewTextBannerInfo banner);
    }

    interface ExpectedVcardIdProvider {
        Long getExpectedVcardId(NewTextBannerInfo originalBanner, ModelChanges<BannerWithSystemFields> modelChanges);
    }
}
