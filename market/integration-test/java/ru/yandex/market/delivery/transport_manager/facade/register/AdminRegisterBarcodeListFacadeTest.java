package ru.yandex.market.delivery.transport_manager.facade.register;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.admin.dto.NewRegisterBarcodeListDto;
import ru.yandex.market.logistics.front.library.dto.FormattedTextObject;

@DbUnitConfiguration(
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"},
    dataSetLoader = ReplacementDataSetLoader.class
)
@DatabaseSetup(value = "/repository/health/dbqueue/empty.xml", connection = "dbUnitDatabaseConnectionDbQueue")
class AdminRegisterBarcodeListFacadeTest extends AbstractContextualTest {
    @Autowired
    private AdminRegisterBarcodeListFacade facade;

    @DatabaseSetup("/repository/transportation/anomaly_linehaul_accepted.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/anomaly_linehaul_accepted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/anomaly_linehaul_register.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/anomaly/put_inbound.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void createRegisterBarcodeList() {
        NewRegisterBarcodeListDto dto = new NewRegisterBarcodeListDto();
        dto.setBarcodes(new FormattedTextObject("1,2;3\n4\t5"));
        facade.createRegisterBarcodeList(dto, 11L);
    }

    @DatabaseSetup({
        "/repository/transportation/anomaly_linehaul_accepted.xml",
        "/repository/register/anomaly_linehaul_register.xml",
    })
    @ExpectedDatabase(
        value = "/repository/transportation/anomaly_linehaul_accepted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/anomaly_linehaul_register_append.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/anomaly/put_inbound.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void createRegisterBarcodeListAppend() {
        NewRegisterBarcodeListDto dto = new NewRegisterBarcodeListDto();
        dto.setBarcodes(new FormattedTextObject("1,2;3\n4\t5"));
        facade.createRegisterBarcodeList(dto, 11L);
    }

    @DatabaseSetup("/repository/transportation/anomaly_linehaul_new.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/anomaly_linehaul_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/anomaly_linehaul_register.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/health/dbqueue/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    public void createRegisterBarcodeListNoInbound() {
        NewRegisterBarcodeListDto dto = new NewRegisterBarcodeListDto();
        dto.setBarcodes(new FormattedTextObject("1,2;3\n4\t5,5,5"));
        facade.createRegisterBarcodeList(dto, 11L);
    }
}
