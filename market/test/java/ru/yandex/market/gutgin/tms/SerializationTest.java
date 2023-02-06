package ru.yandex.market.gutgin.tms;


import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Data;
import ru.yandex.market.partner.content.common.engine.pipeline.PipelineData;

public class SerializationTest {
    private static final String JSON = "{\n" +
            "  \"@class\": \"ru.yandex.market.partner.content.common.engine.pipeline.PipelineData\",\n" +
            "  \"source\": {\n" +
            "    \"@c\": \".RequestProcessSimpleExcelData\",\n" +
            "    \"requestId\": 1\n" +
            "  },\n" +
            "  \"finished\": false,\n" +
            "  \"stepsData\": [\n" +
            "    {\n" +
            "      \"@c\": \"ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.start.Data\",\n" +
            "      \"out\": {\n" +
            "        \"@c\": \".RequestProcessSimpleExcelData\",\n" +
            "        \"requestId\": 1\n" +
            "      },\n" +
            "      \"finished\": true\n" +
            "    },\n" +
            "    {\n" +
            "      \"@c\": \"ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Data\",\n" +
            "      \"out\": {\n" +
            "        \"@c\": \".ProcessSimpleExcelData\",\n" +
            "        \"processId\": 1,\n" +
            "        \"requestId\": 1\n" +
            "      },\n" +
            "      \"taskId\": 1,\n" +
            "      \"finished\": true,\n" +
            "      \"checkTimes\": [\n" +
            "        1545147364901,\n" +
            "        1545147379843\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"@c\": \"ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Data\",\n" +
            "      \"out\": {\n" +
            "        \"@c\": \".ProcessSimpleExcelData\",\n" +
            "        \"processId\": 1,\n" +
            "        \"requestId\": 1\n" +
            "      },\n" +
            "      \"taskId\": 2,\n" +
            "      \"finished\": true,\n" +
            "      \"checkTimes\": [\n" +
            "        1545147379847,\n" +
            "        1545147394887\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"@c\": \"ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Data\",\n" +
            "      \"out\": {\n" +
            "        \"@c\": \".CreateDataBucketSimpleExcelData\",\n" +
            "        \"valid\": true,\n" +
            "        \"processId\": 1,\n" +
            "        \"requestId\": 1,\n" +
            "        \"dataBucketIds\": [\n" +
            "          1\n" +
            "        ]\n" +
            "      },\n" +
            "      \"taskId\": 3,\n" +
            "      \"finished\": true,\n" +
            "      \"checkTimes\": [\n" +
            "        1545147394890,\n" +
            "        1545147410204,\n" +
            "        1545147510812,\n" +
            "        1545147611946,\n" +
            "        1545147710107\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"@c\": \"ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.ifthenelse.Data\",\n" +
            "      \"subData\": {\n" +
            "        \"@class\": \"ru.yandex.market.partner.content.common.engine.pipeline.PipelineData\",\n" +
            "        \"source\": {\n" +
            "          \"@c\": \".CreateDataBucketSimpleExcelData\",\n" +
            "          \"valid\": true,\n" +
            "          \"processId\": 1,\n" +
            "          \"requestId\": 1,\n" +
            "          \"dataBucketIds\": [\n" +
            "            1\n" +
            "          ]\n" +
            "        },\n" +
            "        \"finished\": false,\n" +
            "        \"stepsData\": [\n" +
            "          {\n" +
            "            \"@c\": \"ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.start.Data\",\n" +
            "            \"out\": {\n" +
            "              \"@c\": \".CreateDataBucketSimpleExcelData\",\n" +
            "              \"valid\": true,\n" +
            "              \"processId\": 1,\n" +
            "              \"requestId\": 1,\n" +
            "              \"dataBucketIds\": [\n" +
            "                1\n" +
            "              ]\n" +
            "            },\n" +
            "            \"finished\": true\n" +
            "          },\n" +
            "          {\n" +
            "            \"@c\": \"ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Data\",\n" +
            "            \"out\": {\n" +
            "              \"@c\": \".CreateDataBucketSimpleExcelData\",\n" +
            "              \"valid\": true,\n" +
            "              \"processId\": 1,\n" +
            "              \"requestId\": 1,\n" +
            "              \"dataBucketIds\": [\n" +
            "                1\n" +
            "              ]\n" +
            "            },\n" +
            "            \"taskId\": 4,\n" +
            "            \"finished\": true,\n" +
            "            \"checkTimes\": [\n" +
            "              1545147410111,\n" +
            "              1545147425105\n" +
            "            ]\n" +
            "          },\n" +
            "          {\n" +
            "            \"@c\": \"ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.paralleldata.Data\",\n" +
            "            \"data\": [\n" +
            "              {\n" +
            "                \"@class\": \"ru.yandex.market.partner.content.common.engine.pipeline.PipelineData\",\n" +
            "                \"source\": {\n" +
            "                  \"@c\": \".ProcessDataBucketData\",\n" +
            "                  \"dataBucketId\": 1\n" +
            "                },\n" +
            "                \"finished\": false,\n" +
            "                \"stepsData\": [\n" +
            "                  {\n" +
            "                    \"@c\": \"ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.start.Data\",\n" +
            "                    \"out\": {\n" +
            "                      \"@c\": \".ProcessDataBucketData\",\n" +
            "                      \"dataBucketId\": 1\n" +
            "                    },\n" +
            "                    \"finished\": true\n" +
            "                  },\n" +
            "                  {\n" +
            "                    \"@c\": \"ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Data\"," +
            "\n" +
            "                    \"taskId\": 5,\n" +
            "                    \"finished\": false,\n" +
            "                    \"checkTimes\": [\n" +
            "                      1545147425109\n" +
            "                    ]\n" +
            "                  }\n" +
            "                ]\n" +
            "              }\n" +
            "            ],\n" +
            "            \"finished\": false\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"finished\": false,\n" +
            "      \"predicateResult\": true\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    private static final String STEP_DATA_JSON_NO_CHECK_COUNT = "{\n" +
            "  \"@c\": \"ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Data\",\n" +
            "  \"out\": {\n" +
            "    \"@c\": \".CreateDataBucketSimpleExcelData\",\n" +
            "    \"valid\": true,\n" +
            "    \"processId\": 1,\n" +
            "    \"requestId\": 1,\n" +
            "    \"dataBucketIds\": [\n" +
            "      1\n" +
            "    ]\n" +
            "  },\n" +
            "  \"taskId\": 3,\n" +
            "  \"finished\": true,\n" +
            "  \"checkTimes\": [\n" +
            "    1545147394890,\n" +
            "    1545147410204,\n" +
            "    1545147510812,\n" +
            "    1545147611946,\n" +
            "    1545147710107\n" +
            "  ]\n" +
            "}";
    private static final String STEP_DATA_JSON_WITH_CHECK_COUNT = "{\"@c\":\".Data\",\"out\":{\"@c\":\"" +
            ".CreateDataBucketSimpleExcelData\",\"requestId\":1,\"processId\":1,\"valid\":true," +
            "\"dataBucketIds\":[1]}," +
            "\"finished\":true,\"checkTimes\":[1545147394890,1545147710107],\"checkCount\":5,\"taskId\":3}";

    private static final ObjectMapper OBJECT_MAPPER = getObjectMapper();

    private static ObjectMapper getObjectMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Test
    public void whenUpdateJacksonThenFailDeserialization() throws IOException {
        try {
            PipelineData pipelineData = OBJECT_MAPPER.readerFor(PipelineData.class).readValue(JSON);
            System.out.println(pipelineData);
        } catch (JsonMappingException e) {
            Assert.fail("Не поддерживаемая версия Jackson.");
        }
    }

    @Test
    public void checkTimesAndCountSerializationTest() throws IOException {
        Data processStepData = OBJECT_MAPPER.readerFor(Data.class).readValue(STEP_DATA_JSON_NO_CHECK_COUNT);
        String json = OBJECT_MAPPER.writerFor(Data.class).writeValueAsString(processStepData);
        Assert.assertEquals(STEP_DATA_JSON_WITH_CHECK_COUNT, json);
    }
}
