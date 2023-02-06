package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.io.IOException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.checkout.util.report.BnplFactory;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.json.credit.CreditInfo;
import ru.yandex.market.common.report.model.json.credit.CreditOffer;
import ru.yandex.market.common.report.model.json.credit.YandexBnplInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.BNPL_FOR_UNAUTHORIZED_USER;

public class BnplActualizeV2Test extends AbstractWebTestBase {

    private static final long PUID = 2_190_550_858_753_437_200L;
    private static final long MUID = 1_152_921_504_606_846_977L;

    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;

    @BeforeEach
    void configure() {
        checkouterProperties.setEnableBnpl(true);
    }

    @Test
    void shouldFillBnplInForUnloggedUser() throws IOException {
        checkouterProperties.setEnableBnplInfoFromFetcher(true);
        checkouterFeatureWriter.writeValue(BNPL_FOR_UNAUTHORIZED_USER, true);

        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.setUid(MUID);
        params.setYandexUid("2713818151642086544");
        params.getReportParameters().setOffers(params.getItems().stream()
                .map(FoundOfferBuilder::createFrom)
                .map(this::makeCreditInfo)
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toUnmodifiableList()));

        bnplMockConfigurer.mockPlanCheck(BnplUserId.YANDEX_UID_HEADER_COOKIE);

        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart.getBnplInfo(), notNullValue());
        assertThat(cart.getBnplInfo().isAvailable(), is(true));
        assertThat(cart.getBnplInfo().isSelected(), is(false));
        assertThat(cart.getBnplInfo().getBnplPlanDetails(), notNullValue());
    }

    @Test
    void shouldFillBnplInForUnloggedUserOnAndroid() throws IOException {
        checkouterProperties.setEnableBnplInfoFromFetcher(true);
        checkouterFeatureWriter.writeValue(BNPL_FOR_UNAUTHORIZED_USER, true);

        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.setUid(MUID);
        params.setGoogleServiceId("2713818151642086544");
        params.getReportParameters().setOffers(params.getItems().stream()
                .map(FoundOfferBuilder::createFrom)
                .map(this::makeCreditInfo)
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toUnmodifiableList()));

        bnplMockConfigurer.mockPlanCheck(BnplUserId.GAID);

        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart.getBnplInfo(), notNullValue());
        assertThat(cart.getBnplInfo().isAvailable(), is(true));
        assertThat(cart.getBnplInfo().isSelected(), is(false));
        assertThat(cart.getBnplInfo().getBnplPlanDetails(), notNullValue());
    }

    @Test
    void shouldFillBnplInForUnloggedUserOnIos() throws IOException {
        checkouterProperties.setEnableBnplInfoFromFetcher(true);
        checkouterFeatureWriter.writeValue(BNPL_FOR_UNAUTHORIZED_USER, true);

        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.setUid(MUID);
        params.setIosDeviceId("2713818151642086544");
        params.getReportParameters().setOffers(params.getItems().stream()
                .map(FoundOfferBuilder::createFrom)
                .map(this::makeCreditInfo)
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toUnmodifiableList()));

        bnplMockConfigurer.mockPlanCheck(BnplUserId.MM_DEVICE_ID);

        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart.getBnplInfo(), notNullValue());
        assertThat(cart.getBnplInfo().isAvailable(), is(true));
        assertThat(cart.getBnplInfo().isSelected(), is(false));
        assertThat(cart.getBnplInfo().getBnplPlanDetails(), notNullValue());
    }

    @Test
    void shouldFillBnplInfoWithAvailableFlagOnPrepaidAvailableShopWithCreditInfoFromFetcher() throws IOException {
        checkouterProperties.setEnableBnplInfoFromFetcher(true);

        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.setUid(PUID);
        params.getReportParameters().setOffers(params.getItems().stream()
                .map(FoundOfferBuilder::createFrom)
                .map(this::makeCreditInfo)
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toUnmodifiableList()));

        bnplMockConfigurer.mockPlanCheck(BnplUserId.YANDEX_UID_HEADER);

        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart.getBnplInfo(), notNullValue());
        assertThat(cart.getBnplInfo().isAvailable(), is(true));
        assertThat(cart.getBnplInfo().isSelected(), is(false));
        assertThat(cart.getBnplInfo().getBnplPlanDetails(), notNullValue());
    }

    @Test
    void shouldFillBnplInfoWithAvailableFlagOnPrepaidAvailableShopWithCreditInfoFromCreditPlace() throws IOException {
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.setUid(PUID);
        bnplMockConfigurer.mockPlanCheck(BnplUserId.YANDEX_UID_HEADER);

        var creditInfo = new CreditInfo();
        creditInfo.setCreditOffers(params.getItems().stream()
                .map(i -> {
                    var creditOffer = new CreditOffer();
                    creditOffer.setEntity(i.getOfferId());
                    creditOffer.setHid(i.getCategoryId());
                    creditOffer.setWareId(i.getWareMd5());
                    var bnplInfo = new YandexBnplInfo();
                    bnplInfo.setEnabled(true);
                    creditOffer.setYandexBnplInfo(bnplInfo);
                    return creditOffer;
                }).collect(Collectors.toUnmodifiableList()));
        params.getReportParameters().setCreditInfo(creditInfo);

        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart.getBnplInfo(), notNullValue());
        assertThat(cart.getBnplInfo().isAvailable(), is(true));
        assertThat(cart.getBnplInfo().isSelected(), is(false));
        assertThat(cart.getBnplInfo().getBnplPlanDetails(), notNullValue());
    }

    @Test
    void shouldFillBnplInfoAndNotSetAvailableOnPostpaidShop() throws IOException {
        checkouterProperties.setEnableBnplInfoFromFetcher(true);

        var params = WhiteParametersProvider.defaultWhiteParameters();
        params.setUid(PUID);
        params.getReportParameters().setOffers(params.getItems().stream()
                .map(FoundOfferBuilder::createFrom)
                .map(this::makeCreditInfo)
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toUnmodifiableList()));

        params.addShopMetaData(params.getShopId(), ShopSettingsHelper.getPostpayMeta());

        bnplMockConfigurer.mockPlanCheck(BnplUserId.YANDEX_UID_HEADER);

        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);

        assertThat(cart.getBnplInfo(), notNullValue());
        assertThat(cart.getBnplInfo().isAvailable(), is(false));
        assertThat(cart.getBnplInfo().isSelected(), is(false));
    }

    private FoundOfferBuilder makeCreditInfo(FoundOfferBuilder offer) {
        return offer.bnpl(true, BnplFactory.installments(1, BnplFactory.payment(100)));
    }
}
