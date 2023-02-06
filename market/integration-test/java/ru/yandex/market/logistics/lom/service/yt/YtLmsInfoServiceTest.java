package ru.yandex.market.logistics.lom.service.yt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.service.yt.dto.YtCluster;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Получение информации из yt LOM для данных LMS")
class YtLmsInfoServiceTest extends AbstractContextualTest {

    private static final String CACHED_VERSION = "version1";
    private static final String NEW_VERSION = "version2";

    @Autowired
    private LmsYtProperties lmsYtProperties;

    @Autowired
    private Yt hahnYt;

    @Autowired
    private YtTables ytTables;

    @Autowired
    private YtLmsInfoService ytLmsVersionService;

    @BeforeEach
    void setUp() {
        doReturn(ytTables)
            .when(hahnYt).tables();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(hahnYt, ytTables);
    }

    @Test
    @DisplayName("Получение кешированного значения версии")
    void getCachedVersion() {
        YtLmsVersionsUtils.mockYtVersionTable(ytTables, lmsYtProperties, CACHED_VERSION);
        softly.assertThat(ytLmsVersionService.getCachedActualVersion(YtCluster.HAHN))
            .isEqualTo(CACHED_VERSION);

        YtLmsVersionsUtils.mockYtVersionTable(ytTables, lmsYtProperties, NEW_VERSION);
        softly.assertThat(ytLmsVersionService.getActualVersion(YtCluster.HAHN))
            .isEqualTo(NEW_VERSION);
        for (int i = 0; i < 20; i++) {
            softly.assertThat(ytLmsVersionService.getCachedActualVersion(YtCluster.HAHN))
                .isEqualTo(CACHED_VERSION);
        }

        verify(hahnYt, times(2)).tables();
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties, 2);
    }

    @Test
    @DisplayName("Получение некешированного значения версии")
    void getVersion() {
        YtLmsVersionsUtils.mockYtVersionTable(ytTables, lmsYtProperties, CACHED_VERSION);
        softly.assertThat(ytLmsVersionService.getCachedActualVersion(YtCluster.HAHN))
            .isEqualTo(CACHED_VERSION);

        YtLmsVersionsUtils.mockYtVersionTable(ytTables, lmsYtProperties, NEW_VERSION);
        softly.assertThat(ytLmsVersionService.getActualVersion(YtCluster.HAHN))
            .isEqualTo(NEW_VERSION);
        for (int i = 0; i < 10; i++) {
            softly.assertThat(ytLmsVersionService.getActualVersion(YtCluster.HAHN))
                .isEqualTo(NEW_VERSION);
        }

        verify(hahnYt, times(12)).tables();
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties, 12);
    }
}
