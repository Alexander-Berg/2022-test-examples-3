package ru.yandex.market.logistics.iris.jobs.queue;

import org.junit.Ignore;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.configuration.LGWExchangeConfiguration;
import ru.yandex.market.logistics.iris.configuration.queue.DbQueueConfiguration;

/**
 * TODO https://st.yandex-team.ru/DELIVERY-8972
 * Тесты на DB Queue не должны быть связаны с каким либо конкретным синком.
 * <p>
 * Необходимо переобдумать схему тестирования
 * и сделать проверки максимально изолированными от последующей логики синков.
 */
@Ignore
@Import({DbQueueConfiguration.class, LGWExchangeConfiguration.class})
public class DbQueueTest extends AbstractContextualTest {

   /* private static final String REQUEST_ID = "TestRequestId";
jobs/queue/DbQueueTest.java
    @Autowired
    private CustomSchedulerJob customSchedulerJob;

    @Autowired
    private SpringQueueInitializer queueInitializer;

    @Autowired
    private SourceRetrievalService sourceRetrievalService;

    @SpyBean
    private ReferenceItemSyncService referenceItemSyncService;

    @Autowired
    private ApplicationContext appContext;

    @Before
    public void eraseCache() {
        reflectiveCacheEvict(sourceRetrievalService);
        queueInitializer.onApplicationEvent(new ContextRefreshedEvent(appContext));
        RequestContextHolder.createContext(Optional.of(REQUEST_ID));
    }

    *//**
     * Sanity-тест. Проверяет happy-path работы db-queue: таблица с интервалами соответствует модели интервала,
     * dq-queue поднялась и позволяет создавать и потреблять задачи одной очереди.
     *//*
    @Test
    @Ignore
    @DatabaseSetup(value = "classpath:fixtures/setup/queue/1.xml")
    public void produceAndConsumeTask() {
        FulfillmentService fs = new FulfillmentService();
        fs.setMarketDeliveryServiceId(1);

        FulfillmentService fs2 = new FulfillmentService();
        fs2.setMarketDeliveryServiceId(2);

        doReturn(Lists.newArrayList(fs, fs2)).when(deliveryExportClient).getFulfillmentService();
        int batchSize = QueueType.REFERENCE_SYNC.getBatchSize();
        when(dataExchangeService.getReferenceItems(batchSize, 0, null, new Partner(1)))
            .thenReturn(Collections.emptyList());

        executorWithRequestContext(customSchedulerJob::run).doJob(null);
        reflectiveQueueTrigger(QueueType.REFERENCE_SYNC, queueInitializer);
        LoopingQueueItemPayload testPayload =
            new LoopingQueueItemPayload(REQUEST_ID,
                QueueType.REFERENCE_SYNC.getBatchSize(),
                new Source("1", SourceType.WAREHOUSE));
        verify(dataExchangeService, timeout(1000L).times(1))
            .getReferenceItems(QueueType.REFERENCE_SYNC.getBatchSize(), 0, null, new Partner(1L));

        verify(referenceItemSyncService, timeout(1000L).times(1)).processPayload(testPayload);
    }

    *//**
     * Проверяет, что, если при обработке задачи из очереди произойдет исключение,
     * то задача останется в очереди с увеличенным количеством попыток
     *//*
    @Test
    @Ignore
    @DatabaseSetup(value = "classpath:fixtures/setup/queue/2.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/queue/2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void consumeFailAndReschedule() {
        mockDeliveryExportClient(createMockSingleService());
        mockDataExchangeClientWithException();

        LoopingQueueItemPayload testPayload =
            new LoopingQueueItemPayload(REQUEST_ID,
                QueueType.REFERENCE_SYNC.getBatchSize(),
                new Source("1", SourceType.WAREHOUSE));


        verify(dataExchangeService, timeout(1000L))
            .getReferenceItems(eq(QueueType.REFERENCE_SYNC.getBatchSize()), eq(0), eq(null), eq(new Partner(1)));
        verify(referenceItemSyncService, timeout(1000L)).processPayload(testPayload);
    }

    *//**
     * Проверяет, что синхронизация вгх, сроков годности и шк будет происходить до тех пор,
     * пока сервис обмена отдает непустые данные.
     *//*
    @Test
    @Ignore
    @DatabaseSetup(value = "classpath:fixtures/setup/queue/3.xml")
    public void consumeReferencesWhileThereIsAnything() {
        mockDeliveryExportClient(createMockSingleService());
        Partner partner = new Partner(1L);
        int batchSize = QueueType.REFERENCE_SYNC.getBatchSize();
        List<ItemReference> mockReferences = createMockReferences(10);
        when(dataExchangeService.getReferenceItems(batchSize, 0, null, partner))
            .thenReturn(mockReferences.subList(0, 4));
        when(dataExchangeService.getReferenceItems(batchSize, batchSize, null, partner))
            .thenReturn(mockReferences.subList(4, 10));
        when(dataExchangeService.getReferenceItems(batchSize, batchSize + batchSize, null, partner))
            .thenReturn(Collections.emptyList());

        reflectiveQueueTrigger(QueueType.REFERENCE_SYNC, queueInitializer);

        verify(dataExchangeService, timeout(1000L))
            .getReferenceItems(batchSize, 0, null, new Partner(1));
        verify(dataExchangeService, timeout(1000L))
            .getReferenceItems(batchSize, batchSize, null, new Partner(1));
        verify(dataExchangeService, timeout(1000L))
            .getReferenceItems(batchSize, batchSize + batchSize, null, new Partner(1));
    }

    private List<FulfillmentService> createMockSingleService() {
        FulfillmentService fs = new FulfillmentService();
        fs.setMarketDeliveryServiceId(1);
        return Collections.singletonList(fs);
    }

    private void mockDeliveryExportClient(List<FulfillmentService> mockServices) {
        doReturn(mockServices).when(deliveryExportClient).getFulfillmentService();
    }

    private void mockDataExchangeClientWithException() {
        doThrow(new RuntimeException("Mock exception")).when(dataExchangeService)
            .getReferenceItems(anyInt(), anyInt(), isNull(), any(Partner.class));
    }

    private List<ItemReference> createMockReferences(int count) {
        return IntStream.range(1, count + 1)
            .mapToObj(i -> new ItemReference(new UnitId(i + "", (long) i, "SKU"), null, 100 + i, null))
            .collect(Collectors.toList());
    }*/
}
