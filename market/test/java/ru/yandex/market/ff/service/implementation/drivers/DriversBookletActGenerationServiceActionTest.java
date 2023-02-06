package ru.yandex.market.ff.service.implementation.drivers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.ActGenerationServiceTest;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;

import static org.junit.Assert.assertNotNull;

/**
 * Тесты для локальной загрузки и проверки новых daas-шаблонов от технических писателей
 * (в пайплайне должны быть отключены)
 */
@Disabled
public class DriversBookletActGenerationServiceActionTest extends ActGenerationServiceTest {

    @Autowired
    private DriversBookletActGenerationService driversBookletActGenerationService;

    @Autowired
    private ShopRequestRepository shopRequestRepository;

    @Autowired
    private ConcreteEnvironmentParamService concreteEnvironmentParamService;

    @Test
    @Disabled
    @DatabaseSetup("classpath:service/drivers/requests-to-test-templates.xml")
    public void generateSupplyBooklets() throws IOException {
        Map<Long, String> warehouseIdToTemplateUrlMap =
                concreteEnvironmentParamService.getSupplyTemplatesUrlForDriverBooklets();
        getTemplatesNames(warehouseIdToTemplateUrlMap, true);
    }

    @Test
    @Disabled
    @DatabaseSetup("classpath:service/drivers/requests-to-test-templates-no-time.xml")
    public void generateSupplyBookletsWithNoTime() throws IOException {
        Map<Long, String> warehouseIdToTemplateUrlMap =
                concreteEnvironmentParamService.getSupplyTemplatesUrlForDriverBooklets();
        getTemplatesNames(warehouseIdToTemplateUrlMap, false);
    }

    @Test
    @Disabled
    @DatabaseSetup("classpath:service/drivers/requests-to-test-withdraw-templates-no-time.xml")
    public void generateWithdrawBooklets() throws IOException {
        Map<Long, String> warehouseIdToTemplateUrlMap =
                concreteEnvironmentParamService.getWithdrawalTemplatesUrlForDriverBooklets();
        getTemplatesNames(warehouseIdToTemplateUrlMap, true);
    }

    private void getTemplatesNames(Map<Long, String> warehouseIdToTemplateUrlMap, boolean withTime)
            throws IOException {
        Set<Map.Entry<Long, String>> entries = warehouseIdToTemplateUrlMap.entrySet();
        int i = 1;
        for (Map.Entry<Long, String> entry : entries) {
            generateBooklet(i++, entry.getValue(), withTime);
        }
    }

    private void generateBooklet(long requestId, String templateName, boolean withTime) throws IOException {
        InputStream pdfBooklet =
                driversBookletActGenerationService.generateReport(shopRequestRepository.findById(requestId));
        assertNotNull(pdfBooklet);
        File targetFile = new File("" +
                FilenameUtils.removeExtension(templateName) + (withTime ? "" : "-no-time") + ".pdf");
        FileUtils.copyInputStreamToFile(pdfBooklet, targetFile);
    }
}
