package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupForBannerOperation;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithStrategy;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService;
import ru.yandex.direct.core.entity.uac.service.trackingurl.TrackingUrlParseService;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.testing.data.TestBannerValidationContainers.newBannerValidationContainer;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

// тест в зависимости от включенности оплаты за конверсии на уровне кампании
@RunWith(Parameterized.class)
public class BannerWithHrefValidatorProviderPayForConversionTest {

    private static final Path PATH = path(index(0), field(BannerWithHref.HREF));
    private static final String VALID_HREF = "http://ya.ru";

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public CampaignWithStrategy campaignWithStrategy;

    @Parameterized.Parameter(2)
    public BannerWithHref banner;

    @Parameterized.Parameter(3)
    public Defect expectedDefect;

    @Mock
    private TrustedRedirectsService trustedRedirectsService;
    private TrackingUrlParseService trackingUrlParseService;
    private BannerWithHrefValidatorProvider provider;
    private BannersAddOperationContainer validationContainer;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // TextBanner - один тип баннера тестируем подробно
                {
                        "TextBanner: кампания с оплатой за конверсии без фичи, href не задан",
                        campaignWithPayForConversion(),
                        new TextBanner().withHref(null),
                        null
                },
                {
                        "TextBanner: кампания с оплатой за конверсии без фичи, href задан",
                        campaignWithPayForConversion(),
                        new TextBanner().withHref(VALID_HREF),
                        null
                },
                {
                        "TextBanner: кампания без оплаты за конверсии без фичи, href не задан",
                        campaignWithoutPayForConversion(),
                        new TextBanner().withHref(null),
                        null
                },
                {
                        "TextBanner: кампания без оплаты за конверсии без фичи, href задан",
                        campaignWithoutPayForConversion(),
                        new TextBanner().withHref(VALID_HREF),
                        null
                },

                // CpcVideoBanner - этот тип баннера тестируем ограниченно
                {
                        "CpcVideoBanner: кампания с оплатой за конверсии, href не задан",
                        campaignWithPayForConversion(),
                        new CpcVideoBanner().withHref(null),
                        null
                },
                {
                        "CpcVideoBanner: кампания без оплаты за конверсии, href не задан",
                        campaignWithoutPayForConversion(),
                        new CpcVideoBanner().withHref(null),
                        null
                },
                {
                        "CpcVideoBanner: кампания без оплаты за конверсии, href задан",
                        campaignWithoutPayForConversion(),
                        new CpcVideoBanner().withHref(VALID_HREF),
                        null
                },

                // ImageBanner - этот тип баннера тестируем ограниченно
                {
                        "ImageBanner: кампания с оплатой за конверсии, href не задан",
                        campaignWithPayForConversion(),
                        new ImageBanner().withHref(null),
                        null
                },
                {
                        "ImageBanner: кампания без оплаты за конверсии, href не задан",
                        campaignWithoutPayForConversion(),
                        new ImageBanner().withHref(null),
                        null
                },
                {
                        "ImageBanner: кампания без оплаты за конверсии, href задан",
                        campaignWithoutPayForConversion(),
                        new ImageBanner().withHref(VALID_HREF),
                        null
                },

                // проверяем, что в типе баннера, для которого фича не поддерживается, валидация не срабатывает
                {
                        "CpmBanner: кампания с оплатой за конверсии, href задан",
                        campaignWithPayForConversion(),
                        new CpmBanner().withHref(VALID_HREF),
                        null
                },
        });
    }

    private static CampaignWithStrategy campaignWithPayForConversion() {
        return campaign(true);
    }

    private static CampaignWithStrategy campaignWithoutPayForConversion() {
        return campaign(false);
    }

    private static CampaignWithStrategy campaign(boolean payForConversion) {
        StrategyData strategyData = new StrategyData().withPayForConversion(payForConversion);
        DbStrategy strategy = new DbStrategy();
        strategy.withStrategyData(strategyData);
        return new TextCampaign().withStrategy(strategy);
    }

    @Before
    public void before() {
        initMocks(this);
        provider = new BannerWithHrefValidatorProvider(trustedRedirectsService, trackingUrlParseService);

        Long localCampaignId = 12345L;
        AdGroupForBannerOperation adGroupForBannerOperation =
                new TextAdGroup()
                        .withCampaignId(localCampaignId)
                        .withType(AdGroupType.BASE);
        validationContainer = newBannerValidationContainer()
                .withIndexToAdGroupForOperationMap(Map.of(0, adGroupForBannerOperation))
                .withCampaignIdToCampaignWithStrategyMap(Map.of(localCampaignId, campaignWithStrategy))
                .withBannerToIndexMap(Map.of(banner, 0))
                .build();
    }

    @Test
    public void testValidationProvider() {
        ValidationResult<List<BannerWithHref>, Defect> vr = validate(banner);
        if (expectedDefect != null) {
            assertThat(vr, hasDefectDefinitionWith(validationError(PATH, expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    private ValidationResult<List<BannerWithHref>, Defect> validate(BannerWithHref banner) {
        return ListValidationBuilder.<BannerWithHref, Defect>of(singletonList(banner))
                .checkEachBy(provider.bannerWithHrefValidator(validationContainer))
                .getResult();
    }
}
