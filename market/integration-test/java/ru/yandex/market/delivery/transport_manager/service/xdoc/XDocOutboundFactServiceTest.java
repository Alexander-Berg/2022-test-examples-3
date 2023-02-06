package ru.yandex.market.delivery.transport_manager.service.xdoc;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.service.PropertyService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DatabaseSetup(value = "/repository/health/dbqueue/empty.xml", connection = "dbUnitDatabaseConnectionDbQueue")
@DbUnitConfiguration(
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"},
    dataSetLoader = ReplacementDataSetLoader.class
)
@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
class XDocOutboundFactServiceTest extends AbstractContextualTest {

    @Autowired
    private XDocOutboundFactService xDocOutboundFactService;

    @Autowired
    private PropertyService<TmPropertyKey> propertyService;

    @Test
    @DisplayName("Обработка планового реестра изъятия xdoc-поставки с РЦ")
    @DatabaseSetup(
        value = {
            "/repository/facade/register_facade/xdoc/distribution_unit.xml",
        }
    )
    @DatabaseSetup(
        value = "/repository/health/dbqueue/empty.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/xdoc/distribution_unit_after_unfreeze.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/xdoc/distribution_unit_after_unfreeze_dbqueue.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void processOutboundFactRegister() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOC_TRANSPORT_CALENDARING))
            .thenReturn(true);
        xDocOutboundFactService.processOutboundFactRegister(2L);
    }
}
