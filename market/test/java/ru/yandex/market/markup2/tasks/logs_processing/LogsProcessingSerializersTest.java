package ru.yandex.market.markup2.tasks.logs_processing;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.markup2.entries.group.OfferFilters;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.utils.Markup2TestUtils;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author york
 * @since 29.11.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class LogsProcessingSerializersTest {

    private ObjectMapper mapper;

    @Before
    public void setup() {
        mapper = Markup2TestUtils.defaultMapper(LogsProcessingDataItemPayload.class);
        JsonSerializer reqSerializer = new LogsProcessingDataItemsProcessor().getRequestSerializer();
        SimpleModule serializerModule = new SimpleModule()
            .addSerializer(LogsProcessingDataIdentity.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(LogsProcessingDataIdentity.class,
                new JsonUtils.DefaultJsonDeserializer(LogsProcessingDataIdentity.class))
            .addSerializer(LogsProcessingDataItemPayload.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(LogsProcessingDataItemPayload.class,
                new JsonUtils.DefaultJsonDeserializer(LogsProcessingDataItemPayload.class))
            .addSerializer(OfferFilters.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(OfferFilters.class,
                new JsonUtils.DefaultJsonDeserializer(OfferFilters.class))
            .addSerializer(TaskDataItem.class, reqSerializer);
        mapper = new ObjectMapper().registerModule(serializerModule);
    }

    @Test
    public void testPayloadSerialization() throws IOException {
        LogsProcessingDataItemPayload payload = generatePayload();
        String json = mapper.writeValueAsString(payload);
        System.out.println(json);
        LogsProcessingDataItemPayload deserialized =
            mapper.readerFor(LogsProcessingDataItemPayload.class).readValue(json);

        assertThat(payload.getAttributes()).isEqualTo(deserialized.getAttributes());
        assertThat(payload.getDataIdentifier()).isEqualTo(deserialized.getDataIdentifier());
    }

    @Test
    public void testRequestSerialization() throws IOException {
        LogsProcessingDataItemPayload payload = generatePayload();
        TaskDataItem item = new TaskDataItem(1L, payload);
        String json = mapper.writeValueAsString(item);
        System.out.println(json);
    }

    @Test
    public void testOfferFiltersSerialization() throws IOException {
        OfferFilters offerFilters = new OfferFilters(
            Arrays.asList(
                Markup.OfferFilter.newBuilder()
                    .setOperator(Markup.OfferFilter.Operator.FORMALIZED)
                    .setSourceId(1)
                    .setSourceType(Markup.OfferFilter.SourceType.PARAMETER)
                    .setStringValue("q")
                    .addAllStringValues(Arrays.asList("q", "v"))
                    .setNumericValue(1.1)
                    .addAllValueIds(Arrays.asList(2L, 3L))
                    .build(),
                Markup.OfferFilter.newBuilder()
                    .setOperator(Markup.OfferFilter.Operator.MATCHES)
                    .setSourceId(2)
                    .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                    .build()
            )
        );
        String json = mapper.writeValueAsString(offerFilters);
        System.out.println(json);
        OfferFilters offerFiltersDeserialized =
            mapper.readerFor(OfferFilters.class).readValue(json);
        System.out.println(offerFiltersDeserialized);

        assertThat(offerFilters).isEqualTo(offerFiltersDeserialized);
    }

    @Test
    public void testOldSerializedOfferFilters() throws IOException {
        String json = "{\"filters\": [{\"operator\": \"MATCHES\", \"source_id\": 1, \"value_ids\": []," +
            "\"source_type\": \"PROPERTY\", \"string_value\": \"www.ozon.ru\"}]}";

        OfferFilters offerFiltersDeserialized =
            mapper.readerFor(OfferFilters.class).readValue(json);
        System.out.println(offerFiltersDeserialized);

        assertThat(new OfferFilters(Collections.singletonList(
            Markup.OfferFilter.newBuilder()
                .setOperator(Markup.OfferFilter.Operator.MATCHES)
                .setSourceId(1)
                .setSourceType(Markup.OfferFilter.SourceType.PROPERTY)
                .setStringValue("www.ozon.ru")
                .addStringValues("www.ozon.ru")
                .build()
        ))).isEqualTo(offerFiltersDeserialized);
    }

    private LogsProcessingDataItemPayload generatePayload() {
        AliasMaker.Offer offer = AliasMaker.Offer.newBuilder()
            .setOfferId("o_id")
            .setOfferModel("model")
            .setOfferParams("para|ewr34z|")
            .setOfferVendor("vendor")
            .setShopOfferId("shopid")
            .setBarcode("02342342")
            .setClusterId(10L)
            .setDescription("d3esc")
            .setMatchType(Matcher.MatchType.CUT_OF_WORDS)
            .setModelId(133L)
            .setModelName("mdl")
            .setPictures("pics")
            .setPrice(0.2)
            .setShopCategoryName("shop_cat_nam")
            .setShopName("shop")
            .setTitle("title")
            .setUrl("url")
            .setVendorId(1L)
            .setVendorName("vend")
            .build();
        LogsProcessingDataIdentity identity = new LogsProcessingDataIdentity(offer.getOfferId());
        LogsProcessingDataAttributes attributes = new LogsProcessingDataAttributes(10000,
            "category", offer);
        LogsProcessingDataItemPayload payload = new LogsProcessingDataItemPayload(identity, attributes);
        return payload;
    }
}
