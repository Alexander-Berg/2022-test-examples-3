package ru.yandex.direct.core.entity.banner.type.aggregatordomain;

import java.util.Collection;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperation;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.banner.model.BannerWithAggregatorDomain.HREF;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithAggregatorDomainUpdateTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public String name;
    @Parameterized.Parameter(1)
    public String initialHref;
    @Parameterized.Parameter(2)
    public String initialDomain;
    @Parameterized.Parameter(3)
    public String initialAggregatorDomain;
    @Parameterized.Parameter(4)
    public String newHref;
    @Parameterized.Parameter(5)
    public String expectedAggregatorDomain;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Обычная ссылка -> обычная ссылка",
                        // начальное состояние баннера: href, domain, aggregatorDomain
                        "http://ya.ru", "ya.ru", null,
                        // новое значение href
                        "http://yandex.ru",
                        // ожидаемое значение aggregatorDomain
                        null
                },
                {
                        "Обычная ссылка -> ссылка на аггрегатор",
                        // начальное состояние баннера: href, domain, aggregatorDomain
                        "http://ya.ru", "ya.ru", null,
                        // новое значение href
                        "https://yandex.ru/uslugi/search?worker_id=100-500",
                        // ожидаемое значение aggregatorDomain
                        "100-500.uslugi.yandex.ru"
                },
                {
                        "Ссылка на аггрегатор -> ссылка на аггрегатор",
                        // начальное состояние баннера: href, domain, aggregatorDomain
                        "https://yandex.ru/uslugi/search?worker_id=100-500", "uslugi.yandex.ru", "100-500.uslugi.yandex.ru",
                        // новое значение href
                        "http://vk.com/foobar",
                        // ожидаемое значение aggregatorDomain
                        "foobar.vk.com"
                },
                {
                        "Ссылка на аггрегатор -> обычная ссылка",
                        // начальное состояние баннера: href, domain, aggregatorDomain
                        "http://vk.com/foobar", "vk.com", "foobar.vk.com",
                        // новое значение href
                        "http://ya.ru",
                        // ожидаемое значение aggregatorDomain
                        null
                },
                {
                        "null -> обычная ссылка",
                        // начальное состояние баннера: href, domain, aggregatorDomain
                        null, null, null,
                        // новое значение href
                        "http://yandex.ru",
                        // ожидаемое значение aggregatorDomain
                        null
                },
                {
                        "Обычная ссылка -> null",
                        // начальное состояние баннера: href, domain, aggregatorDomain
                        "http://ya.ru", "ya.ru", null,
                        // новое значение href
                        null,
                        // ожидаемое значение aggregatorDomain
                        null
                },
                {
                        "null -> ссылка на аггрегатор",
                        // начальное состояние баннера: href, domain, aggregatorDomain
                        null, null, null,
                        // новое значение href
                        "http://vk.com/foobar",
                        // ожидаемое значение aggregatorDomain
                        "foobar.vk.com"
                },
                {
                        "Ссылка на аггрегатор -> null",
                        // начальное состояние баннера: href, domain, aggregatorDomain
                        "http://vk.com/foobar", "vk.com", "foobar.vk.com",
                        // новое значение href
                        null,
                        // ожидаемое значение aggregatorDomain
                        null
                }
        });
    }

    @Autowired
    private Steps steps;
    @Autowired
    private BannersUpdateOperationFactory operationFactory;
    @Autowired
    private AggregatorDomainsRepository aggregatorDomainsRepository;
    @Autowired
    private FeatureService featureService;

    private TextBannerInfo bannerInfo;

    @Test
    public void updateBannerWithAggregatorDomain() {
        UserInfo userInfo = steps.userSteps().createDefaultUser();
        ClientInfo clientInfo = userInfo.getClientInfo();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        VcardInfo vcardInfo = steps.vcardSteps().createVcard(campaignInfo);

        bannerInfo = createBanner(activeTextBanner()
                        .withHref(initialHref)
                        .withDomain(initialDomain)
                        .withVcardId(vcardInfo.getVcardId()),
                campaignInfo);
        var bannerId = bannerInfo.getBannerId();
        assumeThat(getAggregatorDomain(bannerId), is(initialAggregatorDomain));

        var mc = new ModelChanges<>(bannerId, TextBanner.class).process(newHref, HREF);
        if (newHref == null) {
            mc.process(null, TextBanner.DISPLAY_HREF);
        }
        var result = createUpdateOperation(mc).prepareAndApply();

        String defectsDescription = String.join("\n\t",
                mapList(result.getValidationResult().flattenErrors(), defect -> defect.toString()));
        assumeThat("Unexpected errors:\n\t" + defectsDescription, result, isFullySuccessful());

        assertThat(getAggregatorDomain(bannerId), is(expectedAggregatorDomain));
    }

    private TextBannerInfo createBanner(OldTextBanner banner, CampaignInfo campaignInfo) {
        if (banner.getHref() == null) {
            banner.setDisplayHref(null);
        }
        return steps.bannerSteps().createBanner(banner, campaignInfo);
    }

    private BannersUpdateOperation<BannerWithSystemFields> createUpdateOperation(ModelChanges<TextBanner> mc) {
        Set<String> clientEnabledFeatures = featureService.getEnabledForClientId(bannerInfo.getClientId());

        return operationFactory.createUpdateOperation(
                Applicability.FULL, false, ModerationMode.DEFAULT,
                singletonList(mc.castModelUp(BannerWithSystemFields.class)),
                bannerInfo.getShard(), bannerInfo.getClientId(), bannerInfo.getUid(), clientEnabledFeatures, false);
    }

    private String getAggregatorDomain(Long bannerId) {
        return aggregatorDomainsRepository
                .getAggregatorDomains(bannerInfo.getShard(), singletonList(bannerId))
                .get(bannerId);
    }
}
