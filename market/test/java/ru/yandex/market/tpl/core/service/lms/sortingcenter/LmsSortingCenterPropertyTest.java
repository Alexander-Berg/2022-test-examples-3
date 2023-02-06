package ru.yandex.market.tpl.core.service.lms.sortingcenter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.dto.grid.GridItem;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.tpl.api.model.lms.IdsDto;
import ru.yandex.market.tpl.core.domain.lms.sortingcenter.property.SortingCenterPropertyDto;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyEntity;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;


@RequiredArgsConstructor
public class LmsSortingCenterPropertyTest extends TplAbstractTest {
    private static final long SORTING_CENTER_ID = 963258L;

    private final LMSClient lmsClient;
    private final TestUserHelper testUserHelper;
    private final LmsSortingCenterPropertyFacade lmsSortingCenterPropertyFacade;
    private final SortingCenterPropertyService sortingCenterPropertyService;

    @BeforeEach
    void setup() {
        reset(lmsClient);
        when(lmsClient.getPartner(SORTING_CENTER_ID))
                .thenReturn(Optional.of(PartnerResponse.newBuilder().partnerType(PartnerType.SORTING_CENTER).build()));
        testUserHelper.sortingCenter(SORTING_CENTER_ID, Set.of());
    }

    @Test
    void createSortingCenterProperties() {
        Long propertyId = lmsSortingCenterPropertyFacade.createProperty(SORTING_CENTER_ID,
                SortingCenterPropertyDto.builder()
                .name(UserProperties.CALL_TO_RECIPIENT_ENABLED.getName())
                .value("true")
                .build());

        assertThat(propertyId).isGreaterThan(0L);

        Optional<SortingCenterPropertyEntity> propertyOpt =
                sortingCenterPropertyService.findByNameAndSortingCenterId(
                        UserProperties.CALL_TO_RECIPIENT_ENABLED.getName(), SORTING_CENTER_ID);
        assertThat(propertyOpt).isPresent();
    }

    @Test
    void getSortingCenterProperties() {
        Long hideClientPhonePropertyId = lmsSortingCenterPropertyFacade.createProperty(SORTING_CENTER_ID,
                SortingCenterPropertyDto.builder()
                        .name(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName())
                        .value("true")
                        .build());
        assertThat(hideClientPhonePropertyId).isGreaterThan(0L);
        Long callToRecipientEnabledPropertyId = lmsSortingCenterPropertyFacade.createProperty(SORTING_CENTER_ID,
                SortingCenterPropertyDto.builder()
                        .name(UserProperties.CALL_TO_RECIPIENT_ENABLED.getName())
                        .value("true")
                        .build());

        assertThat(callToRecipientEnabledPropertyId).isGreaterThan(0L);

        GridData sortingCenterProperties = lmsSortingCenterPropertyFacade
                .getSortingCenterProperties(PageRequest.of(0, 10), SORTING_CENTER_ID);

        assertThat(sortingCenterProperties.getItems()).extracting(GridItem::getId).containsExactlyInAnyOrder(
                hideClientPhonePropertyId, callToRecipientEnabledPropertyId);
    }

    @Test
    void getSortingCenterProperty() {
        Long hideClientPhonePropertyId = lmsSortingCenterPropertyFacade.createProperty(SORTING_CENTER_ID,
                SortingCenterPropertyDto.builder()
                        .name(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName())
                        .value("true")
                        .build());
        assertThat(hideClientPhonePropertyId).isGreaterThan(0L);

        DetailData sortingCenterProperty = lmsSortingCenterPropertyFacade
                .getSortingCenterProperty(hideClientPhonePropertyId);

        assertThat(sortingCenterProperty).isNotNull();
        assertThat(sortingCenterProperty.getItem().getId()).isEqualTo(hideClientPhonePropertyId);
        assertThat(sortingCenterProperty.getItem().getValues().get("value")).isEqualTo("true");
    }

    @Test
    void updateSortingCenterProperty() {
        Long propertyId = lmsSortingCenterPropertyFacade.createProperty(SORTING_CENTER_ID,
                SortingCenterPropertyDto.builder()
                        .name(UserProperties.CALL_TO_RECIPIENT_ENABLED.getName())
                        .value("true")
                        .build());

        assertThat(propertyId).isGreaterThan(0L);

        Optional<SortingCenterPropertyEntity> propertyOpt =
                sortingCenterPropertyService.findByNameAndSortingCenterId(
                        UserProperties.CALL_TO_RECIPIENT_ENABLED.getName(), SORTING_CENTER_ID);
        assertThat(propertyOpt).isPresent();
        assertThat(propertyOpt.get().getValue()).isEqualTo("true");

        DetailData propertyData = lmsSortingCenterPropertyFacade.updateProperty(
                SortingCenterPropertyDto.builder()
                        .id(propertyId)
                        .name(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName())
                        .value("false")
                        .build());
        assertThat(propertyData).isNotNull();
        assertThat(propertyData.getItem().getValues().get("value")).isEqualTo("false");
    }

    @Test
    void updateSortingCenterPropertyDates() {
        Long propertyId = lmsSortingCenterPropertyFacade.createProperty(SORTING_CENTER_ID,
                SortingCenterPropertyDto.builder()
                        .name(UserProperties.CALL_TO_RECIPIENT_ENABLED.getName())
                        .value("true")
                        .build());

        assertThat(propertyId).isGreaterThan(0L);
        assertThat(propertyId).isGreaterThan(0L);

        Optional<SortingCenterPropertyEntity> propertyOpt =
                sortingCenterPropertyService.findByNameAndSortingCenterId(
                        UserProperties.CALL_TO_RECIPIENT_ENABLED.getName(), SORTING_CENTER_ID);
        assertThat(propertyOpt).isPresent();
        assertThat(propertyOpt.get().getValue()).isEqualTo("true");
        assertThat(propertyOpt.get().getStartedAt()).isNull();
        assertThat(propertyOpt.get().getEndedAt()).isNull();

        LocalDate startDate = LocalDate.now();
        LocalTime startTime = LocalTime.now();
        LocalDate endDate = LocalDate.now().plusDays(2);
        LocalTime endTime = LocalTime.now().plusHours(2);
        DetailData propertyData = lmsSortingCenterPropertyFacade.updateProperty(
                SortingCenterPropertyDto.builder()
                        .id(propertyId)
                        .name(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName())
                        .value("false")
                        .startedAtDate(startDate)
                        .startedAtTime(startTime)
                        .endedAtDate(endDate)
                        .endedAtTime(endTime)
                        .build());
        assertThat(propertyData).isNotNull();
        assertThat(propertyData.getItem().getValues().get("value")).isEqualTo("false");
        assertThat(propertyData.getItem().getValues().get("startedAtDate")).isEqualTo(startDate);
        assertThat(propertyData.getItem().getValues().get("startedAtTime")).isEqualTo(startTime);
        assertThat(propertyData.getItem().getValues().get("endedAtDate")).isEqualTo(endDate);
        assertThat(propertyData.getItem().getValues().get("endedAtTime")).isEqualTo(endTime);
    }

    @Test
    void updateSortingCenterPropertyWithoutEndDate() {
        Long propertyId = lmsSortingCenterPropertyFacade.createProperty(SORTING_CENTER_ID,
                SortingCenterPropertyDto.builder()
                        .name(UserProperties.CALL_TO_RECIPIENT_ENABLED.getName())
                        .value("true")
                        .build());

        assertThat(propertyId).isGreaterThan(0L);

        Optional<SortingCenterPropertyEntity> propertyOpt =
                sortingCenterPropertyService.findByNameAndSortingCenterId(
                        UserProperties.CALL_TO_RECIPIENT_ENABLED.getName(), SORTING_CENTER_ID);
        assertThat(propertyOpt).isPresent();
        assertThat(propertyOpt.get().getValue()).isEqualTo("true");
        assertThat(propertyOpt.get().getStartedAt()).isNull();
        assertThat(propertyOpt.get().getEndedAt()).isNull();

        LocalDate startDate = LocalDate.now();
        LocalTime startTime = LocalTime.now();
        DetailData propertyData = lmsSortingCenterPropertyFacade.updateProperty(
                SortingCenterPropertyDto.builder()
                        .id(propertyId)
                        .name(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName())
                        .value("false")
                        .startedAtDate(startDate)
                        .startedAtTime(startTime)
                        .build());
        assertThat(propertyData).isNotNull();
        assertThat(propertyData.getItem().getValues().get("value")).isEqualTo("false");
        assertThat(propertyData.getItem().getValues().get("startedAtDate")).isEqualTo(startDate);
        assertThat(propertyData.getItem().getValues().get("startedAtTime")).isEqualTo(startTime);
        assertThat(propertyData.getItem().getValues().get("endedAtDate")).isNull();
        assertThat(propertyData.getItem().getValues().get("endedAtTime")).isNull();
    }

    @Test
    void updateSortingCenterPropertyWithoutTime() {
        Long propertyId = lmsSortingCenterPropertyFacade.createProperty(SORTING_CENTER_ID,
                SortingCenterPropertyDto.builder()
                        .name(UserProperties.CALL_TO_RECIPIENT_ENABLED.getName())
                        .value("true")
                        .build());

        assertThat(propertyId).isGreaterThan(0L);

        Optional<SortingCenterPropertyEntity> propertyOpt =
                sortingCenterPropertyService.findByNameAndSortingCenterId(
                        UserProperties.CALL_TO_RECIPIENT_ENABLED.getName(), SORTING_CENTER_ID);
        assertThat(propertyOpt).isPresent();
        assertThat(propertyOpt.get().getValue()).isEqualTo("true");
        assertThat(propertyOpt.get().getStartedAt()).isNull();
        assertThat(propertyOpt.get().getEndedAt()).isNull();

        LocalDate startDate = LocalDate.now();
        DetailData propertyData = lmsSortingCenterPropertyFacade.updateProperty(
                SortingCenterPropertyDto.builder()
                        .id(propertyId)
                        .name(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName())
                        .value("false")
                        .startedAtDate(startDate)
                        .build());
        assertThat(propertyData).isNotNull();
        assertThat(propertyData.getItem().getValues().get("value")).isEqualTo("false");
        assertThat(propertyData.getItem().getValues().get("startedAtDate")).isEqualTo(startDate);
        assertThat(propertyData.getItem().getValues().get("startedAtTime")).isEqualTo(LocalTime.MIDNIGHT);
    }


    @Test
    void deleteSortingCenterProperty() {
        Long propertyId = lmsSortingCenterPropertyFacade.createProperty(SORTING_CENTER_ID,
                SortingCenterPropertyDto.builder()
                        .name(UserProperties.CALL_TO_RECIPIENT_ENABLED.getName())
                        .value("true")
                        .build());

        assertThat(propertyId).isGreaterThan(0L);

        lmsSortingCenterPropertyFacade.deleteProperties(IdsDto.builder()
                .ids(List.of(propertyId))
                .build());

        Optional<SortingCenterPropertyEntity> propertyOpt = sortingCenterPropertyService.findByNameAndSortingCenterId(
                        UserProperties.CALL_TO_RECIPIENT_ENABLED.getName(), SORTING_CENTER_ID);
        assertThat(propertyOpt).isEmpty();
    }

}
