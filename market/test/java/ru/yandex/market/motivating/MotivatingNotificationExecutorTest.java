package ru.yandex.market.motivating;

import java.util.List;
import java.util.Objects;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Тест проверяет, работу джобы {@link MotivatingNotificationExecutor}.
 *
 * @author yakun
 */
class MotivatingNotificationExecutorTest extends FunctionalTest {
    @Autowired
    private MotivatingNotificationExecutor motivatingNotificationExecutor;

    @Autowired
    @Qualifier("dataCampShopClient")
    protected DataCampClient dataCampShopClient;

    @Autowired
    protected SaasService saasService;

    /**
     * Предварительные данные:
     * <ol>
     * <li>shopId - 774 - был в очереди, проверяем что не удалится оттуда</li>
     * <li>shopId - 775 - не было в очереди, но попадет</li>
     * <li>shopId - 776 - не было в очереди, не попадет потому что были попытки отправиться на премодерацию</li>
     * <li>shopId - 777 - был в очереди, но не будет отправки уведомления,
     * потому что недостаточно долго там находился</li>
     * <li>shopId - 779 - SMB не в очереди, с незаполненным шагом OFFER</li>
     * </ol>
     */
    @Test
    @DbUnitDataSet(before = "motivatingNotificationExecutorTest.before.csv")
    @DbUnitDataSet(after = "motivatingNotificationExecutorTest.after.csv")
    @DisplayName("Тест на отправку уведомлений")
    void testExecutorSendNotification() {
        var dataCampResponse = ProtoTestUtil.getProtoMessageByJson(SyncGetOffer.GetUnitedOffersResponse.class, "datacamp.smb.filled.json", getClass());
        doReturn(DataCampStrollerConversions.fromStrollerResponse(dataCampResponse))
                .when(dataCampShopClient)
                .searchBusinessOffers(
                        argThat(dataCampRequest -> Objects.equals(dataCampRequest.getPartnerId(), 779L))
                );
        SaasSearchResult resultMock = SaasSearchResult.builder()
                .setTotalCount(1)
                .setOffers(List.of())
                .build();
        doReturn(resultMock).when(saasService).searchBusinessOffers(argThat(filter -> filter.getShopIds().contains(779L)));

        motivatingNotificationExecutor.doJob(null);

        verifySentNotificationType(partnerNotificationClient, 1, MotivatingNotificationService.NN);
    }
}
