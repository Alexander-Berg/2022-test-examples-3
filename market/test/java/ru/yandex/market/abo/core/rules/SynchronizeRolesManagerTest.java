package ru.yandex.market.abo.core.rules;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import one.util.streamex.StreamEx;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.framework.user.blackbox.BlackBoxService;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.market.abo.core.assessor.AboRole;
import ru.yandex.market.abo.core.assessor.AssessorService;
import ru.yandex.market.abo.core.assessor.model.Assessor;
import ru.yandex.market.abo.core.rules.client.JavaSecClient;
import ru.yandex.market.abo.core.rules.model.IdmUserInfoDTO;
import ru.yandex.market.abo.core.rules.model.IdmUsersDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.assessor.AboRole.ROLE_ASSESSOR;
import static ru.yandex.market.abo.core.assessor.AboRole.ROLE_CLONESASSESSOR;
import static ru.yandex.market.abo.core.assessor.AboRole.ROLE_PREMODERATIONASSESSOR;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 20.01.20
 */
class SynchronizeRolesManagerTest {
    private static final String JSON_PATH = "/rules/cs_access_rules_api_get_users_result.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final long USER_ID = 1120000000000001L;
    private static final String LOGIN = "test-login2";

    @Mock
    private Assessor dbAssessor;

    @Mock
    private JavaSecClient javaSecClient;
    @Mock
    private AssessorService assessorService;
    @Mock
    private BlackBoxService yaTeamBlackBoxService;

    @InjectMocks
    private SynchronizeRolesManager synchronizeRolesManager;

    @BeforeEach
    void init() throws Exception {
        MockitoAnnotations.openMocks(this);

        String json = IOUtils.toString(getClass().getResourceAsStream(JSON_PATH), Charsets.UTF_8);
        when(javaSecClient.getUsers()).thenReturn(OBJECT_MAPPER.readValue(json, IdmUsersDTO.class).getUsers());

        when(dbAssessor.getUid()).thenReturn(USER_ID);
        when(dbAssessor.getLogin()).thenReturn(LOGIN);
        when(dbAssessor.getPermissions()).thenReturn(List.of(ROLE_ASSESSOR.getId()));
        when(assessorService.findAll()).thenReturn(List.of(dbAssessor));

        var blackboxUser = mock(BlackBoxUserInfo.class);
        when(blackboxUser.getUserId()).thenReturn(USER_ID);
        when(yaTeamBlackBoxService.getUserInfo(LOGIN)).thenReturn(blackboxUser);
    }

    @Test
    void newRoleForExistingAssessor() {
        synchronizeRolesManager.synchronizeRoles();
        verify(dbAssessor).setPermissions(List.of(ROLE_CLONESASSESSOR.getId()));

        var captor = ArgumentCaptor.forClass(Assessor.class);
        verify(assessorService).saveAssessor(captor.capture());
    }

    @Test
    void newAssessor() {
        when(assessorService.findAll()).thenReturn(Collections.emptyList());
        synchronizeRolesManager.synchronizeRoles();

        var captor = ArgumentCaptor.forClass(Assessor.class);
        verify(assessorService).saveAssessor(captor.capture());
        var assessor = captor.getAllValues().get(0);
        assertEquals(USER_ID, assessor.getUid());
        assertEquals(LOGIN, assessor.getLogin());
        assertEquals(List.of(ROLE_CLONESASSESSOR.getId()), assessor.getPermissions());
        verify(assessorService).saveAssessorInfo(USER_ID, LOGIN, "", false);
    }

    @Test
    void removeAssessor() {
        when(dbAssessor.getLogin()).thenReturn(LOGIN + "1");
        synchronizeRolesManager.synchronizeRoles();
        verify(dbAssessor).setPermissions(Collections.emptyList());
        verify(dbAssessor).setWorking(false);
    }

    @Test
    void rolesDidNotChanged() {
        when(dbAssessor.getPermissions()).thenReturn(List.of(ROLE_CLONESASSESSOR.getId()));
        synchronizeRolesManager.synchronizeRoles();
        verify(dbAssessor, never()).setPermissions(anyList());
        verify(dbAssessor, never()).setWorking(true);
        verify(assessorService, never()).saveAssessor(dbAssessor);
    }

    @Test
    void extractAllRolesTest() {
        var idmRoles = Map.of(
                "login1", Set.of("abo_assessor", "abo_premoderation_assessor"),
                "login2", Set.of("abo_premoderation_assessor", "abo_clones_assessor")
        );
        var expectedRoles = StreamEx.of(ROLE_ASSESSOR, ROLE_PREMODERATIONASSESSOR, ROLE_CLONESASSESSOR)
                .map(AboRole::getId)
                .toSet();
        var idmUser = mock(IdmUserInfoDTO.class);
        when(idmUser.getRoles()).thenReturn(idmRoles);
        assertEquals(expectedRoles, Set.copyOf(SynchronizeRolesManager.extractAllRoles(idmUser)));
    }
}
