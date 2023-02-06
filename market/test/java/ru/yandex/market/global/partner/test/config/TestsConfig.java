package ru.yandex.market.global.partner.test.config;

import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.global.partner.domain.business.BusinessRepository;
import ru.yandex.market.global.partner.domain.legal_entity.LegalEntityRepository;
import ru.yandex.market.global.partner.domain.permission.PermissionRepository;
import ru.yandex.market.global.partner.domain.permission.PermissionService;
import ru.yandex.market.global.partner.domain.shop.ShopRepository;
import ru.yandex.market.global.partner.domain.shop.ShopScheduleRepository;
import ru.yandex.market.global.partner.util.TestDataService;
import ru.yandex.market.global.partner.util.TestPartnerFactory;
import ru.yandex.market.global.partner.util.TestPermissionFactory;

@Configuration
public class TestsConfig {
    @Bean
    public TestDataService testDataService(
            DSLContext dslContext,
            BusinessRepository businessRepository,
            LegalEntityRepository legalEntityRepository,
            ShopRepository shopRepository,
            ShopScheduleRepository shopScheduleRepository,
            PermissionRepository permissionRepository,
            PermissionService permissionService
    ) {
        return new TestDataService(
                dslContext,
                businessRepository,
                legalEntityRepository,
                shopRepository,
                shopScheduleRepository,
                permissionRepository,
                permissionService
        );
    }

    @Bean
    TestPartnerFactory testPartnerFactory() {
        return new TestPartnerFactory();
    }

    @Bean
    TestPermissionFactory testPermissionFactory() {
        return new TestPermissionFactory();
    }

}
