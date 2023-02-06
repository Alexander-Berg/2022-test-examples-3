package ru.yandex.market.logistics.lrm.api.returns;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.lrm.client.model.CreateReturnResponse;
import ru.yandex.market.logistics.lrm.client.model.CreateYaGoCancellationReturnRequest;
import ru.yandex.market.logistics.lrm.client.model.LogisticPoint;
import ru.yandex.market.logistics.lrm.client.model.LogisticPointType;
import ru.yandex.market.logistics.lrm.client.model.OrderItemInfo;
import ru.yandex.market.logistics.lrm.client.model.ValidationError;
import ru.yandex.market.logistics.lrm.client.model.ValidationViolation;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.springframework.http.HttpStatus.ALREADY_REPORTED;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultCancellationBox;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultCancellationItem;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultDimensions;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Создание и коммит невыкупа для заказов Доставки наружу")
class YaGoCancellationRequestTest extends AbstractIntegrationTest {
    private static final Instant DATETIME = Instant.parse("2022-03-02T11:12:13.00Z");

    @BeforeEach
    void setup() {
        clock.setFixed(DATETIME, ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Неуспех: дублируется order_external_id")
    @DatabaseSetup("/database/api/returns/create-cancellation/before/return.xml")
    void duplicateOrderExternalId() {
        CreateReturnResponse response = create(defaultRequest())
            .executeAs(validatedWith(shouldBeCode(ALREADY_REPORTED.value())));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(2L);
    }

    @Test
    @DisplayName("Точка забора невыкупа - ПВЗ")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/ya-go/after/pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnFromPickup() {
        CreateReturnResponse response = create(
            defaultRequest()
                .segments(List.of(sortingCenter(1), sortingCenter(2), pickup()))
        )
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Точка забора невыкупа - СЦ")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/ya-go/after/sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnFromSc() {
        CreateReturnResponse response = create(defaultRequest())
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Возврат едет в дропофф")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/ya-go/after/dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnToDropoff() {
        CreateReturnResponse response = create(
            defaultRequest()
                .segments(List.of(dropoff(), sortingCenter(2)))
        )
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource({
        "requestValidation",
        "itemsValidation",
        "boxesValidation",
    })
    @DisplayName("Валидация запроса")
    void requestValidation(CreateYaGoCancellationReturnRequest request, String field, String message) {
        ValidationError response = create(request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(response.getErrors())
            .containsExactly(
                new ValidationViolation()
                    .field(field)
                    .message(message)
            );
    }

    @Nonnull
    static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(
                defaultRequest().orderExternalId(null),
                "orderExternalId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().goShopPartnerId(null),
                "goShopPartnerId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().goShopId(null),
                "goShopId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().orderItemsInfo(List.of()),
                "orderItemsInfo",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().orderItemsInfo(Collections.nCopies(1001, new OrderItemInfo())),
                "orderItemsInfo",
                "size must be between 1 and 1000"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> itemsValidation() {
        return Stream.of(
            Arguments.of(
                defaultRequest().items(null),
                "items",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().items(List.of()),
                "items",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().items(Collections.nCopies(1001, defaultCancellationItem())),
                "items",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultCancellationItem().supplierId(null))),
                "items[0].supplierId",
                "must not be null"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> boxesValidation() {
        return Stream.of(
            Arguments.of(
                defaultRequest().boxes(null),
                "boxes",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of()),
                "boxes",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().boxes(Collections.nCopies(1001, defaultCancellationBox())),
                "boxes",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().externalId(null))),
                "boxes[0].externalId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().dimensions(null))),
                "boxes[0].dimensions",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().dimensions(defaultDimensions().height(null)))),
                "boxes[0].dimensions.height",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().dimensions(defaultDimensions().width(null)))),
                "boxes[0].dimensions.width",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().dimensions(defaultDimensions().length(null)))),
                "boxes[0].dimensions.length",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().dimensions(defaultDimensions().weight(null)))),
                "boxes[0].dimensions.weight",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static CreateYaGoCancellationReturnRequest defaultRequest() {
        return new CreateYaGoCancellationReturnRequest()
            .orderExternalId("order-external-id")
            .items(List.of(defaultCancellationItem()))
            .boxes(List.of(defaultCancellationBox()))
            .goShopId(0L)
            .goShopPartnerId(100L)
            .segments(defaultSegments());
    }

    @Nonnull
    private static List<LogisticPoint> defaultSegments() {
        return List.of(sortingCenter(1), sortingCenter(2));
    }

    @Nonnull
    private static LogisticPoint sortingCenter(long index) {
        return new LogisticPoint()
            .id(index)
            .partnerId(100 + index)
            .externalId("sc-external-id-" + index)
            .name("sc-name-" + index)
            .type(LogisticPointType.SORTING_CENTER);
    }

    @Nonnull
    private static LogisticPoint dropoff() {
        return new LogisticPoint()
            .id(1L)
            .partnerId(101L)
            .externalId("dropoff-external-id")
            .name("dropoff-name")
            .type(LogisticPointType.DROPOFF);
    }

    @Nonnull
    private static LogisticPoint pickup() {
        return new LogisticPoint()
            .id(3L)
            .partnerId(301L)
            .externalId("pickup-external-id")
            .name("pickup-name")
            .type(LogisticPointType.PICKUP);
    }

    @Nonnull
    private ReturnsApi.CreateAndCommitYaGoCancellationReturnOper create(CreateYaGoCancellationReturnRequest request) {
        return apiClient.returns().createAndCommitYaGoCancellationReturn().body(request);
    }
}
