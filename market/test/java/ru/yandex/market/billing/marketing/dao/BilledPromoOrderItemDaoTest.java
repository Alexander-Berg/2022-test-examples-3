package ru.yandex.market.billing.marketing.dao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.marketing.model.BilledPromoOrderItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

@ParametersAreNonnullByDefault
class BilledPromoOrderItemDaoTest extends FunctionalTest {
    private static final DateTimeFormatter DATETIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    @Autowired
    BilledPromoOrderItemDao billedPromoOrderItemDao;

    @Test
    @DisplayName("Добавление новых данных биллинга")
    @DbUnitDataSet(
            before = "BilledPromoOrderItemDaoTest.persistnew.before.csv",
            after = "BilledPromoOrderItemDaoTest.persistnew.after.csv"
    )
    void persistNewOrderItem() {
        var billedPromoOrderItems = List.of(
                BilledPromoOrderItem.builder()
                        .setItemId(20)
                        .setCampaignId(1234)
                        .setPartnerId(2000)
                        .setTrantime(Instant.from(DATETIME_FORMATTER.parse("2021-07-10 00:00:00.00")))
                        .setAmount(100000)
                        .setExportedToTlog(false)
                        .build()
        );

        billedPromoOrderItemDao.persistBilledPromoOrderItems(billedPromoOrderItems);
    }

    @Test
    @DisplayName("Обновление существующих данных биллинга")
    @DbUnitDataSet(
            before = "BilledPromoOrderItemDaoTest.update.before.csv",
            after = "BilledPromoOrderItemDaoTest.update.after.csv"
    )
    void updateExisting() {
        var billedPromoOrderItems = List.of(
                BilledPromoOrderItem.builder()
                        .setItemId(10)
                        .setCampaignId(100)
                        .setPartnerId(1000)
                        .setTrantime(Instant.from(DATETIME_FORMATTER.parse("2021-07-10 00:00:00.00")))
                        .setAmount(10)
                        .setExportedToTlog(false)
                        .build()
        );

        billedPromoOrderItemDao.persistBilledPromoOrderItems(billedPromoOrderItems);
    }

    @Test
    @DisplayName("Сброс параметров существующих данных биллинга")
    @DbUnitDataSet(
            before = "BilledPromoOrderItemDaoTest.reset.before.csv",
            after = "BilledPromoOrderItemDaoTest.reset.after.csv"
    )
    void resetExisting() {
        billedPromoOrderItemDao.resetBilledAmounts(LocalDate.parse("2021-07-11"));
    }
}
