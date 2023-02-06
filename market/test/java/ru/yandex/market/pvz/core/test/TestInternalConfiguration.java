package ru.yandex.market.pvz.core.test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.validation.Validator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.logs.ControllerLogService;
import ru.yandex.market.pvz.core.test.factory.TestBannerInformationFactory;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestConsumableTypeFactory;
import ru.yandex.market.pvz.core.test.factory.TestCourierDsDayOffFactory;
import ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerTerminationFactory;
import ru.yandex.market.pvz.core.test.factory.TestObjectFactory;
import ru.yandex.market.pvz.core.test.factory.TestOebsReceiptFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderSenderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointCalendarLogFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.pvz.core.test.factory.TestShipmentsFactory;
import ru.yandex.market.pvz.core.test.factory.TestSmsLogFactory;
import ru.yandex.market.pvz.core.test.factory.TestSurveyFactory;
import ru.yandex.market.pvz.core.test.factory.TestVaccinationPickupPointFactory;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_TIME_OFFSET;

@ComponentScan(basePackages = {
        "ru.yandex.market.tpl.common.db.test",
        "ru.yandex.market.pvz.core.test.factory"
})
@Configuration
public class TestInternalConfiguration {

    @Bean
    Clock clock(TestableClock clock) {
        return clock;
    }

    @Bean
    TestableClock clock() {
        TestableClock clock = new TestableClock();
        clock.setFixed(Instant.ofEpochMilli(0), ZoneOffset.ofHours(DEFAULT_TIME_OFFSET));
        return clock;
    }

    @Bean
    TestObjectFactory testObjectFactory() {
        return new TestObjectFactory();
    }

    @Bean
    TestBrandRegionFactory brandRegionFactory() {
        return new TestBrandRegionFactory();
    }

    @Bean
    TestDeliveryServiceFactory serviceDeliveryFactory() {
        return new TestDeliveryServiceFactory();
    }

    @Bean
    TestCourierDsDayOffFactory courierDsDayOffFactory() {
        return new TestCourierDsDayOffFactory();
    }

    @Bean
    TestPickupPointFactory pickupPointFactory() {
        return new TestPickupPointFactory();
    }

    @Bean
    TestOrderSenderFactory orderSenderFactory() {
        return new TestOrderSenderFactory();
    }

    @Bean
    TestOrderFactory orderFactory() {
        return new TestOrderFactory();
    }

    @Bean
    TestSmsLogFactory smsLogFactory() {
        return new TestSmsLogFactory();
    }

    @Bean
    TestPreLegalPartnerFactory preLegalPartnerFactory() {
        return new TestPreLegalPartnerFactory();
    }

    @Bean
    TestLegalPartnerFactory legalPartnerFactory() {
        return new TestLegalPartnerFactory();
    }

    @Bean
    TestOebsReceiptFactory oebsReceiptFactory() {
        return new TestOebsReceiptFactory();
    }

    @Bean
    TestPickupPointCalendarLogFactory pickupPointCalendarLogFactory() {
        return new TestPickupPointCalendarLogFactory();
    }

    @Bean
    TestPickupPointCourierMappingFactory pickupPointCourierMappingFactory() {
        return new TestPickupPointCourierMappingFactory();
    }

    @Bean
    Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    TestReturnRequestFactory returnRequestFactory() {
        return new TestReturnRequestFactory();
    }

    @Bean
    TestCrmPrePickupPointFactory crmPrePickupPointFactory() {
        return new TestCrmPrePickupPointFactory();
    }

    @Bean
    TestLegalPartnerTerminationFactory legalPartnerTerminationFactory() {
        return new TestLegalPartnerTerminationFactory();
    }

    @Bean
    TestVaccinationPickupPointFactory vaccinationPickupPointFactory() {
        return new TestVaccinationPickupPointFactory();
    }

    @Bean
    PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    TestBannerInformationFactory testBannerInformationFactory() {
        return new TestBannerInformationFactory();
    }

    @Bean(value = "controllerLogService")
    ControllerLogService mockControllerLogService() {
        return mock(ControllerLogService.class);
    }

    @Bean
    JmsTemplate createJmsTemplate() {
        return mock(JmsTemplate.class);
    }

    @Bean
    TestConsumableTypeFactory consumableTypeFactory() {
        return new TestConsumableTypeFactory();
    }

    @Bean
    TestSurveyFactory testSurveyFactory() {
        return new TestSurveyFactory();
    }

    @Bean
    TestShipmentsFactory testShipmentsFactory() {
        return new TestShipmentsFactory();
    }
}
