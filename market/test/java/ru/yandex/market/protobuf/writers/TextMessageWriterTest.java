package ru.yandex.market.protobuf.writers;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TextMessageWriterTest {

    @Test
    public void testSuccessfullyPrint() throws IOException {
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(1).setCategoryId(1).setVendorId(1)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setValue("Model title").setIsoCode("ru"))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(1).setValueType(MboParameters.ValueType.STRING).setXslName("xsl-name")
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setIsoCode("ru").setValue("value1"))
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setIsoCode("ru").setValue("value2")))
            .build();


        StringBuilder stringBuilder = new StringBuilder();
        TextMessageWriter<ModelStorage.Model> textMessageWriter = new TextMessageWriter<>(stringBuilder);

        textMessageWriter.writeMessage(model);
        textMessageWriter.writeOffset(1);
        textMessageWriter.writeSize(2);
        textMessageWriter.writeTotalSize(3);
        textMessageWriter.close();

        Assertions.assertThat(stringBuilder.toString())
            .contains("id: 1")
            .contains("category_id: 1")
            .contains("vendor_id: 1")
            .contains("value: \"Model title\"")
            .contains("param_id: 1")
            .contains("xsl_name: \"xsl-name\"")
            .contains("value_type: STRING")
            .contains("value: \"value1\"")
            .contains("value: \"value2\"")
            .contains("Offset: 1")
            .contains("Size: 2")
            .contains("Total size: 3");
    }
}
