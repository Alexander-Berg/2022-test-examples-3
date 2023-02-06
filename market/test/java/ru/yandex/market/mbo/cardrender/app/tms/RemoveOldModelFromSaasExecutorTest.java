package ru.yandex.market.mbo.cardrender.app.tms;

import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.cardrender.app.BaseTest;
import ru.yandex.market.mbo.cardrender.app.model.saas.DeleteModelSaasRow;
import ru.yandex.market.mbo.cardrender.app.repository.DeleteModelLogRepository;
import ru.yandex.market.mbo.cardrender.app.service.SaasPushService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

/**
 * @author apluhin
 * @created 1/12/22
 */
public class RemoveOldModelFromSaasExecutorTest extends BaseTest {

    @Autowired
    private DeleteModelLogRepository deleteModelLogRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    private SaasPushService saasPushService;
    private RemoveOldModelFromSaasExecutor removeOldModelFromSaasExecutor;

    @Before
    public void setUp() throws Exception {
        saasPushService = Mockito.mock(SaasPushService.class);
        removeOldModelFromSaasExecutor = new RemoveOldModelFromSaasExecutor(
                storageKeyValueService,
                saasPushService,
                deleteModelLogRepository
        );
    }

    @Test
    public void testDeleteAfterTtl() throws Exception {
        DeleteModelSaasRow forDelete = DeleteModelSaasRow.fullDelete(4L, LocalDateTime.now().minusDays(8));
        deleteModelLogRepository.insertBatch(
                DeleteModelSaasRow.fullDelete(1L, LocalDateTime.now()),
                DeleteModelSaasRow.fullDelete(2L, LocalDateTime.now().minusHours(1)),
                DeleteModelSaasRow.fullDelete(3L, LocalDateTime.now().minusDays(1)),
                forDelete);
        Assertions.assertThat(deleteModelLogRepository.findAll().size()).isEqualTo(4);
        removeOldModelFromSaasExecutor.doRealJob(null);

        ArgumentCaptor<List<DeleteModelSaasRow>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(saasPushService).sendDelete(captor.capture());
        Assertions.assertThat(deleteModelLogRepository.findAll().stream().map(DeleteModelSaasRow::getModelId))
                .containsExactlyInAnyOrder(1L, 2L, 3L);
        Assertions.assertThat(captor.getValue()).containsExactlyInAnyOrder(forDelete);
    }
}
