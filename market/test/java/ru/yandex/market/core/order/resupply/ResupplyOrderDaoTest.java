package ru.yandex.market.core.order.resupply;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.model.StockType;
import ru.yandex.market.core.shop.BeruVirtualShop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.core.fulfillment.model.StockType.FIT;

/**
 * Тест для {@link ResupplyOrderDao}
 */
@DbUnitDataBaseConfig(@DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true"))
@DbUnitDataSet(before = "ResupplyOrderDao.before.csv")
public class ResupplyOrderDaoTest extends FunctionalTest {

    @Autowired
    private ResupplyOrderDao resupplyOrderDao;

    @Test
    @DbUnitDataSet(after = "ResupplyOrderDaoSaveResupplies.after.csv")
    void saveResuppliesTest() {
        resupplyOrderDao.saveAboResupplies(List.of(
                ResupplyOrderItem.newBuilder()
                        .setPossibleCises(List.of("1", "2", "3", "4", "5"))
                        .setCis("101")
                        .setFFRequestId(122L)
                        .setOrderId(47798285L)
                        .setResupplyType(ResupplyType.UNREDEEMED)
                        .setAttributes(List.of("WAS_USED"))
                        .setCreatedAt(LocalDateTime.of(1, 1, 1, 0, 0))
                        .setItemCount(1)
                        .setItemId(91351328L)
                        .setSource(ResupplySource.ABO)
                        .setSourceId(14444444L)
                        .setSsku("ssku")
                        .setStockType(FIT)
                        .setSupplierId(BeruVirtualShop.ID)
                        .setWarehouseId(172L)
                        .build(),
                ResupplyOrderItem.newBuilder()
                        .setPossibleCises(List.of("1", "2", "3"))
                        .setCis("101")
                        .setFFRequestId(122L)
                        .setOrderId(47798285L)
                        .setResupplyType(ResupplyType.UNREDEEMED)
                        .setAttributes(List.of("WAS_USED"))
                        .setCreatedAt(LocalDateTime.of(1, 1, 1, 0, 0))
                        .setItemCount(1)
                        .setItemId(91351328L)
                        .setSource(ResupplySource.ABO)
                        .setSourceId(5555555L)
                        .setSsku("ssku")
                        .setStockType(StockType.DEFECT)
                        .setSupplierId(BeruVirtualShop.ID)
                        .setWarehouseId(172L)
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(after = "ResupplyOrderDaoSaveFFEnrich.after.csv")
    void saveFFEnrichTest() {
        resupplyOrderDao.saveFFEnrich(
                resupplyOrderDao.getItemsForEnrich(0, 100)
                        .stream()
                        .map(i -> i.toBuilder()
                                .setCis("23456")
                                .setFFRequestFinalization(
                                        LocalDateTime.of(1, 1, 1, 0, 0)).build())
                        .collect(Collectors.toList()));
    }

    @Test
    void getItemsForEnrichTest() {
        assertEquals("" +
                "1736489\n" +
                "1736494\n" +
                "1736499\n" +
                "1736510\n" +
                "1736511\n" +
                "1736516\n" +
                "1736517", resupplyOrderDao.getItemsForEnrich(0, 10)
                .stream()
                .map(ResupplyOrderItem::getSourceId)
                .map(String::valueOf)
                .sorted(String::compareTo)
                .collect(Collectors.joining("\n")));
    }

    @Test
    void ffOrdersResupplies() {
        assertEquals("" +
                "39813291 1736502\n" +
                "39813291 1736509\n" +
                "39813291 1736510\n" +
                "39813291 1736511\n" +
                "39813291 1736513\n" +
                "39813291 1736516\n" +
                "39813291 1736517\n" +
                "39813291 1736518\n" +
                "47798285 1736489\n" +
                "47798285 1736493\n" +
                "47798285 1736494\n" +
                "47798285 1736495\n" +
                "47798285 1736498\n" +
                "47798285 1736499\n" +
                "47798285 1736500", resupplyOrderDao.getFFOrdersResupplies(List.of(39813291L, 47798285L))
                .entries()
                .stream()
                .map(e -> e.getKey() + " " + e.getValue().getSourceId())
                .sorted(String::compareTo)
                .collect(Collectors.joining("\n")));
    }

    @Test
    void ordersResuppliesTest() {
        assertEquals("" +
                "39813291 1736502\n" +
                "39813291 1736509\n" +
                "39813291 1736510\n" +
                "39813291 1736511\n" +
                "39813291 1736513\n" +
                "39813291 1736516\n" +
                "39813291 1736517\n" +
                "39813291 1736518\n" +
                "47798285 1736489\n" +
                "47798285 1736493\n" +
                "47798285 1736494\n" +
                "47798285 1736495\n" +
                "47798285 1736498\n" +
                "47798285 1736499\n" +
                "47798285 1736500", resupplyOrderDao.getFFOrdersResupplies(List.of(39813291L, 47798285L))
                .entries()
                .stream()
                .map(e -> e.getKey() + " " + e.getValue().getSourceId())
                .sorted(String::compareTo)
                .collect(Collectors.joining("\n")));
    }

    @Test
    void getLastUnredeemedCises() {
        assertEquals("431782 7 1",
                resupplyOrderDao.getTotalUnredeemedCises(
                                LocalDateTime.of(0, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
                                LocalDateTime.of(10, 1, 1, 0, 0).toInstant(ZoneOffset.UTC))
                        .stream()
                        .map(t -> t.getPartnerId() + " " + t.getFitStock() + " " + t.getDefectStock())
                        .collect(Collectors.joining("\n")));
    }
}
