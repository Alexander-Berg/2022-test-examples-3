package ru.yandex.market.markup2.tasks.fill_param_by_vendor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesIdentity;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesResponse;
import ru.yandex.market.markup2.tasks.fill_param_values_metric.etalon.EtalonParamValuesDataItemPayload;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.workflow.general.ITaskDataItemPayload;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;

import java.io.IOException;
import java.util.Collections;

public class EtalonParamValuesVendorProtoGeneratorTest {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(
            new SimpleModule()
                .addDeserializer(
                    FillParamValuesIdentity.class,
                    new JsonUtils.DefaultJsonDeserializer<>(FillParamValuesIdentity.class)
                )
                .addDeserializer(
                    EtalonParamValuesDataItemPayload.class,
                    new JsonUtils.DefaultJsonDeserializer<>(EtalonParamValuesDataItemPayload.class)
                )
                .addDeserializer(
                    FillParamValuesResponse.class,
                    new JsonUtils.DefaultJsonDeserializer<>(FillParamValuesResponse.class)
                )
        );

    @Test
    public void generate() throws IOException {
        testGenerate(
            "etalon_param_values_vendor_proto_generator/payload.json",
            "etalon_param_values_vendor_proto_generator/response.json",
            "etalon_param_values_vendor_proto_generator/result.json"
        );
    }

    @Test
    public void generateOldFormat() throws IOException {
        testGenerate(
            "etalon_param_values_vendor_proto_generator/payload_old.json",
            "etalon_param_values_vendor_proto_generator/response_old.json",
            "etalon_param_values_vendor_proto_generator/result.json"
        );
    }

    @Test
    public void generateMultiValue() throws IOException {
        testGenerate(
            "etalon_param_values_vendor_proto_generator/payload_multi.json",
            "etalon_param_values_vendor_proto_generator/response_multi.json",
            "etalon_param_values_vendor_proto_generator/result_multi.json"
        );
    }

    protected void testGenerate(String payloadResource, String responseResource, String resultResource)
        throws IOException {
        final Markup.VendorParametersChecksTaskResponse response =
            new EtalonParamValuesVendorProtoGenerator().generate(
                Collections.singletonList(
                    readParseDataItem(
                        getResourceContent(payloadResource),
                        getResourceContent(responseResource)
                    )
                )
            );
        Assert.assertEquals(
            getProtoResourceContent(
                resultResource,
                Markup.VendorParametersChecksTaskResponse.newBuilder()
            ),
            response
        );
    }

    private static TaskDataItem<ITaskDataItemPayload<FillParamValuesIdentity>,
        FillParamValuesResponse> readParseDataItem(String payload, String response) throws IOException {
        final EtalonParamValuesDataItemPayload dataItemPayload =
            MAPPER.readValue(payload, EtalonParamValuesDataItemPayload.class);
        final FillParamValuesResponse paramValuesResponse =
            MAPPER.readValue(response, FillParamValuesResponse.class);
        return new TaskDataItem<>(
            1, 0, TaskDataItemState.SUCCESSFULLY_PROCEEDED,
            dataItemPayload,
            paramValuesResponse
        );
    }

    private static String getResourceContent(String resourceName) throws IOException {
        return Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
    }

    private static Message getProtoResourceContent(String resourceName, Message.Builder builder) throws IOException {
        JsonFormat.merge(getResourceContent(resourceName), builder);
        return builder.build();
    }
}
