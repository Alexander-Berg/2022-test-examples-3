package ru.yandex.market.markup2.tasks.fill_param_values;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.utils.Markup2TestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 13.06.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class FillParamValuesSerializersTest {

    private ObjectMapper mapper;

    @Before
    public void setup() {
        SimpleModule module = new SimpleModule();

        addSerializer(module, FillParamValuesResponse.class);
        addSerializer(module, FillParamValuesDataItemPayload.class);
        addDeserializer(module, FillParamValuesResponse.class);
        addDeserializer(module, FillParamValuesDataItemPayload.class);

        mapper = new ObjectMapper();
        mapper.registerModule(module);
    }

    private <T> void addSerializer(SimpleModule module, Class<T> clazz) {
        module.addSerializer(clazz, new JsonUtils.DefaultJsonSerializer<>());
    }

    private <T> void addDeserializer(SimpleModule module, Class<T>  clazz) {
        module.addDeserializer(clazz, new JsonUtils.DefaultJsonDeserializer<>(clazz));
    }

    @Test
    public void requestSerialization() throws IOException {
        ParameterTemplate.Builder templateBuilder = ParameterTemplate.newBuilder()
            .setId(1)
            .setXslName("width")
            .setDescription("descr")
            .setMinValue(1d)
            .setMaxValue(2d)
            .setType("ENUM")
            .setName("Ширина")
            .setRequired(true)
            .setMultiValue(true)
            .setOptions(ImmutableList.of(
                new ParameterTemplate.Option(1, "1"),
                new ParameterTemplate.Option(2, "2"))
            )
            .setPossibleValues(ImmutableList.of("1", "2"))
            .setComment("comment")
            .setFormalizedValue("1")
            .setOfferUrl("http://ya.ru/1")
            .setVendorValues(new ArrayList<>(Arrays.asList("10", "20")))
            .setOfferLevel(true);

        ParameterTemplate firstTemplate = templateBuilder.build();

        ParameterTemplate secondTemplate = templateBuilder
            .setId(2)
            .setXslName("height")
            .setName("Высота")
            .setMinValue(4d)
            .setMaxValue(5d)
            .setOptions(ImmutableList.of(
                new ParameterTemplate.Option(3, "4"),
                new ParameterTemplate.Option(4, "5"))
            )
            .setPossibleValues(ImmutableList.of("4", "5"))
            .setFormalizedValue("2")
            .setOfferUrl("http://ya.ru/2")
            .setOfferLevel(false)
            .build();

        List<ParameterTemplate> templateList = Arrays.asList(firstTemplate, secondTemplate);
        FillParamValuesDataAttributes attributes = FillParamValuesDataAttributes.newBuilder()
            .setCategoryId(42)
            .setName("test model")
            .setShoplink("test shop link")
            .setVendor("test vendor")
            .setVendorLink("test vendor link")
            .setTemplate(templateList)
            .build();

        FillParamValuesDataItemPayload payload =
            new FillParamValuesDataItemPayload(42, ImmutableSet.of(1L, 2L), attributes);

        String json = mapper.writeValueAsString(payload);
        System.out.println(json);

        FillParamValuesDataItemPayload deserialized =
            mapper.readerFor(FillParamValuesDataItemPayload.class).readValue(json);

        Assert.assertEquals(payload.getDataIdentifier(), deserialized.getDataIdentifier());
        Assert.assertEquals(payload.getAttributes(), deserialized.getAttributes());
    }

    @Test
    public void responseSerialization() throws IOException {
        List<Map<String, Object>> characteristics = Arrays.asList(
            ImmutableMap.of("a", "1", "b", "2", "c", "3"),
            ImmutableMap.of("a", "1", "b", "NO_VALUE", "c", "3")
        );
        List<String> link = Arrays.asList("a", "b", "c");

        FillParamValuesResponse response = new FillParamValuesResponse(42L, characteristics, link);
        String json = mapper.writeValueAsString(response);

        FillParamValuesResponse deserialized = mapper.readerFor(FillParamValuesResponse.class).readValue(json);

        Assert.assertEquals(response.getId(), deserialized.getId());
        Assert.assertEquals(response.getLink(), deserialized.getLink());
        Assert.assertEquals(response.getCharacteristics(), deserialized.getCharacteristics());
    }

    @Test
    public void emptyResponseDeserialization() throws IOException {
        String json = "{ \"req_id\": \"1\" }";
        FillParamValuesResponse deserialized = mapper.readerFor(FillParamValuesResponse.class).readValue(json);

        Assert.assertEquals(1L, deserialized.getId());
        Assert.assertNull(deserialized.getCharacteristics());
        Assert.assertNull(deserialized.getLink());
    }

    @Test
    public void mapCharacteristicsDeserialization() throws IOException {
        String json = "{\"req_id\": \"1\", \"characteristics\": {\"obj\":\"1\"}}";
        FillParamValuesResponse response = mapper.reader().forType(FillParamValuesResponse.class).readValue(json);

        Assert.assertEquals(1, response.getCharacteristics().size());
        Assert.assertEquals(1, response.getCharacteristics().get(0).size());
        Object obj = response.getCharacteristics().get(0).get("obj");
        Assert.assertEquals(obj, "1");
    }

    @Test
    public void arrayCharacteristicsDeserialization() throws IOException {
        String json = "{\"req_id\": \"1\", \"characteristics\": [{\"obj\":\"1\"}, {\"obj\":\"2\"}]}";
        FillParamValuesResponse response = mapper.reader().forType(FillParamValuesResponse.class).readValue(json);

        Assert.assertEquals(2, response.getCharacteristics().size());
        Assert.assertEquals(1, response.getCharacteristics().get(0).size());
        Assert.assertEquals(1, response.getCharacteristics().get(1).size());

        Object obj = response.getCharacteristics().get(0).get("obj");
        Assert.assertEquals(obj, "1");

        obj = response.getCharacteristics().get(1).get("obj");
        Assert.assertEquals(obj, "2");
    }

    @Test
    public void deserializeWithoutFormalizedValue() throws IOException {
        String json = Markup2TestUtils.getResource("tasks/fill_param_values/template_without_formalized_value.json");
        mapper.readerFor(ParameterTemplate.class).readValue(json);
    }
}
