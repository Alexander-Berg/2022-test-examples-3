package ru.yandex.market.tpl.core.service.company;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.common.util.exception.TplForbiddenException;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.partner.PartnerRoutingInfo;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CHECK_AUTH_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_PARTNER_COMPANY_ROLE_ENABLED;

@RequiredArgsConstructor
public class CompanyAuthenticationServiceTest extends TplAbstractTest {
    public static long DeliveryServiceId = 14045L;
    private final MovementGenerator movementGenerator;
    private final OrderGenerateService orderGenerateService;
    private final CompanyAuthenticationService companyAuthenticationService;
    private final TestUserHelper testUserHelper;
    private final DsRepository dsRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private User courier1;
    private User courier2;
    private CompanyPermissionsProjection company;

    private CompanyPermissionsProjection company2;

    @BeforeEach
    void init() {
        courier1 = testUserHelper.findOrCreateUser(1L);
        courier2 = testUserHelper.findOrCreateUser(2L);
        company =
                CompanyPermissionsProjection.builder().id(courier1.getCompany().getId()).sortingCentersIds(Set.of(1L)).build();
        company2 = CompanyPermissionsProjection.builder().id(0L).build();
        SortingCenter sortingCenter = testUserHelper.sortingCenter(2);
        DeliveryService deliveryService = new DeliveryService();
        deliveryService.setSortingCenter(sortingCenter);
        deliveryService.setId(DeliveryServiceId);
        deliveryService.setName("NAME");
        deliveryService.setToken("gRlxMK7oTikU7utPmqQkI9F0wIUWb5B60T9EV9mAgZ9eYOoEbvIga0ZuEw3WPO8G");
        deliveryService.setDeliveryAreaMarginWidth(0L);
        dsRepository.saveAndFlush(deliveryService);

        this.clearAfterTest(deliveryService);

        given(configurationProviderAdapter.isBooleanEnabled(CHECK_AUTH_ENABLED))
                .willReturn(true);
        given(configurationProviderAdapter.isBooleanEnabled(IS_PARTNER_COMPANY_ROLE_ENABLED))
                .willReturn(true);
    }

    @AfterEach
    void clear() {
        Mockito.reset(configurationProviderAdapter);
    }

    @Test
    void checkAuthenticationByMovementExternalIdGoodAuthorize() {
        Movement movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(-1L)
                .build());
        companyAuthenticationService.checkAuthenticationForMovement(company, movement.getExternalId());
    }

    @Test
    void checkAuthenticationByMovementExternalIdBadAuthorize() {
        Movement movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(DeliveryServiceId)
                .build());
        assertThatThrownBy(() -> companyAuthenticationService.checkAuthenticationForMovement(company,
                movement.getExternalId())).isInstanceOf(TplForbiddenException.class);
    }

    @Test
    void checkAuthenticationByMovementIdGoodAuthorize() {
        Movement movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(-1L)
                .build());
        companyAuthenticationService.checkAuthenticationForMovement(company, movement.getId());
    }

    @Test
    void checkAuthenticationByMovementIdBadAuthorize() {
        Movement movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(DeliveryServiceId)
                .build());
        assertThatThrownBy(() -> companyAuthenticationService.checkAuthenticationForMovement(company,
                movement.getId())).isInstanceOf(TplForbiddenException.class);
    }

    @Test
    void checkAuthenticationByMovementGoodAuthorize() {
        Movement movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(-1L)
                .build());
        companyAuthenticationService.checkAuthenticationForMovement(company, movement);
    }

    @Test
    void checkAuthenticationByMovementBadAuthorize() {
        Movement movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(DeliveryServiceId)
                .build());
        assertThatThrownBy(() -> companyAuthenticationService.checkAuthenticationForMovement(company,
                movement)).isInstanceOf(TplForbiddenException.class);
    }

    @Test
    void checkAuthenticationByOrderBadAuthorize() {
        Order order =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder().deliveryServiceId(DeliveryServiceId)
                        .build());
        assertThatThrownBy(() -> companyAuthenticationService.checkAuthenticationForOrder(company,
                order)).isInstanceOf(TplForbiddenException.class);
    }

    @Test
    void checkAuthenticationByOrderGoodAuthorize() {
        Order order =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder().deliveryServiceId(-1L)
                        .build());
        companyAuthenticationService.checkAuthenticationForOrder(company, order);
    }

    @Test
    void checkAuthenticationByRoutingGoodAuthorize() {
        PartnerRoutingInfo partnerRoutingInfo = new PartnerRoutingInfo();
        partnerRoutingInfo.setSortingCenterId(1L);
        companyAuthenticationService.checkAuthenticationForRouting(company, partnerRoutingInfo);
    }

    @Test
    void checkAuthenticationByRoutingBadAuthorize() {
        PartnerRoutingInfo partnerRoutingInfo = new PartnerRoutingInfo();
        partnerRoutingInfo.setSortingCenterId(2L);
        assertThatThrownBy(() -> companyAuthenticationService.checkAuthenticationForRouting(company,
                partnerRoutingInfo)).isInstanceOf(TplForbiddenException.class);
    }

    @Test
    void checkUserBelongToCompany() {
        companyAuthenticationService.checkUserBelongToCompany(courier1.getId(), company);
        companyAuthenticationService.checkUserBelongToCompany(courier2.getId(), company);
    }

    @Test
    void checkUserNotBelongToCompany() {
        var throwable1 = catchThrowable(
                () -> companyAuthenticationService.checkUserBelongToCompany(courier1.getId(), company2));
        var throwable2 = catchThrowable(
                () -> companyAuthenticationService.checkUserBelongToCompany(courier2.getId(), company2));
        assertThat(throwable1).isInstanceOf(TplInvalidParameterException.class);
        assertThat(throwable2).isInstanceOf(TplInvalidParameterException.class);
    }
}
