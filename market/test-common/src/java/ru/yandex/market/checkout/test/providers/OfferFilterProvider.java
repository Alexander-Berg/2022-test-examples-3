package ru.yandex.market.checkout.test.providers;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ru.yandex.market.common.report.model.filter.Filter;

public abstract class OfferFilterProvider {

    public static final String DEFAULT_FILTER =
            "{\n" +
                    "            \"id\": \"25595695\",\n" +
                    "            \"type\": \"enum\",\n" +
                    "            \"name\": \"allowed\",\n" +
                    "            \"xslname\": \"vendor\",\n" +
                    "            \"subType\": \"\",\n" +
                    "            \"kind\": 1,\n" +
                    "            \"position\": 1,\n" +
                    "            \"noffers\": 1,\n" +
                    "            \"values\": [\n" +
                    "              {\n" +
                    "                \"initialFound\": 1,\n" +
                    "                \"found\": 1,\n" +
                    "                \"value\": \"Производитель\",\n" +
                    "                \"vendor\": {\n" +
                    "                  \"name\": \"Производитель\",\n" +
                    "                  \"entity\": \"vendor\",\n" +
                    "                  \"id\": 152863\n" +
                    "                },\n" +
                    "                \"id\": \"152863\"\n" +
                    "              },\n" +
                    "                   {\n" +
                    "                              \"initialFound\": 1,\n" +
                    "                              \"found\": 1,\n" +
                    "                              \"value\": \"Производитель\",\n" +
                    "                              \"vendor\": {\n" +
                    "                                \"name\": \"Производитель\",\n" +
                    "                                \"entity\": \"vendor\",\n" +
                    "                                \"id\": 1528653353\n" +
                    "                              },\n" +
                    "                              \"id\": \"153552863\"\n" +
                    "                            }\n" +
                    "            ],\n" +
                    "            \"valuesGroups\": [\n" +
                    "              {\n" +
                    "                \"type\": \"all\",\n" +
                    "                \"valuesIds\": [\n" +
                    "                  \"152863\"\n" +
                    "                ]\n" +
                    "              }\n" +
                    "            ]\n" +
                    "          }";

    public static Filter getFilter(@Nullable String filterId, @Nullable String firstValueId,
                                   @Nullable String secondValueId) throws IOException {
        ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        ObjectNode filterNode = mapper.readValue(DEFAULT_FILTER, ObjectNode.class);
        if (Objects.nonNull(filterId)) {
            changeField(filterNode, "id", filterId);
        }
        if (Objects.nonNull(firstValueId)) {
            changeFilterValue(filterNode, firstValueId, 0);
        }
        if (Objects.nonNull(secondValueId)) {
            changeFilterValue(filterNode, secondValueId, 1);
        }
        return mapper.readValue(filterNode.toString(), Filter.class);
    }

    private static void changeFilterValue(ObjectNode parent, String newValue, int index) {
        changeField(((ObjectNode) parent.get("values").get(index)), "id", newValue);
    }

    private static void changeField(ObjectNode parent, String fieldName, String newValue) {
        if (parent.has(fieldName)) {
            parent.put(fieldName, newValue);
        }
    }
}
