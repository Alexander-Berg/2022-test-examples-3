package ru.yandex.market.mbi.api.controller.delivery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.api.client.entity.nesu.ContactDto;
import ru.yandex.market.mbi.api.client.entity.nesu.DeliveryPartnerApplicationDto;
import ru.yandex.market.mbi.api.client.entity.nesu.DeliveryPartnerRegistrationDto;
import ru.yandex.market.mbi.api.client.entity.nesu.PagedDeliveryPartnerApplicationDto;
import ru.yandex.market.mbi.api.client.entity.nesu.PagedDeliveryPartnerRegistrationDto;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryPartnerControllerTest extends FunctionalTest {
    private static final List<DeliveryPartnerRegistrationDto> REGISTRATIONS = ImmutableList.of(
            DeliveryPartnerRegistrationDto.of(
                    1L,
                    10001L,
                    "Тестовый магазин Яндекс.Доставки 1",
                    DateTimes.toInstantAtDefaultTz(2019, 10, 1),
                    ImmutableList.of()
            ),
            DeliveryPartnerRegistrationDto.of(
                    4L,
                    10004L,
                    "Тестовый магазин Яндекс.Доставки 4",
                    DateTimes.toInstantAtDefaultTz(2019, 10, 4),
                    ImmutableList.of(
                            ContactDto.of("Сергеев", "Сергей", "+7 444 444 4444", "contact-14@yandex.ru"),
                            ContactDto.of("Трофимов", "Трофим", "+7 555 555 5555", "contact-15@yandex.ru")
                    )
            )
    );

    private static final List<DeliveryPartnerApplicationDto> APPLICATIONS = ImmutableList.of(
            DeliveryPartnerApplicationDto.of(
                    2L,
                    10002L,
                    DateTimes.toInstantAtDefaultTz(2019, 10, 2),
                    "Тестовый магазин Яндекс.Доставки 2",
                    "ИП Александров Александр",
                    OrganizationType.IP,
                    ImmutableList.of(
                            ContactDto.of("Александров", "Александр", "+7 222 222 2222", "contact-12@yandex.ru"),
                            ContactDto.of("Константинов", "Константин", "+7 777 777 7777", "contact-17-1@yandex.ru"),
                            ContactDto.of("Константинов", "Константин", "+7 777 777 7777", "contact-17-2@yandex.ru")
                    ),
                    1L,
                    PartnerApplicationStatus.INIT,
                    null
            ),
            DeliveryPartnerApplicationDto.of(
                    5L,
                    10005L,
                    DateTimes.toInstantAtDefaultTz(2019, 10, 5),
                    "Тестовый магазин Яндекс.Доставки 5",
                    "ООО Тестовый магазин Яндекс.Доставки",
                    OrganizationType.OOO,
                    ImmutableList.of(),
                    2L,
                    PartnerApplicationStatus.NEED_INFO,
                    "Требуется фотография котика"
            )
    );

    static Stream<Arguments> getNewPartnersArguments() {
        List<Set<Long>> partnerIdsArgs = new ArrayList<>();
        partnerIdsArgs.add(null);
        partnerIdsArgs.add(ImmutableSet.of(1L));
        partnerIdsArgs.add(ImmutableSet.of(1L, 2L));

        List<Set<Long>> clientIdsArgs = new ArrayList<>();
        clientIdsArgs.add(null);
        clientIdsArgs.add(ImmutableSet.of(10001L));
        clientIdsArgs.add(ImmutableSet.of(10001L, 10002L));

        List<Arguments> arguments = new ArrayList<>();
        for (Set<Long> partnerIds : partnerIdsArgs) {
            for (Set<Long> clientIds : clientIdsArgs) {
                arguments.add(Arguments.of(partnerIds, clientIds));
            }
        }

        return arguments.stream();
    }

    static Stream<Arguments> getPartnersApplicationsArguments() {
        List<Set<Long>> partnerIdsArgs = new ArrayList<>();
        partnerIdsArgs.add(null);
        partnerIdsArgs.add(ImmutableSet.of(2L));
        partnerIdsArgs.add(ImmutableSet.of(2L, 5L));

        List<Set<Long>> clientIdsArgs = new ArrayList<>();
        clientIdsArgs.add(null);
        clientIdsArgs.add(ImmutableSet.of(10002L));
        clientIdsArgs.add(ImmutableSet.of(10002L, 10005L));

        List<Set<Long>> requestIdsArgs = new ArrayList<>();
        requestIdsArgs.add(null);
        requestIdsArgs.add(ImmutableSet.of(1L));
        requestIdsArgs.add(ImmutableSet.of(1L, 2L));

        List<Arguments> arguments = new ArrayList<>();
        for (Set<Long> partnerIds : partnerIdsArgs) {
            for (Set<Long> clientIds : clientIdsArgs) {
                for (Set<Long> requestIds : requestIdsArgs) {
                    arguments.add(Arguments.of(partnerIds, clientIds, requestIds));
                }
            }
        }

        return arguments.stream();
    }

    @DisplayName("Получить зарегистрировавшиеся магазины Яндекс.Доставки")
    @ParameterizedTest
    @MethodSource("getNewPartnersArguments")
    @DbUnitDataSet(before = "csv/DeliveryPartnerController.get.before.csv")
    void getNewPartners(Set<Long> partnerIds, Set<Long> clientIds) {
        PagedDeliveryPartnerRegistrationDto actual = mbiApiClient.getNewDeliveryPartners(0, 10, partnerIds, clientIds);
        PagedDeliveryPartnerRegistrationDto expected = createExpectedPageOfPartnerRegistrationDtos(
                partnerIds,
                clientIds
        );
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Получить пустую страницу магазинов")
    @DbUnitDataSet(before = "csv/DeliveryPartnerController.get.before.csv")
    void getNewPartnersEmptyPage() {
        PagedDeliveryPartnerRegistrationDto actual = mbiApiClient.getNewDeliveryPartners(1, 10, null, null);
        PagedDeliveryPartnerRegistrationDto expected = createEmptyPageOfPartnerRegistrationDtos(2);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Получить пустой ответ")
    void getNewPartnersEmpty() {
        PagedDeliveryPartnerRegistrationDto actual = mbiApiClient.getNewDeliveryPartners(0, 10, null, null);
        PagedDeliveryPartnerRegistrationDto expected = createEmptyPageOfPartnerRegistrationDtos(0);
        Assertions.assertEquals(expected, actual);
    }

    @DisplayName("Получить заявления магазинов на подключение к Яндекс.Доставке")
    @ParameterizedTest
    @MethodSource("getPartnersApplicationsArguments")
    @DbUnitDataSet(before = "csv/DeliveryPartnerController.get.before.csv")
    void getPartnersApplicationsByClientIds(Set<Long> partnerIds, Set<Long> clientIds, Set<Long> requestIds) {
        PagedDeliveryPartnerApplicationDto actual = mbiApiClient.getDeliveryPartnersApplications(
                0,
                10,
                partnerIds,
                clientIds,
                requestIds
        );
        PagedDeliveryPartnerApplicationDto expected = createExpectedPageOfPartnerApplicationDtos(
                partnerIds,
                clientIds,
                requestIds
        );
        assertThat(actual.getTotalCount()).isEqualTo(expected.getTotalCount());
        for (int i = 0; i < expected.getTotalCount(); i++) {
            checkDeliveryPartnerApplicationDtoWithoutOrderContacts(actual.getPartners().get(i),
                    expected.getPartners().get(i));
        }
    }


    private void checkDeliveryPartnerApplicationDtoWithoutOrderContacts(DeliveryPartnerApplicationDto actualPartner1,
                                                                        DeliveryPartnerApplicationDto expectedPartner1) {
        assertThat(actualPartner1.getId()).isEqualTo(expectedPartner1.getId());
        assertThat(actualPartner1.getClientId()).isEqualTo(expectedPartner1.getClientId());
        assertThat(actualPartner1.getRequestId()).isEqualTo(expectedPartner1.getRequestId());
        assertThat(actualPartner1.getCreated()).isEqualTo(expectedPartner1.getCreated());
        assertThat(actualPartner1.getName()).isEqualTo(expectedPartner1.getName());
        assertThat(actualPartner1.getOrganizationName()).isEqualTo(expectedPartner1.getOrganizationName());
        assertThat(actualPartner1.getOrganizationType()).isEqualTo(expectedPartner1.getOrganizationType());
        assertThat(actualPartner1.getApplicationStatus()).isEqualTo(expectedPartner1.getApplicationStatus());
        assertThat(actualPartner1.getAboComment()).isEqualTo(expectedPartner1.getAboComment());
        assertTrue(CollectionUtils.isEqualCollection(actualPartner1.getContacts(), expectedPartner1.getContacts()));
    }

    @Test
    @DisplayName("Получить пустую страницу заявлений")
    @DbUnitDataSet(before = "csv/DeliveryPartnerController.get.before.csv")
    void getPartnersApplicationsEmptyPage() {
        PagedDeliveryPartnerApplicationDto actual = mbiApiClient.getDeliveryPartnersApplications(
                1,
                10,
                null,
                null,
                null
        );
        PagedDeliveryPartnerApplicationDto expected = createEmptyPageOfPartnerApplicationDtos(2);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Получить пустой ответ")
    void getPartnersApplicationsEmpty() {
        PagedDeliveryPartnerApplicationDto actual = mbiApiClient.getDeliveryPartnersApplications(
                0,
                10,
                null,
                null,
                null
        );
        PagedDeliveryPartnerApplicationDto expected = createEmptyPageOfPartnerApplicationDtos(0);
        Assertions.assertEquals(expected, actual);
    }

    @Nonnull
    private PagedDeliveryPartnerApplicationDto createExpectedPageOfPartnerApplicationDtos(
            @Nullable Set<Long> partnerIds,
            @Nullable Set<Long> clientIds,
            @Nullable Set<Long> requestIds
    ) {
        List<DeliveryPartnerApplicationDto> applications = APPLICATIONS.stream()
                .filter(a -> Optional.ofNullable(partnerIds).map(set -> set.contains(a.getId())).orElse(true))
                .filter(a -> Optional.ofNullable(clientIds).map(set -> set.contains(a.getClientId())).orElse(true))
                .filter(a -> Optional.ofNullable(requestIds).map(set -> set.contains(a.getRequestId())).orElse(true))
                .sorted(Comparator.comparing(DeliveryPartnerApplicationDto::getId).reversed())
                .collect(Collectors.toList());
        return PagedDeliveryPartnerApplicationDto.of(applications, (long) applications.size());
    }

    @Nonnull
    private PagedDeliveryPartnerRegistrationDto createExpectedPageOfPartnerRegistrationDtos(
            @Nullable Set<Long> partnerIds,
            @Nullable Set<Long> clientIds
    ) {
        List<DeliveryPartnerRegistrationDto> registrations = REGISTRATIONS.stream()
                .filter(a -> Optional.ofNullable(clientIds).map(set -> set.contains(a.getClientId())).orElse(true))
                .filter(a -> Optional.ofNullable(partnerIds).map(set -> set.contains(a.getId())).orElse(true))
                .sorted(Comparator.comparing(DeliveryPartnerRegistrationDto::getId).reversed())
                .collect(Collectors.toList());
        return PagedDeliveryPartnerRegistrationDto.of(registrations, (long) registrations.size());
    }

    @Nonnull
    private PagedDeliveryPartnerRegistrationDto createEmptyPageOfPartnerRegistrationDtos(long totalCount) {
        return PagedDeliveryPartnerRegistrationDto.of(ImmutableList.of(), totalCount);
    }

    @Nonnull
    private PagedDeliveryPartnerApplicationDto createEmptyPageOfPartnerApplicationDtos(long totalCount) {
        return PagedDeliveryPartnerApplicationDto.of(ImmutableList.of(), totalCount);
    }
}
