package ru.yandex.market.wms.common.spring.dao;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.implementation.PrinterDao;
import ru.yandex.market.wms.common.spring.enums.LabelType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class PrinterDaoTest extends IntegrationTest {

    @Autowired
    private PrinterDao printerDao;

    @Test
    @DatabaseSetup("/db/dao/uidlabel/before.xml")
    @ExpectedDatabase(value = "/db/dao/uidlabel/before.xml", assertionMode = NON_STRICT)
    public void getLabelTemplateName() {
        Optional<String> templateName = printerDao.findLabelTemplateName("testPrinter", LabelType.SERIAL_NUMBER);
        assertions.assertThat(templateName.get()).isEqualTo("license_plate");
    }
}
