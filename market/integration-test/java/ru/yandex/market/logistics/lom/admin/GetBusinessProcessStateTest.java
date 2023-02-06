package ru.yandex.market.logistics.lom.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.admin.enums.AdminEntityType;
import ru.yandex.market.logistics.lom.admin.filter.AdminBusinessProcessStateSearchFilterDto;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.enums.EntityType;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateEntityIdYdb;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateYdb;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPartnerIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.ydb.BusinessProcessStateYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateEntityIdTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateTableDescription;
import ru.yandex.market.logistics.lom.utils.TestUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.lom.utils.TestUtils.mockMdsS3ClientDownload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Получение состояния бизнес-процессов")
@DatabaseSetup("/controller/admin/business_process/prepare.xml")
class GetBusinessProcessStateTest extends AbstractContextualYdbTest {

    @Autowired
    private BusinessProcessStateTableDescription businessProcessStateTableDescription;

    @Autowired
    private BusinessProcessStateEntityIdTableDescription businessProcessStateEntityIdTableDescription;

    @Autowired
    private BusinessProcessStateYdbRepository businessProcessStateYdbRepository;

    @Autowired
    private MdsS3Client mdsS3Client;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2019-11-02T12:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        prepareProcessesInYdb();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск бизнес-процессов")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminBusinessProcessStateSearchFilterDto filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/business-processes")
            .params(TestUtils.toParamWithCollections(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgumentWithValidationError")
    @DisplayName("Поиск бизнес-процессов c невалидным фильтром")
    void searchWithValidationError(
        @SuppressWarnings("unused") String displayName,
        AdminBusinessProcessStateSearchFilterDto filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/business-processes")
            .params(TestUtils.toParamWithCollections(filter)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminBusinessProcessStateSearchFilterDto(),
                "controller/admin/business_process/all_from_pg.json"
            ),
            Arguments.of(
                "По идентификатору заказа",
                new AdminBusinessProcessStateSearchFilterDto().setOrderId(1L),
                "controller/admin/business_process/id_1234_and_10_ydb.json"
            ),
            Arguments.of(
                "По идентификатору сегмента",
                new AdminBusinessProcessStateSearchFilterDto().setWaybillSegmentId(11L),
                "controller/admin/business_process/id_10.json"
            ),
            Arguments.of(
                "По идентификатору заказа, у которого нет процессов в Postgres",
                new AdminBusinessProcessStateSearchFilterDto().setOrderId(12345L),
                "controller/admin/business_process/id_12_ydb.json"
            ),
            Arguments.of(
                "По статусу",
                new AdminBusinessProcessStateSearchFilterDto().setStatuses(EnumSet.of(BusinessProcessStatus.ENQUEUED)),
                "controller/admin/business_process/id_456.json"
            ),
            Arguments.of(
                "По типу очереди",
                new AdminBusinessProcessStateSearchFilterDto().setQueueTypes(EnumSet.of(QueueType.GET_ORDER_LABEL)),
                "controller/admin/business_process/id_4.json"
            ),
            Arguments.of(
                "По идентификатору отгрузки с активной заявкой",
                new AdminBusinessProcessStateSearchFilterDto().setShipmentId(1L),
                "controller/admin/business_process/id_7_and_14_ydb.json"
            ),
            Arguments.of(
                "По идентификатору отгрузки без активных заявок",
                new AdminBusinessProcessStateSearchFilterDto().setShipmentId(2L),
                "controller/admin/business_process/empty.json"
            ),
            Arguments.of(
                "По идентификатору реестра",
                new AdminBusinessProcessStateSearchFilterDto().setRegistryId(4L),
                "controller/admin/business_process/id_8_and_13_ydb.json"
            ),
            Arguments.of(
                "По дате создания, нижняя граница интервала",
                new AdminBusinessProcessStateSearchFilterDto().setCreatedFrom(LocalDate.parse("2019-11-08")),
                "controller/admin/business_process/id_8_10.json"
            ),
            Arguments.of(
                "По дате создания, верхняя граница интервала",
                new AdminBusinessProcessStateSearchFilterDto().setCreatedTo(LocalDate.parse("2019-11-04")),
                "controller/admin/business_process/id_1234.json"
            ),
            Arguments.of(
                "По дате обновления, нижняя граница интервала",
                new AdminBusinessProcessStateSearchFilterDto().setUpdatedFrom(LocalDate.parse("2019-11-08")),
                "controller/admin/business_process/id_8_10.json"
            ),
            Arguments.of(
                "По дате обновления, верхняя граница интервала",
                new AdminBusinessProcessStateSearchFilterDto().setUpdatedTo(LocalDate.parse("2019-11-04")),
                "controller/admin/business_process/id_1234.json"
            ),
            Arguments.of(
                "По комментарию",
                new AdminBusinessProcessStateSearchFilterDto().setComment("cannot"),
                "controller/admin/business_process/id_3.json"
            ),
            Arguments.of(
                "По типу сущности",
                new AdminBusinessProcessStateSearchFilterDto().setEntityType(AdminEntityType.REGISTRY),
                "controller/admin/business_process/all_from_pg.json"
            ),
            Arguments.of(
                "По идентификатору сущности",
                new AdminBusinessProcessStateSearchFilterDto().setEntityId(4L),
                "controller/admin/business_process/all_from_pg.json"
            ),
            Arguments.of(
                "По идентификатору и типу сущности",
                new AdminBusinessProcessStateSearchFilterDto()
                    .setEntityType(AdminEntityType.REGISTRY)
                    .setEntityId(4L),
                "controller/admin/business_process/id_8_and_13_ydb.json"
            ),
            Arguments.of(
                "По всем параметрам",
                new AdminBusinessProcessStateSearchFilterDto()
                    .setOrderId(1L)
                    .setCreatedFrom(LocalDate.parse("2019-11-03"))
                    .setCreatedTo(LocalDate.parse("2019-11-04"))
                    .setUpdatedFrom(LocalDate.parse("2019-11-04"))
                    .setUpdatedTo(LocalDate.parse("2019-11-05"))
                    .setStatuses(EnumSet.of(BusinessProcessStatus.ENQUEUED))
                    .setQueueTypes(EnumSet.of(QueueType.GET_ORDER_LABEL))
                    .setEntityId(1L)
                    .setEntityType(AdminEntityType.PARTNER)
                    .setBusinessProcessStateParentId(1L),
                "controller/admin/business_process/id_4.json"
            ),
            Arguments.of(
                "По идентификатору родительского бизнес-процесса",
                new AdminBusinessProcessStateSearchFilterDto().setBusinessProcessStateParentId(1L),
                "controller/admin/business_process/id_24_and_10_ydb.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> searchArgumentWithValidationError() {
        Set<BusinessProcessStatus> statusesWithNull = new HashSet<>();
        statusesWithNull.add(null);
        statusesWithNull.add(BusinessProcessStatus.ASYNC_REQUEST_SENT);
        Set<QueueType> queueTypesWithNull = new HashSet<>();
        queueTypesWithNull.add(null);
        queueTypesWithNull.add(QueueType.GET_ACCEPTANCE_CERTIFICATE);
        return Stream.of(
            Triple.of(
                "С невалидными статусами",
                new AdminBusinessProcessStateSearchFilterDto().setStatuses(statusesWithNull),
                "controller/admin/business_process/not_valid_statuses_filter_response.json"
            ),
            Triple.of(
                "С невалидными типами очередей",
                new AdminBusinessProcessStateSearchFilterDto().setQueueTypes(queueTypesWithNull),
                "controller/admin/business_process/not_valid_queue_type_filter_response.json"
            )
        ).map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Test
    @DisplayName("Получение деталей бизнес-процесса")
    void getOne() throws Exception {
        clock.setFixed(Instant.parse("2019-11-03T13:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        mockMvc.perform(get("/admin/business-processes/3"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/business_process/id_3_detail.json"));
    }

    @Test
    @DisplayName("Получение деталей бизнес-процесса с неактивной кнопкой")
    void getOneRetryDisabled() throws Exception {
        mockMvc.perform(get("/admin/business-processes/2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/business_process/id_2_detail.json"));
    }

    @Test
    @DisplayName("Получение деталей бизнес-процесса из YDB")
    void getOneFromYDB() throws Exception {
        BusinessProcessStateYdb businessProcessStateYdb = getBusinessProcessStateYdb(1000L);

        businessProcessStateYdbRepository.save(businessProcessStateYdb);

        mockMvc.perform(get("/admin/business-processes/0"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/business_process/ydb_detail.json"));
    }

    @Test
    @DisplayName("Получение деталей несуществующего бизнес-процесса")
    void getOneNotFound() throws Exception {
        mockMvc.perform(get("/admin/business-processes/0"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_PROCESS] with id [0]"));
    }

    @MethodSource
    @SneakyThrows
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск перевыставленных процессов для таски массового перевыставления процессов")
    void searchChildProcessesForMultiplyRetryBusinessProcessTask(
        @SuppressWarnings("unused") String displayName,
        AdminBusinessProcessStateSearchFilterDto filter,
        String expectedJson
    ) {
        mockMdsS3ClientDownload(mdsS3Client, "controller/admin/business_process/multiple_retry.xlsx");
        mockMvc.perform(get("/admin/business-processes")
                .params(TestUtils.toParamWithCollections(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(expectedJson));
    }

    @Nonnull
    private static Stream<Arguments> searchChildProcessesForMultiplyRetryBusinessProcessTask() {
        return Stream.of(
            Arguments.of(
                "Поиск всех перевыставленных процессов",
                searchFilterForMultiplyRetryProcess(),
                "controller/admin/business_process/id_12345.json"
            ),
            Arguments.of(
                "Перевыставленные заданного типа",
                searchFilterForMultiplyRetryProcess()
                    .setQueueTypes(Set.of(QueueType.CREATE_ORDER_EXTERNAL)),
                "controller/admin/business_process/id_15.json"
            ),
            Arguments.of(
                "Перевыставленные указанного статуса",
                searchFilterForMultiplyRetryProcess()
                    .setStatuses(Set.of(BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_FAILED)),
                "controller/admin/business_process/id_3.json"
            )
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка получения файла таски массового перевыставления процессов")
    void searchChildProcessesForMultiplyRetryBusinessProcessTaskFileError() {
        when(mdsS3Client.download(any(ResourceLocation.class), any(ContentConsumer.class)))
            .thenThrow(new RuntimeException("MDS S3 client exception"));

        mockMvc.perform(get("/admin/business-processes")
                .params(TestUtils.toParamWithCollections(searchFilterForMultiplyRetryProcess())))
            .andExpect(status().is5xxServerError())
            .andExpect(errorMessage("MDS S3 client exception"));

    }

    @SneakyThrows
    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Пагинация при получении процессов Postgres + YDB")
    void postgresAndYdbPagination(
        @SuppressWarnings("unused") String displayName,
        int page,
        AdminBusinessProcessStateSearchFilterDto filter,
        String expectedJson
    ) {
        mockMvc.perform(get("/admin/business-processes")
                .params(
                    TestUtils.toParamWithCollections(filter))
                .param("page", String.valueOf(page))
                .param("size", "3")
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent(expectedJson));
    }

    @Nonnull
    private static Stream<Arguments> postgresAndYdbPagination() {
        return Stream.of(
            Arguments.of(
                "PG: Страница 0, пустой фильтр",
                0,
                new AdminBusinessProcessStateSearchFilterDto(),
                "controller/admin/business_process/id_10_9_8.json"
            ),
            Arguments.of(
                "PG: Страница 1, пустой фильтр",
                1,
                new AdminBusinessProcessStateSearchFilterDto(),
                "controller/admin/business_process/id_765.json"
            ),
            Arguments.of(
                "PG + YDB: Страница 0, фильтр по дате создания, нижняя граница интервала, статусу и parentId",
                0,
                new AdminBusinessProcessStateSearchFilterDto()
                    .setBusinessProcessStateParentId(7L)
                    .setCreatedFrom(LocalDate.parse("2019-11-08"))
                    .setStatuses(Set.of(BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_SUCCEEDED)),
                "controller/admin/business_process/id_8_and_13_12_ydb.json"
            ),
            Arguments.of(
                "YDB: Страница 1, фильтр по дате создания, нижняя граница интервала, статусу и parentId",
                1,
                new AdminBusinessProcessStateSearchFilterDto()
                    .setBusinessProcessStateParentId(7L)
                    .setCreatedFrom(LocalDate.parse("2019-11-08"))
                    .setStatuses(Set.of(BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_SUCCEEDED)),
                "controller/admin/business_process/id_11_ydb.json"
            ),
            Arguments.of(
                "PG + YDB: Страница 1, по ид заказа(связанная сущность)",
                1,
                new AdminBusinessProcessStateSearchFilterDto().setOrderId(1L),
                "controller/admin/business_process/id_1_and_10_ydb.json"
            ),
            Arguments.of(
                "YDB: Страница 0, по статусу, типу очереди и сущности",
                0,
                new AdminBusinessProcessStateSearchFilterDto()
                    .setEntityType(AdminEntityType.SHIPMENT_APPLICATION)
                    .setEntityId(51L)
                    .setQueueTypes(Set.of(QueueType.GET_ORDER_LABEL))
                    .setStatuses(Set.of(BusinessProcessStatus.UNPROCESSED)),
                "controller/admin/business_process/id_14_ydb.json"
            ),
            Arguments.of(
                "Данных нет: Страница 100, пустой фильтр",
                100,
                new AdminBusinessProcessStateSearchFilterDto(),
                "controller/admin/business_process/empty.json"
            )
        );
    }

    private void prepareProcessesInYdb() {
        businessProcessStateYdbRepository.saveAll(List.of(
            getBusinessProcessStateYdb(1010L).setId(10L).setParentId(1L),
            getBusinessProcessStateYdb(1011L).setId(11L)
                .setParentId(7L)
                .setCreated(Instant.parse("2019-11-09T12:20:30.00Z"))
                .setStatus(BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_SUCCEEDED)
                .setEntityIds(List.of()),
            getBusinessProcessStateYdb(1012L).setId(12L)
                .setParentId(7L)
                .setCreated(Instant.parse("2019-11-09T12:20:30.00Z"))
                .setStatus(BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_SUCCEEDED)
                .setEntityIds(List.of(
                    new BusinessProcessStateEntityIdYdb()
                        .setEntityType(EntityType.ORDER)
                        .setEntityId(12345L)
                )),
            getBusinessProcessStateYdb(1013L).setId(13L)
                .setParentId(7L)
                .setCreated(Instant.parse("2019-11-09T12:20:30.00Z"))
                .setStatus(BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_SUCCEEDED)
                .setEntityIds(List.of(
                        new BusinessProcessStateEntityIdYdb()
                            .setEntityType(EntityType.REGISTRY)
                            .setEntityId(4L)
                    )
                ),
            getBusinessProcessStateYdb(1014L).setId(14L)
                .setStatus(BusinessProcessStatus.UNPROCESSED)
                .setEntityIds(List.of(
                    new BusinessProcessStateEntityIdYdb()
                        .setEntityType(EntityType.SHIPMENT_APPLICATION)
                        .setEntityId(51L)
                ))
        ));
    }

    @Nonnull
    private static AdminBusinessProcessStateSearchFilterDto searchFilterForMultiplyRetryProcess() {
        return new AdminBusinessProcessStateSearchFilterDto()
            .setBusinessProcessStateParentId(9L);
    }

    @Nonnull
    @SneakyThrows
    private BusinessProcessStateYdb getBusinessProcessStateYdb(long sequenceId) {
        OrderIdPartnerIdPayload payload = PayloadFactory.createOrderIdPartnerIdPayload(1L, 2L, 1L);
        payload.setSequenceId(sequenceId);
        return new BusinessProcessStateYdb()
            .setId(0L)
            .setQueueType(QueueType.GET_ORDER_LABEL)
            .setEntityIds(List.of(
                BusinessProcessStateEntityIdYdb.of(EntityType.ORDER, 1L),
                BusinessProcessStateEntityIdYdb.of(EntityType.PARTNER, 2L)
            ))
            .setSequenceId(sequenceId)
            .setStatus(BusinessProcessStatus.ENQUEUED)
            .setAuthor(
                new OrderHistoryEventAuthor()
                    .setTvmServiceId(222L)
                    .setYandexUid(BigDecimal.ONE)
            )
            .setPayload(objectMapper.writeValueAsString(payload))
            .setMessage("comment")
            .setParentId(777L)
            .setCreated(clock.instant())
            .setUpdated(clock.instant());
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(businessProcessStateTableDescription, businessProcessStateEntityIdTableDescription);
    }
}
