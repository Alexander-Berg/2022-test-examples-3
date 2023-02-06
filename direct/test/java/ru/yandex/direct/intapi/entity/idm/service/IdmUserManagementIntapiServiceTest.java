package ru.yandex.direct.intapi.entity.idm.service;

import java.util.Map;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.model.IdmResponse;
import ru.yandex.direct.intapi.entity.idm.model.IdmRole;
import ru.yandex.direct.intapi.entity.idm.model.RemoveRoleRequest;
import ru.yandex.direct.rbac.RbacRole;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.direct.intapi.entity.idm.converter.IdmCommonNames.MAIN_MANAGER_ROLE_SLUG_NAME;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmUserManagementIntapiServiceTest {

    @Autowired
    IdmUserManagementIntapiService userManagementIntapiService;
    @Autowired
    Steps steps;
    @Autowired
    private ClientService clientService;
    @Autowired
    private UserService userService;
    @Autowired
    private CampaignService campaignService;

    private UserInfo managerInfo;
    private User managerUser;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        managerInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                .getChiefUserInfo();
        managerUser = managerInfo.getUser();
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void removeRole_success_whenMainManagerRemovedBeforeManager() {
        // DIRECT-101791: Проверяем случай, когда роль "главный менеджер" отзывается перед ролью "менеджер"
        // У клиента нет кампаний -- роль "менеджер" отзывается сразу
        steps.idmGroupSteps().addIdmPrimaryManager(managerInfo, clientInfo);

        removeRole(MAIN_MANAGER_ROLE_SLUG_NAME, managerUser, clientInfo.getClientId().asLong());
        removeManagerRole(managerUser);

        // Ожидаем, что обе роли успешно отозваны
        checkRole(managerUser.getUid(), RbacRole.EMPTY);
        Client clientAfter = checkNotNull(clientService.getClient(clientInfo.getClientId()));
        assertThat(clientAfter.getPrimaryManagerUid())
                .as("primary_manager_uid")
                .isNull();
    }

    @Test
    public void removeRole_blockUser_whenManagerRemovedAndMainManagerExists() {
        // DIRECT-101791: Проверяем случай, когда роль "главный менеджер" отзывается после роли "менеджер"
        // В таком случае ничего не делаем и возвращаем Idm'у "OK"
        steps.idmGroupSteps().addIdmPrimaryManager(managerInfo, clientInfo);

        removeManagerRole(managerUser);

        // Ожидаем, что роль осталась
        checkRole(managerUser.getUid(), RbacRole.MANAGER);
        // При этом пользователь должен быть заблокирован в интерфейсе
        User user = checkNotNull(userService.getUser(managerUser.getUid()));
        assertThat(user.getStatusBlocked())
                .as("user.statusBlocked")
                .isEqualTo((Boolean) true);
        // Роль "главный менеджер" сохраняется, её Idm должен отзывать отдельно
        Client clientAfter = checkNotNull(clientService.getClient(clientInfo.getClientId()));
        assertThat(clientAfter.getPrimaryManagerUid())
                .as("primary_manager_uid")
                .isNotNull();
    }

    @Test
    public void removeRole_unserviceCamps_whenMainManagerRevoked() {
        // У клиента есть кампании -- они снимаются с сервисирования при отзыве роли главный менеджер
        CampaignInfo campaign = steps.campaignSteps().createActiveCampaign(clientInfo);
        steps.idmGroupSteps().addIdmPrimaryManager(managerInfo, clientInfo);
        Campaign camp = getCampaignFromDb(campaign);
        assumeThat(camp.getManagerUserId(), is(notNullValue()));

        removeRole(MAIN_MANAGER_ROLE_SLUG_NAME, managerInfo.getUser(), clientInfo.getClientId().asLong());

        // Ожидаем, что роль успешно отозвана и кампания клиента снята с сервисирования
        Client clientAfter = checkNotNull(clientService.getClient(clientInfo.getClientId()));
        assertThat(clientAfter.getPrimaryManagerUid())
                .as("primary_manager_uid")
                .isNull();
        camp = getCampaignFromDb(campaign);
        assertThat(camp.getManagerUserId())
                .as("client campaign managerUid")
                .isNull();
    }

    /**
     * Вспомогательный метод для получения актуальной кампании из БД
     */
    private Campaign getCampaignFromDb(CampaignInfo campaign) {
        return campaignService.getCampaigns(clientInfo.getClientId(), singleton(campaign.getCampaignId())).get(0);
    }

    /**
     * Проверяем, что у пользователя {@code uid} в базе роль {@code expectedRole}
     */
    private void checkRole(Long uid, RbacRole expectedRole) {
        Map<Long, Client> clientMap = clientService.massGetClientsByUids(singleton(uid));
        Client client = clientMap.get(uid);
        assertThat(client.getRole()).as("role").isEqualTo(expectedRole);
    }

    /**
     * Удаление роли {@link IdmRole#MANAGER} через {@link IdmUserManagementIntapiService#removeRole}
     */
    private void removeManagerRole(User user) {
        removeRole(IdmRole.MANAGER.getTypedValue(), user, null);
    }

    /**
     * Удаление роли {@code role} через {@link IdmUserManagementIntapiService#removeRole}.
     * Если роль per-client, требуется задать {@code clientId}
     */
    private void removeRole(String role, User user, @Nullable Long clientId) {
        RemoveRoleRequest request = new RemoveRoleRequest()
                .withRole(role)
                .withClientId(clientId)
                .withPassportLogin(user.getLogin())
                .withDomainLogin(user.getDomainLogin());

        IdmResponse idmResponse = userManagementIntapiService.removeRole(request);

        assumeThat(String.format("remove manager response should be success but was: %s", toJson(idmResponse)),
                idmResponse.getCode(), is(0));
    }
}
