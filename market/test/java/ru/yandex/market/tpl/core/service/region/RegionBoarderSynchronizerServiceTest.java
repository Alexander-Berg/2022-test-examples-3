package ru.yandex.market.tpl.core.service.region;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.region.actualization.RegionBorderHistoryRepository;
import ru.yandex.market.tpl.core.domain.region.actualization.TplRegionBorderGisDao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RegionBoarderSynchronizerServiceTest {

    @Mock
    private TplRegionStorage tplRegionLocalStorageService;
    @Mock
    private TplRegionStorage tplRegionDbStorageService;
    @Mock
    private TplRegionBorderGisDao tplRegionBorderGisDao;
    @Mock
    private RegionBorderHistoryRepository regionBorderHistoryRepository;

    private RegionBoarderSynchronizerService regionBoarderSynchronizerService;

    @BeforeEach
    void setUp() {
        //Not using InjectMocks cause incorrect init with TplRegionStorage's
        regionBoarderSynchronizerService = new RegionBoarderSynchronizerService(
                tplRegionLocalStorageService,
                tplRegionDbStorageService,
                tplRegionBorderGisDao,
                regionBorderHistoryRepository
        );
    }

    @Test
    void isNeedToUpdate_whenDbBeforeLocal() {
        //given
        Instant dbActualizationDate = Instant.now().minusSeconds(1_000);
        Mockito.doReturn(TplRegionStorage.RegionStorageVersion.of(dbActualizationDate))
                .when(tplRegionDbStorageService).getVersion();

        Instant localActualizationDate = Instant.now();
        Mockito.doReturn(TplRegionStorage.RegionStorageVersion.of(localActualizationDate))
                .when(tplRegionLocalStorageService).getVersion();

        //when
        boolean isNeedToUpdate = regionBoarderSynchronizerService.isNeedToUpdate();

        //then
        assertTrue(isNeedToUpdate);
    }

    @Test
    void isNeedToUpdate_whenLocalNull() {
        //given
        Instant dbActualizationDate = Instant.now().minusSeconds(1_000);
        Mockito.doReturn(TplRegionStorage.RegionStorageVersion.of(dbActualizationDate))
                .when(tplRegionDbStorageService).getVersion();

        Mockito.doReturn(null)
                .when(tplRegionLocalStorageService).getVersion();

        //when
        boolean isNeedToUpdate = regionBoarderSynchronizerService.isNeedToUpdate();

        //then
        assertFalse(isNeedToUpdate);
    }

    @Test
    void isNeedToUpdate_whenDbNull() {
        //given

        Mockito.doReturn(null)
                .when(tplRegionDbStorageService).getVersion();

        //when
        boolean isNeedToUpdate = regionBoarderSynchronizerService.isNeedToUpdate();

        //then
        assertTrue(isNeedToUpdate);
    }
}
