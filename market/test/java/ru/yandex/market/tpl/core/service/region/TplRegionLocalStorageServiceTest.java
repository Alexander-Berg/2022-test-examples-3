package ru.yandex.market.tpl.core.service.region;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.region.RegionMeta;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TplRegionLocalStorageServiceTest {

    private static final String NOT_EXISTED_FILE = "notExistedFile";
    //Архив с границами 4ех регионов: 40, 20414, 99661, 186923
    private static final String BORDERS_BORDERS_TEST_TAR_BZ_2 = "/borders/borders_test.tar.bz2";

    @Test
    void getVersion_whenSourceExists() {
        //given
        TplRegionLocalStorageService tplRegionLocalStorageService =
                new TplRegionLocalStorageService(this.getClass().getResource(BORDERS_BORDERS_TEST_TAR_BZ_2).getPath());

        //when
        TplRegionStorage.RegionStorageVersion version = tplRegionLocalStorageService.getVersion();

        //then
        assertNotNull(version);
    }

    @Test
    void getVersion_whenSourceNotExists() {
        //given
        TplRegionLocalStorageService tplRegionLocalStorageService =
                new TplRegionLocalStorageService(NOT_EXISTED_FILE);

        //when
        TplRegionStorage.RegionStorageVersion version = tplRegionLocalStorageService.getVersion();

        //then
        assertNull(version);
    }

    @Test
    void forEachRegion_whenExists() {
        //given
        TplRegionLocalStorageService tplRegionLocalStorageService =
                new TplRegionLocalStorageService(this.getClass().getResource(BORDERS_BORDERS_TEST_TAR_BZ_2).getPath());

        Consumer<RegionMeta> mockedConsumer = mock(Consumer.class);
        //when
        tplRegionLocalStorageService.forEachRegion(mockedConsumer);

        //then
        Mockito.verify(mockedConsumer, times(4)).accept(any());
    }

    @Test
    void forEachRegion_whenNotExists() {
        //given
        TplRegionLocalStorageService tplRegionLocalStorageService =
                new TplRegionLocalStorageService(this.getClass().getResource(BORDERS_BORDERS_TEST_TAR_BZ_2).getPath());

        Consumer<RegionMeta> mockedConsumer = mock(Consumer.class);
        //when
        tplRegionLocalStorageService.forEachRegion(mockedConsumer);

        //then
        Mockito.verify(mockedConsumer, times(4)).accept(any());
    }
}
