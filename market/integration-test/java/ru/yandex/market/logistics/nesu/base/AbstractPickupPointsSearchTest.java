package ru.yandex.market.logistics.nesu.base;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.validation.Validator;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter.LogisticsPointFilterBuilder;
import ru.yandex.market.logistics.management.entity.response.FileCollectionResponse;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PickupPointType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.dto.pickuppoints.CoordinateLimits;
import ru.yandex.market.logistics.nesu.dto.pickuppoints.PickupPointsFilter;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartnerResponseBuilder;
import static ru.yandex.market.logistics.nesu.service.lms.VirtualPartnerServiceImpl.PARTNER_SUBTYPE_TO_VIRTUAL_PARTNER;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.objectErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPickupPointsSearchTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private Validator validator;

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(lmsClient);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("filterValidationSource")
    @DisplayName("Валидация фильтра")
    void filterValidation(
        ValidationErrorDataBuilder error,
        UnaryOperator<PickupPointsFilter> filterAdjuster
    ) throws Exception {
        search(filterAdjuster.apply(defaultFilter()))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error.forObject("pickupPointsFilter")));
    }

    @Nonnull
    private static Stream<Arguments> filterValidationSource() {
        return Stream.<Pair<ValidationErrorDataBuilder, UnaryOperator<PickupPointsFilter>>>of(
            Pair.of(
                objectErrorBuilder("Must specify either pickupPointIds or locationId", "ValidPickupPointsFilter"),
                filter -> filter.setPickupPointIds(null)
            ),
            Pair.of(
                fieldErrorBuilder("pickupPointIds", ErrorType.NOT_NULL_ELEMENTS),
                filter -> filter.setPickupPointIds(Collections.singletonList(null))
            ),
            Pair.of(
                fieldErrorBuilder("pickupPointIds", ErrorType.size(1, 100)),
                filter -> filter.setPickupPointIds(List.of())
            ),
            Pair.of(
                fieldErrorBuilder("pickupPointIds", ErrorType.size(1, 100)),
                filter -> filter.setPickupPointIds(LongStream.range(0, 101).boxed().collect(Collectors.toList()))
            ),
            Pair.of(
                fieldErrorBuilder("orderLength", ErrorType.POSITIVE),
                filter -> filter.setOrderLength(0)
            ),
            Pair.of(
                fieldErrorBuilder("orderWidth", ErrorType.POSITIVE),
                filter -> filter.setOrderWidth(0)
            ),
            Pair.of(
                fieldErrorBuilder("orderHeight", ErrorType.POSITIVE),
                filter -> filter.setOrderHeight(0)
            ),
            Pair.of(
                fieldErrorBuilder("orderWeight", ErrorType.POSITIVE),
                filter -> filter.setOrderWeight(0.0)
            )
        )
            .map(t -> Arguments.of(t.getLeft(), t.getRight()));
    }

    @Test
    @DisplayName("Поля фильтра")
    void fullFilterSearch() throws Exception {
        mockSearchLogisticPointsWithResult(pickupPoint(1L, PickupPointType.PICKUP_POINT));
        mockSearchPartnersById(501L);

        search(
            f -> f.setType(ru.yandex.market.logistics.nesu.dto.enums.PickupPointType.PICKUP_POINT)
                .setPickupPointIds(List.of(123L, 234L))
                .setLatitude(
                    new CoordinateLimits()
                        .setFrom(new BigDecimal("34.5"))
                        .setTo(new BigDecimal("45.6"))
                )
                .setLongitude(
                    new CoordinateLimits()
                        .setFrom(new BigDecimal("56.7"))
                        .setTo(new BigDecimal("67.8"))
                )
                .setLocationId(789)
                .setOrderLength(10)
                .setOrderWidth(11)
                .setOrderHeight(12)
                .setOrderWeight(13.4)
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/pickup-points/search_result_pickup_point.json"));

        verifySearchLogisticPoints(
            f -> f.pickupPointType(PickupPointType.PICKUP_POINT)
                .ids(Set.of(123L, 234L))
                .latitudeFrom(new BigDecimal("34.5"))
                .latitudeTo(new BigDecimal("45.6"))
                .longitudeFrom(new BigDecimal("56.7"))
                .longitudeTo(new BigDecimal("67.8"))
                .locationId(789)
                .orderLength(10)
                .orderWidth(11)
                .orderHeight(12)
                .orderWeight(13.4)
        );
        verifySearchPartnersById(501L);
    }

    @Test
    @DisplayName("Поиск почтовых офисов")
    void postOfficeSearch() throws Exception {
        mockSearchLogisticPointsWithResult(pickupPoint(2L, PickupPointType.POST_OFFICE));
        mockSearchPartnersById(502L);

        search(f -> f.setType(ru.yandex.market.logistics.nesu.dto.enums.PickupPointType.POST_OFFICE))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/pickup-points/search_result_post_office.json"));

        verifySearchLogisticPoints(f -> f.pickupPointType(PickupPointType.POST_OFFICE));
        verifySearchPartnersById(502L);
    }

    @Test
    @DisplayName("Поиск постаматов")
    void terminalSearch() throws Exception {
        mockSearchLogisticPointsWithResult(pickupPoint(3L, PickupPointType.TERMINAL));
        mockSearchPartnersById(503L);

        search(f -> f.setType(ru.yandex.market.logistics.nesu.dto.enums.PickupPointType.TERMINAL))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/pickup-points/search_result_terminal.json"));

        verifySearchLogisticPoints(f -> f.pickupPointType(PickupPointType.TERMINAL));
        verifySearchPartnersById(503L);
    }

    @Test
    @DisplayName("Минимальный ответ")
    void searchMinimalResponse() throws Exception {
        mockSearchLogisticPointsWithResult(
            LogisticsPointResponse.newBuilder()
                .id(4L)
                .partnerId(500L)
                .type(PointType.PICKUP_POINT)
                .pickupPointType(PickupPointType.TERMINAL)
                .name("Пусто")
                .address(Address.newBuilder().build())
                .phones(Set.of())
                .active(true)
                .prepayAllowed(false)
                .cardAllowed(false)
                .returnAllowed(false)
                .services(Set.of())
                .build()
        );
        mockSearchPartnersById(500L);

        search()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/pickup-points/search_result_minimal.json"));

        verifySearchLogisticPoints();
        verifySearchPartnersById(500L);
    }

    @Test
    @DisplayName("Сортировка")
    void searchSorting() throws Exception {
        mockSearchLogisticPointsWithResult(
            pickupPoint(3L, PickupPointType.TERMINAL),
            pickupPoint(2L, PickupPointType.PICKUP_POINT),
            pickupPoint(1L, PickupPointType.POST_OFFICE)
        );
        mockSearchPartnersById(501L, 502L, 503L);

        search()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/pickup-points/search_result_all.json"));

        verifySearchLogisticPoints();
        verifySearchPartnersById(501L, 502L, 503L);
    }

    @Test
    @DisplayName("Поиск по locationId")
    void searchByLocationId() throws Exception {
        mockSearchLogisticPointsWithResult(
            pickupPoint(3L, PickupPointType.TERMINAL),
            pickupPoint(2L, PickupPointType.PICKUP_POINT),
            pickupPoint(1L, PickupPointType.POST_OFFICE)
        );
        mockSearchPartnersById(501L, 502L, 503L);

        search(f -> f.setPickupPointIds(null).setLocationId(1))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/pickup-points/search_result_all.json"));

        verifySearchLogisticPoints(f -> f.ids(null).locationId(1));
        verifySearchPartnersById(501L, 502L, 503L);
    }

    @Test
    @DisplayName("Подмена партнёра на виртуального")
    void virtualPartnerSubstitution() throws Exception {
        mockSearchLogisticPointsWithResult(
            pickupPoint(1L, PickupPointType.PICKUP_POINT),
            pickupPoint(2L, PickupPointType.PICKUP_POINT),
            pickupPoint(3L, PickupPointType.PICKUP_POINT),
            pickupPoint(4L, PickupPointType.PICKUP_POINT),
            pickupPoint(5L, PickupPointType.PICKUP_POINT),
            pickupPoint(6L, PickupPointType.PICKUP_POINT)
        );
        when(lmsClient.searchPartners(
            SearchPartnerFilter.builder()
                .setIds(Set.of(501L, 502L, 503L, 504L, 505L, 506L))
                .build()
        )).thenReturn(List.of(
            createPartner(501L),
            createPartnerWithSubtype(502L, 2L),
            createPartnerWithSubtype(503L, 3L),
            createPartnerWithSubtype(504L, 4L), // Не будет найден
            createPartnerWithSubtype(505L, 1L), // Не попадет в поиск
            createPartnerWithSubtype(506L, 2L)
        ));
        SearchPartnerFilter virtualPartnerFilter = createVirtualPartnerFilter(2L, 3L, 4L);
        when(lmsClient.searchPartners(virtualPartnerFilter))
            .thenReturn(List.of(
                createVirtualPartner(2),
                createVirtualPartner(3)
            ));

        search()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/pickup-points/search_result_with_virtual.json"));

        verifySearchLogisticPoints();
        verifySearchPartnersById(501L, 502L, 503L, 504L, 505L, 506L);
        verify(lmsClient).searchPartners(virtualPartnerFilter);
    }

    @Nonnull
    private LogisticsPointResponse pickupPoint(long id, PickupPointType pickupPointType) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .partnerId(500L + id)
            .externalId("abc")
            .type(PointType.PICKUP_POINT)
            .pickupPointType(pickupPointType)
            .name("ПВЗ " + id)
            .address(address())
            .phones(Set.of(phone(PhoneType.ADDITIONAL), phone(PhoneType.PRIMARY)))
            .schedule(Set.of(schedule(3), schedule(2), schedule(1)))
            .contact(contact())
            .prepayAllowed(true)
            .cardAllowed(true)
            .photos(new FileCollectionResponse())
            .instruction("От остановки направо")
            .returnAllowed(false)
            .services(Set.of())
            .storagePeriod(0)
            .maxWeight(0.0)
            .maxLength(0)
            .maxWeight(0.0)
            .maxHeight(0)
            .maxSidesSum(0)
            .isFrozen(false)
            .build();
    }

    @Nonnull
    private SearchPartnerFilter createVirtualPartnerFilter(Long... subtypes) {
        return SearchPartnerFilter.builder()
            .setPlatformClientIds(Set.of(3L))
            .setIds(Arrays.stream(subtypes).map(PARTNER_SUBTYPE_TO_VIRTUAL_PARTNER::get).collect(Collectors.toSet()))
            .build();
    }

    @Nonnull
    private PartnerResponse createVirtualPartner(long partnerSubtype) {
        Long id = PARTNER_SUBTYPE_TO_VIRTUAL_PARTNER.get(partnerSubtype);
        return createPartnerResponseBuilder(id, PartnerType.DELIVERY, id).build();
    }

    @Nonnull
    private PartnerResponse createPartner(long id) {
        return createPartnerResponseBuilder(id, PartnerType.DELIVERY, id).build();
    }

    @Nonnull
    private PartnerResponse createPartnerWithSubtype(long id, long subtypeId) {
        return createPartnerResponseBuilder(id, PartnerType.DELIVERY, id)
            .subtype(PartnerSubtypeResponse.newBuilder().id(subtypeId).build())
            .build();
    }

    @Nonnull
    private Address address() {
        return Address.newBuilder()
            .locationId(789)
            .settlement("поселок Шушары")
            .postCode("196634")
            .latitude(new BigDecimal("43.2"))
            .longitude(new BigDecimal("54.3"))
            .street("улица Изборская (Славянка)")
            .house("4")
            .housing("1")
            .building("0")
            .apartment("ignored")
            .comment("Комментарий")
            .region("Санкт-Петербург")
            .subRegion("Петербургский округ")
            .addressString("196634, Санкт-Петербург, поселок Шушары, "
                + "улица Изборская (Славянка), 4, корп. 1, стр. 0, кв. 10")
            .shortAddressString("улица Изборская (Славянка), 4, корп. 1, стр. 0, кв. 10")
            .build();
    }

    @Nonnull
    private Phone phone(PhoneType type) {
        return new Phone(
            "+79876543210",
            "456",
            "ignored",
            type
        );
    }

    @Nonnull
    private ScheduleDayResponse schedule(int day) {
        return new ScheduleDayResponse(
            1L,
            day,
            LocalTime.of(10, 0),
            LocalTime.of(16, 0)
        );
    }

    @Nonnull
    private Contact contact() {
        return new Contact(
            "Дарья",
            "Григорьевна",
            "Гаврилова"
        );
    }

    @Nonnull
    private LogisticsPointFilterBuilder basicLmsFilter() {
        return LogisticsPointFilter.newBuilder()
            .active(true)
            .platformClientId(3L)
            .type(PointType.PICKUP_POINT)
            .ids(Set.of(4L));
    }

    @Nonnull
    private static PickupPointsFilter defaultFilter() {
        return new PickupPointsFilter()
            .setPickupPointIds(List.of(4L));
    }

    @Nonnull
    private ResultActions search() throws Exception {
        return search(UnaryOperator.identity());
    }

    @Nonnull
    private ResultActions search(UnaryOperator<PickupPointsFilter> filterAdjuster) throws Exception {
        return search(filterAdjuster.apply(defaultFilter()));
    }

    @Nonnull
    protected abstract ResultActions search(PickupPointsFilter filter) throws Exception;

    private void mockSearchPartnersById(Long... ids) {
        when(lmsClient.searchPartners(
            SearchPartnerFilter.builder()
                .setIds(Set.of(ids))
                .build()
        ))
            .thenReturn(
                Stream.of(ids)
                    .map(this::createPartner)
                    .collect(Collectors.toList())
            );
    }

    private void verifySearchPartnersById(Long... ids) {
        verify(lmsClient).searchPartners(SearchPartnerFilter.builder().setIds(Set.of(ids)).build());
    }

    private void mockSearchLogisticPointsWithResult(LogisticsPointResponse... result) {
        when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class)))
            .thenReturn(Arrays.asList(result));
    }

    private void verifySearchLogisticPoints() {
        verifySearchLogisticPoints(UnaryOperator.identity());
    }

    private void verifySearchLogisticPoints(UnaryOperator<LogisticsPointFilterBuilder> adjuster) {
        ArgumentCaptor<LogisticsPointFilter> captor = ArgumentCaptor.forClass(LogisticsPointFilter.class);

        verify(lmsClient).getLogisticsPoints(captor.capture());

        LogisticsPointFilter actual = captor.getValue();
        softly.assertThat(validator.validate(actual)).isEmpty();
        softly.assertThat(actual).isEqualTo(adjuster.apply(basicLmsFilter()).build());
    }

}
