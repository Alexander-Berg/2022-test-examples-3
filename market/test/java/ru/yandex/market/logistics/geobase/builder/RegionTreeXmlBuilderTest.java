package ru.yandex.market.logistics.geobase.builder;

import java.net.URL;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.region.CustomRegionAttribute;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.logistics.geobase.AbstractTest;
import ru.yandex.market.logistics.geobase.config.RegionTreeXmlBuilder;

@DisplayName("Тесты на RegionTreeXmlBuilder")
public class RegionTreeXmlBuilderTest extends AbstractTest {

    @Test
    @DisplayName("Успешный разбор xml геобазы")
    void successParsing() {
        RegionTreeXmlBuilder regionTreeXmlBuilder = constructBuilder("data/geobase/geobase.xml");
        Region rootRegion = rootRegion();
        Region childRegion = childRegion(rootRegion);
        RegionTree<Region> result = regionTreeXmlBuilder.buildRegionTree();
        softly.assertThat(result.getRoot()).usingRecursiveComparison().isEqualTo(rootRegion);
        softly.assertThat(result.getRoot().getChildren()).containsExactlyInAnyOrderElementsOf(
            List.of(childRegion)
        );
    }

    @Test
    @DisplayName("Успешный разбор xml геобазы - один из регионов невалиден")
    void parsingInvalidXml() {
        RegionTreeXmlBuilder regionTreeXmlBuilder = constructBuilder("data/geobase/invalid_geobase.xml");
        Region rootRegion = rootRegion();
        RegionTree<Region> result = regionTreeXmlBuilder.buildRegionTree();
        softly.assertThat(result.getRoot()).usingRecursiveComparison().isEqualTo(rootRegion);
    }

    @Nonnull
    @SneakyThrows
    private RegionTreeXmlBuilder constructBuilder(String filename) {
         return new RegionTreeXmlBuilder()
            .setUrl(new URL(
                "classpath",
                "",
                80,
                filename,
                TomcatURLStreamHandlerFactory.getInstance().createURLStreamHandler("classpath")
            ))
            .setReadTimeoutMillis(1000)
            .setAttemptsCount(3)
            .setConnectTimeoutMillis(1000)
            .setSkipUnRootRegions(true);
    }

    @Nonnull
    private Region childRegion(Region parentRegion) {
        Region result = new Region(10001, "Евразия", RegionType.CONTINENT, parentRegion);
        result.setCustomAttributeValue(CustomRegionAttribute.TIMEZONE_OFFSET, "0");
        return result;
    }

    @Nonnull
    private Region rootRegion() {
        Region result = new Region(10000, "Земля", RegionType.OTHERS_UNIVERSAL, null);
        result.setCustomAttributeValue(CustomRegionAttribute.TIMEZONE_OFFSET, "0");
        return result;
    }
}
