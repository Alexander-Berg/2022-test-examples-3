package ru.yandex.market.mboc.common.services.imports;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.tracker.TrackerService;
import ru.yandex.market.mbo.users.MboUsers;
import ru.yandex.market.mbo.users.MboUsersService;
import ru.yandex.market.mboc.common.BaseIntegrationTestClass;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;
import ru.yandex.market.mboc.common.utils.MbocComparators;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MboUsersImportServiceTest extends BaseIntegrationTestClass {

    @Autowired
    MboUsersService mboUsersServiceMock;
    @Autowired
    MboUsersRepository mboUsersRepository;
    @Autowired
    CategoryInfoRepository categoryInfoRepository;
    @Autowired
    TransactionHelper transactionHelper;
    private MboUsersImportService mboUsersImportService;
    private TrackerService trackerService;

    private static Map<String, String> trackerResponse() {
        return ImmutableMap.of("s-ermakov", "Sergey Ermakov");
    }

    private static MboUsers.GetMboUsersResponse firstResponse() {
        MboUsers.MboUser fio = MboUsers.MboUser.newBuilder()
            .setUid(1)
            .setMboFullname("fio")
            .setYandexLogin("my_fio")
            .addContentManagerCategories(1)
            .addInputManagerCategories(1)
            .build();
        MboUsers.MboUser sermakov = MboUsers.MboUser.newBuilder()
            .setUid(101)
            .setMboFullname("Sergey Ermakov")
            .setYandexLogin("sergeyermakov2")
            .setStaffLogin("s-ermakov")
            .addContentManagerCategories(2)
            .build();
        MboUsers.MboUser azbuka = MboUsers.MboUser.newBuilder()
            .setUid(123)
            .setMboFullname("Azbuka")
            .setYandexLogin("uzbekkrut")
            .build();
        return MboUsers.GetMboUsersResponse.newBuilder()
            .addUser(fio).addUser(sermakov).addUser(azbuka).build();
    }

    private static MboUsers.GetMboUsersResponse secondResponse() {
        MboUsers.MboUser fio = MboUsers.MboUser.newBuilder()
            .setUid(1)
            .setMboFullname("fio")
            .setYandexLogin("my_fio")
            .addContentManagerCategories(1)
            .addInputManagerCategories(1)
            .build();
        MboUsers.MboUser azbuka = MboUsers.MboUser.newBuilder()
            .setUid(2)
            .setMboFullname("second")
            .setYandexLogin("second")
            .addContentManagerCategories(2)
            .build();
        MboUsers.MboUser sermakov = MboUsers.MboUser.newBuilder()
            .setUid(101)
            .setMboFullname("Sergey ErmakOFF")
            .setYandexLogin("sergeyermakov2")
            .setStaffLogin("s-ermakov")
            .addInputManagerCategories(2)
            .build();
        return MboUsers.GetMboUsersResponse.newBuilder()
            .addUser(fio).addUser(sermakov).addUser(azbuka).build();
    }

    private static MboUsers.GetMboUsersResponse thirdResponse() {
        MboUsers.MboUser fio = MboUsers.MboUser.newBuilder()
            .setUid(1)
            .setMboFullname("fio")
            .setYandexLogin("my_fio")
            .setStaffLogin("fio")
            .build();
        MboUsers.MboUser sermakov = MboUsers.MboUser.newBuilder()
            .setUid(101)
            .setMboFullname("Sergey Ermak")
            .setYandexLogin("sergeyermakov2")
            .setStaffLogin("s-ermakov")
            .build();
        return MboUsers.GetMboUsersResponse.newBuilder()
            .addUser(fio).addUser(sermakov).build();
    }

    private static MboUsers.GetMboUsersResponse foursResponse() {
        MboUsers.MboUser fio = MboUsers.MboUser.newBuilder()
            .setUid(1)
            .setMboFullname("fio")
            .setYandexLogin("my_fio")
            .build();
        MboUsers.MboUser sermakov = MboUsers.MboUser.newBuilder()
            .setUid(101)
            .setMboFullname("SERGEY")
            .setYandexLogin("sergey")
            .setStaffLogin("s-ermakov")
            .build();
        return MboUsers.GetMboUsersResponse.newBuilder()
            .addUser(fio).addUser(sermakov).build();
    }

    private static MboUsers.GetMboUsersResponse doubleUsers() {
        MboUsers.MboUser sermakov = MboUsers.MboUser.newBuilder()
            .setUid(11)
            .setMboFullname("Sergey ermakov")
            .setYandexLogin("sergey1")
            .setStaffLogin("s-ermakov")
            .build();
        MboUsers.MboUser sermakov2 = MboUsers.MboUser.newBuilder()
            .setUid(101)
            .setMboFullname("SERGEY")
            .setYandexLogin("sergey2")
            .setStaffLogin("s-ermakov")
            .build();
        return MboUsers.GetMboUsersResponse.newBuilder()
            .addUser(sermakov).addUser(sermakov2).build();
    }

    private static MboUser user(long uid, String fio, String yandexLogin) {
        return user(uid, fio, yandexLogin, null, null);
    }

    private static MboUser user(long uid, String fio, String yandexLogin, String staffLogin, String staffFullname) {
        MboUser mboUser = new MboUser(uid, fio, yandexLogin);
        mboUser.setStaffLogin(staffLogin);
        mboUser.setStaffFullname(staffFullname);
        return mboUser;
    }

    private static CategoryInfo info(long categoryId, Long contentManagerUid, Long inputManagerUid,
                                     boolean moderationInYang) {
        CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.setCategoryId(categoryId);
        categoryInfo.setContentManagerUid(contentManagerUid);
        categoryInfo.setInputManagerUid(inputManagerUid);
        categoryInfo.setModerationInYang(moderationInYang);
        return categoryInfo;
    }

    @Before
    public void setUp() throws Exception {
        mboUsersServiceMock = Mockito.mock(MboUsersService.class);
        trackerService = Mockito.mock(TrackerService.class);
        mboUsersImportService = new MboUsersImportService(mboUsersServiceMock, mboUsersRepository,
            categoryInfoRepository, transactionHelper, trackerService);
    }

    @Test
    public void testCorrectInsert() {
        categoryInfoRepository.deleteAll();
        Mockito.when(trackerService.getUsersByLogin(Mockito.any()))
            .then(__ -> trackerResponse());
        Mockito.when(mboUsersServiceMock.getMboUsers(Mockito.any()))
            .then(__ -> firstResponse());

        mboUsersImportService.importUsers();

        List<MboUser> allUsers = mboUsersRepository.findAll();
        Assertions.assertThat(allUsers)
            .usingElementComparator(MbocComparators.MBO_USER_COMPARATOR)
            .containsExactlyInAnyOrder(
                user(1, "fio", "my_fio"),
                user(101, "Sergey Ermakov", "sergeyermakov2", "s-ermakov", "Sergey Ermakov"),
                user(123, "Azbuka", "uzbekkrut")
            );

        List<CategoryInfo> allInfos = categoryInfoRepository.findAll();
        Assertions.assertThat(allInfos)
            .usingElementComparator(MbocComparators.CATEGORY_INFO_COMPARATOR)
            .containsExactlyInAnyOrder(
                info(CategoryTree.ROOT_CATEGORY_ID, null, null, true),
                info(1, 1L, 1L, true),
                info(2, 101L, null, true)
            );
    }

    @Test
    public void testCorrectUpdateExistingData() {
        categoryInfoRepository.insertBatch(Arrays.asList(
            info(1, null, null, true),
            info(2, null, null, false)));
        Mockito.when(trackerService.getUsersByLogin(Mockito.any()))
            .then(__ -> trackerResponse());
        Mockito.when(mboUsersServiceMock.getMboUsers(Mockito.any()))
            .then(__ -> firstResponse())
            .then(__ -> secondResponse());
        mboUsersImportService.importUsers();
        mboUsersImportService.importUsers();

        List<MboUser> allUsers = mboUsersRepository.findAll();
        Assertions.assertThat(allUsers)
            .usingElementComparator(MbocComparators.MBO_USER_COMPARATOR)
            .containsExactlyInAnyOrder(
                user(1, "fio", "my_fio"),
                user(2, "second", "second"),
                user(101, "Sergey ErmakOFF", "sergeyermakov2", "s-ermakov", "Sergey Ermakov"),
                user(123, "Azbuka", "uzbekkrut")
            );

        List<CategoryInfo> allInfos = categoryInfoRepository.findAll();
        Assertions.assertThat(allInfos)
            .usingElementComparator(MbocComparators.CATEGORY_INFO_COMPARATOR)
            .containsExactlyInAnyOrder(
                info(CategoryTree.ROOT_CATEGORY_ID, null, null, true),
                info(1, 1L, 1L, true),
                info(2, 2L, 101L, false)
            );
    }

    @Test
    public void testValidatingStaffLogin() {
        Mockito.when(trackerService.getUsersByLogin(Mockito.any()))
            .then(__ -> trackerResponse());
        Mockito.when(mboUsersServiceMock.getMboUsers(Mockito.any()))
            .then(__ -> thirdResponse());

        mboUsersImportService.importUsers();

        List<MboUser> allUsers = mboUsersRepository.findAll();
        Assertions.assertThat(allUsers)
            .usingElementComparator(MbocComparators.MBO_USER_COMPARATOR)
            .containsExactlyInAnyOrder(
                user(1, "fio", "my_fio"),
                user(101, "Sergey Ermak", "sergeyermakov2", "s-ermakov", "Sergey Ermakov")
            );
    }

    @Test
    public void wontFetchUserInfoOfNotChangesUsers() {
        Mockito.when(trackerService.getUsersByLogin(Mockito.any()))
            .then(__ -> trackerResponse());
        Mockito.when(mboUsersServiceMock.getMboUsers(Mockito.any()))
            .then(__ -> thirdResponse())
            .then(__ -> foursResponse());

        mboUsersImportService.importUsers();
        mboUsersImportService.importUsers();

        List<MboUser> allUsers = mboUsersRepository.findAll();
        Assertions.assertThat(allUsers)
            .usingElementComparator(MbocComparators.MBO_USER_COMPARATOR)
            .containsExactlyInAnyOrder(
                user(1, "fio", "my_fio"),
                user(101, "SERGEY", "sergey", "s-ermakov", "Sergey Ermakov")
            );
        Mockito.verify(trackerService, Mockito.times(1)).getUsersByLogin(Mockito.any());
    }

    @Test
    public void usersWithDuplicateStaffLoginsCorrectlyProcessing() {
        Mockito.when(trackerService.getUsersByLogin(Mockito.any()))
            .then(__ -> trackerResponse());
        Mockito.when(mboUsersServiceMock.getMboUsers(Mockito.any()))
            .then(__ -> doubleUsers());

        mboUsersImportService.importUsers();
        mboUsersImportService.importUsers();

        List<MboUser> allUsers = mboUsersRepository.findAll();
        Assertions.assertThat(allUsers)
            .usingElementComparator(MbocComparators.MBO_USER_COMPARATOR)
            .containsExactlyInAnyOrder(
                user(11, "Sergey ermakov", "sergey1", "s-ermakov", "Sergey Ermakov"),
                user(101, "SERGEY", "sergey2", "s-ermakov", "Sergey Ermakov")
            );
        Mockito.verify(trackerService, Mockito.times(1)).getUsersByLogin(Mockito.any());
    }
}
