package ru.yandex.market.tpl.core.domain.routing;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.schedule.UserScheduleStatus;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.api.model.user.UserType;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.tag.RequiredRoutingTag;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTag;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.external.routing.api.AdditionalTag;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingLocationType;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleData;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class RoutingCourierMapperTest extends TplAbstractTest {

    private static final String ORDER_PVZ_TAG = "order_pvz";
    private static final String DROPSHIP_TAG = "dropship";
    private static final String DELIVERY_TAG = "client";

    private final RoutingCourierMapper routingCourierMapper;
    private final TestUserHelper testUserHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final Clock clock;

    @Test
    void mapCourierFromRuleWithDropshipTagsWithMixEnabled() {
        //given
        List<String> tags = List.of(DROPSHIP_TAG);
        User user = buildUser(tags);
        clearEntities(user);

        //when
        RoutingCourier routingCourier = mapRoutingCourier(user, true, false);

        //then
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DROPSHIP.getCode());
        assertThat(routingCourier.getExcludedTags()).isEmpty();
    }

    @Test
    void mapCourierFromRuleWithDropshipTagsWithMixDisabled() {
        //given
        List<String> tags = List.of(DROPSHIP_TAG);
        User user = buildUser(tags);
        clearEntities(user);

        //when
        RoutingCourier routingCourier = mapRoutingCourier(user, false, false);

        //then
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DROPSHIP.getCode());
        assertThat(routingCourier.getExcludedTags()).doesNotContain(RequiredRoutingTag.DROPSHIP.getCode());
    }

    @Test
    void mapCourierFromRuleWithDropshipAndDropOffagsWithMixEnabled() {
        //given
        List<String> tags = List.of(RequiredRoutingTag.DROPSHIP.getCode(),
                RequiredRoutingTag.DROPOFF_CARGO_RETURN.getCode());
        User user = buildUser(tags);
        clearEntities(user);

        //when
        RoutingCourier routingCourier = mapRoutingCourier(user, true, false);

        //then
        assertThat(routingCourier.getAdditionalTags()).containsAll(tags);
        assertThat(routingCourier.getExcludedTags()).isEmpty();
    }

    @Test
    void mapCourierFromRuleWithNoTags() {
        List<String> tags = List.of(ORDER_PVZ_TAG);
        User user = buildUser(tags);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, false, false);

        assertThat(routingCourier.getServicedLocationType()).isEqualTo(RoutingLocationType.delivery);
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DELIVERY.getCode());
        assertThat(routingCourier.getAdditionalTags()).contains(ORDER_PVZ_TAG);
        assertThat(routingCourier.getAdditionalTags()).doesNotContain(RequiredRoutingTag.DROPSHIP.getCode());
    }

    @Test
    void mapCourierFromRuleWithDropshipsTagAndOffFlag() {
        List<String> tags = List.of(DROPSHIP_TAG);
        User user = buildUser(tags);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, false, true);
        assertThat(routingCourier.getServicedLocationType()).isEqualTo(RoutingLocationType.pickup);
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DROPSHIP.getCode());
        assertThat(routingCourier.getAdditionalTags()).doesNotContain(RequiredRoutingTag.DELIVERY.getCode());
    }

    @Test
    void mapCourierFromRuleWithDeliveryTagAndOffFlag() {
        List<String> tags = List.of(DELIVERY_TAG);
        User user = buildUser(tags);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, false, false);
        assertThat(routingCourier.getServicedLocationType()).isEqualTo(RoutingLocationType.delivery);
        assertThat(routingCourier.getAdditionalTags()).doesNotContain(RequiredRoutingTag.DROPSHIP.getCode());
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DELIVERY.getCode());
    }

    @Test
    void mapCourierFromRuleWithDeliveryAndDropshipsTagAndOffFlagAndDropshipExist() {
        List<String> tags = List.of(DROPSHIP_TAG, DELIVERY_TAG);
        User user = buildUser(tags);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, false, true);
        assertThat(routingCourier.getServicedLocationType()).isEqualTo(RoutingLocationType.pickup);
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DROPSHIP.getCode());
        assertThat(routingCourier.getAdditionalTags()).doesNotContain(RequiredRoutingTag.DELIVERY.getCode());
    }

    @Test
    void mapCourierFromRuleWithDeliveryAndDropshipsTagAndOffFlagAndDropshipNotExist() {
        List<String> tags = List.of(DROPSHIP_TAG, DELIVERY_TAG);
        User user = buildUser(tags);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, false, false);
        assertThat(routingCourier.getServicedLocationType()).isEqualTo(RoutingLocationType.delivery);
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DROPSHIP.getCode());
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DELIVERY.getCode());
    }

    @Test
    void mapCourierFromRuleWithDeliveryTagAndOnFlag() {
        List<String> tags = List.of(DELIVERY_TAG);
        User user = buildUser(tags);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, true, false);
        assertThat(routingCourier.getServicedLocationType()).isEqualTo(RoutingLocationType.delivery);
        assertThat(routingCourier.getAdditionalTags()).doesNotContain(RequiredRoutingTag.DROPSHIP.getCode());
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DELIVERY.getCode());
    }

    @Test
    void mapCourierFromRuleWithDropshipsTagAndOnFlag() {
        List<String> tags = List.of(DROPSHIP_TAG);
        User user = buildUser(tags);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, true, true);
        assertThat(routingCourier.getServicedLocationType()).isEqualTo(RoutingLocationType.pickup);
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DROPSHIP.getCode());
        assertThat(routingCourier.getAdditionalTags()).doesNotContain(RequiredRoutingTag.DELIVERY.getCode());
    }

    @Test
    void mapCourierFromRuleWithDeliveryAndDropshipsTagAndOnFlag() {
        List<String> tags = List.of(DROPSHIP_TAG, DELIVERY_TAG);
        User user = buildUser(tags);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, true, true);
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DROPSHIP.getCode());
        assertThat(routingCourier.getAdditionalTags()).contains(RequiredRoutingTag.DELIVERY.getCode());
    }

    @Test
    void addsOnlyPartnerTagForPartnerCourier() {
        List<String> tags = List.of(ORDER_PVZ_TAG);
        User user = testUserHelper.createUserWithTransportTags(9000L, tags);
        UserUtil.setUserType(user, UserType.PARTNER);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, true, true);
        assertThat(routingCourier.getAdditionalTags()).contains(
                RequiredRoutingTag.ONLY_PARTNER.getCode()
        );
    }

    @Test
    void shouldNotFilterVehicleTagsForPartnerCourier() {
        List<String> tags = List.of(ORDER_PVZ_TAG, DROPSHIP_TAG, DELIVERY_TAG);
        User user = testUserHelper.createUserWithTransportTags(9000L, tags);
        UserUtil.setUserType(user, UserType.PARTNER);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, true, true);
        assertThat(routingCourier.getAdditionalTags()).containsAll(
                user.getTransportType().getRoutingOrderTags().stream()
                        .map(RoutingOrderTag::getName)
                        .collect(Collectors.toList())
        );
    }

    @Test
    void checkVehicleTagsForSelfEmployerCourierWithoutJewelry() {
        List<String> tags = List.of(ORDER_PVZ_TAG, DROPSHIP_TAG, DELIVERY_TAG);
        User user = testUserHelper.createUserWithTransportTags(9000L, tags);
        UserUtil.setUserType(user, UserType.SELF_EMPLOYED);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, true, true);
        assertThat(routingCourier.getAdditionalTags()).doesNotContain(
                AdditionalTag.JEWELRY.getCode());
    }

    @Test
    void checkVehicleTagsForPartnerEmployerCourierWithJewelry() {
        List<String> tags = List.of(ORDER_PVZ_TAG);
        User user = buildUser(tags);
        clearEntities(user);

        RoutingCourier routingCourier = mapRoutingCourier(user, false, false);

        assertThat(routingCourier.getAdditionalTags()).contains(
                AdditionalTag.JEWELRY.getCode());
    }

    private RoutingCourier mapRoutingCourier(User user, boolean mixDropshipWithDelivery, boolean isDropshipExist) {
        UserScheduleRule userScheduleRule = new UserScheduleRule();
        SortingCenter sortingCenter = testUserHelper.sortingCenter(1L);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(sortingCenter,
                SortingCenterProperties.MIX_DROPSHIP_WITH_DELIVERY_ENABLED, mixDropshipWithDelivery);
        userScheduleRule.init(
                user,
                UserScheduleType.ALWAYS_WORKS,
                sortingCenter,
                LocalDate.now(clock),
                LocalDate.now(clock),
                LocalDate.now(clock),
                new UserScheduleData(CourierVehicleType.CAR, RelativeTimeInterval.valueOf("09:00-18:00")),
                UserScheduleStatus.READY,
                UserScheduleType.ALWAYS_WORKS.getMaskWorkDays()
        );
        return routingCourierMapper.mapCourierFromRule(
                userScheduleRule,
                Collections.emptyMap(),
                Set.of(1),
                isDropshipExist
        );
    }

    private User buildUser(List<String> tags) {
        return testUserHelper.createUserWithTransportTags(1L, tags);
    }

    private void clearEntities(User user) {
        clearAfterTest(user.getTransportType());
    }
}
