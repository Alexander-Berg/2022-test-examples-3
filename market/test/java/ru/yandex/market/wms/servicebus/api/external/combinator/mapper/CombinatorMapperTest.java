package ru.yandex.market.wms.servicebus.api.external.combinator.mapper;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.servicebus.model.dto.BoxDto;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.ItemDto;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.RecommendedBoxDto;
import ru.yandex.market.wms.common.spring.servicebus.model.request.OrderMaxParcelDimensionsRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.request.RecommendCartonRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.response.OrderMaxParcelDimensionsResponse;
import ru.yandex.market.wms.common.spring.servicebus.model.response.RecommendCartonResponse;
import ru.yandex.market.wms.servicebus.api.external.combinator.model.entity.AvailableBox;
import ru.yandex.market.wms.servicebus.api.external.combinator.model.entity.Item;
import ru.yandex.market.wms.servicebus.api.external.combinator.model.entity.Recommendation;
import ru.yandex.market.wms.servicebus.api.external.combinator.model.entity.UnitId;
import ru.yandex.market.wms.servicebus.api.external.combinator.model.request.CombinatorCartonRecommendationRequest;
import ru.yandex.market.wms.servicebus.api.external.combinator.model.response.CombinatorCartonRecommendationResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class CombinatorMapperTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String YMA = "YMA";
    private static final String YMB = "YMB";
    private final CombinatorMapper combinatorMapper = new CombinatorMapper();

    @Test
    public void testConvertGetOrderMaxDimensionsRequest() throws JsonProcessingException {
        OrderMaxParcelDimensionsRequest expected = OrderMaxParcelDimensionsRequest.builder()
                .externalOrderKey("0000123")
                .build();
        String actualJson = combinatorMapper.convertGetOrderMaxDimensionsRequest(expected);

        assertEquals(getFileContent("api/external/combinator/order-max-dimensions-request.json"), actualJson);


    }


    @Test
    public void testConvertGetOrderMaxDimensionsResponse() throws JsonProcessingException {
        String actualJson = getFileContent("api/external/combinator/order-max-dimensions-response.json");

        OrderMaxParcelDimensionsResponse response = combinatorMapper.convertGetOrderMaxDimensionsResponse(actualJson);
        assertEquals(100, response.getLength());
        assertEquals(200, response.getWidth());
        assertEquals(250, response.getHeight());
        assertEquals(500, response.getDimSum());
        assertEquals(5000, response.getWeight());

    }

    @Test
    public void testConvertRecommendCartonRequest() throws JsonProcessingException {
        List<BoxDto> boxes = new ArrayList<>();
        boxes.add(BoxDto.builder()
                .id(YMA)
                .length(11).width(12).height(13)
                .maxWeight(999)
                .build());
        List<ItemDto> items = new ArrayList<>();
        items.add(ItemDto.builder()
                .storerKey("465852")
                .manufacturerSku("sku1")
                .qty(7)
                .length(1).width(2).height(3)
                .weightGross(4)
                .build());
        RecommendCartonRequest expected = RecommendCartonRequest.builder()
                .items(items).boxes(boxes).build();

        String actualJson = combinatorMapper.convertRecommendCartonRequest(expected);
        CombinatorCartonRecommendationRequest actual = MAPPER.readValue(actualJson,
                CombinatorCartonRecommendationRequest.class);
        assertNotNull(actual);

        assertEquals(1, actual.getBoxes().size());
        BoxDto expectedBox = expected.getBoxes().iterator().next();
        AvailableBox actualBox = actual.getBoxes().iterator().next();
        assertEquals(expectedBox.getId(), actualBox.getId());
        assertEquals(expectedBox.getLength() * 10, actualBox.getDimensions().getLength(), 10e-2);
        assertEquals(expectedBox.getWidth() * 10, actualBox.getDimensions().getWidth(), 10e-2);
        assertEquals(expectedBox.getHeight() * 10, actualBox.getDimensions().getHeight(), 10e-2);
        assertEquals(expectedBox.getMaxWeight() * 1000, actualBox.getDimensions().getWeightGross(), 10e-2);

        assertEquals(1, actual.getItems().size());
        ItemDto expectedItem = expected.getItems().iterator().next();
        Item actualItem = actual.getItems().iterator().next();
        assertEquals(expectedItem.getStorerKey(), actualItem.getUnitId().getStorerKey());
        assertEquals(expectedItem.getManufacturerSku(), actualItem.getUnitId().getManufacturerSku());
        assertEquals(expectedItem.getQty(), actualItem.getAmount());
        assertNotNull(actualItem.getDimensions());
        assertEquals(expectedItem.getLength() * 10, actualItem.getDimensions().getLength(), 10e-2);
        assertEquals(expectedItem.getWidth() * 10, actualItem.getDimensions().getWidth(), 10e-2);
        assertEquals(expectedItem.getHeight() * 10, actualItem.getDimensions().getHeight(), 10e-2);
        assertEquals(expectedItem.getWeightGross() * 1000, actualItem.getDimensions().getWeightGross(), 10e-2);
    }

    @Test
    public void testConvertRecommendCartonResponse() throws JsonProcessingException {
        List<Recommendation> recommendations = new ArrayList<>();
        Recommendation expectedYma = Recommendation.builder()
                .boxId(YMA)
                .items(List.of(Item.builder()
                        .unitId(UnitId.builder()
                                .manufacturerSku("sku1")
                                .storerKey("111")
                                .build())
                        .amount(2)
                        .build()))
                .build();
        Recommendation expectedYmb = Recommendation.builder()
                .boxId(YMB)
                .items(List.of(Item.builder()
                        .unitId(UnitId.builder()
                                .manufacturerSku("sku2")
                                .storerKey("222")
                                .build())
                        .amount(5)
                        .build()))
                .build();
        recommendations.add(expectedYma);
        recommendations.add(expectedYmb);
        CombinatorCartonRecommendationResponse expected = CombinatorCartonRecommendationResponse.builder()
                .recommendations(recommendations).build();
        String expectedJson = MAPPER.writeValueAsString(expected);
        RecommendCartonResponse actual = combinatorMapper.convertRecommendCartonResponse(expectedJson);
        assertNotNull(actual);
        assertNotNull(actual.getRecommendations());
        assertEquals(2, actual.getRecommendations().size());
        RecommendedBoxDto actualBoxYma = actual.getRecommendations().stream()
                .filter(r -> YMA.equals(r.getBoxId()))
                .findAny()
                .orElseThrow();
        RecommendedBoxDto actualBoxYmb = actual.getRecommendations().stream()
                .filter(r -> YMB.equals(r.getBoxId()))
                .findAny()
                .orElseThrow();

        assertRecommendationEquals(expectedYma, actualBoxYma);
        assertRecommendationEquals(expectedYmb, actualBoxYmb);
    }

    private void assertRecommendationEquals(Recommendation expected,
                                 RecommendedBoxDto actual) {
        assertEquals(expected.getBoxId(), actual.getBoxId());
        assertEquals(expected.getItems().size(), actual.getItems().size());
        for (Item item : expected.getItems()) {
            ItemDto actualItem = actual.getItems().stream()
                    .filter(i -> item.getUnitId().getStorerKey().equals(i.getStorerKey())
                            && item.getUnitId().getManufacturerSku().equals(i.getManufacturerSku()))
                    .findAny().orElseThrow();
            assertEquals(item.getAmount(), actualItem.getQty());
        }
    }
}
