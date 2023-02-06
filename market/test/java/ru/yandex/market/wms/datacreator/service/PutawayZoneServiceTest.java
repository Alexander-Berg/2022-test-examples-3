package ru.yandex.market.wms.datacreator.service;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.common.spring.dao.implementation.AreaDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.AreaDetailDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.BuildingDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.PutawayZoneDAO;
import ru.yandex.market.wms.datacreator.exception.SuffixNotFoundException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.datacreator.service.SuffixService.SUFFIX_REGEXP;

@SpringBootTest(classes = {PutawayZoneService.class})
class PutawayZoneServiceTest {

    private final String zonePrefix = "zonePrefix";
    private final String newZoneName = "NewZoneName";
    private final List<String> zoneList = Arrays.asList("One", "Two", "Three");
    @MockBean
    private PutawayZoneDAO putawayZoneDAO;

    @Autowired
    PutawayZoneService putawayZoneService;

    @MockBean
    private LocationService locationService;

    @MockBean
    AreaDAO areaDAO;

    @MockBean
    AreaDetailDAO areaDetailDAO;

    @MockBean
    SuffixService suffixService;

    @MockBean
    BuildingDAO buildingDAO;

    @Test
    void create() {
        doNothing().when(suffixService).cleanupExpiredSuffixes();
        when(putawayZoneDAO.findZoneByRegexp(zonePrefix + SUFFIX_REGEXP)).thenReturn(zoneList);
        doThrow(new SuffixNotFoundException("Free suffix not found")).when(suffixService)
                .getNewName(zonePrefix, zoneList);
        assertThrows(SuffixNotFoundException.class, () -> putawayZoneService.getNewZoneName(zonePrefix));
        verify(suffixService).cleanupExpiredSuffixes();
    }
}
