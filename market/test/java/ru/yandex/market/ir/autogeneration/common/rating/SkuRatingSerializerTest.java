package ru.yandex.market.ir.autogeneration.common.rating;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.ir.autogeneration.common.rating.operation.Operation;

import static ru.yandex.market.ir.autogeneration.common.rating.SkuRatingTestUtils.PARAM_ID_1;
import static ru.yandex.market.ir.autogeneration.common.rating.SkuRatingTestUtils.PARAM_ID_2;
import static ru.yandex.market.ir.autogeneration.common.rating.SkuRatingTestUtils.PARAM_ID_3;
import static ru.yandex.market.ir.autogeneration.common.rating.SkuRatingTestUtils.PARAM_ID_IMPORTANT_1;
import static ru.yandex.market.ir.autogeneration.common.rating.SkuRatingTestUtils.PARAM_ID_IMPORTANT_2;

public class SkuRatingSerializerTest {

    private final SkuRatingFormula formula = SkuRatingTestUtils.buildFormula(0L,
            Set.of(
                    PARAM_ID_IMPORTANT_1,
                    PARAM_ID_IMPORTANT_2,
                    PARAM_ID_1,
                    PARAM_ID_2,
                    PARAM_ID_3
            )
    );

    @Test
    public void testSerialization() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(SkuRatingEvaluatorTest.class.getResourceAsStream("/sku_rating_formula_example.json"),
                writer, "utf-8");

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new SimpleModule()
                        .addSerializer(Operation.class, new OperationSerializer())
                        .addSerializer(SkuRatingFormula.class, new SkuRatingFormulaSerializer()));

        String serializedFormulaExpected = writer.toString().replaceAll("\\s+", "");
        String serializedFormula = mapper.writeValueAsString(formula).replaceAll("\\s+", "");

        Assert.assertEquals(serializedFormula, serializedFormulaExpected);
    }
}
