package ru.yandex.market.logistics.lrm.api.returns;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi.CreateClientCourierReturnOper;
import ru.yandex.market.logistics.lrm.client.model.CourierInterval;
import ru.yandex.market.logistics.lrm.client.model.CourierReturnAddress;
import ru.yandex.market.logistics.lrm.client.model.CourierReturnClient;
import ru.yandex.market.logistics.lrm.client.model.CourierReturnItem;
import ru.yandex.market.logistics.lrm.client.model.CreateClientCourierReturnRequest;
import ru.yandex.market.logistics.lrm.client.model.CreateReturnResponse;
import ru.yandex.market.logistics.lrm.client.model.Dimensions;
import ru.yandex.market.logistics.lrm.client.model.OrderItemInfo;
import ru.yandex.market.logistics.lrm.client.model.ReturnReasonType;
import ru.yandex.market.logistics.lrm.client.model.ReturnSubreason;
import ru.yandex.market.logistics.lrm.client.model.ValidationError;
import ru.yandex.market.logistics.lrm.client.model.ValidationViolation;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientItemMeta;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta.Address;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta.Client;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta.Interval;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

class ClientCourierReturnCreateTest extends AbstractIntegrationYdbTest {

    @Autowired
    private EntityMetaTableDescription entityMetaTable;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTable);
    }

    @Test
    @DisplayName("Минимальный запрос")
    @ExpectedDatabase(
        value = "/database/api/returns/create-client-courier/after/minimal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void minimal() {
        CreateReturnResponse response = create(defaultRequest())
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);

        softly.assertThat(getReturnMeta()).contains(
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .client(
                    Client.builder().build()
                )
                .address(
                    Address.builder().build()
                )
                .interval(
                    Interval.builder()
                        .dateFrom(LocalDate.of(2022, 3, 4))
                        .timeFrom(LocalTime.of(10, 11))
                        .dateTo(LocalDate.of(2022, 3, 4))
                        .timeTo(LocalTime.of(11, 12))
                        .build()
                )
                .build()
        );

        softly.assertThat(getItemMeta()).contains(
            CourierClientItemMeta.builder()
                .name("item-name")
                .dimensions(
                    CourierClientItemMeta.Dimensions.builder()
                        .weight(100L)
                        .length(200L)
                        .width(300L)
                        .height(400L)
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Поля товара")
    @ExpectedDatabase(
        value = "/database/api/returns/create-client-courier/after/item.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void itemFields() {
        CreateClientCourierReturnRequest request = defaultRequest()
            .items(List.of(
                defaultItem()
                    .returnSubreason(ReturnSubreason.DAMAGED)
                    .returnReasonType(ReturnReasonType.BAD_QUALITY)
                    .categoryName("item-category-name")
                    .description("item-description")
                    .previewPhotoUrl("item-preview-photo")
                    .itemDetailsUrl("item-details-url")
                    .clientPhotoUrls(List.of("item-client-photo"))
                    .buyerPrice(new BigDecimal("12.34"))
            ));
        CreateReturnResponse response = create(request)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);

        softly.assertThat(getItemMeta()).contains(
            CourierClientItemMeta.builder()
                .name("item-name")
                .categoryName("item-category-name")
                .description("item-description")
                .previewPhotoUrl("item-preview-photo")
                .itemDetailsUrl("item-details-url")
                .clientPhotoUrls(List.of("item-client-photo"))
                .dimensions(
                    CourierClientItemMeta.Dimensions.builder()
                        .weight(100L)
                        .length(200L)
                        .width(300L)
                        .height(400L)
                        .build()
                )
                .buyerPrice(new BigDecimal("12.34"))
                .build()
        );
    }

    @Test
    @DisplayName("Поля возврата")
    @ExpectedDatabase(
        value = "/database/api/returns/create-client-courier/after/return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnFields() {
        CreateClientCourierReturnRequest request = defaultRequest()
            .orderItemsInfo(List.of(
                new OrderItemInfo()
                    .supplierId(300L)
                    .vendorCode("item-vendor-code")
                    .instances(List.of(Map.of("CIS", "876IUYkjh")))
            ))
            .client(
                defaultClient()
                    .fullName("client-full-name")
                    .phone("client-phone")
                    .email("client-email")
                    .personalFullNameId("personal-client-full-name-id")
                    .personalPhoneId("personal-client-phone-id")
                    .personalEmailId("personal-client-email-id")
            )
            .address(
                defaultAddress()
                    .city("address-city")
                    .street("address-street")
                    .house("address-house")
                    .entrance("address-entrance")
                    .apartment("address-apartment")
                    .floor("address-floor")
                    .entryPhone("address-entry-phone")
                    .lat(new BigDecimal("12.34"))
                    .lon(new BigDecimal("56.78"))
                    .comment("address-comment")
                    .personalAddressId("personal-address-id")
                    .personalGpsCoordId("personal-gps-coord-id")
            );

        CreateReturnResponse response = create(request)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);

        softly.assertThat(getReturnMeta()).contains(
            CourierClientReturnMeta.builder()
                .deliveryServiceId(100L)
                .client(
                    Client.builder()
                        .fullName("client-full-name")
                        .phone("client-phone")
                        .email("client-email")
                        .personalFullNameId("personal-client-full-name-id")
                        .personalPhoneId("personal-client-phone-id")
                        .personalEmailId("personal-client-email-id")
                        .build()
                )
                .address(
                    Address.builder()
                        .city("address-city")
                        .street("address-street")
                        .house("address-house")
                        .entrance("address-entrance")
                        .apartment("address-apartment")
                        .floor("address-floor")
                        .entryPhone("address-entry-phone")
                        .lat(new BigDecimal("12.34"))
                        .lon(new BigDecimal("56.78"))
                        .comment("address-comment")
                        .personalAddressId("personal-address-id")
                        .personalGpsCoordId("personal-gps-coord-id")
                        .build()
                )
                .interval(
                    Interval.builder()
                        .dateFrom(LocalDate.of(2022, 3, 4))
                        .timeFrom(LocalTime.of(10, 11))
                        .dateTo(LocalDate.of(2022, 3, 4))
                        .timeTo(LocalTime.of(11, 12))
                        .build()
                )
                .build()
        );
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource({
        "requestValidation",
        "itemsValidation",
        "clientValidation",
        "addressValidation",
        "intervalValidation",
    })
    @DisplayName("Валидация запроса")
    void requestValidation(CreateClientCourierReturnRequest request, String field, String message) {
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
                defaultRequest().externalId(null),
                "externalId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().orderExternalId(null),
                "orderExternalId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().partnerFromId(null),
                "partnerFromId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().orderItemsInfo(null),
                "orderItemsInfo",
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
                defaultRequest().items(List.of(defaultItem().name(null))),
                "items[0].name",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().returnReason(null))),
                "items[0].returnReason",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().dimensions(null))),
                "items[0].dimensions",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().dimensions(defaultDimensions().weight(null)))),
                "items[0].dimensions.weight",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().dimensions(defaultDimensions().length(null)))),
                "items[0].dimensions.length",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().dimensions(defaultDimensions().width(null)))),
                "items[0].dimensions.width",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().dimensions(defaultDimensions().height(null)))),
                "items[0].dimensions.height",
                "must not be null"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> clientValidation() {
        return Stream.of(
            Arguments.of(
                defaultRequest().client(null),
                "client",
                "must not be null"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> addressValidation() {
        return Stream.of(
            Arguments.of(
                defaultRequest().address(null),
                "address",
                "must not be null"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> intervalValidation() {
        return Stream.of(
            Arguments.of(
                defaultRequest().interval(null),
                "interval",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().interval(defaultInterval().dateFrom(null)),
                "interval.dateFrom",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().interval(defaultInterval().timeFrom(null)),
                "interval.timeFrom",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().interval(defaultInterval().dateTo(null)),
                "interval.dateTo",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().interval(defaultInterval().timeTo(null)),
                "interval.timeTo",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static CreateClientCourierReturnRequest defaultRequest() {
        return new CreateClientCourierReturnRequest()
            .externalId("return-external-id")
            .orderExternalId("order-external-id")
            .partnerFromId(100L)
            .items(List.of(defaultItem()))
            .orderItemsInfo(List.of(new OrderItemInfo()))
            .client(defaultClient())
            .address(defaultAddress())
            .interval(defaultInterval());
    }

    @Nonnull
    private static CourierReturnClient defaultClient() {
        return new CourierReturnClient();
    }

    @Nonnull
    private static CourierReturnAddress defaultAddress() {
        return new CourierReturnAddress();
    }

    @Nonnull
    private static CourierInterval defaultInterval() {
        return new CourierInterval()
            .dateFrom(LocalDate.of(2022, 3, 4))
            .timeFrom(LocalTime.of(10, 11))
            .dateTo(LocalDate.of(2022, 3, 4))
            .timeTo(LocalTime.of(11, 12));
    }

    @Nonnull
    private static CourierReturnItem defaultItem() {
        return new CourierReturnItem()
            .supplierId(200L)
            .vendorCode("item-vendor-code")
            .name("item-name")
            .returnReason("item-return-reason")
            .dimensions(defaultDimensions());
    }

    @Nonnull
    private static Dimensions defaultDimensions() {
        return new Dimensions()
            .weight(100)
            .length(200)
            .width(300)
            .height(400);
    }

    @Nonnull
    private Optional<CourierClientReturnMeta> getReturnMeta() {
        return getEntityMetaRecord(RETURN_1_HASH, "RETURN", 1L, "courier-client-return")
            .map(EntityMetaTableDescription.EntityMetaRecord::value)
            .map(v -> readValue(v, CourierClientReturnMeta.class));
    }

    @Nonnull
    private Optional<CourierClientItemMeta> getItemMeta() {
        return getEntityMetaRecord(-1381707035, "RETURN_ITEM", 1L, "courier-client-return-item")
            .map(EntityMetaTableDescription.EntityMetaRecord::value)
            .map(v -> readValue(v, CourierClientItemMeta.class));
    }

    @Nonnull
    private CreateClientCourierReturnOper create(CreateClientCourierReturnRequest request) {
        return apiClient.returns().createClientCourierReturn().body(request);
    }

}

