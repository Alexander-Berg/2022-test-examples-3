package ru.yandex.market.mbo.gwt.utils;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class ParametersInheritanceUtilTest {

    private ParameterValue param1p;
    private ParameterValue param2p;
    private ParameterValue param3p;
    private ParameterValue param4p;
    private ParameterValue param5p;
    private ParameterValue param1c;
    private ParameterValue param2c;
    private ParameterValue param3c;
    private ParameterValueHypothesis hypo1p;
    private ParameterValueHypothesis hypo2p;
    private ParameterValueHypothesis hypo1c;

    @Before
    public void setup() {
        param1p = new ParameterValue(1L, "1L", Param.Type.STRING);
        param1p.setStringValue(Arrays.asList(
            WordUtil.defaultWord("strVal1"),
            WordUtil.defaultWord("strVal2")
        ));
        param2p = new ParameterValue(2L, "2L", Param.Type.STRING);
        param2p.setStringValue(Arrays.asList(
            WordUtil.defaultWord("strVal3"),
            WordUtil.defaultWord("strVal4")
        ));
        param3p = new ParameterValue(3L, "3L", Param.Type.STRING);
        param3p.setStringValue(Collections.singletonList(
            WordUtil.defaultWord("strVal5")
        ));
        param4p = new ParameterValue(7L, "7L", Param.Type.STRING);
        param4p.setStringValue(Collections.singletonList(
            WordUtil.defaultWord("strVal51")
        ));
        param5p = new ParameterValue(7L, "7L", Param.Type.STRING);
        param5p.setStringValue(Collections.singletonList(
            WordUtil.defaultWord("strVal52")
        ));

        param1c = new ParameterValue(1L, "1L", Param.Type.STRING);
        param1c.setStringValue(Arrays.asList(
            WordUtil.defaultWord("strVal6"),
            WordUtil.defaultWord("strVal7")
        ));
        param2c = new ParameterValue(4L, "4L", Param.Type.STRING);
        param2c.setStringValue(Arrays.asList(
            WordUtil.defaultWord("strVal8"),
            WordUtil.defaultWord("strVal4")
        ));
        param3c = new ParameterValue(1L, "1L", Param.Type.STRING);
        param3c.setStringValue(Collections.singletonList(
            WordUtil.defaultWord("strVal9")
        ));

        hypo1p = new ParameterValueHypothesis(4L, "4L", Param.Type.STRING, Collections.singletonList(
            WordUtil.defaultWord("hypo1")
        ), null);
        hypo2p = new ParameterValueHypothesis(5L, "5L", Param.Type.STRING, Collections.singletonList(
            WordUtil.defaultWord("hypo2")
        ), null);
        hypo1c = new ParameterValueHypothesis(3L, "1L", Param.Type.STRING, Collections.singletonList(
            WordUtil.defaultWord("hypo3")
        ), null);
    }

    @Test
    public void paramAndHypoInheritance() {
        CommonModel parent = new CommonModel();
        parent.addParameterValue(param1p);
        parent.addParameterValue(param2p);
        parent.addParameterValue(param3p);
        parent.addParameterValue(param4p);
        parent.addParameterValue(param5p);
        parent.putParameterValueHypothesis(hypo1p);
        parent.putParameterValueHypothesis(hypo2p);

        CommonModel child = new CommonModel();
        child.setParentModel(parent);
        child.addParameterValue(param1c);
        child.addParameterValue(param2c);
        child.addParameterValue(param3c);
        child.putParameterValueHypothesis(hypo1c);

        ParametersInheritanceUtil.inheritParametersAndHypotheses(child);

        ParameterValues pvs1 = new ParameterValues(param1c);
        pvs1.addValue(param3c);

        ParameterValues pvs2 = new ParameterValues(param4p);
        pvs2.addValue(param5p);

        assertThat(child.getParameterValues()).containsExactlyInAnyOrder(
            pvs1, pvs2, new ParameterValues(param2p), new ParameterValues(param2c));

        assertThat(child.getParameterValueHypotheses()).containsExactlyInAnyOrder(hypo2p, hypo1c);
    }
}
