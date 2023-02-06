package ru.yandex.travel.api.models.hotels.breadcrumbs;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Test;

import ru.yandex.travel.api.models.Linguistics;
import ru.yandex.travel.api.models.Region;
import ru.yandex.travel.commons.jackson.MoneySerializersModule;

import static org.assertj.core.api.Assertions.assertThat;

public class BreadcrumbTest {
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SnakeCaseStrategy.SNAKE_CASE)
            .registerModule(new MoneySerializersModule())
            .registerModule(new JavaTimeModule());

    @SuppressWarnings("deprecation")
    @Test
    public void testSerialization() throws Exception {
        Region region = Region.builder()
                .slug("moscow")
                .type(6)
                .geoId(213)
                .linguistics(new Linguistics())
                .build();
        Breadcrumbs breadcrumbs = Breadcrumbs.builder()
                .items(List.of(new GeoRegionBreadcrumb(region), new FilterBreadcrumb("msc", "moscow")))
                .geoRegions(List.of(new GeoRegionBreadcrumb(region)))
                .build();

        String json = mapper.writeValueAsString(breadcrumbs);
        // the API code uses the same 2-phase parsing: json -> tree -> pojo
        JsonNode parsedJsonTree = mapper.readTree(json);
        Breadcrumbs deserialized = mapper.treeToValue(parsedJsonTree, Breadcrumbs.class);

        assertThat(deserialized).isEqualTo(breadcrumbs);
        assertThat(deserialized).isNotEqualTo(new Breadcrumbs());

        // the fields are needed for UI
        assertThat(parsedJsonTree.at("/items/0/breadcrumbType").textValue())
                .isEqualTo(BreadcrumbType.GEO_REGION_BREADCRUMB.getTypeName());
        assertThat(parsedJsonTree.at("/items/1/breadcrumbType").textValue())
                .isEqualTo(BreadcrumbType.FILTER_BREADCRUMB.getTypeName());
    }
}
