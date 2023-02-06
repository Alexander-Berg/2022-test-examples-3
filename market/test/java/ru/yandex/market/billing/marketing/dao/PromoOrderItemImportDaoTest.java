package ru.yandex.market.billing.marketing.dao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.marketing.model.PromoOrderImportDTO;
import ru.yandex.market.billing.marketing.model.PromoOrderItemImportDTO;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.tax.model.VatRate;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DbUnitDataSet(
        before = "../PromoOrderItemImport.enable.csv"
)
@ParametersAreNonnullByDefault
class PromoOrderItemImportDaoTest extends FunctionalTest {
    private static final DateTimeFormatter DATETIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    @Autowired
    PromoOrderItemImportDao promoOrderItemImportDao;

    @Test
    @DisplayName("Получение промозаказов из чекаутера, доставленных в заданную дату")
    @DbUnitDataSet(before = "PromoOrderItemImportDaoTest.before.csv")
    void getPromoOrderImportDTOForDate() {

        LocalDate date = LocalDate.parse("2021-06-30");

        List<PromoOrderImportDTO> dto =
                promoOrderItemImportDao.getPromoOrderImportDTOForDate(date);
        assertThat(dto.size()).isEqualTo(2);

        var expectedPromoOrderImportDTOs = List.of(
                PromoOrderImportDTO.builder()
                        .setPromoId(3L)
                        .setOrderId(4L)
                        .setDeliveredDateTime(Instant.from(DATETIME_FORMATTER.parse("2021-06-30 03:00:00.00")))
                        .setAnaplanId(2L)
                        .setPromoType(PromoType.CHEAPEST_AS_GIFT)
                        .build(),
                PromoOrderImportDTO.builder()
                        .setPromoId(4L)
                        .setOrderId(5L)
                        .setDeliveredDateTime(Instant.from(DATETIME_FORMATTER.parse("2021-06-30 04:00:00.00")))
                        .setAnaplanId(3L)
                        .setPromoType(PromoType.MARKET_PROMOCODE)
                        .build()
        );
        Assertions.assertThat(dto)
                .usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedPromoOrderImportDTOs);
    }

    @Test
    @DisplayName("Получение товаров промозаказов из чекаутера")
    @DbUnitDataSet(before = "PromoOrderItemImportDaoTest.before.csv")
    void getPromoOrderItemImportDTOByOrderIds() {
        LocalDate date = LocalDate.parse("2021-06-30");
        List<Long> promoIds =
                promoOrderItemImportDao.getPromoOrderImportDTOForDate(date).stream()
                        .map(PromoOrderImportDTO::getOrderId)
                        .collect(Collectors.toList());
        assertThat(promoIds.size()).isEqualTo(2);
        assertThat(promoIds).containsExactlyInAnyOrder(4L, 5L);

        List<PromoOrderItemImportDTO> items =
                promoOrderItemImportDao.getPromoOrderItemImportDTOByOrderIds(promoIds);
        assertThat(items.size()).isEqualTo(2);

        var expectedPromoOrderItemImportDTOs = List.of(
                PromoOrderItemImportDTO.builder()
                        .setOrderId(4L)
                        .setItemId(1L)
                        .setPromoId(3L)
                        .setAnaplanId(2L)
                        .setCashbackAmount(0L)
                        .setDiscountAmount(1000L)
                        .setItemCount(1)
                        .setOrderStatus(MbiOrderStatus.DELIVERED)
                        .setVatType(VatRate.VAT_20)
                        .setOrderCreationDate(LocalDate.of(2021, 6, 25))
                        .build(),
                PromoOrderItemImportDTO.builder()
                        .setOrderId(5L)
                        .setItemId(2L)
                        .setPromoId(4L)
                        .setAnaplanId(3L)
                        .setCashbackAmount(0L)
                        .setDiscountAmount(2000L)
                        .setItemCount(1)
                        .setOrderStatus(MbiOrderStatus.DELIVERED)
                        .setVatType(VatRate.VAT_20)
                        .setOrderCreationDate(LocalDate.of(2021, 6, 25))
                        .build()
        );
        Assertions.assertThat(items)
                .usingRecursiveComparison().isEqualTo(expectedPromoOrderItemImportDTOs);
    }
}
