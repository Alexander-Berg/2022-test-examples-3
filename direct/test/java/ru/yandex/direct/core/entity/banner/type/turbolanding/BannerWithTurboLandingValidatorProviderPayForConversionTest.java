package ru.yandex.direct.core.entity.banner.type.turbolanding;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpcVideoBanners.clientCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

// тест в зависимости от включенности оплаты за конверсии на уровне кампании
@CoreTest
@RunWith(Parameterized.class)
public class BannerWithTurboLandingValidatorProviderPayForConversionTest
        extends BannerAdGroupInfoAddOperationTestBase {

    private static final Path PATH = path(field(BannerWithTurboLanding.TURBO_LANDING_ID));

    @Autowired
    private CampaignOperationService campaignOperationService;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public boolean payForConversion;

    @Parameterized.Parameter(2)
    public BannerWithTurboLanding banner;

    @Parameterized.Parameter(3)
    public boolean bannerHasTurbo;

    @Parameterized.Parameter(4)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // TextBanner - один тип баннера тестируем подробно
                {
                        "TextBanner: кампания с оплатой за конверсии, turbo задан",
                        campaignWithPayForConversion(),
                        clientTextBanner().withHref(null),
                        withTurbo(),
                        null
                },
                {
                        "TextBanner: кампания с оплатой за конверсии, turbo не задан",
                        campaignWithPayForConversion(),
                        clientTextBanner(),
                        withoutTurbo(),
                        null
                },
                {
                        "TextBanner: кампания без оплаты за конверсии, turbo не задан",
                        campaignWithoutPayForConversion(),
                        clientTextBanner(),
                        withoutTurbo(),
                        null
                },
                {
                        "TextBanner: кампания без оплаты за конверсии, turbo задан",
                        campaignWithoutPayForConversion(),
                        clientTextBanner(),
                        withTurbo(),
                        null
                },

                // CpcVideoBanner - этот тип баннера тестируем ограниченно
                {
                        "CpcVideoBanner: кампания с оплатой за конверсии, turbo задан",
                        campaignWithPayForConversion(),
                        clientCpcVideoBanner(null).withHref(null),
                        withTurbo(),
                        null
                },
                {
                        "CpcVideoBanner: кампания без оплаты за конверсии, turbo не задан",
                        campaignWithoutPayForConversion(),
                        clientCpcVideoBanner(null),
                        withoutTurbo(),
                        null
                },
                {
                        "CpcVideoBanner: кампания без оплаты за конверсии, turbo задан",
                        campaignWithoutPayForConversion(),
                        clientCpcVideoBanner(null),
                        withTurbo(),
                        null
                },

                // todo тесты на ГО
        });
    }

    private static boolean withoutTurbo() {
        return false;
    }

    private static boolean withTurbo() {
        return true;
    }

    private static boolean campaignWithPayForConversion() {
        return true;
    }

    private static boolean campaignWithoutPayForConversion() {
        return false;
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        setCampaignPayForConversion();
        setDesktopLandingFeature();
    }

    private void setCampaignPayForConversion() {
        List<TextCampaign> campaigns = campaignTypedRepository
                .getSafely(adGroupInfo.getShard(), singleton(adGroupInfo.getCampaignId()), TextCampaign.class);
        TextCampaign campaign = campaigns.get(0);
        DbStrategy newStrategy = campaign.getStrategy();
        newStrategy.getStrategyData().withPayForConversion(payForConversion);

        ModelChanges<TextCampaign> modelChanges = new ModelChanges<>(adGroupInfo.getCampaignId(), TextCampaign.class);
        modelChanges.process(newStrategy, TextCampaign.STRATEGY);

        var options = new CampaignOptions();
        RestrictedCampaignsUpdateOperation operation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(singletonList(modelChanges),
                        adGroupInfo.getUid(), UidAndClientId.of(adGroupInfo.getUid(), adGroupInfo.getClientId()),
                        options);
        MassResult<Long> result = operation.apply();
        result.getValidationResult().flattenErrors().forEach(System.out::println);
        assumeThat(result, isFullySuccessful());
    }

    private void setDesktopLandingFeature() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(),
                FeatureName.DESKTOP_LANDING, true);
    }

    @Test
    public void testValidationProvider() {
        fillBanner();

        ValidationResult<?, Defect> vr = createOperation(banner, false)
                .prepareAndApply()
                .get(0)
                .getValidationResult();

        assertThat(vr, hasNoDefectsDefinitions());
    }

    private void fillBanner() {
        if (bannerHasTurbo) {
            OldBannerTurboLanding turbo = steps.turboLandingSteps()
                    .createDefaultBannerTurboLanding(adGroupInfo.getClientId());
            banner.withTurboLandingId(turbo.getId());
        } else {
            banner.withTurboLandingId(null);
        }

        if (banner instanceof CpcVideoBanner) {
            CreativeInfo creativeInfo = steps.creativeSteps()
                    .addDefaultCpcVideoCreative(adGroupInfo.getClientInfo(), steps.creativeSteps().getNextCreativeId());
            ((CpcVideoBanner) banner).withCreativeId(creativeInfo.getCreativeId());
        }

        banner.withAdGroupId(adGroupInfo.getAdGroupId());
    }
}
