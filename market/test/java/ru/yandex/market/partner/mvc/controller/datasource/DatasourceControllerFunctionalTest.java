package ru.yandex.market.partner.mvc.controller.datasource;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.balance.model.BalanceException;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Функциональные тесты для {@link DatasourceController}.
 *
 * @author Vladislav Bauer
 */
class DatasourceControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher;

    @BeforeEach
    void setUp() {
        when(logbrokerPartnerChangesEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));

    }

    @Autowired
    private BalanceService balanceService;

    @DisplayName("Магазин без менеджера")
    @Test
    @DbUnitDataSet(before = "getDatasource.before.csv")
    void testGetDatasource() {
        var response = FunctionalTestHelper.get(baseUrl + "/shop/datasource?_user_id=1068106468&_remote_ip=2a02%3A6b8" +
                "%3A0%3A5407%3A9163%3A509d%3Acb92%3Acfa4&euid=677348053&campaign_id=10774");
        JsonTestUtil.assertEquals(response, getClass(), "testGetDatasource.json");
    }

    @DisplayName("Поставщик с заданным индустриальным менеджером. Не показываем менеджера")
    @Test
    @DbUnitDataSet(before = "getDatasource.before.csv")
    void testGetSupplierWithIndustrial() {
        var response = FunctionalTestHelper.get(baseUrl + "/shop/datasource?_user_id=1068106468&_remote_ip=2a02%3A6b8" +
                "%3A0%3A5407%3A9163%3A509d%3Acb92%3Acfa4&euid=677348053&campaign_id=10776");
        JsonTestUtil.assertEquals(response, getClass(), "testGetSupplierWithIndustrial.json");
    }

    @DisplayName("Поставщик с дефолтным менеджером")
    @Test
    @DbUnitDataSet(before = "getDatasource.before.csv")
    void testGetSupplier() {
        var response = FunctionalTestHelper.get(baseUrl + "/shop/datasource?_user_id=1068106468&_remote_ip=2a02%3A6b8" +
                "%3A0%3A5407%3A9163%3A509d%3Acb92%3Acfa4&euid=677348053&campaign_id=10775");
        JsonTestUtil.assertEquals(response, getClass(), "testGetSupplier.json");
    }

    @Test
    @DbUnitDataSet(before = "DatasourceControllerFunctionalTest.testAgencyName.before.csv")
    void testAgencyName() {
        when(balanceService.getClient(100))
                .thenReturn(new ClientInfo(100, ClientType.PHYSICAL, false, 200));

        var response = FunctionalTestHelper.get(baseUrl + "/shop/datasource?_user_id=1068106468&_remote_ip=2a02%3A6b8" +
                "%3A0%3A5407%3A9163%3A509d%3Acb92%3Acfa4&euid=677348053&campaign_id=1001");
        JsonTestUtil.assertEquals(response, getClass(), "DatasourceControllerFunctionalTest.testAgencyName.json");
    }

    @Test
    @DbUnitDataSet(
            before = "changeDatasourceManager.before.csv",
            after = "changeDatasourceManager.after.csv"
    )
    void changeManager() {
        ResponseEntity<String> result =
                FunctionalTestHelper.post(baseUrl + "/shop/change-manager?_user_id=123&new_manager_id=456" +
                        "&datasource_ids=774,777");
        verify(balanceService, times(1)).createOrUpdateOrderByCampaign(any(), anyLong());
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        var captor = ArgumentCaptor.forClass(PartnerChangesProtoLBEvent.class);
        verify(logbrokerPartnerChangesEventPublisher, times(2))
                .publishEventAsync(captor.capture());
        assertThat(captor.getValue().getPayload().getManager().getManagerId()).isEqualTo(456L);
        verifySentNotificationType(partnerNotificationClient, 2, 127L);
    }

    @DisplayName("Проверить случай, когда менеджера не существует")
    @Test
    @DbUnitDataSet
    void testWrongManager() {
        assertThatThrownBy(() -> FunctionalTestHelper.post(baseUrl
                + "/shop/change-manager?_user_id=123&new_manager_id=456&datasource_ids=774"))
                .hasMessage("400 Bad Request");
        verifyNoInteractions(logbrokerPartnerChangesEventPublisher);
    }

    @DisplayName("Проверить случай, когда менеджер не найден в Балансе")
    @Test
    @DbUnitDataSet(before = "wrongBalanceAccess.csv")
    void testWrongBalanceAccess() {
        doThrow(new BalanceException(
                "<error>" +
                        "    <msg>Object not found: Manager for uid was not found</msg>" +
                        "    <object>Manager for uid 648078683 was not found</object>" +
                        "    <wo-rollback>0</wo-rollback>" +
                        "    <method>process_order</method>" +
                        "    <code>MANAGER_NOT_FOUND</code>" +
                        "    <parent-codes>" +
                        "        <code>EXCEPTION</code>" +
                        "    </parent-codes>" +
                        "    <contents>Object not found: Manager for uid was not found</contents>" +
                        "</error>"))
                .when(balanceService)
                .createOrUpdateOrderByCampaign(any(), anyLong());
        assertThatThrownBy(() -> FunctionalTestHelper.post(baseUrl
                + "/shop/change-manager?_user_id=123&new_manager_id=456&datasource_ids=774"))
                .hasMessage("403 Forbidden");
        verify(balanceService, times(1)).createOrUpdateOrderByCampaign(any(), anyLong());
    }
}
