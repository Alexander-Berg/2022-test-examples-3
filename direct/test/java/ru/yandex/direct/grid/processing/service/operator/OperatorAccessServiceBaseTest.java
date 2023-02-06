package ru.yandex.direct.grid.processing.service.operator;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerService;
import ru.yandex.direct.core.entity.payment.service.AutopayService;
import ru.yandex.direct.core.entity.promoextension.PromoExtensionRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.client.ClientDataService;
import ru.yandex.direct.rbac.RbacClientsRelations;
import ru.yandex.direct.rbac.RbacService;

public abstract class OperatorAccessServiceBaseTest {

    @Mock
    protected RbacService rbacService;
    @Mock
    protected RbacClientsRelations rbacClientsRelations;
    @Mock
    protected ClientService clientService;
    @Mock
    protected AgencyClientRelationService agencyClientRelationService;
    @Mock
    protected UserService userService;
    @Mock
    protected FeatureService featureService;
    @Mock
    protected FreelancerService freelancerService;
    @Mock
    protected AutopayService autopayService;

    @Mock
    protected CampaignInfoService campaignInfoService;
    @Mock
    protected ClientDataService clientDataService;
    @Mock
    protected PromoExtensionRepository promoExtensionRepository;
    @Mock
    protected ClientLimitsService clientLimitsService;

    @InjectMocks
    protected OperatorAccessService operatorAccessService;
}
