package ru.yandex.market.logistics.lrm.api.returns;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi.CreateReturnOper;
import ru.yandex.market.logistics.lrm.client.model.CreateReturnRequest;
import ru.yandex.market.logistics.lrm.client.model.CreateReturnResponse;
import ru.yandex.market.logistics.lrm.client.model.OrderItemInfo;
import ru.yandex.market.logistics.lrm.client.model.ReturnBoxRequest;
import ru.yandex.market.logistics.lrm.client.model.ReturnCourier;
import ru.yandex.market.logistics.lrm.client.model.ReturnReasonType;
import ru.yandex.market.logistics.lrm.client.model.ReturnSource;
import ru.yandex.market.logistics.lrm.client.model.ReturnSubreason;
import ru.yandex.market.logistics.lrm.client.model.ValidationError;
import ru.yandex.market.logistics.lrm.client.model.ValidationViolation;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.model.ReturnEntityEnrichEnqueuedMeta;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultBox;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultItem;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultOrderItemInfo;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Создание возврата")
@ParametersAreNonnullByDefault
class ReturnCreateTest extends AbstractIntegrationYdbTest {

    private static final long RETURN_ID = 1L;
    private static final long LOGISTIC_POINT_FROM_ID = 1234L;
    private static final Instant DATETIME = Instant.parse("2022-03-02T11:12:13.00Z");

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private EntityMetaTableDescription entityMetaTable;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTable);
    }

    @BeforeEach
    void setup() {
        clock.setFixed(DATETIME, ZoneId.systemDefault());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Минимальный запрос. Нет коробок, не ставим таску обогащения возврата")
    @ExpectedDatabase(
        value = "/database/api/returns/create/after/minimal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void minimal() {
        CreateReturnResponse response = createReturn(defaultRequest().boxes(null))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertCreatedId(response);
        assertEmptyYdb();
    }

    @Test
    @DisplayName("Все поля")
    @ExpectedDatabase(
        value = "/database/api/returns/create/after/all_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void allFields() {
        OrderItemInfo orderItemInfo1 = new OrderItemInfo()
            .supplierId(765L)
            .vendorCode("item-vendor-code-1")
            .instances(List.of(Map.of(
                "CIS", "item-cis",
                "UIT", "item-uit"
            )));
        OrderItemInfo orderItemInfo2 = new OrderItemInfo()
            .supplierId(766L)
            .vendorCode("item-vendor-code-2")
            .instances(List.of());
        CreateReturnRequest request = defaultRequest()
            .source(ReturnSource.COURIER)
            .externalId("return-external-id")
            .courier(defaultCourier())
            .boxes(List.of(defaultBox()))
            .items(List.of(
                defaultItem()
                    .boxExternalId("box-external-id")
                    .instances(Map.of(
                        "CIS", "item-cis"
                    ))
                    .returnReason("return-reason")
                    .returnSubreason(ReturnSubreason.WRONG_ITEM)
                    .returnReasonType(ReturnReasonType.WRONG_ITEM)
            ))
            .orderItemsInfo(List.of(orderItemInfo1, orderItemInfo2));
        CreateReturnResponse response = createReturn(request)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertCreatedId(response);
        assertYdb();
    }

    @Test
    @DisplayName("Полный возврат")
    @ExpectedDatabase(
        value = "/database/api/returns/create/after/full_return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void fullReturn() {
        CreateReturnResponse response = createReturn(defaultRequest().full(true))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertCreatedId(response);
        assertYdb();
    }

    @Test
    @DisplayName("Клиентский возврат")
    @ExpectedDatabase(
        value = "/database/api/returns/create/after/client_return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void clientReturn() {
        CreateReturnResponse response = createReturn(
            defaultRequest()
                .source(ReturnSource.CLIENT)
                .orderItemsInfo(List.of())
        )
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertCreatedId(response);
        assertYdb();
    }

    @Test
    @DisplayName("Возврат из курьерки с указанием partnerFromId")
    @ExpectedDatabase(
        value = "/database/api/returns/create/after/courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @SneakyThrows
    void courierPartnerFromId() {
        long partnerFromId = 123;

        try (var ignored = mockGetPartnerWarehouse(partnerFromId)) {
            CreateReturnResponse response = createReturn(
                defaultRequest()
                    .source(ReturnSource.COURIER)
                    .logisticPointFromId(null)
                    .partnerFromId(partnerFromId)
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            assertCreatedId(response);
            assertYdb();
        }
    }

    @Test
    @DisplayName("Невыкуп без указания logisticPointId")
    @ExpectedDatabase(
        value = "/database/api/returns/create/after/cancellation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancellationWithoutLogisticPointId() {
        CreateReturnResponse response = createReturn(
            defaultRequest()
                .source(ReturnSource.CANCELLATION)
                .logisticPointFromId(null)
        )
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertCreatedId(response);
        assertYdb();
    }

    @Test
    @DisplayName("Создание и оформление одним запросом")
    @ExpectedDatabase(
        value = "/database/api/returns/create/after/commit.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createAndCommit() {
        CreateReturnResponse response = createReturn(defaultRequest())
            .commitQuery(true)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertCreatedId(response);
        assertYdb();
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource
    @DisplayName("Валидация запроса")
    @DatabaseSetup("/database/api/returns/create/before/existing_return.xml")
    void requestValidation(CreateReturnRequest request, String field, String message) {
        ValidationError response = createReturn(request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(response.getErrors())
            .containsExactly(
                new ValidationViolation()
                    .field(field)
                    .message(message)
            );
        assertEmptyYdb();
    }

    @Nonnull
    public static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(defaultRequest().source(null), "source", "must not be null"),
            Arguments.of(defaultRequest().orderExternalId(null), "orderExternalId", "must not be null"),
            Arguments.of(
                defaultRequest().logisticPointFromId(null),
                null,
                "Exactly one of logisticPointFromId and partnerFromId must be specified"
            ),
            Arguments.of(
                defaultRequest().partnerFromId(123L),
                null,
                "Exactly one of logisticPointFromId and partnerFromId must be specified"
            ),
            Arguments.of(defaultRequest().boxes(List.of()), "boxes", "size must be between 1 and 1000"),
            Arguments.of(
                defaultRequest().boxes(Collections.nCopies(1001, defaultBox())),
                "boxes",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().boxes(Collections.nCopies(3, defaultBox())),
                "boxes",
                "non-unique externalId: [box-external-id]"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultBox().externalId(null))),
                "boxes[0].externalId",
                "must not be null"
            ),
            Arguments.of(defaultRequest().items(null), "items", "must not be null"),
            Arguments.of(defaultRequest().items(List.of()), "items", "size must be between 1 and 1000"),
            Arguments.of(
                defaultRequest().items(Collections.nCopies(1001, defaultItem())),
                "items",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().supplierId(null))),
                "items[0].supplierId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().vendorCode(null))),
                "items[0].vendorCode",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().orderItemsInfo(Collections.nCopies(1001, defaultOrderItemInfo())),
                "orderItemsInfo",
                "size must be between 0 and 1000"
            ),
            Arguments.of(
                defaultRequest().orderItemsInfo(List.of(
                    defaultOrderItemInfo().instances(Collections.nCopies(1001, Map.of()))
                )),
                "orderItemsInfo[0].instances",
                "size must be between 0 and 1000"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem(), defaultItem().boxExternalId("invalid-box-id"))),
                "items[1].boxExternalId",
                "non-existent box id"
            ),
            Arguments.of(
                defaultRequest().courier(defaultCourier().carNumber(null)),
                "courier.carNumber",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().courier(defaultCourier().name(null)),
                "courier.name",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().courier(defaultCourier().uid(null)),
                "courier.uid",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(new ReturnBoxRequest().externalId("existing-box-external-id"))),
                "boxes[0].externalId",
                "already used in returns: [1]"
            )
        );
    }

    @Nonnull
    private AutoCloseable mockGetPartnerWarehouse(long partnerFromId) {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(partnerFromId))
            .partnerTypes(Set.of(PartnerType.SORTING_CENTER))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(filter))
            .thenReturn(List.of(LogisticsPointResponse.newBuilder().id(LOGISTIC_POINT_FROM_ID).build()));
        return () -> verify(lmsClient).getLogisticsPoints(filter);
    }

    private void assertCreatedId(@Nullable CreateReturnResponse response) {
        softly.assertThat(response)
            .isNotNull()
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(RETURN_ID);
    }

    @Nonnull
    private static CreateReturnRequest defaultRequest() {
        return new CreateReturnRequest()
            .source(ReturnSource.PICKUP_POINT)
            .orderExternalId("order-external-id")
            .logisticPointFromId(LOGISTIC_POINT_FROM_ID)
            .boxes(List.of(defaultBox()))
            .items(List.of(defaultItem()));
    }

    @Nonnull
    private static ReturnCourier defaultCourier() {
        return new ReturnCourier()
            .carNumber("courier-car-number")
            .name("courier-name")
            .uid("courier-uid");
    }

    @Nonnull
    private CreateReturnOper createReturn(CreateReturnRequest request) {
        return apiClient.returns().createReturn().body(request);
    }

    private void assertYdb() {
        softly.assertThat(
            getEntityMetaRecord(RETURN_1_HASH, "RETURN", 1L, "return-entity-enrich-enqueued")
                .map(EntityMetaTableDescription.EntityMetaRecord::value)
                .map(v -> readValue(v, ReturnEntityEnrichEnqueuedMeta.class))
        ).contains(
            ReturnEntityEnrichEnqueuedMeta.builder()
                .datetime(DATETIME)
                .build()
        );
    }

    private void assertEmptyYdb() {
        softly.assertThat(getEntityMetaRecord(RETURN_1_HASH, "RETURN", 1L, "return-entity-enrich-enqueued")).isEmpty();
    }
}
