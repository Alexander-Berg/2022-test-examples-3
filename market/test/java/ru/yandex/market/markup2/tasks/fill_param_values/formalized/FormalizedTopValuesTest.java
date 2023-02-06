package ru.yandex.market.markup2.tasks.fill_param_values.formalized;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.tasks.fill_param_values.ParameterValuesStats;
import ru.yandex.market.markup2.utils.Markup2TestUtils;
import ru.yandex.market.markup2.utils.YqlDao;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.mbo.export.MboParameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.markup2.utils.ParameterTestUtils.createOption;
import static ru.yandex.market.markup2.utils.ParameterTestUtils.createParameter;
import static ru.yandex.market.markup2.utils.ParameterTestUtils.createParameterBuilder;

/**
 * @author V.Zaytsev
 * @since 13.07.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class FormalizedTopValuesTest {

    private static final int CATEGORY_ID = 42;

    private static final long MODEL_ID1 = 1;
    private static final long MODEL_ID2 = 2;

    private static final long OPTION_ID1 = 1;
    private static final long OPTION_ID2 = 2;
    private static final long OPTION_ID3 = 3;
    private static final long OPTION_ID4 = 4;

    private static final String OFFER_URL = "http://ya.ru";

    private static final Set<Long> MODEL_IDS = ImmutableSet.of(MODEL_ID1, MODEL_ID2);

    private static final MboParameters.Parameter PARAMETER1 =
        createParameter(1, MboParameters.ValueType.ENUM, createOption(OPTION_ID1, "o1"));
    private static final MboParameters.Parameter PARAMETER2 =
        createParameter(2, MboParameters.ValueType.NUMERIC_ENUM, createOption(OPTION_ID2, "o2"));
    private static final MboParameters.Parameter PARAMETER3 =
        createParameter(3, MboParameters.ValueType.BOOLEAN,
            createOption(OPTION_ID3, "TRUE"), createOption(OPTION_ID4, "FALSE"));
    private static final MboParameters.Parameter PARAMETER4 =
        createParameterBuilder(4, MboParameters.ValueType.NUMERIC, "p4")
            .setPrecision(3)
            .build();

    private static final List<MboParameters.Parameter> PARAMETERS =
        Arrays.asList(PARAMETER1, PARAMETER2, PARAMETER3, PARAMETER4);

    private YqlDao yqlDao;
    private FormalizedValuesService formalizedValuesService = new FormalizedValuesService();

    private Map<Long, ParameterValuesStats> valuesStats = new HashMap<>();

    @Before
    public void setup() {
        yqlDao = mock(YqlDao.class);
        formalizedValuesService.setYqlDao(yqlDao);
    }

    @Test
    public void noFormalizedValues() {
        mockYqlDao(yqlDao, Collections.emptyMap());
        FormalizedTopValues values =
            formalizedValuesService.getFormalizedTopValues(CATEGORY_ID, MODEL_IDS, PARAMETERS);

        for (long modelId : MODEL_IDS) {
            for (MboParameters.Parameter parameter : PARAMETERS) {
                FormalizedTopValues.Value value = values.getValue(modelId, parameter.getId());
                Assert.assertNull(value);
            }
        }
    }

    @Test
    public void oneFormalizedValue() {
        double numberValue = 2.352;
        ParameterValuesStats values = new ParameterValuesStats(MODEL_ID1);
        values.increment(PARAMETER1.getId(), OPTION_ID1, 0, OFFER_URL);
        values.increment(PARAMETER2.getId(), OPTION_ID2, 0, OFFER_URL);
        values.increment(PARAMETER3.getId(), 0, 0, OFFER_URL);
        values.increment(PARAMETER4.getId(), -1, numberValue, OFFER_URL);

        mockYqlDao(yqlDao, ImmutableMap.of(MODEL_ID1, values));

        FormalizedTopValues formalizedTopValues =
            formalizedValuesService.getFormalizedTopValues(CATEGORY_ID, MODEL_IDS, PARAMETERS);

        assertEnumValue(formalizedTopValues, MODEL_ID1, PARAMETER1, OPTION_ID1);
        assertEnumValue(formalizedTopValues, MODEL_ID1, PARAMETER2, OPTION_ID2);
        assertValue(formalizedTopValues, MODEL_ID1, PARAMETER3, ParamUtils.BOOLEAN_FALSE_VALUE_NAME);
        assertValue(formalizedTopValues, MODEL_ID1, PARAMETER4, "2.352");
    }

    @Test
    public void valuesInSameOffersCount() {
        ParameterValuesStats values = new ParameterValuesStats(MODEL_ID1);
        values.increment(PARAMETER3.getId(), 1, 0, OFFER_URL);
        values.increment(PARAMETER3.getId(), 0, 0, OFFER_URL);

        mockYqlDao(yqlDao, ImmutableMap.of(MODEL_ID1, values));

        FormalizedTopValues formalizedTopValues =
            formalizedValuesService.getFormalizedTopValues(CATEGORY_ID, MODEL_IDS, PARAMETERS);

        FormalizedTopValues.Value value = formalizedTopValues.getValue(MODEL_ID1, PARAMETER3.getId());
        Assert.assertNull(value);
    }

    @Test
    public void valuesInSameOffersCountAndOtherDifferent() {
        ParameterValuesStats values = new ParameterValuesStats(MODEL_ID1);
        values.increment(PARAMETER4.getId(), -1, 1, OFFER_URL);
        values.increment(PARAMETER4.getId(), -1, 1, OFFER_URL);
        values.increment(PARAMETER4.getId(), -1, 2, OFFER_URL);
        values.increment(PARAMETER4.getId(), -1, 2, OFFER_URL);
        values.increment(PARAMETER4.getId(), -1, 3, OFFER_URL);

        mockYqlDao(yqlDao, ImmutableMap.of(MODEL_ID1, values));

        FormalizedTopValues formalizedTopValues =
            formalizedValuesService.getFormalizedTopValues(CATEGORY_ID, MODEL_IDS, PARAMETERS);

        FormalizedTopValues.Value value = formalizedTopValues.getValue(MODEL_ID1, PARAMETER3.getId());
        Assert.assertNull(value);
    }

    @Test
    public void realValuesTest() throws IOException {
        Long modelId = 1721684114L;
        String valuesCsv = Markup2TestUtils.getResource("tasks/fill_param_values/formalized/values.csv");
        ParameterValuesStats values = new ParameterValuesStats(modelId);
        for (String valueCsv : valuesCsv.split("\n")) {
            String[] tokens = valueCsv.split(",");
            String paramId = tokens[1];
            String valueId = tokens[2];
            String numericValue = tokens[3];
            values.increment(Long.valueOf(paramId), Long.valueOf(valueId), Double.valueOf(numericValue), OFFER_URL);

        }

        mockYqlDao(yqlDao, ImmutableMap.of(modelId, values));

        Map<Long, MboParameters.Parameter> params = new HashMap<>();
        for (Map.Entry<Long, ParameterValuesStats.ValueStats>  stat : values.getValuesStats().entrySet()) {
            Long paramId = stat.getKey();
            MboParameters.Parameter.Builder param = null;
            for (ParameterValuesStats.ValueInfo info : stat.getValue().getValuesInfo()) {
                if (param == null) {
                    if (info.getValueId() <= 0) {
                        param =  createParameterBuilder(
                            paramId, MboParameters.ValueType.NUMERIC, String.valueOf(paramId));
                    } else {
                        param =  createParameterBuilder(
                            paramId, MboParameters.ValueType.ENUM, String.valueOf(paramId));
                    }
                }
                switch (param.getValueType()) {
                    case NUMERIC:
                        param.setPrecision(3);
                    case ENUM:
                        param.addOption(createOption(info.getValueId(), String.valueOf(info.getValueId())));
                    default:
                }
            }
            params.put(param.getId(), param.build());
        }
        FormalizedTopValues formalizedTopValues =
            formalizedValuesService.getFormalizedTopValues(
                CATEGORY_ID, Collections.singletonList(modelId), params.values());

        assertEquals(11, formalizedTopValues.getValues(modelId).size());
        assertEnumValue(formalizedTopValues, modelId, params.get(7893318L), 12220999L);
        assertValue(formalizedTopValues, modelId, params.get(10732698L), "0.30");
        assertValue(formalizedTopValues, modelId, params.get(10732699L), "28.00");
        assertEnumValue(formalizedTopValues, modelId, params.get(13887626L), 13899071L);
        assertValue(formalizedTopValues, modelId, params.get(14811326L), "11.00");
        assertValue(formalizedTopValues, modelId, params.get(14811404L), "250.00");
        assertEnumValue(formalizedTopValues, modelId, params.get(14879205L), 14879208L);
        assertEnumValue(formalizedTopValues, modelId, params.get(14994076L), 1L);
        assertValue(formalizedTopValues, modelId, params.get(14994157L), "10.00");
    }

    private static void mockYqlDao(YqlDao mock, Map<Long, ParameterValuesStats> valuesStats) {
        when(mock.getParamValuesStats(anyInt(), anyCollection())).thenReturn(valuesStats);
    }

    private static void assertEnumValue(FormalizedTopValues values, long modelId,
                                        MboParameters.Parameter parameter, long optionId) {
        assertValue(values, modelId, parameter, ParamUtils.getOptionName(parameter.getOptionList(), optionId));
    }

    private static void assertValue(FormalizedTopValues values, long modelId,
                                    MboParameters.Parameter parameter, String stringValue) {
        FormalizedTopValues.Value value = values.getValue(modelId, parameter.getId());
        Assert.assertNotNull(value);
        assertEquals(parameter.getId(), value.getParamId());
        assertEquals(stringValue, value.getValue());
        assertEquals(OFFER_URL, value.getOfferUrl());
    }
}
