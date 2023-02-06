package ru.yandex.market.abo.tms.indexer;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.indexer.Generation;
import ru.yandex.market.abo.core.indexer.GenerationService;
import ru.yandex.market.abo.tms.indexer.listener.GenerationListener;
import ru.yandex.market.common.report.indexer.IdxAPI;
import ru.yandex.market.common.report.indexer.model.IdxGeneration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GenerationUpdateExecutorTest {
    private static final long LOCAL_GEN_ID = 1;

    @InjectMocks
    GenerationUpdateExecutor generationUpdateExecutor;
    @Mock
    IdxAPI idxApiService;
    @Mock
    GenerationService generationService;
    @Mock
    Generation localGeneration;
    @Mock
    GenerationListener generationListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        generationUpdateExecutor.setGenerationListeners(Collections.singletonList(generationListener));
        when(generationService.loadLastReleaseGeneration()).thenReturn(localGeneration);
        when(localGeneration.getOriginalId()).thenReturn(LOCAL_GEN_ID);
    }

    @Test
    void testUpdateGenerations() throws Exception {
        long newGenId = LOCAL_GEN_ID + 1;
        IdxGeneration newIdxGen = createIdxGeneration(newGenId);
        when(idxApiService.findLastGenerationId(true)).thenReturn(newGenId);
        when(idxApiService.findGenerations(LOCAL_GEN_ID)).thenReturn(Collections.singletonList(newIdxGen));

        generationUpdateExecutor.doRealJob(null);

        verify(idxApiService).findGenerations(LOCAL_GEN_ID);
        verify(generationService).loadGeneration(newGenId);
        verify(generationListener).notify(any());
    }

    private static IdxGeneration createIdxGeneration(long id) {
        IdxGeneration idxGen = new IdxGeneration();
        idxGen.setId(id);
        return idxGen;
    }
}
