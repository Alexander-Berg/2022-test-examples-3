package ru.yandex.market.personal_market;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import ru.yandex.market.personal_market.client.model.CommonType;
import ru.yandex.market.personal_market.client.model.CommonTypeEnum;
import ru.yandex.market.personal_market.client.model.FullName;
import ru.yandex.market.personal_market.client.model.GpsCoord;
import ru.yandex.market.personal_market.client.model.MultiTypeRetrieveRequestItem;
import ru.yandex.market.personal_market.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.personal_market.client.model.MultiTypeStoreResponseItem;
import ru.yandex.market.personal_market.client.model.PersonalMultiTypeRetrieveResponse;
import ru.yandex.market.personal_market.client.model.PersonalMultiTypeStoreResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersonalMarketServiceTest {

    private final PersonalMarketClient personalMarketClient = mock(PersonalMarketClient.class);
    private final PersonalMarketService service = new PersonalMarketService(personalMarketClient);

    @Test
    void storePersonalData() throws ExecutionException, InterruptedException {
        when(personalMarketClient.multiTypesStore(argThat(req ->
                Set.copyOf(req.getItems()).equals(Set.of(
                        new CommonType().phone("+712312312312"),
                        new CommonType().email("email@email"),
                        new CommonType().fullName(
                                new FullName().forename("first").surname("last").patronymic("middle")),
                        new CommonType().gpsCoord(
                                new GpsCoord().latitude(BigDecimal.ONE).longitude(BigDecimal.TEN)),
                        new CommonType().address(Map.of("city", "Moscow", "house", "3")))))
        )).thenReturn(CompletableFuture.completedFuture(
                new PersonalMultiTypeStoreResponse()
                        .addItemsItem(new MultiTypeStoreResponseItem()
                                .id("some_phone_id")
                                .value(new CommonType().phone("+712312312312")))
                        .addItemsItem(new MultiTypeStoreResponseItem()
                                .id("some_email_id")
                                .value(new CommonType().email("email@email")))
                        .addItemsItem(new MultiTypeStoreResponseItem()
                                .id("some_full_name_id")
                                .value(new CommonType().fullName(new FullName().forename("first").surname("last").patronymic("middle"))))
                        .addItemsItem(new MultiTypeStoreResponseItem()
                                .id("some_gps_coord_id")
                                .value(new CommonType().gpsCoord(new GpsCoord().latitude(BigDecimal.ONE).longitude(BigDecimal.TEN))))
                        .addItemsItem(new MultiTypeStoreResponseItem()
                                .id("some_address_id")
                                .value(new CommonType().address(Map.of("city", "Moscow", "house", "3"))))
        ));

        CompletableFuture<PersonalStoreResponse> response = service.store(new PersonalStoreRequestBuilder()
                .phone("+712312312312")
                .email("email@email")
                .fullName("first", "last", "middle")
                .gpsCoord(BigDecimal.ONE, BigDecimal.TEN)
                .address(PersonalAddress.builder()
                        .withCity("Moscow")
                        .withHouse("3")
                        .build())
        );

        assertThat(response.get()).isNotNull()
                .satisfies(resp -> {
                    assertThat(resp.getPhoneId()).isEqualTo("some_phone_id");
                    assertThat(resp.getEmailId()).isEqualTo("some_email_id");
                    assertThat(resp.getFullNameId()).isEqualTo("some_full_name_id");
                    assertThat(resp.getGpsCoordId()).isEqualTo("some_gps_coord_id");
                    assertThat(resp.getAddressId()).isEqualTo("some_address_id");
                });
    }

    @Test
    void storePersonalDataWithError() {
        when(personalMarketClient.multiTypesStore(argThat(req ->
                Set.copyOf(req.getItems()).equals(Set.of(
                        new CommonType().phone("+712312312312"),
                        new CommonType().email("email@email"),
                        new CommonType().gpsCoord(new GpsCoord()
                                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ONE)))))
        )).thenReturn(CompletableFuture.completedFuture(
                new PersonalMultiTypeStoreResponse()
                        .addItemsItem(new MultiTypeStoreResponseItem()
                                .id("some_phone_id")
                                .value(new CommonType().phone("+712312312312")))
        ));

        CompletableFuture<PersonalStoreResponse> response = service.store(new PersonalStoreRequestBuilder()
                .phone("+712312312312")
                .email("email@email")
                .gpsCoord(BigDecimal.ZERO, BigDecimal.ONE)
        );

        assertThat(response.isCompletedExceptionally()).isTrue();
        assertThatThrownBy(response::get)
                .getCause()
                .isExactlyInstanceOf(PersonalServiceException.class)
                .hasMessage("Store error: \"email\" was not saved")
                .hasSuppressedException(new PersonalServiceException("Store error: \"gpsCoord\" was not saved"));
    }

    @Test
    void retrievePersonalData() throws ExecutionException, InterruptedException {
        when(personalMarketClient.multiTypesRetrieve(argThat(req ->
                Set.copyOf(req.getItems()).equals(Set.of(
                        new MultiTypeRetrieveRequestItem()
                                .id("some_phone_id")
                                .type(CommonTypeEnum.PHONE),
                        new MultiTypeRetrieveRequestItem()
                                .id("some_email_id")
                                .type(CommonTypeEnum.EMAIL),
                        new MultiTypeRetrieveRequestItem()
                                .id("some_full_name_id")
                                .type(CommonTypeEnum.FULL_NAME),
                        new MultiTypeRetrieveRequestItem()
                                .id("some_gps_coord_id")
                                .type(CommonTypeEnum.GPS_COORD),
                        new MultiTypeRetrieveRequestItem()
                                .id("some_address_id")
                                .type(CommonTypeEnum.ADDRESS))))
        )).thenReturn(CompletableFuture.completedFuture(
                new PersonalMultiTypeRetrieveResponse()
                        .addItemsItem(new MultiTypeRetrieveResponseItem()
                                .id("some_phone_id")
                                .type(CommonTypeEnum.PHONE)
                                .value(new CommonType().phone("+712312312312")))
                        .addItemsItem(new MultiTypeRetrieveResponseItem()
                                .id("some_email_id")
                                .type(CommonTypeEnum.EMAIL)
                                .value(new CommonType().email("email@email")))
                        .addItemsItem(new MultiTypeRetrieveResponseItem()
                                .id("some_full_name_id")
                                .type(CommonTypeEnum.FULL_NAME)
                                .value(new CommonType().fullName(new FullName().forename("first").surname("last").patronymic("middle"))))
                        .addItemsItem(new MultiTypeRetrieveResponseItem()
                                .id("some_gps_coord_id")
                                .type(CommonTypeEnum.GPS_COORD)
                                .value(new CommonType().gpsCoord(new GpsCoord().latitude(BigDecimal.ONE).longitude(BigDecimal.TEN))))
                        .addItemsItem(new MultiTypeRetrieveResponseItem()
                                .id("some_address_id")
                                .type(CommonTypeEnum.ADDRESS)
                                .value(new CommonType().address(Map.of("city", "Moscow", "house", "3"))))
        ));

        CompletableFuture<PersonalRetrieveResponse> response = service.retrieve(new PersonalRetrieveRequestBuilder()
                .phone("some_phone_id")
                .email("some_email_id")
                .fullName("some_full_name_id")
                .gpsCoord("some_gps_coord_id")
                .address("some_address_id")
        );
        assertThat(response.get()).satisfies(resp -> {
            assertThat(resp.getPhone("some_phone_id")).isEqualTo("+712312312312");
            assertThat(resp.getPhone("some_phone_id", "ignored_fallback")).isEqualTo("+712312312312");
            assertThat(resp.getEmail("some_email_id")).isEqualTo("email@email");
            assertThat(resp.getEmail("some_email_id", "ignored_fallback")).isEqualTo("email@email");
            assertThat(resp.getFullName("some_full_name_id")).isEqualTo(new FullName().forename("first").surname("last").patronymic("middle"));
            assertThat(resp.getFullName("some_full_name_id", "ignored_fallback", "ignored_fallback", "ignored_fallback"))
                    .isEqualTo(new FullName().forename("first").surname("last").patronymic("middle"));
            assertThat(resp.getFirstName("some_full_name_id")).isEqualTo("first");
            assertThat(resp.getLastName("some_full_name_id")).isEqualTo("last");
            assertThat(resp.getMiddleName("some_full_name_id")).isEqualTo("middle");
            assertThat(resp.getGpsCoord("some_gps_coord_id")).isEqualTo(new GpsCoord().latitude(BigDecimal.ONE).longitude(BigDecimal.TEN));
            assertThat(resp.getGpsCoord("some_gps_coord_id", BigDecimal.ZERO, BigDecimal.ZERO))
                    .isEqualTo(new GpsCoord().latitude(BigDecimal.ONE).longitude(BigDecimal.TEN));
            assertThat(resp.getAddress("some_address_id")).isEqualTo(PersonalAddress.builder()
                    .withCity("Moscow")
                    .withHouse("3")
                    .build()
            );
        });
    }

    @Test
    void retrievePersonalDataFromFallbacks() throws ExecutionException, InterruptedException {
        when(personalMarketClient.multiTypesRetrieve(argThat(req ->
                Set.copyOf(req.getItems()).equals(Set.of(
                        new MultiTypeRetrieveRequestItem()
                                .id("some_phone_id")
                                .type(CommonTypeEnum.PHONE),
                        new MultiTypeRetrieveRequestItem()
                                .id("some_email_id")
                                .type(CommonTypeEnum.EMAIL),
                        new MultiTypeRetrieveRequestItem()
                                .id("some_full_name_id")
                                .type(CommonTypeEnum.FULL_NAME),
                        new MultiTypeRetrieveRequestItem()
                                .id("some_gps_coord_id")
                                .type(CommonTypeEnum.GPS_COORD),
                        new MultiTypeRetrieveRequestItem()
                                .id("some_address_id")
                                .type(CommonTypeEnum.ADDRESS))))
        )).thenReturn(CompletableFuture.completedFuture(new PersonalMultiTypeRetrieveResponse()));

        CompletableFuture<PersonalRetrieveResponse> response = service.retrieve(new PersonalRetrieveRequestBuilder()
                .phone("some_phone_id")
                .email("some_email_id")
                .fullName("some_full_name_id")
                .gpsCoord("some_gps_coord_id")
                .address("some_address_id")
        );
        assertThat(response.get()).satisfies(resp -> {
            assertThat(resp.getEmail("some_email_id")).isNull();
            assertThat(resp.getPhone("some_phone_id", "+799912312312")).isEqualTo("+799912312312");
            assertThat(resp.getEmail("some_email_id")).isNull();
            assertThat(resp.getEmail("some_email_id", "fallback@email")).isEqualTo("fallback@email");
            assertThat(resp.getFullName("some_full_name_id")).isNull();
            assertThat(resp.getFullName("some_full_name_id", "firstNameFallback", "lastNameFallback", "middleNameFallback"))
                    .isEqualTo(new FullName().forename("firstNameFallback").surname("lastNameFallback").patronymic("middleNameFallback"));
            assertThat(resp.getGpsCoord("some_gps_coord_id")).isNull();
            assertThat(resp.getGpsCoord("some_gps_coord_id", BigDecimal.ZERO, BigDecimal.ZERO))
                    .isEqualTo(new GpsCoord().latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO));
            assertThat(resp.getAddress("some_address_id")).isNull();

            assertThat(resp.getFirstName("some_full_name_id")).isNull();
            assertThat(resp.getLastName("some_full_name_id")).isNull();
            assertThat(resp.getMiddleName("some_full_name_id")).isNull();
            assertThat(resp.getFirstName("some_full_name_id", "first")).isEqualTo("first");
            assertThat(resp.getLastName("some_full_name_id", "last")).isEqualTo("last");
            assertThat(resp.getMiddleName("some_full_name_id", "middle")).isEqualTo("middle");
        });
    }
}
