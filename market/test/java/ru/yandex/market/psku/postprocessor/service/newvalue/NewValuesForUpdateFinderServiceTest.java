package ru.yandex.market.psku.postprocessor.service.newvalue;

import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuParamHypothesisUpdateDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuHypothesisUpdateType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuParamHypothesisUpdate;
import ru.yandex.market.psku.postprocessor.service.yt.YtNewValueService;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class NewValuesForUpdateFinderServiceTest extends BaseDBTest {

    final static long CATEGORY_ID = 11L;
    final static long ID_1 = 1L;
    final static long ID_2 = 2L;
    final static String VENDOR = "vendor";
    final static long PSKU_ID = 5L;

    @Mock
    protected YtNewValueService ytNewValueService;
    @Mock
    protected CategoryDataHelper categoryDataHelper;
    @Autowired
    protected PskuParamHypothesisUpdateDao dao;

    void checkResult(PskuParamHypothesisUpdate value, long expectedParamId, long expectedOptionId,
                     String expectedValue, PskuHypothesisUpdateType expectedUpdateType) {
        assertThat(value.getPskuId()).isEqualTo(PSKU_ID);
        assertThat(value.getParamId()).isEqualTo(expectedParamId);
        assertThat(value.getValue()).isEqualTo(expectedValue);
        assertThat(value.getOptionId()).isEqualTo(expectedOptionId);
        assertThat(value.getExecuted()).isEqualTo(false);
        assertThat(value.getUpdateType()).isEqualTo(expectedUpdateType);
    }
}
