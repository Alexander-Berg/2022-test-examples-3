package ru.yandex.travel.hotels.common.partners.dolphin.model;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelTests {

    private ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("UnstableApiUsage")
    private String load(String name) throws IOException {
        return Resources.toString(Resources.getResource(String.format("dolphinResponses/%s.json", name)),
                Charset.defaultCharset());
    }

    @Test
    public void testRooms() throws IOException {
        IdNameMap rooms = mapper.readerFor(IdNameMap.class).readValue(load("rooms"));
        assertThat(rooms).isNotNull();
        assertThat(rooms).hasSize(141);
        assertThat(rooms.get(61L)).isEqualTo("2-мест.");
    }

    @Test
    public void testRoomCategories() throws IOException {
        IdNameMap categories = mapper.readerFor(IdNameMap.class).readValue(load("roomCategories"));
        assertThat(categories).isNotNull();
        assertThat(categories).hasSize(7632);
        assertThat(categories.get(6339L)).isEqualTo("станд. с видом ");
    }

    @Test
    public void testPansions() throws IOException {
        PansionList pansions = mapper.readerFor(PansionList.class).readValue(load("pansions"));
        assertThat(pansions).isNotNull();
        assertThat(pansions).hasSize(111);
        assertThat(pansions.toMap().get(15L).getName()).isEqualTo("Завтрак");
    }

    @Test
    public void testAreas() throws IOException {
        AreaList areas = mapper.readerFor(AreaList.class).readValue(load("areas"));
        assertThat(areas).isNotEmpty();
        assertThat(areas).hasSize(965);
    }

    @Test
    public void testAreaMap() throws IOException {
        AreaList areas = mapper.readerFor(AreaList.class).readValue(load("areas"));
        Map<AreaType, Map<Long, Area>> map = areas.toMap();
        assertThat(map.get(AreaType.Country).get(37730L).getTitle()).isEqualTo("Россия");
    }

    @Test
    public void testSearchResponse() throws IOException {
        SearchResponse resp = mapper.readerFor(SearchResponse.class).readValue(load("searchResponse"));
        assertThat(resp).isNotNull();
        assertThat(resp.getOfferLists()).isNotEmpty();
        assertThat(resp.getOfferLists().get(0).getDate()).isEqualTo("2019-07-15");
        assertThat(resp.getOfferLists().get(0).getOffers()).hasSize(2);
        assertThat(resp.getOfferLists().get(0).getOffers().get(0).checkAllowed()).isTrue();
        assertThat(resp.getOfferLists().get(0).getOffers().get(0).isQuoted()).isTrue();
        assertThat(resp.getOfferLists().get(0).getOffers().get(0).getPrice()).isEqualTo(4500.0d);
    }
}
