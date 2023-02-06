package ru.yandex.market.psku.postprocessor.service.newvalue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuHypothesisUpdateType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuParamHypothesisUpdate;
import ru.yandex.market.psku.postprocessor.service.newvalue.pojo.YtNewVendorInfo;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class RawVendorForUpdateFinderServiceTest extends NewValuesForUpdateFinderServiceTest {

    private final static String NEW_VENDOR = "new vendor";
    private final static String NEW_IS_NOT_GURU_VENDOR = "new is not guru vendor";
    private final static String RAW_VENDOR_NAME = "raw vendor";

    private RawVendorForUpdateFinderService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        MboParameters.Option.Builder option1 = MboParameters.Option.newBuilder()
                .setId(ID_1)
                .addName(MboParameters.Word.newBuilder().setName(NEW_VENDOR))
                .setIsGuruVendor(true);
        MboParameters.Option.Builder option2 = MboParameters.Option.newBuilder()
                .setId(ID_2)
                .addName(MboParameters.Word.newBuilder().setName(NEW_IS_NOT_GURU_VENDOR));

        MboParameters.Parameter.Builder parameter = MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.VENDOR_ID)
                .setXslName(VENDOR)
                .addOption(option1).addOption(option2);
        MboParameters.Category.Builder category = MboParameters.Category.newBuilder()
                .addParameter(parameter);
        CategoryData categoryData = CategoryData.build(category);

        when(categoryDataHelper.getCategoryData(CATEGORY_ID)).thenReturn(categoryData);
    }

    @Test
    public void whenRawVendorForUpdateIsFound() {
        when(ytNewValueService.getNewVendorValuesInfo())
                .thenReturn(getNewValueMap(NEW_VENDOR));

        service = new RawVendorForUpdateFinderService(ytNewValueService, categoryDataHelper, dao);

        service.findValuesForUpdate();

        List<PskuParamHypothesisUpdate> result = dao.fetchByValue(NEW_VENDOR);
        assertThat(result).hasSize(1);
        checkResult(result.get(0), ParameterValueComposer.VENDOR_ID, ID_1, NEW_VENDOR, PskuHypothesisUpdateType.VENDOR);
    }

    @Test
    public void whenRawVendorIsNotGuru() {
        when(ytNewValueService.getNewVendorValuesInfo())
                .thenReturn(getNewValueMap(NEW_IS_NOT_GURU_VENDOR));

        service = new RawVendorForUpdateFinderService(ytNewValueService, categoryDataHelper, dao);

        service.findValuesForUpdate();

        List<PskuParamHypothesisUpdate> result = dao.fetchByValue(NEW_IS_NOT_GURU_VENDOR);
        assertThat(result).hasSize(1);
        checkResult(result.get(0), ParameterValueComposer.VENDOR_ID, ID_2, NEW_IS_NOT_GURU_VENDOR, PskuHypothesisUpdateType.VENDOR);
    }

    @Test
    public void whenRawVendorForUpdateIsNotFound() {
        when(ytNewValueService.getNewVendorValuesInfo())
                .thenReturn(getNewValueMap(RAW_VENDOR_NAME));

        service = new RawVendorForUpdateFinderService(ytNewValueService, categoryDataHelper, dao);

        service.findValuesForUpdate();

        List<PskuParamHypothesisUpdate> result = dao.fetchByValue(RAW_VENDOR_NAME);
        assertThat(result).isEmpty();
    }

    private Map<Long, Set<YtNewVendorInfo>> getNewValueMap(String value) {
        List<Long> pskuList = Collections.singletonList(PSKU_ID);
        YtNewVendorInfo newValueForUpdate = new YtNewVendorInfo(
                CATEGORY_ID, value, pskuList.size(), pskuList);
        Map<Long, Set<YtNewVendorInfo>> newValuesByCategory = new HashMap<>();
        newValuesByCategory.put(CATEGORY_ID, Collections.singleton(newValueForUpdate));

        return newValuesByCategory;
    }
}
