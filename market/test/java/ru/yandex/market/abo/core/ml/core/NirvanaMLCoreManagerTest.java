package ru.yandex.market.abo.core.ml.core;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.abo.core.yt.YtService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NirvanaMLCoreManagerTest {
    private static final String YT_PATH = "//home/market/fake/abo/ml/core/result/";

    @InjectMocks
    private NirvanaMLCoreManager nirvanaMLCoreManager;
    @Mock
    private YtService ytService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadResultFromYtWithEmptyList() {
        nirvanaMLCoreManager.loadResultFromYt(Collections.emptyList());
        verify(ytService, never()).readTableJson(any(), eq(SuspiciousOffer.class), any());
    }

    @ParameterizedTest(name = "loadResultFromYt_{index}")
    @MethodSource("loadResultFromYtMethodSource")
    void loadResultFromYt(YPath expectedResultTable, List<String> unprocessedTables) {
        var yPathArgumentCaptor = ArgumentCaptor.forClass(YPath.class);
        nirvanaMLCoreManager.loadResultFromYt(unprocessedTables);
        verify(ytService).readTableJson(yPathArgumentCaptor.capture(), eq(SuspiciousOffer.class), any());
        assertEquals(expectedResultTable, yPathArgumentCaptor.getValue());
    }

    static Stream<Arguments> loadResultFromYtMethodSource() {
        return Stream.of(
                Arguments.of(generateYPaths("2019_01_01"), withPrefix("2001_01_01", "2019_01_01", "2009_01_01")),
                Arguments.of(generateYPaths("2000_00_00"), withPrefix("2000_00_00"))
        );
    }

    private static List<String> withPrefix(String... tableNames) {
        return StreamEx.of(tableNames).map(table -> YT_PATH + table).toList();
    }

    private static YPath generateYPaths(String tableName) {
        return YPath.simple(YT_PATH + tableName);
    }
}
