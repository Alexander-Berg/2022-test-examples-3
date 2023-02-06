package ru.yandex.market.sc.core.domain.ReportTemplates;

import net.sf.jasperreports.engine.JRException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.tpl.report.test.JasperTemplateTester;

@EmbeddedDbTest
public class ReportTemplatesTest {

    @Autowired
    JasperTemplateTester jasperTemplateTester;

    @Test
    public void testAllTemplates() throws JRException {
        jasperTemplateTester.compileAllTemplates();
    }

}
