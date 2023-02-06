package ru.yandex.market.crm.campaign.http.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.dao.AccountsDao;
import ru.yandex.market.crm.dao.RolesDao;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;
import ru.yandex.market.crm.domain.UserRole;
import ru.yandex.market.crm.external.blackbox.YandexTeamBlackboxClient;
import ru.yandex.market.crm.external.blackbox.response.UserInfo;
import ru.yandex.market.crm.http.configuration.AuthenticationSettings;
import ru.yandex.market.mcrm.http.internal.TvmServiceMockImpl;
import ru.yandex.market.mcrm.http.tvm.TvmService;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.TicketStatus;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.mcrm.http.HttpHeaderNames.TVM_SERVICE_TICKET;

@ContextConfiguration(classes = {IdmApiControllerTest.Configuration.class})
public class IdmApiControllerTest extends AbstractControllerMediumTest {
    public static class Configuration {
        @Bean
        public AccountsDao accountsDao() {
            return mock(AccountsDao.class);
        }

        @Bean
        public RolesDao rolesDao() {
            return mock(RolesDao.class);
        }

        @Bean
        public UsersRolesDao usersRolesDao() {
            return mock(UsersRolesDao.class);
        }

        @Bean
        public AuthenticationSettings authenticationSettings() {
            return mock(AuthenticationSettings.class);
        }

        @Bean
        public TvmService tvmService() {
            return mock(TvmServiceMockImpl.class);
        }
    }

    @Inject
    private UsersRolesDao usersRolesDao;
    @Inject
    private AccountsDao accountsDao;
    @Inject
    private RolesDao rolesDao;
    @Inject
    private AuthenticationSettings authenticationSettings;
    @Inject
    private TvmService tvmService;
    @Inject
    private YandexTeamBlackboxClient yandexTeamBlackboxClient;

    @Value("${tvm.allowed.ids}")
    private Set<Integer> allowedIds;


    @BeforeEach
    void setUp() {
        when(authenticationSettings.checkTvm()).thenReturn(false);
    }

    /**
     * Сервис должен возвращать информацию дерево ролей в корректном формате для IDM
     */
    @Test
    public void testCorrectReturnRolesTreeForIdm() throws Exception {
        when(accountsDao.getAccounts()).thenReturn(Map.of(
                "account1", new Account("account1", "Аккаунт1"),
                "account2", new Account("account2", "Аккаунт2")
        ));
        when(rolesDao.getRoles()).thenReturn(Map.of(
                "role1", new UserRole("role1", "Роль1"),
                "role2", new UserRole("role2", "Роль2")
        ));

        var actualResponseForIdm = mockMvc.perform(get("/api/idm/info"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        var expectedResponseForIdm = """
                {
                  "code": 0,
                  "roles": {
                    "slug": "account",
                    "name": {"ru": "Аккаунт"},
                    "values": {
                        "account1": {
                          "name": {"ru": "Аккаунт1"},
                          "roles": {
                            "slug": "role",
                            "name": {"ru": "Роль"},
                            "values": {
                              "role1": {
                                "name": {"ru": "Роль1"}
                              },
                              "role2": {
                                "name": {"ru": "Роль2"}
                              }
                            }
                          }
                        },
                        "account2": {
                          "name": {"ru": "Аккаунт2"},
                          "roles": {
                            "slug": "role",
                            "name": {"ru": "Роль"},
                            "values": {
                              "role1": {
                                "name": {"ru": "Роль1"}
                              },
                              "role2": {
                                "name": {"ru": "Роль2"}
                              }
                            }
                          }
                        }
                    }
                  }
                }""";

        JSONAssert.assertEquals(expectedResponseForIdm, actualResponseForIdm, false);
    }

    /**
     * Сервис должен возвращать информацию по ролям пользователей в системе в корректном формате для IDM
     */
    @Test
    public void testCorrectReturnUsersRolesForIdm() throws Exception {
        when(usersRolesDao.getUsersRoles()).thenReturn(Map.of(
                1L, Set.of(
                        new CompositeUserRole("account1", "role1"),
                        new CompositeUserRole("account2", "role2")
                ),
                2L, Set.of(new CompositeUserRole("account1", "role2"))
        ));

        var userInfo1 = new UserInfo();
        userInfo1.setUid(1L);
        userInfo1.setLogin("user1");
        when(yandexTeamBlackboxClient.getUserInfoByUid(eq(1L), anySet(), anySet(), anySet()))
                .thenReturn(userInfo1);

        var userInfo2 = new UserInfo();
        userInfo2.setUid(2L);
        userInfo2.setLogin("user2");
        when(yandexTeamBlackboxClient.getUserInfoByUid(eq(2L), anySet(), anySet(), anySet()))
                .thenReturn(userInfo2);

        var actualResponseForIdm = mockMvc.perform(get("/api/idm/get-all-roles"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        var expectedResponseForIdm = """
                {
                	"code": 0,
                	"users": [
                		{
                			"login": "user1",
                			"roles": [
                				{"account": "account1", "role": "role1"},
                				{"account": "account2", "role": "role2"}
                			]
                		},
                		{
                			"login": "user2",
                			"roles": [
                				{"account": "account1", "role": "role2"}
                			]
                		}
                	]
                }""";

        JSONAssert.assertEquals(expectedResponseForIdm, actualResponseForIdm, false);
    }

    /**
     * Если в настройках аутенификации включена проверка tvm,
     * то при обращении к ручкам idm производится сверка тикета. При этом, если у сервиса нет доступа по tvm,
     * то возвращается ошибка
     */
    @Test
    public void testForbiddenIfServiceTvmIdIsNotAllowed() throws Exception {
        var ticket = "ticket";

        when(authenticationSettings.checkTvm()).thenReturn(true);
        when(tvmService.checkTicket(ticket)).thenReturn(
                new CheckedServiceTicket(TicketStatus.OK, "", 0, 0)
        );

        mockMvc.perform(
                get("/api/idm/get-all-roles").header(TVM_SERVICE_TICKET, ticket)
        )
                .andExpect(status().isForbidden());
    }

    /**
     * Если в настройках аутенификации включена проверка tvm,
     * то при обращении к ручкам idm производится сверка тикета. Если у сервиса есть доступ по tvm, и тикет корректный,
     * то возвращается корректный ответ
     */
    @Test
    public void testCorrectAnswerIfServiceTvmIdIsAllowedAndTicketStatusIsOk() throws Exception {
        var ticket = "correct_ticket";

        when(authenticationSettings.checkTvm()).thenReturn(true);
        when(tvmService.checkTicket(ticket)).thenReturn(
                new CheckedServiceTicket(TicketStatus.OK, "", allowedIds.iterator().next(), 0)
        );
        when(usersRolesDao.getUsersRoles()).thenReturn(Map.of(
                1L, Set.of(new CompositeUserRole("account1", "role1"))
        ));

        var userInfo1 = new UserInfo();
        userInfo1.setUid(1L);
        userInfo1.setLogin("user1");
        when(yandexTeamBlackboxClient.getUserInfoByUid(eq(1L), anySet(), anySet(), anySet()))
                .thenReturn(userInfo1);

        var actualResponseForIdm = mockMvc.perform(
                get("/api/idm/get-all-roles").header(TVM_SERVICE_TICKET, ticket)
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        var expectedResponseForIdm = """
                {
                	"code": 0,
                	"users": [
                		{
                			"login": "user1",
                			"roles": [
                				{"account": "account1", "role": "role1"}
                			]
                		}
                	]
                }""";

        JSONAssert.assertEquals(expectedResponseForIdm, actualResponseForIdm, false);
    }
}
