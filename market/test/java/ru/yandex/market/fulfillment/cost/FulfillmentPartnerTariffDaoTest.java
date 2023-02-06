package ru.yandex.market.fulfillment.cost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.model.BillingUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

@DbUnitDataSet(before = "FulfillmentPartnerTariffDaoTest.before.csv")
public class FulfillmentPartnerTariffDaoTest extends FunctionalTest {

    private static final FulfillmentPartnerTariff EXPECTED_TARIFF = FulfillmentPartnerTariff.builder()
            .setTariffId(1L)
            .setWarehouseId(1L)
            .setTitle("Хранение обычного товара")
            .setTariffRule(FulfillmentPartnerTariffRule.STORAGE)
            .setValue(BigDecimal.valueOf(30))
            .setUnit(BillingUnit.ITEM)
            .setDateFrom(LocalDate.of(2018, 8, 1))
            .setDateTo(null)
            .build();

    private final FulfillmentPartnerTariffDao fulfillmentTariffDao;

    @Autowired
    public FulfillmentPartnerTariffDaoTest(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        fulfillmentTariffDao = new FulfillmentPartnerTariffDao(namedParameterJdbcTemplate);
    }

    @Test
    public void getTariffs() {
        List<FulfillmentPartnerTariff> tariffs =
                fulfillmentTariffDao.getTariffs(LocalDate.now());
        assertThat(tariffs, hasSize(3));
        assertThat(tariffs, hasItem(EXPECTED_TARIFF));
    }
}
