package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.Objects;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;

import static org.mockito.ArgumentMatchers.argThat;

/**
 * Функциональные тесты для шага wizard'a "Фиды".
 * См {@link ru.yandex.market.core.wizard.step.FeedStepStatusCalculator}
 *
 * @author Vladislav Bauer
 */
@DbUnitDataSet(before = "csv/commonWhiteWizardData.before.csv")
class WizardControllerFeedFunctionalTest extends AbstractWizardControllerFunctionalTest {

    /**
     * Проверить что фиды не настроены.
     */
    @Test
    void testFeedStepWithoutFeeds() {
        mockPartnerOffers(0);
        final ResponseEntity<String> response = requestStep(CAMPAIGN_ID, WizardStepType.FEED);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    /**
     * Проверить что есть фид, но он протух.
     */
    @Test
    @DbUnitDataSet(before = "csv/testFeedStepWithAllExpired.before.csv")
    void testFeedStepWithAllExpired() {
        final ResponseEntity<String> response = requestStep(CAMPAIGN_ID, WizardStepType.FEED);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    /**
     * Проверить что есть фид и среди протухших фидов, есть непротухшие.
     */
    @Test
    @DbUnitDataSet(before = "csv/testFeedStepWithSomeExpired.before.csv")
    void testFeedStepWithSomeExpired() {
        final ResponseEntity<String> response = requestStep(CAMPAIGN_ID, WizardStepType.FEED);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    /**
     * Проверить что есть фид и он не протух.
     */
    @Test
    @DbUnitDataSet(before = "csv/testFeedStepFilled.before.csv")
    void testFeedStepFilled() {
        final ResponseEntity<String> response = requestStep(CAMPAIGN_ID, WizardStepType.FEED);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    /**
     * Проверить что есть дефолтный фид не учитывается при расчете статуса шага визарда.
     */
    @Test
    @DbUnitDataSet(before = "csv/testFeedStepDefaultFeedIsIgnored.csv")
    void testFeedStepDefaultFeedIsIgnored() {
        mockPartnerOffers(0);
        final ResponseEntity<String> response = requestStep(CAMPAIGN_ID, WizardStepType.FEED);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    /**
     * Если фид только дефолтный, но есть оффера в ЕОХ, считаем шаг загрузки фида пройденным.
     */
    @Test
    @DbUnitDataSet(before = "csv/testFeedStepDefaultFeedIsIgnored.csv")
    void testFeedStepDefaultFeedHasOffersInDatacamp() {
        mockPartnerOffers(1);
        final var dataCampResponse =
                ProtoTestUtil.getProtoMessageByJson(SyncGetOffer.GetUnitedOffersResponse.class,
                        "json/datacamp.filled.json", getClass());
        Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(dataCampResponse))
                .when(dataCampShopClient)
                .searchBusinessOffers(
                        argThat(request -> Objects.equals(request.getPartnerId(), 774L))
                );

        final ResponseEntity<String> response = requestStep(CAMPAIGN_ID, WizardStepType.FEED);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    private static WizardStepStatus makeResponseStepStatus(Status status) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.FEED)
                .withStatus(status)
                .build();
    }
}
