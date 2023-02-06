package ru.yandex.market.gutgin.tms.pipeline.dcp.xls;


import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.gutgin.tms.config.DcpXlsValidationConfig;
import ru.yandex.market.gutgin.tms.config.TestDcpXlsConfig;
import ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation.ValidationFlow;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.partner.content.common.BaseDcpExcelDBStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.FileProcessMessageService;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcRawSkuDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessMessageType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawParamValue;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {
    DcpXlsValidationConfig.class,
    TestDcpXlsConfig.class
})
public class ValidateRawSkuTaskActionTest extends BaseDcpExcelDBStateGenerator {

    private static final RawParamValue VENDOR_VALUE = new RawParamValue(
        ParameterValueComposer.VENDOR_ID,
        MainParamCreator.VENDOR_NAME,
        "Dummy Vendor"
    );

    private static final RawParamValue BARCODE = new RawParamValue(
            ParameterValueComposer.BARCODE_ID,
            ParameterValueComposer.BARCODE,
            "9785990980518"
    );

    @Autowired
    ValidationFlow<GcRawSku> dcpValidationFlow;

    @Autowired
    FileProcessMessageService fileProcessMessageService;

    @Autowired
    GcRawSkuDao gcRawSkuDao;

    ValidateRawSkuTaskAction taskAction;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        taskAction = new ValidateRawSkuTaskAction(gcRawSkuDao,
            fileProcessMessageService,
            dcpValidationFlow);
    }

    @Test
    public void whenAllValidShouldMarkValidAndSaveNoMessages() {
        generateGcRawSkus(10, gcRawSkus -> {
            gcRawSkus.forEach(rawSku -> {
                List<RawParamValue> rawParamValues = new ArrayList<>(rawSku.getRawParamValues());
                rawParamValues.add(VENDOR_VALUE);
                rawParamValues.add(BARCODE);
                rawSku.setRawParamValues(rawParamValues);
            });
        });
        taskAction.doRun(processFileOptionalResultData);
        List<MessageInfo> messages =
            fileProcessMessageService.getFileProcessMessages(processId, FileProcessMessageType.FILE_VALIDATION);
        assertThat(messages).isEmpty();
        List<GcRawSku> gcRawSkus = gcRawSkuDao.fetchByFileProcessId(processId);
        assertThat(gcRawSkus)
            .extracting(GcRawSku::getValidForDatacamp)
            .containsOnly(true);
    }

    @Test
    public void whenHaveInvalidShouldMarkInvalidAndSaveMessages() {
        int invalidCount = 10;
        generateGcRawSkus(invalidCount, gcRawSkus -> {
            gcRawSkus.forEach(rawSku -> {
                List<RawParamValue> rawParamValues = new ArrayList<>(rawSku.getRawParamValues());
                rawParamValues.add(BARCODE);
                rawSku.setRawParamValues(rawParamValues);
            });
        });
        taskAction.doRun(processFileOptionalResultData);
        List<MessageInfo> messages =
            fileProcessMessageService.getFileProcessMessages(processId, FileProcessMessageType.FILE_VALIDATION);
        assertThat(messages).hasSize(invalidCount);
        List<GcRawSku> gcRawSkus = gcRawSkuDao.fetchByFileProcessId(processId);
        assertThat(gcRawSkus)
            .extracting(GcRawSku::getValidForDatacamp)
            .containsOnlyNulls();
    }
}
