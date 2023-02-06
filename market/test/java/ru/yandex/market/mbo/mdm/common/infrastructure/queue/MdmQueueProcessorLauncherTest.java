package ru.yandex.market.mbo.mdm.common.infrastructure.queue;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendReferenceItemQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuToRefreshProcessingService;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessSskuToRefreshQueueService;
import ru.yandex.market.mbo.mdm.common.service.queue.UploadReferenceItemsToLogbrokerService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author albina-gima
 * @date 7/7/21
 */
public class MdmQueueProcessorLauncherTest extends MdmBaseDbTestClass {
    private static final String SEND_REFERENCE_ITEM_QUEUE_PROCESSOR =
        "SendReferenceItemQueueProcessor";
    private static final String SSKU_TO_REFRESH_QUEUE_PROCESSOR =
        "SskuToRefreshQueueProcessor";

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private StorageKeyValueService keyValueService;

    private UploadReferenceItemsToLogbrokerService uploadReferenceItemsToLogbrokerService;
    private ProcessSskuToRefreshQueueService sskuToRefreshQueueService;

    private MdmQueueProcessor uploadReferenceItemsToLogbrokerQProcessor;
    private MdmQueueProcessor sskuToRefreshQProcessor;

    private MdmQueueProcessorRegistry mdmQueueProcessorRegistry;

    private MdmQueueProcessorLauncher queueProcessorLauncher;

    @Before
    public void setUp() {
        uploadReferenceItemsToLogbrokerQProcessor = initSendReferenceItemQueueProcessor();
        sskuToRefreshQProcessor = initSskuToRefreshQueueProcessor();

        mdmQueueProcessorRegistry = new MdmQueueProcessorRegistry(List.of(
            uploadReferenceItemsToLogbrokerQProcessor,
            sskuToRefreshQProcessor
        ));

        queueProcessorLauncher = new MdmQueueProcessorLauncher(mdmQueueProcessorRegistry, keyValueService);
    }

    @Test
    public void dummyTestToCheckThatQProcessorIsLaunching() {
        keyValueService.putValue(MdmProperties.QUEUE_PROCESSORS_TO_LAUNCH,
            List.of(
                SEND_REFERENCE_ITEM_QUEUE_PROCESSOR,
                SSKU_TO_REFRESH_QUEUE_PROCESSOR
            ));

        Assertions.assertThat(queueProcessorLauncher.runAllQProcessors()).isTrue();
    }

    private MdmQueueProcessor initSendReferenceItemQueueProcessor() {
        uploadReferenceItemsToLogbrokerService = new UploadReferenceItemsToLogbrokerService(
            new ReferenceItemRepositoryMock(),
            Mockito.mock(SendReferenceItemQRepository.class),
            new MdmLogbrokerServiceMock(),
            keyValueService
        );
        return new MdmQueueProcessorImpl(SEND_REFERENCE_ITEM_QUEUE_PROCESSOR, uploadReferenceItemsToLogbrokerService);
    }

    private MdmQueueProcessor initSskuToRefreshQueueProcessor() {
        sskuToRefreshQueueService = new ProcessSskuToRefreshQueueService(
            Mockito.mock(SskuToRefreshRepository.class),
            keyValueService,
            Mockito.mock(MdmSskuGroupManager.class),
            Mockito.mock(SskuToRefreshProcessingService.class),
            transactionTemplate
        );

        return new MdmQueueProcessorImpl(SSKU_TO_REFRESH_QUEUE_PROCESSOR, sskuToRefreshQueueService);
    }
}
