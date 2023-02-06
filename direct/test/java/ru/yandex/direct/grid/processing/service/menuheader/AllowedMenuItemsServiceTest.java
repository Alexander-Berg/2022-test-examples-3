package ru.yandex.direct.grid.processing.service.menuheader;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.model.client.GdMenuItem;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

@RunWith(JUnitParamsRunner.class)
public class AllowedMenuItemsServiceTest {

    private static final ClientId TEST_CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final long TEST_USER_ID = RandomNumberUtils.nextPositiveLong();
    private static final long TEST_CHIEF_USER_ID = RandomNumberUtils.nextPositiveLong();
    private static final ClientId SUBJECT_USER_CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());

    @Mock
    private ClientService clientService;
    @Mock
    private RbacService rbacService;
    @Mock
    private FeatureService featureService;

    @InjectMocks
    private AllowedMenuItemsService allowedMenuItemsService;

    private static User operator() {
        return TestUsers.defaultUser()
                .withClientId(TEST_CLIENT_ID)
                .withChiefUid(TEST_CHIEF_USER_ID)
                .withUid(TEST_USER_ID)
                .withDeveloper(false);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        "Супер без фич",
                        operator()
                                .withRole(RbacRole.SUPER),
                        ImmutableSet.of(
                                GdMenuItem.ADMIN,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.SHOW_SEARCH_PAGE,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        false,
                        TEST_CLIENT_ID,
                },
                {
                        "Супер с фичами",
                        operator()
                                .withRole(RbacRole.SUPER),
                        ImmutableSet.of(
                                GdMenuItem.ADMIN,
                                GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.DEALS_LIST,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.SHOW_MINUS_KEYWORDS_LIB,
                                GdMenuItem.SHOW_SEARCH_PAGE,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.MULTI_CLIENTS_IN_STAT,
                                GdMenuItem.WORDSTAT),
                        ImmutableSet.of(FeatureName.CPM_DEALS.getName(),
                                FeatureName.MINUS_WORDS_LIB.getName(),
                                FeatureName.MULTI_CLIENTS_IN_STAT_ALLOWED.getName()),
                        true,
                        false,
                        TEST_CLIENT_ID,
                },
                {
                        "Суперридер",
                        operator()
                                .withRole(RbacRole.SUPERREADER),
                        ImmutableSet.of(
                                GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.SHOW_SEARCH_PAGE,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        false,
                        TEST_CLIENT_ID,
                },
                {
                        "Разработчик",
                        operator()
                                .withRole(RbacRole.SUPERREADER)
                                .withDeveloper(true),
                        ImmutableSet.of(
                                GdMenuItem.ADMIN,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.SHOW_SEARCH_PAGE,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        false,
                        TEST_CLIENT_ID,
                },
                {
                        "Агентство без фич",
                        operator()
                                .withRole(RbacRole.AGENCY),
                        ImmutableSet.of(
                                GdMenuItem.ADMIN_AGENCY,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.AGENCY_SEARCH,
                                GdMenuItem.SHOW_AGENCY_CLIENTS,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        true,
                        TEST_CLIENT_ID,
                },
                {
                        "Агентство с фичами",
                        operator()
                                .withRole(RbacRole.AGENCY),
                        ImmutableSet.of(
                                GdMenuItem.ADMIN_AGENCY,
                                GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.AGENCY_SEARCH,
                                GdMenuItem.DEALS_LIST,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.SHOW_AGENCY_CLIENTS,
                                GdMenuItem.SHOW_MINUS_KEYWORDS_LIB,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        ImmutableSet.of(FeatureName.CPM_DEALS.getName(),
                                FeatureName.MINUS_WORDS_LIB.getName()),
                        false,
                        true,
                        TEST_CLIENT_ID,
                },
                {
                        "Ограниченный представитель агентства без фич",
                        operator()
                                .withRole(RbacRole.AGENCY)
                                .withRepType(RbacRepType.LIMITED),
                        ImmutableSet.of(
                                GdMenuItem.ADMIN_AGENCY,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.AGENCY_SEARCH,
                                GdMenuItem.SHOW_AGENCY_CLIENTS,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        true,
                        TEST_CLIENT_ID,
                },
                {
                        "Ограниченный представитель агентства с фичами",
                        operator()
                                .withRole(RbacRole.AGENCY)
                                .withRepType(RbacRepType.LIMITED),
                        ImmutableSet.of(
                                GdMenuItem.ADMIN_AGENCY,
                                GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.AGENCY_SEARCH,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.SHOW_AGENCY_CLIENTS,
                                GdMenuItem.SHOW_MINUS_KEYWORDS_LIB,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        ImmutableSet.of(FeatureName.CPM_DEALS.getName(),
                                FeatureName.MINUS_WORDS_LIB.getName()),
                        false,
                        true,
                        TEST_CLIENT_ID,
                },
                {
                        "Менеджер",
                        operator()
                                .withRole(RbacRole.MANAGER),
                        ImmutableSet.of(
                                GdMenuItem.ADMIN_MANAGER,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.SHOW_MANAGER_CLIENTS,
                                GdMenuItem.SHOW_SEARCH_PAGE,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        true,
                        TEST_CLIENT_ID,
                },
                {
                        "Саппорт",
                        operator()
                                .withRole(RbacRole.SUPPORT),
                        ImmutableSet.of(
                                GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.DOCUMENTS_AND_PAYMENTS,
                                GdMenuItem.SHOW_SEARCH_PAGE,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        false,
                        TEST_CLIENT_ID,
                },
                {
                        "Вешальщик",
                        operator()
                                .withRole(RbacRole.PLACER),
                        ImmutableSet.of(
                                GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.SHOW_SEARCH_PAGE,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        false,
                        TEST_CLIENT_ID,
                },
                {
                        "Медиапланнер",
                        operator()
                                .withRole(RbacRole.MEDIA),
                        ImmutableSet.of(GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.SHOW_SEARCH_PAGE,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        false,
                        TEST_CLIENT_ID,
                },
                {
                        "Клиент без фич, не агентский",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        ImmutableSet.of(GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.CREATE,
                                GdMenuItem.DOCUMENTS_AND_PAYMENTS,
                                GdMenuItem.SHOW_CAMPS,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        true,
                        false,
                        TEST_CLIENT_ID,
                },
                {
                        "Клиент с фичами",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        ImmutableSet.of(GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.CREATE,
                                GdMenuItem.SHOW_CAMPS,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.SHOW_MINUS_KEYWORDS_LIB,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        ImmutableSet.of(FeatureName.CPM_DEALS.getName(),
                                FeatureName.MINUS_WORDS_LIB.getName()),
                        true,
                        true,
                        TEST_CLIENT_ID,
                },
                {
                        "Субклиент без права редактирования",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        ImmutableSet.of(GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.SHOW_CAMPS,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        true,
                        TEST_CLIENT_ID,
                },
                {
                        "Суперсубклиент",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        ImmutableSet.of(GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.CREATE,
                                GdMenuItem.SHOW_CAMPS,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        true,
                        true,
                        TEST_CLIENT_ID,
                },
                {
                        "Объединение фич оператора и клиента",
                        operator()
                                .withRole(RbacRole.AGENCY),
                        ImmutableSet.of(GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.SHOW_AGENCY_CLIENTS,
                                GdMenuItem.AGENCY_SEARCH,
                                GdMenuItem.ADMIN_AGENCY,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.SHOW_MINUS_KEYWORDS_LIB,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        true,
                        SUBJECT_USER_CLIENT_ID,
                },
                {
                        "Связанный клиент",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        ImmutableSet.of(GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.CREATE,
                                GdMenuItem.SHOW_CAMPS,
                                GdMenuItem.SHOW_MINUS_KEYWORDS_LIB,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        false,
                        SUBJECT_USER_CLIENT_ID,
                },
                {
                        "Readonly представитель клиента",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withRepType(RbacRepType.READONLY)
                                .withIsReadonlyRep(true),
                        ImmutableSet.of(GdMenuItem.ADVANCED_FORECAST,
                                GdMenuItem.RECOMMENDATIONS,
                                GdMenuItem.SHOW_CAMPS,
                                GdMenuItem.TURBO_LANDING_CONSTRUCTOR,
                                GdMenuItem.WORDSTAT),
                        emptySet(),
                        false,
                        false,
                        TEST_CLIENT_ID,
                },
        });
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        doReturn(emptySet())
                .when(clientService)
                .clientIdsWithApiEnabled(any());

        doReturn(true)
                .when(rbacService)
                .isOwner(anyLong(), anyLong());

        doReturn(singleton(FeatureName.MINUS_WORDS_LIB.getName()))
                .when(featureService)
                .getEnabledForClientId(SUBJECT_USER_CLIENT_ID);
    }


    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{0}")
    public void getAllowedMenuItemsWithoutULogin(@SuppressWarnings("unused") String testName, User operator,
                                                 Set<GdMenuItem> expectedMenuItems, Set<String> availableFeatures,
                                                 Boolean isSuperSubclient, Boolean isUnderAgency, ClientId subjectUserClientId) {
        doReturn(isUnderAgency)
                .when(rbacService)
                .isUnderAgency(anyLong());

        doReturn(isSuperSubclient)
                .when(clientService)
                .isSuperSubclient(any());

        doReturn(availableFeatures)
                .when(featureService)
                .getEnabledForClientId(TEST_CLIENT_ID);

        Set<GdMenuItem> allowedMenuItems = allowedMenuItemsService.getAllowedMenuItems(operator, subjectUserClientId);
        assertThat(allowedMenuItems).containsExactlyInAnyOrder(expectedMenuItems.toArray(new GdMenuItem[0]));
    }
}
