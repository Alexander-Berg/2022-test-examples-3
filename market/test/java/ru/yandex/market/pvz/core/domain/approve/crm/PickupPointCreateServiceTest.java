package ru.yandex.market.pvz.core.domain.approve.crm;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.approve.MbiCabinetService;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointCommandService;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointParams;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointParamsMapper;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointParamsMapper;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.schedule.PickupPointScheduleParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory.CrmPrePickupPointTestParams.DEFAULT_CLIENT_AREA;
import static ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory.CrmPrePickupPointTestParams.DEFAULT_WAREHOUSE_AREA;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_IS_WORKING_DAY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_FROM;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_TO;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleTestParams.DEFAULT_WORKS_ON_HOLIDAY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_BRAND_REGION;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CARD_COMPENSATION_RATE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CASH_COMPENSATION_RATE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_TRANSMISSION_REWARD;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointCreateServiceTest {

    private static final long DEFAULT_OWNER_UID = 42;
    private static final long DEFAULT_CAMPAIGN_ID = 42;


    private final TestBrandRegionFactory brandRegionFactory;
    private final TestCrmPrePickupPointFactory crmPrePickupPointFactory;

    private final TestableClock clock;
    private final TransactionTemplate transactionTemplate;

    private final CrmPrePickupPointCommandService crmPrePickupPointCommandService;
    private final PickupPointCreateService pickupPointCreateService;
    private final PickupPointRepository pickupPointRepository;
    private final CrmPrePickupPointParamsMapper crmPrePickupPointParamsMapper;
    private final PickupPointParamsMapper pickupPointParamsMapper;
    private final TestPickupPointFactory pickupPointFactory;

    @MockBean
    private MbiCabinetService mbiCabinetService;

    @BeforeEach
    void setup() {
        brandRegionFactory.createDefaults();

        clock.setFixed(Instant.EPOCH, DateTimeUtil.DEFAULT_ZONE_ID);

        when(mbiCabinetService.createForPrePickupPoint(any(), any())).thenReturn(new SimpleShopRegistrationResponse() {{
            setOwnerId(DEFAULT_OWNER_UID);
            setCampaignId(DEFAULT_CAMPAIGN_ID);
        }});
    }

    @Test
    void testCreate() {
        pickupPointFactory.addFirstDeactivationReason();
        CrmPrePickupPointParams params = crmPrePickupPointFactory.create();

        params.setCashCompensation(DEFAULT_CASH_COMPENSATION_RATE);
        params.setCardCompensation(DEFAULT_CARD_COMPENSATION_RATE);
        params.setOrderTransmissionReward(DEFAULT_TRANSMISSION_REWARD);

        params.setBrandingType(PickupPointBrandingType.FULL);
        params.setBrandedSince(LocalDate.now(clock));
        params.setBrandRegion(DEFAULT_BRAND_REGION);

        CrmPrePickupPointParams approved = crmPrePickupPointCommandService.approveFromCrm(params);
        PickupPoint created = pickupPointCreateService.createPickupPoint(approved);

        transactionTemplate.execute(s -> {
            PickupPoint pickupPoint = pickupPointRepository.findByIdOrThrow(created.getId());

            assertThat(pickupPoint.getCrmPrePickupPoint()).isNotNull();
            assertThat(pickupPoint.getCrmPrePickupPoint().getId()).isEqualTo(params.getId());
            assertThat(pickupPoint.getLegalPartner().getId()).isEqualTo(params.getLegalPartnerId());
            assertThat(pickupPoint.getPvzMarketId()).isEqualTo(DEFAULT_CAMPAIGN_ID);

            assertThat(pickupPoint.getName()).isEqualTo(params.getName());
            assertThat(pickupPoint.getPhone()).isEqualTo(params.getPhone());
            assertThat(pickupPoint.getPrepayAllowed()).isEqualTo(false);
            assertThat(pickupPoint.getCardAllowed()).isEqualTo(false);
            assertThat(pickupPoint.getCashAllowed()).isEqualTo(false);
            assertThat(pickupPoint.getInstruction()).isEqualTo(params.getInstruction());

            assertThat(pickupPointParamsMapper.map(pickupPoint.getLocation()))
                    .isEqualTo(crmPrePickupPointParamsMapper.map(params.getLocation()));

            assertThat(pickupPoint.getSchedule().getWorksOnHoliday()).isEqualTo(!DEFAULT_WORKS_ON_HOLIDAY);
            assertThat(pickupPointParamsMapper.map(pickupPoint.getSchedule()).getScheduleDays())
                    .containsExactlyInAnyOrder(
                            PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                    .dayOfWeek(DayOfWeek.MONDAY)
                                    .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                    .timeFrom(DEFAULT_TIME_FROM)
                                    .timeTo(DEFAULT_TIME_TO)
                                    .build(),
                            PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                    .dayOfWeek(DayOfWeek.TUESDAY)
                                    .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                    .timeFrom(DEFAULT_TIME_FROM)
                                    .timeTo(DEFAULT_TIME_TO)
                                    .build(),
                            PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                    .dayOfWeek(DayOfWeek.WEDNESDAY)
                                    .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                    .timeFrom(DEFAULT_TIME_FROM)
                                    .timeTo(DEFAULT_TIME_TO)
                                    .build(),
                            PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                    .dayOfWeek(DayOfWeek.THURSDAY)
                                    .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                    .timeFrom(DEFAULT_TIME_FROM)
                                    .timeTo(DEFAULT_TIME_TO)
                                    .build(),
                            PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                    .dayOfWeek(DayOfWeek.FRIDAY)
                                    .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                    .timeFrom(DEFAULT_TIME_FROM)
                                    .timeTo(DEFAULT_TIME_TO)
                                    .build(),
                            PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                    .dayOfWeek(DayOfWeek.SATURDAY)
                                    .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                    .timeFrom(DEFAULT_TIME_FROM)
                                    .timeTo(DEFAULT_TIME_TO)
                                    .build(),
                            PickupPointScheduleParams.PickupPointScheduleDayParams.builder()
                                    .dayOfWeek(DayOfWeek.SUNDAY)
                                    .isWorkingDay(DEFAULT_IS_WORKING_DAY)
                                    .timeFrom(DEFAULT_TIME_FROM)
                                    .timeTo(DEFAULT_TIME_TO)
                                    .build()
                    );

            assertThat(pickupPoint.getCashOrderCompensationRate()).isEqualTo(DEFAULT_CASH_COMPENSATION_RATE);
            assertThat(pickupPoint.getCardOrderCompensationRate()).isEqualTo(DEFAULT_CARD_COMPENSATION_RATE);
            assertThat(pickupPoint.getTransmissionReward()).isCloseTo(DEFAULT_TRANSMISSION_REWARD, withPercentage(0));

            assertThat(pickupPoint.getBrandingType()).isEqualTo(PickupPointBrandingType.FULL);
            assertThat(pickupPoint.getActualBrandData().getBrandedSince()).isEqualTo(params.getBrandedSince());
            assertThat(pickupPoint.getActualBrandData().getBrandRegion().getRegion())
                    .isEqualTo(params.getBrandRegion());

            assertThat(pickupPoint.getWarehouseArea()).isEqualByComparingTo(DEFAULT_WAREHOUSE_AREA);
            assertThat(pickupPoint.getClientArea()).isEqualByComparingTo(DEFAULT_CLIENT_AREA);

            return null;
        });
    }

}
