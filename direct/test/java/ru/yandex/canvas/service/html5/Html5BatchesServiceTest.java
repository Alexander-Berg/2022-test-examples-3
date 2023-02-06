package ru.yandex.canvas.service.html5;

import com.mongodb.client.result.UpdateResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.exceptions.SourceValidationError;
import ru.yandex.canvas.model.html5.Batch;
import ru.yandex.canvas.model.html5.Source;
import ru.yandex.canvas.model.validation.Html5SizeValidator;
import ru.yandex.canvas.repository.html5.BatchesRepository;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.ScreenshooterService;
import ru.yandex.canvas.service.SequenceService;
import ru.yandex.canvas.service.SessionParams;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_BANNER;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_YNDX_FRONTPAGE;
import static ru.yandex.canvas.service.SessionParams.SessionTag.CPM_YNDX_FRONTPAGE;

@RunWith(SpringJUnit4ClassRunner.class)
public class Html5BatchesServiceTest {
    private static final long clientId = 1L;
    private static final String name = "test-batch";

    @Mock
    private SequenceService sequenceService;

    @Mock
    private DirectService directService;

    @Mock
    private BatchesRepository batchesRepository;

    @Mock
    private ScreenshooterService screenshooterService;

    @Mock
    private SessionParams sessionParams;

    @InjectMocks
    private Html5SizeValidator html5SizeValidator;

    //    @InjectMocks
    private Html5BatchesService service;

    @Before
    public void setUp() {
        Mockito.when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_BANNER);
        service = new Html5BatchesService(sequenceService, batchesRepository, null, html5SizeValidator,
                directService);
    }


    @Test(expected = SourceValidationError.class)
    public void createBatchFromSources_defaultProductType_sizeDoesNotBelong_errorIsThrown() {
        Batch batch = service.createBatchFromSources(clientId, name,
                singletonList(new Source().setWidth(1456).setHeight(180)), HTML5_CPM_BANNER, null);
    }

    @Test(expected = SourceValidationError.class)
    public void createBatchFromSources_customProductType_sizeDoesNotBelong_errorIsThrown() {
        when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_YNDX_FRONTPAGE);
        Batch batch = service.createBatchFromSources(clientId, name,
                singletonList(new Source().setWidth(240).setHeight(400)),
                HTML5_CPM_YNDX_FRONTPAGE, null);
    }

    @Test
    public void createBatchFromSource_customProductType_sizeBelongs_success() {
        Batch repoBatch = new Batch();
        when(batchesRepository.createBatch(any()))
                .thenReturn(repoBatch);
        when(sequenceService.getNextCreativeIdsList(1))
                .thenReturn(singletonList(1L));

        when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_YNDX_FRONTPAGE);

        Batch batch = service.createBatchFromSources(clientId, name,
                singletonList(new Source().setWidth(1456).setHeight(180).setScreenshotIsDone(true).setScreenshotUrl(
                        "url")),
                HTML5_CPM_YNDX_FRONTPAGE, null);
        assertThat(batch).isSameAs(repoBatch);
    }

    @Test
    public void renameBatch_productTypeGeneralIsConvertedToNull() {
        Batch batch = new Batch();
        UpdateResult result = mock(UpdateResult.class);
        when(result.getMatchedCount()).thenReturn(1L);

        when(batchesRepository.updateBatchName(anyString(), eq(clientId), eq(null), anyString()))
                .thenReturn(result);

        UpdateResult actual = service.updateBatchName("1", clientId, HTML5_CPM_BANNER, "new name");

        verify(batchesRepository).updateBatchName(anyString(), eq(clientId), eq(null), anyString());
    }

    @Test
    public void getBatchWithDefaultProductType_repositoryCalledWithoutType() {
        when(batchesRepository.getBatchById(any(), any(), eq(null))).thenReturn(new Batch());

        Batch result = service.getBatch("1", clientId, HTML5_CPM_BANNER);

        verify(batchesRepository, atLeastOnce()).getBatchById(any(), any(), eq(null));
    }

    @Test
    public void getBatchWithCustomProductType_repositoryCalledWithType() {
        when(batchesRepository.getBatchById(any(), any(), eq(CPM_YNDX_FRONTPAGE)))
                .thenReturn(new Batch());

        Batch result = service.getBatch("1", clientId, HTML5_CPM_YNDX_FRONTPAGE);

        verify(batchesRepository, atLeastOnce()).getBatchById(any(), any(), eq(CPM_YNDX_FRONTPAGE));
    }
}
