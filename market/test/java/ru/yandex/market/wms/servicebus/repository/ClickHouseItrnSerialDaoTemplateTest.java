package ru.yandex.market.wms.servicebus.repository;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.async.dto.ItrnSerialDto;

public class ClickHouseItrnSerialDaoTemplateTest extends IntegrationTest {

    @Autowired
    ClickHouseItrnSerialDaoTemplate daoTemplate;

    @Test
    @ExpectedDatabase(
            value = "/repository/itrn-serial/after.xml",
            connection = "clickHouseConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void insert() {

        final ItrnSerialDto itrnSerial = ItrnSerialDto.builder()
                .itrnSerialKey("0000000401")
                .itrnKey("0000000301")
                .storerKey("STORER")
                .sku("SKU")
                .lot("LOT01")
                .id("DRP0000001")
                .loc("PACK")
                .serialNumber("0010000001")
                .qty(BigDecimal.valueOf(1))
                .tranType("MV")
                .addWho("TEST")
                .editWho("TEST")
                .build();

        daoTemplate.insert(itrnSerial);
    }

    @Test
    @DatabaseSetup(
            value = "/repository/itrn-serial/before.xml",
            connection = "clickHouseConnection")
    @ExpectedDatabase(
            value = "/repository/itrn-serial/after.xml",
            connection = "clickHouseConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void update() {

        final ItrnSerialDto itrnSerial = ItrnSerialDto.builder()
                .serialkey(10000L)
                .itrnSerialKey("0000000401")
                .itrnKey("0000000301")
                .storerKey("STORER")
                .sku("SKU")
                .lot("LOT01")
                .id("DRP0000001")
                .loc("PACK")
                .serialNumber("0010000001")
                .qty(BigDecimal.valueOf(1))
                .tranType("MV")
                .addWho("TEST")
                .editWho("TEST")
                .build();

        daoTemplate.update(itrnSerial);
    }
}
