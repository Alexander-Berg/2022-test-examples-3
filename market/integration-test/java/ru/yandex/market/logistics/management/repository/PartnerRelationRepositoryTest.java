package ru.yandex.market.logistics.management.repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolationException;

import com.google.common.collect.Sets;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.PartnerShopDto;
import ru.yandex.market.logistics.management.domain.entity.Calendar;
import ru.yandex.market.logistics.management.domain.entity.CalendarDay;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelation;
import ru.yandex.market.logistics.management.domain.entity.PartnerShop;
import ru.yandex.market.logistics.management.domain.entity.PartnerSubtype;
import ru.yandex.market.logistics.management.domain.entity.PlatformClient;
import ru.yandex.market.logistics.management.domain.entity.PlatformClientPartner;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.ShipmentType;
import ru.yandex.market.logistics.management.domain.entity.validation.EnabledPartnerRelationHasAcceptedToPartnerSubtype;
import ru.yandex.market.logistics.management.service.client.PartnerService;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TransactionalUtils;

import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_1;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_2;
import static ru.yandex.market.logistics.management.util.TestPartnerSubtypes.SUB_TYPE_3;

@CleanDatabase
@SuppressWarnings("checkstyle:MagicNumber")
class PartnerRelationRepositoryTest extends AbstractContextualTest {

    private static final long PLATFORM_CLIENT_ID = 1L;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private PartnerSubtypeRepository partnerSubtypeRepository;

    @Autowired
    private PartnerRelationRepository relationRepository;

    @Autowired
    private PlatformClientRepository platformClientRepository;

    @Autowired
    private PlatformClientPartnerRepository platformClientPartnerRepository;

    @Autowired
    private TransactionalUtils transactionalUtils;

    private Partner savedDelivery;
    private Partner savedFulfillment;
    private PartnerSubtype partnerSubtype1;
    private PartnerSubtype partnerSubtype2;
    private PartnerSubtype partnerSubtype3;

    @BeforeEach
    void setup() {
        preparePartnerSubtypes();
        prepareServices();
    }

    @Test
    void testInsertWithoutChildren() {
        preparePartnerRelation();
        List<PartnerRelation> found =
            relationRepository.findAll(
                platformClientRepository.getOne(PLATFORM_CLIENT_ID),
                PartnerService.WAREHOUSE_TYPES,
                EnumSet.of(PartnerType.DELIVERY)
            );
        softly.assertThat(found).hasSize(1);
        softly.assertThat(found.get(0).getFromPartner()).as("FulfillmentServices should be equal")
            .isEqualTo(savedFulfillment);
        softly.assertThat(found.get(0).getToPartner()).as("DeliveryServices should be equal")
            .isEqualTo(savedDelivery);
    }

    @Test
    void testFindAllShops() {
        preparePartnerRelation();
        prepareFulfillmentWithDisabledRelation();
        Set<PartnerShopDto> allShops = relationRepository.
            findAllShops(platformClientRepository.getOne(PLATFORM_CLIENT_ID));
        softly.assertThat(allShops)
            .as("All shops should be mapped to proper Partners")
            .extracting("partnerId", "shopId", "isDefault")
            .containsExactlyInAnyOrder(
                new Tuple(2L, 3, true),
                new Tuple(2L, 4, false)
            );
    }

    @Test
    void shouldCreateDisabledPartnerRelation_WithIneligibleToPartnerSubtype() {
        // given:
        Partner partnerWithSubtype3 = preparePartner(3L, partnerSubtype3);

        // when: сохраняем новую выключенную связь, у которой партнёр назначения имеет неподходящий подтип
        PartnerRelation newDisabledRelation = relation(false, partnerWithSubtype3);
        PartnerRelation savedDisabledRelation = relationRepository.save(newDisabledRelation);

        // then: успешное сохранение новой активной связи
        softly.assertThat(savedDisabledRelation.getId()).isNotNull();
    }

    @Test
    void shouldFail_WhenTryToCreateEnabledPartnerRelation_WithIneligibleToPartnerSubtype() {
        // given:
        Partner partnerWithSubtype3 = preparePartner(3L, partnerSubtype3);

        // expect: ошибка при попытке сохранения новой активной связи,
        // у которой партнёр назначения имеет неподходящий подтип
        PartnerRelation newEnabledRelation = relation(true, partnerWithSubtype3);
        softly.assertThatThrownBy(() -> relationRepository.save(newEnabledRelation))
            .isInstanceOf(ConstraintViolationException.class)
            .hasMessageContaining(EnabledPartnerRelationHasAcceptedToPartnerSubtype.ERROR_MESSAGE);
    }

    @Test
    void shouldFail_WhenTryToEnablePartnerRelation_WithIneligibleToPartnerSubtype() {
        // given:
        Partner partnerWithSubtype3 = preparePartner(3L, partnerSubtype3);
        PartnerRelation savedDisabledRelation = prepareRelation(false, partnerWithSubtype3);

        // expect: ошибка при попытке активации связи, у которой партнёр назначения имеет неподходящий подтип
        softly.assertThatThrownBy(() -> relationRepository.save(savedDisabledRelation.setEnabled(true)))
            .hasRootCauseInstanceOf(ConstraintViolationException.class);
    }

    private void preparePartnerSubtypes() {
        partnerSubtype1 = preparePartnerSubtype(SUB_TYPE_1);
        partnerSubtype2 = preparePartnerSubtype(SUB_TYPE_2);
        partnerSubtype3 = preparePartnerSubtype(SUB_TYPE_3);
    }

    private PartnerSubtype preparePartnerSubtype(final PartnerSubtype partnerSubtype) {
        PartnerSubtype savedPartnerSubtype = partnerSubtypeRepository.save(partnerSubtype);
        assert savedPartnerSubtype.getId().equals(SUB_TYPE_3.getId())
            : "saved partner subtype id must match with input one";
        return savedPartnerSubtype;
    }

    private Partner preparePartner(long id, PartnerSubtype partnerSubtype) {
        Partner partner = new Partner()
            .setId(id)
            .setName("Partner " + id)
            .setPartnerType(partnerSubtype.getPartnerType())
            .setPartnerSubtype(partnerSubtype)
            .setStatus(PartnerStatus.ACTIVE);
        return partnerRepository.save(partner);
    }

    private PartnerRelation relation(boolean enabled, Partner toPartner) {
        return new PartnerRelation()
            .setEnabled(enabled)
            .setToPartner(toPartner)
            .setHandlingTime(1)
            .setReturnPartner(savedFulfillment)
            .setShipmentType(ShipmentType.WITHDRAW)
            .setFaults(Set.of());
    }

    private PartnerRelation prepareRelation(boolean enabled, Partner toPartner) {
        PartnerRelation relation = relation(enabled, toPartner);
        return relationRepository.save(relation);
    }

    private void preparePartnerRelation() {
        PartnerRelation relation = new PartnerRelation()
            .setFromPartner(savedFulfillment)
            .setToPartner(savedDelivery)
            .setReturnPartner(savedFulfillment)
            .setEnabled(true)
            .setHandlingTime(1)
            .setShipmentType(ShipmentType.WITHDRAW);
        relationRepository.save(relation);
    }

    private void prepareServices() {
        Partner ds = new Partner()
            .setId(1L)
            .setName("Delivery service 1")
            .setPartnerType(PartnerType.DELIVERY)
            .setRating(1)
            .setBillingClientId(123L)
            .setStatus(PartnerStatus.ACTIVE)
            .addShops(createDeliveryShops())
            .setCalendarId(createPartnerCalendar(1L))
            .setTrackingType("status1");
        savedDelivery = partnerRepository.save(ds);

        Partner fs = new Partner()
            .setId(2L)
            .setName("Fulfillment service 1")
            .setPartnerType(PartnerType.FULFILLMENT)
            .setRating(1)
            .setBillingClientId(123L)
            .addShops(createFulfilmentShops())
            .setCalendarId(createPartnerCalendar(2L))
            .setStatus(PartnerStatus.ACTIVE);
        savedFulfillment = partnerRepository.save(fs);

        PlatformClient pc = new PlatformClient();
        pc.setId(PLATFORM_CLIENT_ID);
        pc.setName("Тестовый клиент");
        platformClientRepository.save(pc);

        PlatformClientPartner pcp = new PlatformClientPartner()
            .setPartner(fs)
            .setPlatformClient(pc)
            .setStatus(PartnerStatus.ACTIVE);
        platformClientPartnerRepository.save(pcp);
    }

    private void prepareFulfillmentWithDisabledRelation() {
        Partner fs = new Partner()
            .setId(3L)
            .setName("Fulfillment service 2")
            .setPartnerType(PartnerType.FULFILLMENT)
            .setRating(1)
            .setBillingClientId(123L)
            .addShop(createShop(5, true))
            .setStatus(PartnerStatus.ACTIVE);

        fs = partnerRepository.save(fs);

        PartnerRelation relation = new PartnerRelation()
            .setFromPartner(fs)
            .setToPartner(savedDelivery)
            .setReturnPartner(fs)
            .setEnabled(false)
            .setHandlingTime(1)
            .setShipmentType(ShipmentType.WITHDRAW);
        relationRepository.save(relation);
    }

    private Set<PartnerShop> createDeliveryShops() {
        return new HashSet<>(Arrays.asList(
            createShop(1, true),
            createShop(2, false)
        ));
    }

    private Long createPartnerCalendar(Long id) {
        Calendar calendar = new Calendar().addCalendarDays(createDays(id));
        transactionalUtils.saveInTransaction(calendar);
        return calendar.getId();
    }

    private Set<CalendarDay> createDays(long partnerId) {
        return Sets.newHashSet(
            new CalendarDay()
                .setDay(LocalDate.now().plusDays(partnerId))
                .setIsHoliday(true),
            new CalendarDay()
                .setDay(LocalDate.now().plusDays(3 + partnerId))
                .setIsHoliday(true)
        );
    }

    private Set<PartnerShop> createFulfilmentShops() {
        return new HashSet<>(Arrays.asList(
            createShop(3, true),
            createShop(4, false)
        ));
    }

    private PartnerShop createShop(int id, boolean isDefault) {
        return new PartnerShop()
            .setDefault(isDefault)
            .setShopId(id);
    }
}
