package ru.yandex.market.partner.mvc.controller.moderation;

import com.google.common.collect.ImmutableSet;
import io.grpc.stub.StreamObserver;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feature.model.cutoff.DSBSCutoffs;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.partner.util.FunctionalTestHelper.get;

/**
 * Тест на логику работы {@link  PushReadyButtonController}.
 */
@ExtendWith(MockitoExtension.class)
public class PushReadyButtonControllerTest extends FunctionalTest {

    @Autowired
    private CheckouterClient checkouterClient;

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Mock
    private CheckouterShopApi shopApi;

    @BeforeEach
    void configure() {
        doReturn(shopApi).when(checkouterClient).shops();
    }

    /**
     * Отправляем ДСБС магазин на премодерацию. У магазина фича MARKETPLACE_SELF_DELIVERY в
     * статусе DONT_WANT.
     * Ожидаемый результат: модерация стартанула. Фича MARKETPLACE_SELF_DELIVERY переводится в NEW.
     */
    @Test
    @DbUnitDataSet(before = {"requestCpaModeration.success.before.csv",
            "successMarketplaceSelfDeliveryFeature.selfTariffs.before.csv"},
            after = "requestCpaModeration.success.after.csv")
    void successfulStartOfCpaModeration() {
        ResponseEntity<String> response = get(String.format("%s/pushReadyForTesting?id=%d", baseUrl, 200));

        assertResponse(response.getBody(), "api/cpaModeration.success.xml");
    }

    /**
     * Отправляем ДСБС магазин на модерацию.
     * У магазина фича MARKETPLACE_SELF_DELIVERY в статусе FAIL, т.к. был открыт
     * катофф {@link DSBSCutoffs#QUALITY_OTHER}. Также открыт катофф {@link DSBSCutoffs#ORDER_NOT_ACCEPTED} (или любой
     * другой, у которого {@link FeatureCustomCutoffType#getTargetStatus()} = {@link ParamCheckStatus#SUCCESS}.
     * Ожидаемый результат: модерация стартанула. Фича MARKETPLACE_SELF_DELIVERY переводится в NEW,
     * катофф {@link DSBSCutoffs#QUALITY_OTHER} закрывается, {@link DSBSCutoffs#ORDER_NOT_ACCEPTED} остается.
     */
    @Test
    @DbUnitDataSet(before = {"requestCpaModerationFromFail.success.before.csv",
            "successMarketplaceSelfDeliveryFeature.selfTariffs.before.csv"},
            after = "requestCpaModerationFromFail.success.after.csv")
    void successfulStartOfCpaModerationFromFail() {
        ResponseEntity<String> response = get(String.format("%s/pushReadyForTesting?id=%d", baseUrl, 200));

        assertResponse(response.getBody(), "api/cpaModeration.success.xml");
    }

    /**
     * Отправляем реплицированный ДСБС магазин из белого списка СКК на премодерацию.
     * У магазина фича MARKETPLACE_SELF_DELIVERY в статусе DONT_WANT.
     * Ожидаемый результат: модерация пропущена. Фича MARKETPLACE_SELF_DELIVERY переводится в NEW.
     */
    @Test
    @DbUnitDataSet(before = {"requestCpaModeration.success.before.csv",
            "successMarketplaceSelfDeliveryFeature.selfTariffs.before.csv",
            "requestCpaModeration.skip.success.before.csv"},
            after = "requestCpaModeration.skip.after.csv")
    void successfulSkipOfCpaModeration() {
        ResponseEntity<String> response = get(String.format("%s/pushReadyForTesting?id=%d", baseUrl, 200));

        assertResponse(response.getBody(), "api/cpaModeration.success.xml");
    }

    /**
     * Отправляем ДСБС магазин на премодерацию. У магазина фича MARKETPLACE_SELF_DELIVERY в
     * статусе DONT_WANT. Заявка на предоплату магазина уже в статусе COMPLETED
     * Ожидаемый результат: модерация стартанула. Фича MARKETPLACE_SELF_DELIVERY переводится в NEW.
     * Заявка на предоплату ни в какой статус не переходит
     */
    @Test
    @DbUnitDataSet(before = "requestCpaModeration.success.prepayApproved.before.csv",
            after = "requestCpaModeration.success.prepayApproved.after.csv")
    void successfulStartOfCpaModeration_prepayApproved() {
        mockMarketId(123L);

        ResponseEntity<String> response = get(String.format("%s/pushReadyForTesting?id=%d", baseUrl, 200));

        assertResponse(response.getBody(), "api/cpaModeration.success.xml");
    }

    /**
     * Отправляем ДСБС магазин на премодерацию. У магазина фича MARKETPLACE_SELF_DELIVERY в
     * статусе REVOKE, т.к. был открыт катоф {@link DSBSCutoffs#QUALITY_SERIOUS}, по которому магазин не может сам
     * запросить модерацию.
     * Ожидаемый результат: модерация не стартует. Фича MARKETPLACE_SELF_DELIVERY остается в статусе REVOKE.
     */
    @Test
    @DbUnitDataSet(before = "unsuccessfulStartOfCpaModerationByBadCutoffs.before.csv",
            after = "unsuccessfulStartOfCpaModerationByBadCutoffs.after.csv")
    void unsuccessfulStartOfCpaModerationByBadCutoffs() {
        ResponseEntity<String> response = get(String.format("%s/pushReadyForTesting?id=%d", baseUrl, 200));

        assertResponse(response.getBody(), "api/cpaModeration.failed.xml");
    }

    /**
     * Отправляем ДСБС магазин на премодерацию. У магазина фича MARKETPLACE_SELF_DELIVERY в
     * статусе DONT_WANT, но у магазина не настроены тарифы  => не срабатывает прекондишн на фиче.
     * Ожидаемый результат: модерация не стартует. Фича MARKETPLACE_SELF_DELIVERY остается DONT_WANT.
     */
    @Test
    @DbUnitDataSet(before = "requestCpaModeration.fail.before.csv",
            after = "requestCpaModeration.fail.after.csv")
    void unsccessfulStartOfCpaModeration() {
        ResponseEntity<String> response = get(String.format("%s/pushReadyForTesting?id=%d", baseUrl, 200));

        assertResponse(response.getBody(), "api/cpaModeration.failed.xml");
    }

    /**
     * Запрашиваем проверку магазина, имеющего катофф за непринятый заказ.
     */
    @Test
    @DbUnitDataSet(before = "requestCpaCheckForDsbsWithOrderNotAcceptedCutoff.before.csv",
            after = "requestCpaCheckForDsbsWithOrderNotAcceptedCutoff.after.csv")
    void successfulStartOfCheckForShopWithSuspendedOrder() {
        ResponseEntity<String> response = get(String.format("%s/pushReadyForTesting?id=%d", baseUrl, 200));

        assertResponse(response.getBody(), "api/cpaModeration.success.xml");
    }

    private void assertResponse(String actualContent, String expectedXmlFile) {
        final String expectedContent = FunctionalTestHelper.getResource(getClass(), expectedXmlFile);

        MatcherAssert.assertThat(actualContent, MbiMatchers.xmlEquals(
                expectedContent, ImmutableSet.of("servant", "version", "host", "executing-time", "actions")));
    }

    private void mockMarketId(long marketId) {
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            GetByPartnerResponse response = GetByPartnerResponse.newBuilder()
                    .setMarketId(MarketAccount.newBuilder().setMarketId(marketId).build())
                    .setSuccess(true)
                    .build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(any(GetByPartnerRequest.class), any());
    }
}
