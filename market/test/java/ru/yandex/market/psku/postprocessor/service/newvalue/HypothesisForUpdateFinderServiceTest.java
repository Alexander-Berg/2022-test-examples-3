package ru.yandex.market.psku.postprocessor.service.newvalue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuHypothesisUpdateType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuParamHypothesisUpdate;
import ru.yandex.market.psku.postprocessor.service.newvalue.pojo.YtNewParamValueInfo;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


public class HypothesisForUpdateFinderServiceTest extends NewValuesForUpdateFinderServiceTest {

    private final static String NEW_VALUE = "new value";
    private final static String HYPOTHESIS = "hypothesis";
    private final static long PARAM_ID = 12L;

    private HypothesisForUpdateFinderService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        MboParameters.Option.Builder option1 = MboParameters.Option.newBuilder()
                .setId(ID_1)
                .addName(MboParameters.Word.newBuilder().setName(NEW_VALUE));
        MboParameters.Parameter.Builder parameter = MboParameters.Parameter.newBuilder()
                .setId(PARAM_ID)
                .addOption(option1);
        MboParameters.Parameter.Builder vendorParameter = MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.VENDOR_ID)
                .setXslName(VENDOR);
        MboParameters.Category.Builder category = MboParameters.Category.newBuilder()
                .addParameter(vendorParameter).addParameter(parameter);
        CategoryData categoryData = CategoryData.build(category);

        when(categoryDataHelper.getCategoryData(CATEGORY_ID)).thenReturn(categoryData);
    }

    @Test
    public void whenNewValueForUpdateIsFound() {
        when(ytNewValueService.getNewParamValuesInfo()).thenReturn(getNewValueMap(NEW_VALUE));

        service = new HypothesisForUpdateFinderService(ytNewValueService, categoryDataHelper, dao);

        service.findValuesForUpdate();

        List<PskuParamHypothesisUpdate> result = dao.fetchByValue(NEW_VALUE);
        assertThat(result).hasSize(1);
        checkResult(result.get(0), PARAM_ID, ID_1, NEW_VALUE, PskuHypothesisUpdateType.PARAMETER);
    }

    @Test
    public void whenNewValueForUpdateIsNotFound() {
        when(ytNewValueService.getNewParamValuesInfo()).thenReturn(getNewValueMap(HYPOTHESIS));

        service = new HypothesisForUpdateFinderService(ytNewValueService, categoryDataHelper, dao);

        service.findValuesForUpdate();

        List<PskuParamHypothesisUpdate> result = dao.fetchByValue(HYPOTHESIS);
        assertThat(result).isEmpty();
    }

    @Test
    public void whenParameterNotInCategory() {
        service = Mockito.mock(HypothesisForUpdateFinderService.class);
        when(service.getOptionId(Mockito.any(), Mockito.any())).thenCallRealMethod();

        YtNewParamValueInfo ytNewParamValueInfo = Mockito.mock(YtNewParamValueInfo.class);
        when(ytNewParamValueInfo.getParamId()).thenReturn(1L);

        final CategoryData categoryData = CategoryData.EMPTY;

        Long optionId = service.getOptionId(categoryData, ytNewParamValueInfo);

        assertThat(optionId).isNull();
    }

    private Map<Long, Set<YtNewParamValueInfo>> getNewValueMap(String value) {
        List<Long> pskuList = Collections.singletonList(PSKU_ID);
        YtNewParamValueInfo newValueForUpdate = new YtNewParamValueInfo(
                CATEGORY_ID, PARAM_ID, null, value, pskuList.size(), pskuList);
        Map<Long, Set<YtNewParamValueInfo>> newValuesByCategory = new HashMap<>();
        newValuesByCategory.put(CATEGORY_ID, Collections.singleton(newValueForUpdate));

        return newValuesByCategory;
    }
}
