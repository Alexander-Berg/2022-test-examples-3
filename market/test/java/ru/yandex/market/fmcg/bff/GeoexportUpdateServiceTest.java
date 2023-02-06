package ru.yandex.market.fmcg.bff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.fmcg.bff.geoexport.GeoexportRegionListUpdateService;
import ru.yandex.market.fmcg.bff.geoexport.model.GeoexportRegionBff;
import ru.yandex.market.fmcg.bff.geoexport.model.GeoexportRegionDto;
import ru.yandex.market.fmcg.bff.test.FmcgBffTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class GeoexportUpdateServiceTest extends FmcgBffTest {

    @Autowired
    RestTemplate geoexportRestTemplate;

    @Autowired
    GeoexportRegionListUpdateService geoexportRegionListUpdateService;

    @Autowired
    AtomicReference<List<GeoexportRegionBff>> geoexportRegionListBean;

    @Test
    public void generalTest() {
        when(geoexportRestTemplate.getForObject(GeoexportRegionListUpdateService.uri, GeoexportRegionDto[].class))
            .thenAnswer(inv -> createStubGeoexportRegionDto());
        geoexportRegionListUpdateService.update();
        List<GeoexportRegionBff> list = geoexportRegionListBean.get();
        assertEquals(
            createStubGeoexportRegionBff(),
            list);
    }

    private GeoexportRegionDto[] createStubGeoexportRegionDto() {
        return new GeoexportRegionDto[]{new GeoexportRegionDto("213", "Москва", 55.751244d, 37.618423d),
            new GeoexportRegionDto("2", "Санкт-Петербург",59.976665d, 30.320833d)};
    }

    private List<GeoexportRegionBff> createStubGeoexportRegionBff() {
        return Stream.of(
            new GeoexportRegionBff("213", "Москва", true, 55.751244d, 37.618423d),
            new GeoexportRegionBff("2", "Санкт-Петербург", true,59.976665d, 30.320833d))
            .collect(Collectors.toList());
    }

}
