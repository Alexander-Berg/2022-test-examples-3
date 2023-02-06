package ru.yandex.calendar.logic.ics.iv5j.ical.parameter;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.XParameter;
import org.junit.Test;

import ru.yandex.calendar.logic.ics.iv5j.ical.meta.IcssMeta;
import ru.yandex.calendar.logic.ics.iv5j.ical.meta.IcssMetaTestSupport;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class ParametersMetaTest extends IcssMetaTestSupport<IcsParameter, Parameter> {

    @Override
    protected Class<Parameter> dataClass() {
        return Parameter.class;
    }

    @Override
    protected IcssMeta<IcsParameter, ?, Parameter, ?, ?> meta() {
        return ParametersMeta.M;
    }

    @Override
    protected Class<? extends Parameter> xDataClass() {
        return XParameter.class;
    }

    @Override
    protected String packageSuffix() {
        return "parameter";
    }

    @Test
    public void test() {
        Assert.assertTrue(ParametersMeta.M.newTheirParameter(Parameter.CN, "sdfsdf") instanceof Cn);
        Assert.assertTrue(ParametersMeta.M.newTheirParameter("SDFSFD", "sdfsdf") instanceof XParameter);
    }

} //~
