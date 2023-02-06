package ru.yandex.market.admin.service.remote;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.user.cellview.client.Column;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.model.convert.UniConverter;
import ru.yandex.market.admin.ui.model.fileupload.UIFileUploadInfo;
import ru.yandex.market.admin.ui.model.mapping.UiWarehouseMappingFileUploadInfo;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.common.Mbi;

class RemoteFulfillmentWarehouseMappingUploadUiServiceTest extends FunctionalTest {

    @Autowired
    RemoteFulfillmentWarehouseMappingUploadUiService service;

    @Autowired
    UniConverter uniConverter;

    @Test
    @DbUnitDataSet(before = "RemoteFulfillmentWarehouseMappingUploadUiServiceTest.before.csv")
    void testGetUploadInfos() {
        final List<UiWarehouseMappingFileUploadInfo> fileUploadInfos = service.getFileUploadInfos(586244343L, 100);

        Assertions.assertEquals(1, fileUploadInfos.size());

        final UiWarehouseMappingFileUploadInfo info = fileUploadInfos.get(0);
        Assertions.assertEquals(300, info.getIntegerField(UiWarehouseMappingFileUploadInfo.WAREHOUSE_ID));
        Assertions.assertEquals("mappings.xlsx", info.getStringField(UIFileUploadInfo.ORIGINAL_FILE_NAME));
        Assertions.assertEquals("2021-08-11T14:43:53", info.getDateField(UIFileUploadInfo.UPLOAD_DATE).toInstant()
                .atZone(Mbi.DEFAULT_TIME_ZONE).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

}
