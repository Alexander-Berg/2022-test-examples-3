package ru.yandex.market.tpl.report.test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.AllArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;

import ru.yandex.market.tpl.report.core.ReportTemplate;
import ru.yandex.market.tpl.report.core.ReportTemplateRepository;

@AllArgsConstructor
public class JasperTemplateTester {
    private final ReportTemplateRepository templateRepository;

    public void compileAllTemplates() throws JRException {
        List<ReportTemplate> templateList = templateRepository.findAll();
        for (ReportTemplate item : templateList) {
            if (!item.getName().startsWith("zpl_")) {
                JasperCompileManager.compileReport(
                        new ByteArrayInputStream(item.getTemplate().getBytes(StandardCharsets.UTF_8)));
            }
        }
    }
}
