package ru.yandex.direct.grid.processing.service.banner;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.BannerWithPrice;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPrice;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithPrice;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerTurboAppsRepository;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.GdAdPrice;
import ru.yandex.direct.grid.processing.model.banner.GdAdPriceCurrency;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPrice;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerDataService2Test extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithPrice> {

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private OldBannerTurboAppsRepository turboAppsRepository;

    @Autowired
    private BannerDataService bannerDataService;

    private BannerPrice defaultNewBannerPrice;
    private OldBannerPrice defaultOldBannerPrice;

    @Before
    public void setUp() {
        defaultNewBannerPrice = new BannerPrice()
                .withPrice(new BigDecimal("4.00"))
                .withCurrency(BannerPricesCurrency.RUB);

        defaultOldBannerPrice = new OldBannerPrice()
                .withPrice(new BigDecimal("3.00"))
                .withCurrency(OldBannerPricesCurrency.RUB);
    }

    // Если включен BannerWithHrefAndPriceAndTurboAppUpdateValidationTypeSupport
    // то сейчас этот код должен падать, т.к. я добавить support, который добавляет дефект.
    // Но через интерфейс саппорт по идее не подцепится и тест пройдёт.
    // Надо сделать так, чтобы и через интерфейс все саппорты подцеплялись.
    @Test
    public void updateBanner_UpdateBannerPrice_BannerPriceUpdated() {
        bannerInfo = createBanner(defaultOldBannerPrice);
        Long bannerId = bannerInfo.getBannerId();

        User operator = new User();
        operator.setUid(bannerInfo.getUid());

        GdUpdateAdPrice x = new GdUpdateAdPrice();
        x.withId(bannerInfo.getBannerId());

        x.withPrice(new GdAdPrice()
                .withPrice("4.00")
                .withCurrency(GdAdPriceCurrency.RUB));

        GdUpdateAdsPayload result = bannerDataService.updateAdPrices(bannerInfo.getClientId(), operator, List.of(x));
        System.out.println(result);
//
//        ModelChanges<NewTextBanner> modelChanges = createModelChanges(bannerId, defaultNewBannerPrice);
//        prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(bannerId, TextBanner.class);

        assertThat(actualBanner.getBannerPrice(), equalTo(defaultNewBannerPrice));
    }

    private ModelChanges<TextBanner> createModelChanges(Long bannerId, BannerPrice bannerPrice) {
        return new ModelChanges<>(bannerId, TextBanner.class)
                .process(bannerPrice, BannerWithPrice.BANNER_PRICE);
    }

    private TextBannerInfo createBanner(OldBannerPrice bannerPrice) {
        OldTextBanner banner = activeTextBanner(null, null)
                .withBannerPrice(bannerPrice);

        return steps.bannerSteps().createActiveTextBanner(banner);
    }
}
