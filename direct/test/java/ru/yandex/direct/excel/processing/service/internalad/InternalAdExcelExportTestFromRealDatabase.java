package ru.yandex.direct.excel.processing.service.internalad;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.excel.processing.configuration.ExcelProcessingConfiguration;
import ru.yandex.direct.excel.processing.model.internalad.InternalAdExportParameters;

import static ru.yandex.direct.excel.processing.service.internalad.InternalAdExcelExportTest.compareWithExpectedData;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ExcelProcessingConfiguration.class})
@Ignore("only for manual runs, because it connects to real database")
@ParametersAreNonnullByDefault
public class InternalAdExcelExportTestFromRealDatabase {

    @Autowired
    private InternalAdExcelService internalAdExcelService;

    private ClientId clientId;
    private long campaignId;

    @Before
    public void initTestData() {
        clientId = ClientId.fromLong(57246342);
        campaignId = 42830239;
    }

    @Test
    public void checkInternalAdExcelExportFromRealDatabase() {
        InternalAdExportParameters exportParameters = new InternalAdExportParameters()
                .withClientId(clientId)
                .withExportAdGroupsWithAds(true)
                .withCampaignIds(Set.of(campaignId))
                .withHideEmptyColumns(false);

        Workbook workbook = internalAdExcelService.createWorkbook(exportParameters).getWorkbook();
        compareWithExpectedData(workbook, "export-internal-ad_from_real_database.xlsx",
                DefaultCompareStrategies.allFields());
    }

}
