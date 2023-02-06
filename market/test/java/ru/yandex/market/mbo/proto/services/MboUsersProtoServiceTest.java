package ru.yandex.market.mbo.proto.services;

import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.CollectionUtils;
import ru.yandex.market.mbo.catalogue.category.CategoryManagersManager;
import ru.yandex.market.mbo.catalogue.category.CategoryManagersManagerMock;
import ru.yandex.market.mbo.security.MboRole;
import ru.yandex.market.mbo.statistic.model.TaskType;
import ru.yandex.market.mbo.user.CategoryRole;
import ru.yandex.market.mbo.user.MboUser;
import ru.yandex.market.mbo.user.UserBinding;
import ru.yandex.market.mbo.user.UserManagerMock;
import ru.yandex.market.mbo.user.UserRolesManagerMock;
import ru.yandex.market.mbo.users.MboUsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MboUsersProtoServiceTest {
    private static final MboUser SERGEIERMAKOV = user(1, "sergeiermakov", "Сереженька :)", "s-ermakov");
    private static final MboUser YEREMEY = user(2, "yndx-yeremey", "Еремин Евгений", null);
    private static final MboUser MVORONTSOVA = user(3, "mvorontsova-spb", "auto_Воронцова Марина", "marina1906");
    private static final MboUser LNGBS = user(4, "lngbs", "test_Бородкин Сергей", null);
    private static final MboUser TKHLUDOVA = user(5, "tkhludova", "Татьяна Хлудова", null);
    private static final MboUser ANMALYSH = user(6, "amalysh86", "Александр Малышев", "anmalysh");

    private UserManagerMock userManager;
    private UserRolesManagerMock userRolesManagerMock;
    private CategoryManagersManagerMock categoryManagersManager;
    private MboUsersProtoService mboUsersProtoService;

    @Before
    public void setUp() throws Exception {
        userManager = new UserManagerMock();
        userRolesManagerMock = new UserRolesManagerMock(userManager);
        categoryManagersManager = new CategoryManagersManagerMock();
        mboUsersProtoService = new MboUsersProtoService(userManager, userRolesManagerMock, categoryManagersManager);
    }

    @Test
    public void testGetMboUsers() {
        int hid1 = 40;
        int hid2 = 10;
        // arrange
        userManager.addUserWithRole(SERGEIERMAKOV, 1);
        userManager.addUserWithRole(YEREMEY, 1);
        userManager.addUserWithRole(MVORONTSOVA, 1);
        userManager.addUserWithRole(LNGBS, 1);
        userManager.addUserWithRole(TKHLUDOVA, 1);
        userManager.addUser(ANMALYSH);
        categoryManagersManager.addCategoryManagers(
                new CategoryManagersManager.CategoryManagers(10, 100, SERGEIERMAKOV, SERGEIERMAKOV));
        categoryManagersManager.addCategoryManagers(
                new CategoryManagersManager.CategoryManagers(20, 200, YEREMEY, null));
        categoryManagersManager.addCategoryManagers(
                new CategoryManagersManager.CategoryManagers(30, 300, SERGEIERMAKOV, MVORONTSOVA));

        List<TaskType> mboProject = Collections.singletonList(TaskType.MBO);
        UserBinding sergeiermakovBinding = new UserBinding(SERGEIERMAKOV,
            Collections.singleton(MboRole.CATEGORY_SUPPORT_OPERATOR),
            Collections.singletonList(
                new CategoryRole(MboRole.CATEGORY_SUPPORT_OPERATOR, hid1, mboProject)
        ));
        UserBinding yeremeyBinding = new UserBinding(YEREMEY,
            Arrays.asList(MboRole.OPERATOR, MboRole.SUPER),
            Arrays.asList(
                new CategoryRole(MboRole.OPERATOR, hid1, mboProject),
                new CategoryRole(MboRole.SUPER, hid2, mboProject)
        ));
        UserBinding mvorontsovaBinding = new UserBinding(MVORONTSOVA,
            Collections.singleton(MboRole.CATEGORY_SUPPORT_OPERATOR),
            Arrays.asList(
                new CategoryRole(MboRole.CATEGORY_SUPPORT_OPERATOR, hid1, mboProject),
                new CategoryRole(MboRole.CATEGORY_SUPPORT_OPERATOR, hid2, mboProject)
        ));
        UserBinding lngbsBinding = new UserBinding(LNGBS,
            Collections.singleton(MboRole.SUPER),
            Arrays.asList(
                new CategoryRole(MboRole.SUPER, hid1, mboProject),
                new CategoryRole(MboRole.SUPER, hid2, mboProject)
        ));
        UserBinding tkhludovaBinding = new UserBinding(TKHLUDOVA,
            Arrays.asList(MboRole.OPERATOR, MboRole.SUPER),
            Arrays.asList(
                new CategoryRole(MboRole.OPERATOR, hid2, mboProject),
                new CategoryRole(MboRole.SUPER, hid1, mboProject)
        ));
        userRolesManagerMock.saveUserRoles(Arrays.asList(
                sergeiermakovBinding,
                yeremeyBinding,
                mvorontsovaBinding,
                lngbsBinding,
                tkhludovaBinding
        ));

        // act
        MboUsers.GetMboUsersRequest request = MboUsers.GetMboUsersRequest.newBuilder().build();
        MboUsers.GetMboUsersResponse response = mboUsersProtoService.getMboUsers(request);

        // assert
        List<MboUsers.MboUser> expected = new ArrayList<>();
        expected.add(protoUser(SERGEIERMAKOV, arr(10, 30), arr(10), arr(), arr(), arr(hid1),
                sergeiermakovBinding.getCategoryRoles()));
        expected.add(protoUser(YEREMEY, arr(20), arr(), arr(hid1), arr(hid2), arr(),
                yeremeyBinding.getCategoryRoles()));
        expected.add(protoUser(MVORONTSOVA, arr(), arr(30), arr(), arr(), arr(hid1, hid2),
                mvorontsovaBinding.getCategoryRoles()));
        expected.add(protoUser(LNGBS, arr(), arr(), arr(), arr(hid1, hid2), arr(),
                lngbsBinding.getCategoryRoles()));
        expected.add(protoUser(TKHLUDOVA, arr(), arr(), arr(hid2), arr(hid1), arr(),
                tkhludovaBinding.getCategoryRoles()));

        Assertions.assertThat(response.getUserList()).containsOnlyElementsOf(expected);
    }

    @Test
    public void testGetMboUsersFiltered() {
        int hid = 10;
        long guru = 100;
        // arrange
        userManager.addUserWithRole(SERGEIERMAKOV, 1);
        userManager.addUser(ANMALYSH);
        categoryManagersManager.addCategoryManagers(
                new CategoryManagersManager.CategoryManagers(hid, guru, SERGEIERMAKOV, SERGEIERMAKOV));

        List<TaskType> mboProject = Collections.singletonList(TaskType.MBO);
        UserBinding sergeiermakovBinding = new UserBinding(SERGEIERMAKOV,
            Arrays.asList(MboRole.CATEGORY_SUPPORT_OPERATOR, MboRole.OPERATOR, MboRole.SUPER),
            Arrays.asList(
                new CategoryRole(MboRole.CATEGORY_SUPPORT_OPERATOR, hid, mboProject),
                new CategoryRole(MboRole.OPERATOR, hid, Collections.singletonList(TaskType.BLUE_LOGS)),
                new CategoryRole(MboRole.SUPER, hid, Collections.singletonList(TaskType.WHITE_LOGS))
        ));
        userRolesManagerMock.saveUserRoles(Collections.singletonList(sergeiermakovBinding));

        MboUsers.MboUser sergeiermakov =
                protoUser(SERGEIERMAKOV, arr(hid), arr(hid), arr(hid),
                        arr(hid), arr(hid), sergeiermakovBinding.getCategoryRoles());
        MboUsers.MboUser anmalysh =
                protoUser(ANMALYSH, arr(), arr(), arr(), arr(), arr(), Collections.emptyList());

        MboUsers.GetMboUsersRequest request = MboUsers.GetMboUsersRequest.newBuilder().build();
        MboUsers.GetMboUsersResponse response = mboUsersProtoService.getMboUsers(request);

        Assertions.assertThat(response.getUserList()).containsExactlyInAnyOrder(sergeiermakov);

        request = MboUsers.GetMboUsersRequest.newBuilder()
                .setFilter(MboUsers.MboUserFiter.newBuilder().build())
                .build();
        response = mboUsersProtoService.getMboUsers(request);

        Assertions.assertThat(response.getUserList()).containsExactlyInAnyOrder(sergeiermakov, anmalysh);

        request = MboUsers.GetMboUsersRequest.newBuilder()
                .setFilter(MboUsers.MboUserFiter.newBuilder()
                        .setRoles(MboUsers.RoleFilter.WITH_ROLES)
                        .build())
                .build();
        response = mboUsersProtoService.getMboUsers(request);

        Assertions.assertThat(response.getUserList()).containsExactlyInAnyOrder(sergeiermakov);
    }

    @Test
    public void testGetMboUserWithNullName() {
        // arrange
        MboUser userWithNullName = user(1, "login", null, null);
        userManager.addUserWithRole(userWithNullName, 2);

        // act
        MboUsers.GetMboUsersRequest request = MboUsers.GetMboUsersRequest.newBuilder().build();
        MboUsers.GetMboUsersResponse response = mboUsersProtoService.getMboUsers(request);

        // assert
        MboUsers.MboUser expected = MboUsers.MboUser.newBuilder()
                .setUid(1).setYandexLogin("login").setMboFullname("")
                .build();
        Assertions.assertThat(response.getUserList())
                .containsExactlyInAnyOrder(expected);
    }

    private static MboUser user(long uid, String login, String fullname, String staffLogin) {
        return new MboUser(login, uid, fullname, login + "@yandex.ru", staffLogin);
    }

    private static MboUsers.MboUser protoUser(MboUser mboUser,
                                              int[] contentManagerCategories,
                                              int[] inputManagerCategories,
                                              int[] operatorCategories,
                                              int[] superOperatorCategories,
                                              int[] supportOperatorCategories,
                                              Collection<CategoryRole> categoryRoles
    ) {
        List<Integer> contentManagerCategoriesList = Arrays.stream(contentManagerCategories)
                .boxed().collect(Collectors.toList());
        List<Integer> inputManagerCategoriesList = Arrays.stream(inputManagerCategories)
                .boxed().collect(Collectors.toList());
        List<Integer> operatorCategoriesList = Arrays.stream(operatorCategories)
                .boxed().collect(Collectors.toList());
        List<Integer> superOperatorCategoriesList = Arrays.stream(superOperatorCategories)
                .boxed().collect(Collectors.toList());
        List<Integer> supportOperatorCategoriesList = Arrays.stream(supportOperatorCategories)
                .boxed().collect(Collectors.toList());

        MboUsers.MboUser.Builder mboUserBuilder = MboUsers.MboUser.newBuilder()
                .setUid(mboUser.getUid())
                .setMboFullname(mboUser.getFullname())
                .setYandexLogin(mboUser.getPureLogin());

        if (StringUtils.isNotEmpty(mboUser.getStaffLogin())) {
            mboUserBuilder.setStaffLogin(mboUser.getStaffLogin());
        }

        if (!CollectionUtils.isEmpty(categoryRoles)) {
            mboUserBuilder.addAllCategoryRoles(categoryRoles.stream()
                    .map(categoryRole -> MboUsers.MboUserBinding.newBuilder()
                            .setMboRole(MboUsers.MboRole.valueOf(categoryRole.getRole().getId()))
                            .setCategoryId(categoryRole.getHid())
                            .addAllProjects(categoryRole.getProjects().stream()
                                    .map(TaskType::convertToProtoProjectType)
                                    .collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList()));
        }

        mboUserBuilder
                .addAllContentManagerCategories(contentManagerCategoriesList)
                .addAllInputManagerCategories(inputManagerCategoriesList)
                .addAllOperatorCategories(operatorCategoriesList)
                .addAllSuperOperatorCategories(superOperatorCategoriesList)
                .addAllSupportOperatorCategories(supportOperatorCategoriesList);

        return mboUserBuilder.build();
    }

    private int[] arr(int... categories) {
        return categories;
    }
}
