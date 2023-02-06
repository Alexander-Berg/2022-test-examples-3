package ru.yandex.mbo.tool.jira.MBO29741;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.db.params.CategoryParameterValuesService;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;

@SuppressWarnings("checkstyle:MagicNumber")
public class RemoveCategoryParamValuesToolTest {
    private RemoveCategoryParamValuesTool tool;
    private ArgumentCaptor<List<ParameterValues>> valueCapture;

    @Before
    public void setUp() {
        tool = new RemoveCategoryParamValuesTool();
    }

    @Test
    public void testRemoveParams() {
        List<ParameterValues> paramsBefore = buildParamValues(ImmutableMap.of(
            17495667L, 1L,
            14474379L, 2L,
            16452646L, 3L,
            16452645L, 4L));
        List<ParameterValues> expected = buildParamValues(ImmutableMap.of(
            17495667L, 1L,
            14474379L, 2L));
        CategoryParameterValuesService parameterValuesService = createMockParamValueService(paramsBefore);
        tool.processCategory(0, parameterValuesService, 0, true);
        var saved = valueCapture.getValue();

        Mockito.verify(parameterValuesService, Mockito.times(1))
            .saveCategoryParameterValues(Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
        Assertions.assertThat(saved).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testWhenNoParamsFoRemoveThenNoSave() {
        List<ParameterValues> paramsBefore = buildParamValues(ImmutableMap.of(
                17495667L, 1L,
                14474379L, 2L));
        CategoryParameterValuesService parameterValuesService = createMockParamValueService(paramsBefore);
        var exported = tool.processCategory(0, parameterValuesService, 0, true);

        Mockito.verify(parameterValuesService, Mockito.times(0))
                .saveCategoryParameterValues(Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
        Assertions.assertThat(exported).isEmpty();
    }

    @Test
    public void testExportParams() {
        List<ParameterValues> paramsBefore = buildParamValues(ImmutableMap.of(
                17495667L, 1L,
                14474379L, 2L,
                16452646L, 3L,
                16452645L, 4L));
        Map<Long, Long> expected = ImmutableMap.of(
                16452646L, 3L,
                16452645L, 4L);
        CategoryParameterValuesService parameterValuesService = createMockParamValueService(paramsBefore);
        var exported = tool.processCategory(0, parameterValuesService, 0, true);

        Assertions.assertThat(exported).isEqualTo(expected);
    }

    private CategoryParameterValuesService createMockParamValueService(List<ParameterValues> params) {
        CategoryParameterValuesService res = Mockito.mock(CategoryParameterValuesService.class);
        Mockito.when(res.loadCategoryParameterValues(Mockito.anyLong())).thenReturn(params);
        valueCapture = ArgumentCaptor.forClass(List.class);
        Mockito.doNothing()
            .when(res)
            .saveCategoryParameterValues(Mockito.anyLong(), valueCapture.capture(), Mockito.anyLong());
        return res;
    }

    private List<ParameterValues> buildParamValues(Map<Long, Long> values) {
        return values.entrySet().stream()
            .map(e -> ParameterValues.of(
                new ParameterValue(e.getKey(), null, Param.Type.NUMERIC,
                    BigDecimal.valueOf(e.getValue()), null, null, null, null)
            ))
            .collect(Collectors.toList());
    }
}
