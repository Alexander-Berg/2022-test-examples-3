package ru.yandex.market.admin.param;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.param.response.ParamEditInfo;
import ru.yandex.market.admin.param.response.ParamsForEdit;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.param.model.ParamValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для класса {@link EditParamsService}.
 */
class EditParamsServiceTest extends FunctionalTest {

    private static final int DATA_SOURCE_ID = 776;
    private static final int DATA_SOURCE_ID_CLICK_COLLECT = 777;
    private static final int DATA_SOURCE_ID_SUPPLIER = 778;
    private static final int ADMIN_COMMON_PARAMS_SIZE = 26;


    @Autowired
    private EditParamsService editParamsService;

    /**
     * Тест проверяет, что у клик и коллекта на выдаче есть срок хранения товаров на ПВЗ, а у простого поставщика нет.
     */
    @Test
    @DbUnitDataSet(before = "getParamsForEditTest.before.csv")
    void getParamsForEditClickCollect() {
        var clickCollect = editParamsService.getParamsForEdit(DATA_SOURCE_ID_CLICK_COLLECT);
        assertThat(getActualTypes(clickCollect)).contains(142);

        var supplier = editParamsService.getParamsForEdit(DATA_SOURCE_ID_SUPPLIER);
        assertThat(getActualTypes(supplier)).doesNotContain(142);
    }

    /**
     * Тест проверяет, что у не красного не CPA магазина на фронте нет доп редактируемых параметров
     */
    @Test
    @DbUnitDataSet(before = "getParamsForEditTest.before.csv")
    void getParamsForEditCPATest() {
        var actual = editParamsService.getParamsForEdit(DATA_SOURCE_ID);
        actual.getParamEditInfos().removeIf(pi -> "readonly".equals(pi.getEdit()));
        assertThat(actual.getParamEditInfos()).hasSize(ADMIN_COMMON_PARAMS_SIZE);

        var actualTypes = getActualTypes(actual);
        assertThat(actualTypes).doesNotContain(125);
        assertThat(actualTypes).doesNotContain(66);
    }


    private static List<Integer> getActualTypes(ParamsForEdit actual) {
        return actual.getParamEditInfos().stream()
                .map(ParamEditInfo::getParamValue)
                .map(ParamValue::getTypeId)
                .collect(Collectors.toList());
    }
}
