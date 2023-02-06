package ru.yandex.market.mboc.common.repo.bindings;

import java.io.IOException;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.repo.bindings.pojos.MskuCargoParameter;
import ru.yandex.market.mboc.common.repo.bindings.pojos.MskuParameters;

public class AbstractJsonbToObjectBindingTest {

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testObjectMapper() throws IOException {
        MskuParameters original = new MskuParameters()
            .setCargoParameters(ImmutableMap.of(1L, new MskuCargoParameter(1L, "uno", 10L),
                2L, new MskuCargoParameter(2L, "dos", 20L)));
        String write = AbstractJsonbToObjectBinding.DEFAULT_OBJECT_MAPPER.writeValueAsString(original);
        Assertions.assertThat(write)
            .isEqualTo("{\"cargoParameters\":{" +
                "\"1\":{" +
                "\"xslName\":\"uno\",\"paramId\":1,\"booleanValue\":true,\"valueType\":\"BOOLEAN\",\"lmsId\":10" +
                "}," +
                "\"2\":{" +
                "\"xslName\":\"dos\",\"paramId\":2,\"booleanValue\":true,\"valueType\":\"BOOLEAN\",\"lmsId\":20" +
                "}" +
                "}}");
        MskuParameters read = AbstractJsonbToObjectBinding.DEFAULT_OBJECT_MAPPER.readValue(write, MskuParameters.class);
        Assertions.assertThat(read).isEqualTo(original);
    }
}
