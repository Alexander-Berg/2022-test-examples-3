package ru.yandex.market.logistics.management.service.yado;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.DeliveryInterval;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamType;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamValue;
import ru.yandex.market.logistics.management.domain.entity.PartnerRoute;
import ru.yandex.market.logistics.management.domain.entity.Schedule;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.exception.JobFailedException;
import ru.yandex.market.logistics.management.repository.PartnerExternalParamTypeRepository;
import ru.yandex.market.logistics.management.repository.yado.YadoDao;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TransactionalUtils;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@CleanDatabase
@Transactional
@ParametersAreNonnullByDefault
@Disabled("Ignore until DELIVERY-20177")
class YadoServiceTest extends AbstractContextualTest {

    private static final String[] IGNORING_FIELDS = {"created", "updated", "externalParamValues",
        "deliveryIntervals.id", "deliveryIntervals.schedule.id", "deliveryIntervals.schedule.scheduleDays.id"};

    @Autowired
    private YadoService yadoService;

    @Autowired
    private PartnerExternalParamTypeRepository paramTypeRepository;

    @Autowired
    private TransactionalUtils transactionalUtils;

    @Autowired
    private YadoDao dao;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("Синхронизовать новых партнеров")
    @Sql("/data/repository/partner_external_param_types.sql")
    void insertNewPartners() {
        when(dao.findDeliveryServiceIds()).thenReturn(ImmutableList.of(1L, 2L));
        when(dao.getDeliveryService(eq(1L), anyMap())).thenReturn(createPartner1());
        when(dao.getDeliveryService(eq(2L), anyMap())).thenReturn(createPartner2());

        yadoService.syncPartners();

        List<Partner> partners = transactionalUtils.loadFullPartners();

        softly.assertThat(partners)
            .usingElementComparatorIgnoringFields(IGNORING_FIELDS)
            .isEqualTo(ImmutableList.of(createPartner1(), createPartner2()));
    }

    @Test
    @DisplayName("Синхронизовать обновленное расписание существующего партнера")
    @Sql({
        "/data/repository/partner_external_param_types.sql",
        "/data/service/yado/partner1.sql"
    })
    void updatePartnerIntervals() {
        when(dao.findDeliveryServiceIds()).thenReturn(ImmutableList.of(1L));
        when(dao.getDeliveryService(eq(1L), anyMap())).thenReturn(createPartner1());

        yadoService.syncPartners();

        Partner expected = createPartner1();

        List<Partner> partners = transactionalUtils.loadFullPartners();
        softly.assertThat(partners)
            .usingElementComparatorIgnoringFields(IGNORING_FIELDS)
            .isEqualTo(ImmutableList.of(expected));
    }

    @Test
    @DisplayName("Ошибка синхронизации партнера неподходящего типа")
    @Sql({
        "/data/repository/partner_external_param_types.sql",
        "/data/service/yado/partner1.sql",
        "/data/service/yado/set_xdoc_type_to_partner1.sql"
    })
    void errorUpdateXdocPartner() {
        when(dao.findDeliveryServiceIds()).thenReturn(ImmutableList.of(1L));
        when(dao.getDeliveryService(eq(1L), anyMap())).thenReturn(createPartner1());

        softly.assertThatThrownBy(() -> yadoService.syncPartners())
            .isInstanceOf(JobFailedException.class)
            .hasMessage("Errors during partners sync: Cannot sync partner '1', " +
                "error: Cannot sync partner 1, wrong type XDOC");

        List<Partner> partners = transactionalUtils.loadFullPartners();
    }

    @Test
    @DisplayName("Синхронизовать удаленное расписание существующего партнера")
    @Sql({
        "/data/repository/partner_external_param_types.sql",
        "/data/service/yado/partner1.sql"
    })
    void deletePartnerIntervals() {
        Partner partnerMock = createPartner1();
        partnerMock.getDeliveryIntervals().clear();
        when(dao.findDeliveryServiceIds()).thenReturn(ImmutableList.of(1L));
        when(dao.getDeliveryService(eq(1L), anyMap())).thenReturn(partnerMock);

        yadoService.syncPartners();

        Partner expected = createPartner1();
        expected.getDeliveryIntervals().clear();

        List<Partner> partners = transactionalUtils.loadFullPartners();
        softly.assertThat(partners)
            .usingElementComparatorIgnoringFields(IGNORING_FIELDS)
            .isEqualTo(ImmutableList.of(expected));
    }

    private Partner createPartner1() {
        return new Partner()
            .setId(1L)
            .setMarketId(1L)
            .setPartnerType(PartnerType.DELIVERY)
            .setName("Delivery service 1")
            .setReadableName("ReadableName")
            .setRating(1)
            .setBillingClientId(123L)
            .setTrackingType("status1")
            .setDomain("www.test-domain-1.com")
            .addExternalParams(createExternalParams())
            .addDeliveryIntervals(createIntervals(1000))
            .setAutoSwitchStockSyncEnabled(false)
            .setStockSyncEnabled(false)
            .setStatus(PartnerStatus.ACTIVE);
    }

    private Partner createPartner2() {
        return new Partner()
            .setId(2L)
            .setPartnerType(PartnerType.DELIVERY)
            .setName("Delivery service 2")
            .setRating(2)
            .setBillingClientId(12L)
            .setTrackingType("status2")
            .setDomain("www.test-domain-2.com")
            .addDeliveryIntervals(createIntervals(2000))
            .addPartnerRoute(createPartnerRoute())
            .setStatus(PartnerStatus.ACTIVE);
    }

    @Nonnull
    private Collection<DeliveryInterval> createIntervals(int random) {
        return ImmutableList.of(
            new DeliveryInterval()
                .setSchedule(new Schedule().setScheduleDays(generateScheduleDays()))
                .setLocationId(110 + random),
            new DeliveryInterval()
                .setSchedule(new Schedule().setScheduleDays(generateScheduleDays()))
                .setLocationId(120 + random),
            new DeliveryInterval()
                .setSchedule(new Schedule().setScheduleDays(generateScheduleDays()))
                .setLocationId(130 + random)
        );
    }

    @Nonnull
    private Set<PartnerExternalParamValue> createExternalParams() {
        List<PartnerExternalParamType> paramTypes = paramTypeRepository.findAll();
        return new HashSet<>(Arrays.asList(
            new PartnerExternalParamValue(paramTypes.get(0), "test-value-for-type-0-created"),
            new PartnerExternalParamValue(paramTypes.get(1), "test-value-for-type-1-created"))
        );
    }

    @Nonnull
    private static Set<ScheduleDay> generateScheduleDays() {
        Set<ScheduleDay> result = new HashSet<>();
        for (int i = 1; i < 15; i++) {
            result.add(new ScheduleDay()
                .setDay(i % 7)
                .setFrom(LocalTime.of(i, i))
                .setTo(LocalTime.of(1 + i, 1 + i))
            );
        }
        return result;
    }

    @Nonnull
    private PartnerRoute createPartnerRoute() {
        return new PartnerRoute()
            .setLocationFrom(1)
            .setLocationTo(2);
    }
}
