package ru.yandex.market.wms.shippingsorter.sorting.repository;

import java.time.Clock;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.shared.libs.configproperties.dao.DbConfigRepository;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(ShippingSorterSecurityTestConfiguration.class)
@RequiredArgsConstructor
public class DbConfigRepositoryTest extends IntegrationTest {

    @Autowired
    @Qualifier("configPropertyPostgreSqlDao")
    private DbConfigRepository repository;

    @Autowired
    private Clock clock;

    @Test
    @DatabaseSetup("/sorting/repository/db-config/get-by-key/immutable.xml")
    @ExpectedDatabase(value = "/sorting/repository/db-config/get-by-key/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void selectConfigKeyTest() {
        assertEquals("30.0", repository.getStringConfigValue("PACKING_MAX_WEIGHT"));
        assertEquals("60", repository.getStringConfigValue("PACKING_MAX_WIDTH"));
        assertNull(repository.getStringConfigValue("NOT_EXISTING"));
    }

    @Test
    @DatabaseSetup("/sorting/repository/db-config/update-value/before.xml")
    @ExpectedDatabase(value = "/sorting/repository/db-config/update-value/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void updateConfigKeyValue() {
        repository.updateConfigByValue("PACKING_MAX_WEIGHT", "90.85", clock);
    }
}
