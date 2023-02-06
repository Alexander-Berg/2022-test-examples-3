package ru.yandex.travel.hotels.common.partners.bnovo.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.bnovo.model.RatePlan;
import ru.yandex.travel.hotels.common.partners.bnovo.model.Service;
import ru.yandex.travel.hotels.common.partners.bnovo.model.ServiceBoardType;
import ru.yandex.travel.hotels.common.partners.bnovo.model.ServiceType;
import ru.yandex.travel.hotels.proto.EPansionType;

import static org.assertj.core.api.Assertions.assertThat;

public class BNovoPansionHelperTests {

    @Test
    public void testNoPansion() {
        var plan = RatePlan.builder()
                .nutrition(new Boolean[]{false, false, false})
                .build();
        assertThat(BNovoPansionHelper.getPansionType(plan, Collections.emptyMap())).isEqualTo(EPansionType.PT_RO);
    }

    @Test
    public void testBreakfastFlag() {
        var plan = RatePlan.builder()
                .nutrition(new Boolean[]{true, false, false})
                .build();
        assertThat(BNovoPansionHelper.getPansionType(plan, Collections.emptyMap())).isEqualTo(EPansionType.PT_BB);
    }

    @Test
    public void testFBFlag() {
        var plan = RatePlan.builder()
                .nutrition(new Boolean[]{true, true, true})
                .build();
        assertThat(BNovoPansionHelper.getPansionType(plan, Collections.emptyMap())).isEqualTo(EPansionType.PT_FB);
    }

    @Test
    public void testServicesOverrideFlags() {
        var plan = RatePlan.builder()
                .nutrition(new Boolean[]{true, true, true})
                .boardFromServices(true)
                .build();
        assertThat(BNovoPansionHelper.getPansionType(plan, Collections.emptyMap())).isEqualTo(EPansionType.PT_RO);
    }

    @Test
    public void testServiceBreakfast() {
        var plan = RatePlan.builder()
                .boardFromServices(true)
                .additionalServicesIds(List.of(1L))
                .build();
        var services = Map.of(1L,
                service(1L, ServiceBoardType.BREAKFAST));
        assertThat(BNovoPansionHelper.getPansionType(plan, services)).isEqualTo(EPansionType.PT_BB);
    }

    @Test
    public void testMultiServiceHalfBoard() {
        var plan = RatePlan.builder()
                .boardFromServices(true)
                .additionalServicesIds(List.of(1L, 2L))
                .build();
        var services = Map.of(1L,
                service(1L, ServiceBoardType.BREAKFAST),
                2L, service(2L, ServiceBoardType.DINNER));
        assertThat(BNovoPansionHelper.getPansionType(plan, services)).isEqualTo(EPansionType.PT_HB);
    }


    @Test
    public void testMultiServiceHalfBoard2() {
        var plan = RatePlan.builder()
                .boardFromServices(true)
                .additionalServicesIds(List.of(1L, 2L))
                .build();
        var services = Map.of(1L,
                service(1L, ServiceBoardType.BREAKFAST),
                2L, service(2L, ServiceBoardType.LUNCH));
        assertThat(BNovoPansionHelper.getPansionType(plan, services)).isEqualTo(EPansionType.PT_BB);
    }

    @Test
    public void testMultiServiceFullBoard() {
        var plan = RatePlan.builder()
                .boardFromServices(true)
                .additionalServicesIds(List.of(1L, 2L, 3L))
                .build();
        var services = Map.of(1L,
                service(1L, ServiceBoardType.BREAKFAST),
                2L, service(2L, ServiceBoardType.LUNCH),
                3L, service(2L, ServiceBoardType.DINNER));
        assertThat(BNovoPansionHelper.getPansionType(plan, services)).isEqualTo(EPansionType.PT_FB);
    }

    @Test
    public void testMultiServiceLunchAndDinner() {
        var plan = RatePlan.builder()
                .boardFromServices(true)
                .additionalServicesIds(List.of(2L, 3L))
                .build();
        var services = Map.of(2L, service(2L, ServiceBoardType.LUNCH),
                3L, service(2L, ServiceBoardType.DINNER));
        assertThat(BNovoPansionHelper.getPansionType(plan, services)).isEqualTo(EPansionType.PT_BD);
    }


    @Test
    public void testPackageServiceHalfBoard() {
        var plan = RatePlan.builder()
                .boardFromServices(true)
                .additionalServicesIds(List.of(0L))
                .build();
        var services = Map.of(0L, packageService(0, 1L, 2L),
                1L, service(1, ServiceBoardType.BREAKFAST),
                2L, service(2, ServiceBoardType.DINNER));
        assertThat(BNovoPansionHelper.getPansionType(plan, services)).isEqualTo(EPansionType.PT_HB);
    }

    @Test
    public void testPackageServiceFullBoard() {
        var plan = RatePlan.builder()
                .boardFromServices(true)
                .additionalServicesIds(List.of(0L))
                .build();
        var services = Map.of(0L, packageService(0, 1L, 2L, 3L),
                1L, service(1, ServiceBoardType.BREAKFAST),
                2L, service(2, ServiceBoardType.DINNER),
                3L, service(3, ServiceBoardType.LUNCH));
        assertThat(BNovoPansionHelper.getPansionType(plan, services)).isEqualTo(EPansionType.PT_FB);
    }

    @Test
    public void testCompositeServiceFullBoard() {
        var plan = RatePlan.builder()
                .boardFromServices(true)
                .additionalServicesIds(List.of(0L, 1L))
                .build();
        var services = Map.of(0L, service(0L, ServiceBoardType.BREAKFAST),
                1L, packageService(1, 2L, 3L),
                2L, service(2, ServiceBoardType.DINNER),
                3L, service(3, ServiceBoardType.LUNCH));
        assertThat(BNovoPansionHelper.getPansionType(plan, services)).isEqualTo(EPansionType.PT_FB);
    }

    @Test
    public void testCompositeServiceFullBoardWithOverlap() {
        var plan = RatePlan.builder()
                .boardFromServices(true)
                .additionalServicesIds(List.of(0L, 1L))
                .build();
        var services = Map.of(0L, service(0L, ServiceBoardType.BREAKFAST),
                1L, packageService(1, 2L, 3L, 4L),
                2L, service(2, ServiceBoardType.DINNER),
                3L, service(3, ServiceBoardType.LUNCH),
                4L, service(4, ServiceBoardType.BREAKFAST));
        assertThat(BNovoPansionHelper.getPansionType(plan, services)).isEqualTo(EPansionType.PT_FB);
    }

    private Service service(long id, ServiceBoardType type) {
        return Service.builder()
                .id(id)
                .enabled(true)
                .type(ServiceType.BOARD)
                .boardType(type)
                .build();
    }

    private Service packageService(long id, Long... nested) {
        return Service.builder()
                .id(id)
                .enabled(true)
                .type(ServiceType.BOARD)
                .boardType(ServiceBoardType.PACKAGE)
                .packageAdditionalServicesIds(Arrays.asList(nested))
                .build();
    }


}
