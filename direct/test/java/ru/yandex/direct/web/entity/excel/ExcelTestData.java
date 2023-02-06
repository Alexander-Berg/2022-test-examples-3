package ru.yandex.direct.web.entity.excel;

import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.web.entity.excel.model.ExcelFileKey;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportMode;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportRequest;

@ParametersAreNonnullByDefault
public class ExcelTestData {

    private ExcelTestData() {
    }

    public static InternalAdImportRequest getDefaultInternalAdImportRequest() {
        return new InternalAdImportRequest()
                .withExcelFileKey(new ExcelFileKey()
                        .withFileName(RandomStringUtils.randomAlphanumeric(12))
                        .withMdsGroupId(RandomStringUtils.randomNumeric(7)))
                .withOnlyValidation(false)
                .withImportMode(InternalAdImportMode.AD_GROUPS_WITH_ADS)
                .withAdsImages(Collections.emptyList());
    }

}
