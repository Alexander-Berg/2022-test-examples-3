package ru.yandex.market.tpl.core.service.user.partner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.tpl.api.model.user.partner.PartnerUserParamsDto;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.company.Company.DEFAULT_COMPANY_NAME;
import static ru.yandex.market.tpl.core.domain.user.UserProperties.SERVICE_TIME_MULTIPLIER_CAR;
import static ru.yandex.market.tpl.core.domain.user.UserProperties.SHARED_SERVICE_TIME_MULTIPLIER_CAR;
import static ru.yandex.market.tpl.core.domain.user.UserProperties.TRAVEL_TIME_MULTIPLIER_CAR;

/**
 * @author a-bryukhov
 */
@Slf4j
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerReportCourierRepositoryTest {

    private static final long UID = 1231L;

    private final PartnerReportCourierRepository partnerReportCourierRepository;
    private final UserPropertyService userPropertyService;

    private final TestUserHelper testUserHelper;

    @BeforeEach
    void setUp() {
        testUserHelper.findOrCreateUser(UID);
    }

    @Test
    void findWithNullFilters() {
        Page<PartnerReportCourier> couriers = partnerReportCourierRepository.findAll(
                new PartnerReportCourierSpecification(null, null),
                PageRequest.of(0, 10)
        );

        assertThat(couriers.getContent()).isNotEmpty();
    }

    @Test
    void findWithFilters() {
        var existingUser = testUserHelper.findOrCreateUser(UID);

        var params = new PartnerUserParamsDto();
        params.setRole(existingUser.getRole());
        params.setDeleted(existingUser.isDeleted());
        params.setCarMultiplierTo(userPropertyService.findPropertyForUser(TRAVEL_TIME_MULTIPLIER_CAR, existingUser));
        params.setCarMultiplierFrom(userPropertyService.findPropertyForUser(TRAVEL_TIME_MULTIPLIER_CAR, existingUser));
        params.setCarServiceMultiplierTo(userPropertyService.findPropertyForUser(SERVICE_TIME_MULTIPLIER_CAR,
                existingUser));
        params.setCarServiceMultiplierFrom(userPropertyService.findPropertyForUser(SERVICE_TIME_MULTIPLIER_CAR,
                existingUser));
        params.setCarServiceMultiplierTo(userPropertyService.findPropertyForUser(SHARED_SERVICE_TIME_MULTIPLIER_CAR,
                existingUser));
        params.setCarServiceMultiplierFrom(userPropertyService.findPropertyForUser(SHARED_SERVICE_TIME_MULTIPLIER_CAR,
                existingUser));

        Page<PartnerReportCourier> couriers = partnerReportCourierRepository.findAll(
                new PartnerReportCourierSpecification(params, null),
                PageRequest.of(0, 10)
        );

        assertThat(couriers.getContent())
                .hasOnlyOneElementSatisfying(user -> assertThat(user.getId()).isEqualTo(existingUser.getId()));
    }

    @Test
    void findWithCompany() {
        var existingUser = testUserHelper.findOrCreateUser(UID);
        var company = testUserHelper.findOrCreateCompany(DEFAULT_COMPANY_NAME);

        Page<PartnerReportCourier> couriers = partnerReportCourierRepository.findAll(
                new PartnerReportCourierSpecification(new PartnerUserParamsDto(), company.getId()),
                PageRequest.of(0, 10)
        );

        assertThat(couriers.getContent())
                .hasOnlyOneElementSatisfying(user -> assertThat(user.getId()).isEqualTo(existingUser.getId()));
    }

    @Test
    void findWithWrongCompany() {
        testUserHelper.findOrCreateUser(UID);
        var company = testUserHelper.findOrCreateCompany(DEFAULT_COMPANY_NAME);

        Page<PartnerReportCourier> couriers = partnerReportCourierRepository.findAll(
                new PartnerReportCourierSpecification(new PartnerUserParamsDto(), company.getId() + 1),
                PageRequest.of(0, 10)
        );

        assertThat(couriers.getContent()).isEmpty();
    }

}
