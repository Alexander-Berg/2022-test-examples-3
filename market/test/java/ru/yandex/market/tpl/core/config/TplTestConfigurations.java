package ru.yandex.market.tpl.core.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnService;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.company.CompanyRoleRepository;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.partial_return_order.repository.PartialReturnOrderRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTagRepository;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.ShiftRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.ReceptionService;
import ru.yandex.market.tpl.core.domain.util.AddDeliveryTaskHelper;
import ru.yandex.market.tpl.core.domain.util.DeliveryTaskDataFactory;
import ru.yandex.market.tpl.core.external.routing.vrp.mapper.coordinate.GeoPointScaleSettingService;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.DeliveryTaskAddressToDeliveryAddressMapper;
import ru.yandex.market.tpl.core.service.lms.deliveryservice.DeliveryServiceCommandService;
import ru.yandex.market.tpl.core.service.order.TaskErrorSaver;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.service.user.transport.TransportRepository;
import ru.yandex.market.tpl.core.service.user.transport.TransportTypeRepository;
import ru.yandex.market.tpl.core.task.service.TaskService;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.mockito.Mockito.spy;

/**
 * @author kukabara
 */
public class TplTestConfigurations {

    @Configuration
    @Import({ClockConfig.class, ConfigurationProviderAdapter.class})
    public static class Domain {

        @Bean
        public TestUserHelper userHelper(
                UserRepository userRepository,
                ShiftRepository shiftRepository,
                UserShiftRepository userShiftRepository,
                CompanyRepository companyRepository,
                UserShiftCommandService commandService,
                UserShiftCommandDataHelper dataHelper,
                UserScheduleRuleRepository userScheduleRuleRepository,
                PartnerRepository<SortingCenter> scRepository,
                PartnerRepository<DeliveryService> dsRepository,
                Clock clock,
                ReceptionService receptionService,
                TaskErrorSaver taskErrorSaver,
                TransportRepository transportRepository,
                TransportTypeRepository transportTypeRepository,
                RoutingOrderTagRepository routingOrderTagRepository,
                UserPropertyService userPropertyService,
                CompanyDraftRepository companyDraftRepository,
                CompanyRoleRepository companyRoleRepository,
                PartnerRepository<DeliveryService> partnerRepository,
                DeliveryServiceCommandService deliveryServiceCommandService,
                PartialReturnOrderRepository partialReturnOrderRepository,
                ClientReturnService clientReturnService,
                ClientReturnRepository clientReturnRepository,
                SpecialRequestGenerateService specialRequestGenerateService,
                UserShiftCommandService userShiftCommandService,
                ShiftManager shiftManager,
                TaskService taskService
        ) {
            return new TestUserHelper(
                    userRepository,
                    shiftRepository,
                    userShiftRepository,
                    companyRepository,
                    commandService,
                    dataHelper,
                    userScheduleRuleRepository,
                    dsRepository,
                    scRepository,
                    clock,
                    receptionService,
                    taskErrorSaver,
                    transportRepository,
                    transportTypeRepository,
                    routingOrderTagRepository,
                    userPropertyService,
                    companyDraftRepository,
                    deliveryServiceCommandService,
                    companyRoleRepository,
                    partnerRepository,
                    partialReturnOrderRepository,
                    clientReturnService,
                    clientReturnRepository,
                    specialRequestGenerateService,
                    userShiftCommandService,
                    shiftManager,
                    taskService
            );
        }

        @Bean
        public AddDeliveryTaskHelper addDeliveryTaskHelper(
                ReceptionService receptionService,
                DeliveryTaskAddressToDeliveryAddressMapper deliveryTaskAddressToDeliveryAddressMapper,
                GeoPointScaleSettingService geoPointScaleSettingService
        ) {
            return new AddDeliveryTaskHelper(
                    new DeliveryTaskDataFactory(receptionService, deliveryTaskAddressToDeliveryAddressMapper),
                    geoPointScaleSettingService
            );
        }

    }

    @Configuration
    @ComponentScan(basePackages = {
            "ru.yandex.market.tpl.core.test",
            "ru.yandex.market.tpl.common.db.test"
    })
    @Import(ClockConfig.class)
    public static class Core {

    }

    @Configuration
    public static class ClockConfig {

        @Bean
        public Clock clock() {
            TestableClock clock = new TestableClock();
            clock.setFixed(
                    ClockUtil.defaultDateTime().toInstant(DateTimeUtil.DEFAULT_ZONE_ID),
                    DateTimeUtil.DEFAULT_ZONE_ID
            );
            return spy(clock);
        }

    }

}
