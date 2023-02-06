package ru.yandex.market.api.abtest;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by fettsery on 25.06.19.
 */
public class SingleExperimentParameterJsonParserTest extends UnitTestBase {
    @Test
    public void parseRearrFactors() {
        ExperimentParameter parameter = new SingleExperimentParameterJsonParser().parse(ResourceHelpers.getResource("rearr.json"));

        Collection<String> rearrFactors = parameter.getRearrFactors();
        Assert.assertEquals(3, rearrFactors.size());

        Iterator<String> iterator = rearrFactors.iterator();
        Assert.assertEquals("lowercase", iterator.next());
        Assert.assertEquals("UPPERCASE", iterator.next());
        Assert.assertEquals("MiXeD", iterator.next());
    }

    @Test
    public void parseAliasToLowerCase() {
        ExperimentParameter parameter = new SingleExperimentParameterJsonParser().parse(ResourceHelpers.getResource("rearr.json"));

        Assert.assertEquals("rearr_for_loyalty_test", parameter.getAliases().iterator().next());
    }

}
