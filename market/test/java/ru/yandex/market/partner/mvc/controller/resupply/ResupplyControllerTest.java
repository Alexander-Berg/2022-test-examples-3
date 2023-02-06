package ru.yandex.market.partner.mvc.controller.resupply;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.order.returns.os.OrderServiceReturnDao;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "ResupplyControllerTest.before.csv")
class ResupplyControllerTest extends FunctionalTest {

    @Autowired
    public OrderServiceReturnDao orderServiceReturnDao;

    @Test
    @DisplayName("Не возвращается СЦ для партнера, не работающего через СЦ")
    void testGetReadyToPickupResuppliesPointsNonScPartner() {
        when(orderServiceReturnDao.getReturnLinesReadyForPickup(anyLong())).thenReturn(List.of());
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getReadyForPickupPartnersUrl(101));
        JsonTestUtil.assertEquals(entity, "[]");
    }

    @Test
    @DisplayName("Получить список сортировочных центров -  у партнера только невыкупы")
    void testGetReadyToPickupResuppliesPointsScPartner() {
        when(orderServiceReturnDao.getReturnLinesReadyForPickup(anyLong())).thenReturn(List.of());
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getReadyForPickupPartnersUrl(202));
        JsonTestUtil.assertEquals(entity, "[{\"id\":9,\"name\":\"Служба доставки пегасами\"},{\"id\":51," +
                "\"name\":\"Служба доставки единорогами\"}]");
    }

    @Test
    @DisplayName("Получить список сортировочных центров -  у партнера и невыкупы, и возвраты")
    void testGetReadyToPickupResuppliesAndReturnsPointsScPartner() {
        when(orderServiceReturnDao.getReturnLinesReadyForPickup(anyLong())).thenReturn(List.of());
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getReadyForPickupPartnersUrl(303));
        JsonTestUtil.assertEquals(entity, "[{\"id\":133,\"name\":\"Сортировочный центр\"},{\"id\":73355," +
                "\"name\":\"Тарный\"}]");
    }

    @Test
    @DisplayName("Не возвращается СЦ - у партнера нет невыкупов и возвраты не в нужном для выдачи статусе")
    void testNoReadyToPickupReturnsPartner() {
        when(orderServiceReturnDao.getReturnLinesReadyForPickup(anyLong())).thenReturn(List.of());
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getReadyForPickupPartnersUrl(404));
        JsonTestUtil.assertEquals(entity, "[]");
    }

    @Test
    @DisplayName("Получить данные для виджета сводки по невыкупам и возвратам")
    void testGetUnredeemedAndReturnsSummary() {
        when(orderServiceReturnDao.getReturnLinesReadyForPickup(anyLong())).thenReturn(List.of());
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getReadyForPickupSummaryUrl(707));
        JsonTestUtil.assertEquals(entity, "{\"unredeemedCount\":7,\"returnCount\":2,\"billedUnredeemedCount\":0," +
                "\"billedReturnCount\":0,\"summaryBilling\":0}");
    }

    @Nonnull
    private String getReadyForPickupPartnersUrl(int supplierId) {
        return baseUrl + "/resupply/readyToPickupSortingCenters?datasource_id=" + supplierId;
    }

    @Nonnull
    private String getReadyForPickupSummaryUrl(int supplierId) {
        return baseUrl + "/resupply/unredeemedAndReturnsSummary?datasource_id=" + supplierId;
    }
}
