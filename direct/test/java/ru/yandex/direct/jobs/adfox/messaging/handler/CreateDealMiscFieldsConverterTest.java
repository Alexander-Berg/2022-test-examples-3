package ru.yandex.direct.jobs.adfox.messaging.handler;

import java.util.Map;

import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.adfoxmessaging.protos.AdfoxDealCreatePayload;
import ru.yandex.direct.utils.JsonUtils;

import static com.google.common.primitives.Longs.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Не работающая фича 'частные сделки'")
class CreateDealMiscFieldsConverterTest {

    @Test
    void extractMiscParameters_success() {
        Struct adfoxSpecials = Struct.newBuilder()
                .putFields("numberValue", Value.newBuilder().setNumberValue(1.1).build())
                .putFields("stringField", Value.newBuilder().setStringValue("stringValue").build())
                .putFields("listOfStruct", Value.newBuilder().setListValue(
                                ListValue.newBuilder().addValues(
                                                Value.newBuilder()
                                                        .setStructValue(Struct.newBuilder()
                                                                .putFields("structFieldKey",
                                                                        Value.newBuilder().setStringValue(
                                                                                "structFieldValue").build())
                                                                .build())
                                                        .build())
                                        .build())
                        .build())
                .build();

        AdfoxDealCreatePayload.Placement placement = AdfoxDealCreatePayload.Placement.newBuilder()
                .setPageId(101)
                .addAllImpId(asList(201L, 202L, 203L))
                .build();

        AdfoxDealCreatePayload createPayload = AdfoxDealCreatePayload.newBuilder()
                .addPlacements(placement)
                .setAdfoxSpecials(adfoxSpecials)
                .build();

        Map<String, Object> actual = CreateDealMiscFieldsConverter.extractMiscParameters(createPayload);
        assertThat(JsonUtils.toJson(actual)).isEqualTo(
                "{\"adfoxSpecials\":{\"listOfStruct\":[{\"structFieldKey\":\"structFieldValue\"}],\"numberValue\":1" +
                        ".1,\"stringField\":\"stringValue\"}}");
    }

    @Test
    void extractMiscParameters_success_whenNoParameters() {
        AdfoxDealCreatePayload emptyPayload = AdfoxDealCreatePayload.newBuilder().build();

        Map<String, Object> actual = CreateDealMiscFieldsConverter.extractMiscParameters(emptyPayload);

        assertThat(JsonUtils.toJson(actual)).isEqualTo("{}");
    }

}
