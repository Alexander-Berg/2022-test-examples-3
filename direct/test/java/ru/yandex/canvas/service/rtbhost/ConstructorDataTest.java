package ru.yandex.canvas.service.rtbhost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.canvas.model.Bundle;
import ru.yandex.canvas.model.CreativeData;
import ru.yandex.canvas.model.MediaSet;
import ru.yandex.canvas.model.MediaSetItem;
import ru.yandex.canvas.model.MediaSetSubItem;

import static junit.framework.TestCase.assertEquals;

public class ConstructorDataTest {
    private ObjectMapper mapper;
    private CreativeData creativeData;

    @Before
    public void init() {
        mapper = new ObjectMapper();

        Bundle bundle = new Bundle();
        bundle.setName("name");
        bundle.setVersion(1);

        creativeData = new CreativeData();
        CreativeData.Options options = new CreativeData.Options();
        options.setMinHeight(250);
        options.setMinWidth(300);
        creativeData.setOptions(options);
        creativeData.setElements(Collections.emptyList());
        creativeData.setMediaSets(Collections.emptyMap());
        creativeData.setBundle(bundle);
    }

    @Test
    public void withoutAdaptive() {
        CreativeData.Options options = creativeData.getOptions();
        options.setBackgroundColor("#000000");
        options.setBorderColor("#000000");

        String expected =
                "{\"elements\":[],\"template\":\"name\",\"backgroundColor\":\"#000000\",\"borderColor\":\"#000000\","
                        + "\"creative_parameters\":{\"Html\":{\"Data\":{\"Options\":{\"BackgroundColor\":\"#000000\","
                        + "\"BorderColor\":\"#000000\",\"IsAdaptive\":false,\"MinSize\":{\"Width\":300,\"Height\":250}},"
                        + "\"Bundle\":{\"Name\":\"name\",\"Version\":1}}}}}";
        try {
            String json = mapper.writeValueAsString(new ConstructorData(creativeData));
            assertEquals("Json as expected", expected, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void withAdaptiveMinSize() {
        CreativeData.Options options = creativeData.getOptions();
        options.setBackgroundColor("#000000");
        options.setBorderColor("#000000");
        options.setIsAdaptive(true);

        String expected =
                "{\"elements\":[],\"template\":\"name\",\"backgroundColor\":\"#000000\",\"borderColor\":\"#000000\","
                        + "\"minWidth\":300,\"minHeight\":250,\"maxWidth\":300,\"maxHeight\":250,\"creative_parameters\":"
                        + "{\"Html\":{\"Data\":{\"Options\":{\"BackgroundColor\":\"#000000\",\"BorderColor\":\"#000000\","
                        + "\"IsAdaptive\":true,\"MinSize\":{\"Width\":300,\"Height\":250}},\"Bundle\":{\"Name\":\"name\","
                        + "\"Version\":1}},\"MinSize\":{\"Width\":300,\"Height\":250},\"MaxSize\":{\"Width\":300,\"Height\":250}}}}";
        try {
            String json = mapper.writeValueAsString(new ConstructorData(creativeData));
            assertEquals("Json as expected", expected, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void withAdaptiveActualize() {
        CreativeData.Options options = creativeData.getOptions();
        options.setBackgroundColor("#000000");
        options.setBorderColor("#000000");
        options.setIsAdaptive(true);

        final MediaSetSubItem mediaSetSubItem = new MediaSetSubItem();
        mediaSetSubItem.setWidth(400);
        mediaSetSubItem.setHeight(400);

        final MediaSetItem mediaSetItem = new MediaSetItem();
        mediaSetItem.setType("image");
        mediaSetItem.setItems(Collections.singletonList(mediaSetSubItem));

        final MediaSet mediaSet = new MediaSet();
        mediaSet.setItems(Collections.singletonList(mediaSetItem));

        creativeData.setMediaSets(Collections.singletonMap("mediaSet", mediaSet));

        String expected =
                "{\"elements\":[],\"template\":\"name\",\"backgroundColor\":\"#000000\",\"borderColor\":\"#000000\","
                        + "\"minWidth\":300,\"minHeight\":250,\"maxWidth\":700,\"maxHeight\":650,\"creative_parameters\":"
                        + "{\"Html\":{\"Data\":{\"Options\":{\"BackgroundColor\":\"#000000\",\"BorderColor\":\"#000000\","
                        + "\"IsAdaptive\":true,\"MinSize\":{\"Width\":300,\"Height\":250}},\"MediaSets\":[{\"Name\":\"mediaSet\","
                        + "\"Items\":[{\"Type\":\"image\",\"Items\":[{\"Size\":{\"Width\":400,\"Height\":400},"
                        + "\"IsDefault\":false}]}]}],\"Bundle\":{\"Name\":\"name\",\"Version\":1}},"
                        + "\"MinSize\":{\"Width\":300,\"Height\":250},\"MaxSize\":{\"Width\":700,\"Height\":650}}}}";
        try {
            String json = mapper.writeValueAsString(new ConstructorData(creativeData));
            assertEquals("Json as expected", expected, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void withAdaptiveBigLogo() {
        CreativeData.Options options = creativeData.getOptions();
        options.setBackgroundColor("#000000");
        options.setBorderColor("#000000");
        options.setIsAdaptive(true);

        List<MediaSetItem> items = new ArrayList<>();

        final MediaSetSubItem mediaSetSubItemImage = new MediaSetSubItem();
        mediaSetSubItemImage.setWidth(500);
        mediaSetSubItemImage.setHeight(500);
        final MediaSetItem mediaSetItemImage = new MediaSetItem();
        mediaSetItemImage.setType("image");
        mediaSetItemImage.setItems(Collections.singletonList(mediaSetSubItemImage));
        items.add(mediaSetItemImage);

        final MediaSetSubItem mediaSetSubItemLogo = new MediaSetSubItem();
        mediaSetSubItemLogo.setWidth(800);
        mediaSetSubItemLogo.setHeight(800);
        final MediaSetItem mediaSetItemLogo = new MediaSetItem();
        mediaSetItemLogo.setType("logo");
        mediaSetItemLogo.setItems(Collections.singletonList(mediaSetSubItemLogo));
        items.add(mediaSetItemLogo);

        final MediaSet mediaSet = new MediaSet();
        mediaSet.setItems(items);

        creativeData.setMediaSets(Collections.singletonMap("mediaSet", mediaSet));

        String expected =
                "{\"elements\":[],\"template\":\"name\",\"backgroundColor\":\"#000000\",\"borderColor\":\"#000000\","
                        + "\"minWidth\":300,\"minHeight\":250,\"maxWidth\":800,\"maxHeight\":750,\"creative_parameters\":"
                        + "{\"Html\":{\"Data\":{\"Options\":{\"BackgroundColor\":\"#000000\",\"BorderColor\":\"#000000\","
                        + "\"IsAdaptive\":true,\"MinSize\":{\"Width\":300,\"Height\":250}},\"MediaSets\":[{\"Name\":\"mediaSet\","
                        + "\"Items\":[{\"Type\":\"image\",\"Items\":[{\"Size\":{\"Width\":500,\"Height\":500},"
                        + "\"IsDefault\":false}]},{\"Type\":\"logo\",\"Items\":[{\"Size\":{\"Width\":800,\"Height\":800},"
                        + "\"IsDefault\":false}]}]}],\"Bundle\":{\"Name\":\"name\",\"Version\":1}},"
                        + "\"MinSize\":{\"Width\":300,\"Height\":250},\"MaxSize\":{\"Width\":800,\"Height\":750}}}}";
        try {
            String json = mapper.writeValueAsString(new ConstructorData(creativeData));
            assertEquals("Json as expected", expected, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
