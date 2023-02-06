package ru.yandex.market.wrap.infor.repository;

import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenContextHolder;
import ru.yandex.market.wrap.infor.configuration.AbstractContextualTest;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.SkuRow;

import static org.assertj.core.data.MapEntry.entry;

class SkuRepositoryTest extends AbstractContextualTest {

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private TokenContextHolder tokenContextHolder;

    @BeforeEach
    void setUp() {
        tokenContextHolder.setToken("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    }

    @AfterEach
    void tearDown() {
        tokenContextHolder.clearToken();
    }

    /**
     * Проверяем, что при попытке выбрать PACKKEY и SUSR1 для 3х SKU (..001,002,003) со следующим состоянием в БД:
     * <p>
     * * (TST0000000000000000001 - STD, 0)
     * * (TST0000000000000000002 - P_TST0000000000000000002, 1)
     * * (TST0000000000000000003) - SKU отсутствует в БД
     * * (TST0000000000000000004 - P_TST0000000000000000004, null)
     * <p>
     * Будет возвращена соответствующая мапа, в которой будут записаны key-value только для SKU.
     */
    @Test
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/integration/sku_repository/find_pack_keys.xml")
    @ExpectedDatabase(
        connection = "wmsConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:fixtures/integration/sku_repository/find_pack_keys.xml")
    void findPackKeys() {
        Map<String, SkuRow> result = skuRepository.findSkuRows(Sets.newSet(
            "TST0000000000000000001",
            "TST0000000000000000002",
            "TST0000000000000000003"
            )
        );

        softly.assertThat(result).containsOnly(
            entry("TST0000000000000000001", new SkuRow("TST0000000000000000001", "STD", "0")),
            entry("TST0000000000000000002", new SkuRow("TST0000000000000000002", "P_TST0000000000000000002", "1"))
        );
    }
}
