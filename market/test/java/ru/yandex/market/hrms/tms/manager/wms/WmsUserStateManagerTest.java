package ru.yandex.market.hrms.tms.manager.wms;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.ad.AccountInfo;
import ru.yandex.market.ad.ActiveDirectoryClientMock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffEntity;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffV2Entity;
import ru.yandex.market.hrms.core.domain.wms.WmsClientFactory;
import ru.yandex.market.hrms.core.domain.wms.WmsUserEntity;
import ru.yandex.market.hrms.core.domain.wms.repo.WmsUserRepo;
import ru.yandex.market.hrms.core.service.outstaff.account.Gender;
import ru.yandex.market.hrms.core.service.util.HrmsCollectionUtils;
import ru.yandex.market.hrms.core.service.wms.WmsUserStateManager;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.wms.WmsClientMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class WmsUserStateManagerTest extends AbstractTmsTest {

    @MockBean
    private WmsClientFactory wmsClientFactory;

    @SpyBean
    private WmsClientMock wmsClient;

    @SpyBean
    private ActiveDirectoryClientMock adClient;

    @Autowired
    private WmsUserRepo wmsUserRepo;

    @Autowired
    private WmsUserStateManager manager;

    @BeforeEach
    public void initClock() {
        mockClock(LocalDateTime.of(2021, 10, 18, 1, 30));
        when(wmsClientFactory.create(any())).thenReturn(wmsClient);
    }

    @AfterEach
    public void release() {
        adClient.clear();
        wmsClient.clear();
        Mockito.clearInvocations(wmsClientFactory, wmsClient, adClient);
    }

    @Test
    @DbUnitDataSet(
            before = "WmsUserStateManagerTest.ToInProgressTransitionTests.happyPath.before.csv",
            after = "WmsUserStateManagerTest.ToInProgressTransitionTests.happyPath.after.csv"
    )
    public void toInProgressTransitionForEmployeesHappyPath() {
        manager.toInProgressTransitionForEmployees();
        Mockito.verifyNoMoreInteractions(adClient);
    }

    @Test
    @DbUnitDataSet(
            before = "WmsUserStateManagerTest.ToInProgressTransitionTests.happyPath.before.csv",
            after = "WmsUserStateManagerTest.ToInProgressTransitionTests.withException.after.csv"
    )
    public void toInProgressTransitionForEmployeesWithException() {
        doThrow(new RuntimeException("something goes wrong"))
                .when(wmsClient).findByLogin(anyString());

        Assertions.assertThrows(TplIllegalStateException.class, () -> manager.toInProgressTransitionForEmployees());
        Mockito.reset(wmsClient);
    }

    @Test
    @DbUnitDataSet(
            before = "WmsUserStateManagerTest.ToInProgressTransitionForOutstaffTests.happyPath.before.csv",
            after = "WmsUserStateManagerTest.ToInProgressTransitionForOutstaffTests.happyPath.after.csv"
    )
    public void toInProgressTransitionForOutstaffHappyPath() {
        manager.toInProgressTransitionForOutstaff();
        Mockito.verifyNoMoreInteractions(adClient);
    }

    @Test
    @DbUnitDataSet(
            before = "WmsUserStateManagerTest.FromInProgressToReadyTransitionTests.happyPath.before.csv",
            after = "WmsUserStateManagerTest.FromInProgressToReadyTransitionTests.reuseLogins.after.csv"
    )
    public void toInProgressTransitionForOutstaffReuse() {
        manager.toInProgressTransitionForOutstaff();
        Mockito.verifyNoMoreInteractions(adClient);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "WmsUserStateManagerTest.ToInProgressTransitionForOutstaffTests.happyPath.before.csv",
                    "WmsUserStateManagerTest.ToInProgressTransitionForOutstaffTests.duplicated.before.csv"
            },
            after = "WmsUserStateManagerTest.ToInProgressTransitionForOutstaffTests.duplicated.after.csv"
    )
    public void toInProgressTransitionForOutstaffDuplicatedCandidates() {
        manager.toInProgressTransitionForOutstaff();
    }

    @Test
    @DbUnitDataSet(
            before = "WmsUserStateManagerTest.ToInProgressTransitionForOutstaffTests.happyPath.before.csv",
            after = "WmsUserStateManagerTest.ToInProgressTransitionForOutstaffTests.withException.after.csv"
    )
    public void toInProgressTransitionForOutstaffWithException() {
        doThrow(new RuntimeException("something goes wrong"))
                .when(wmsClient).findByLogin(startsWith("sof-ll-petr"));
        Assertions.assertThrows(TplIllegalStateException.class, () -> manager.toInProgressTransitionForOutstaff());
        Mockito.verifyNoMoreInteractions(adClient);
        Mockito.reset(wmsClient);
    }

    @Test
    @DbUnitDataSet(
            before = "WmsUserStateManagerTest.FromInProgressToReadyTransitionTests.happyPath.before.csv",
            after = "WmsUserStateManagerTest.FromInProgressToReadyTransitionTests.happyPath.after.csv"
    )
    public void fromInProgressToReadyTransitionHappyPath() {
        manager.fromInProgressToReadyTransition();

        var expectedGender = "male";
        var expectedStaffLogins = Map.of(1L, "user1", 2L, "user2");
        wmsUserRepo.findAll().forEach(u -> Mockito.verify(wmsClient)
                .createUser(
                        u.getLogin(),
                        u.getFullName(),
                        u.getEmail(),
                        List.of("role1", "role2"),
                        expectedGender,
                        expectedStaffLogins.get(u.getId()),
                        u.getOutstaffId() != null
                )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "WmsUserStateManagerTest.createWmsLoginTest.before.csv",
            after = "WmsUserStateManagerTest.createWmsLoginTest.after.csv"
    )
    public void createWmsUserTest() {
        manager.createWmsUserOutstaffV2(OutstaffV2Entity.builder()
                        .id(1L)
                        .birthday(LocalDate.MIN)
                        .city("Москва")
                        .companyId(10L)
                        .firstName("Жора")
                        .lastName("Жожа")
                        .midName("Жорочкин")
                        .companyLoginId(2L)
                        .email("qwe")
                        .gender(Gender.MALE)
                        .hasRussianCitizenship(false)
                        .phone("123")
                        .oldOutstaffs(List.of(OutstaffEntity.builder()
                                .id(100L)
                                .build()))
                        .build(),
                1L, 100L);

        manager.createWmsUserOutstaffV2(OutstaffV2Entity.builder()
                        .id(1L)
                        .birthday(LocalDate.MIN)
                        .city("Москва")
                        .companyId(10L)
                        .firstName("Жора")
                        .lastName("Жожа")
                        .midName("Жорочкин")
                        .companyLoginId(2L)
                        .email("qwe")
                        .gender(Gender.MALE)
                        .hasRussianCitizenship(false)
                        .phone("123")
                        .oldOutstaffs(List.of(OutstaffEntity.builder()
                                .id(101L)
                                .build()))
                        .build(),
                3L, 101L);

        Mockito.verify(adClient, times(2)).getByLogin(any());
        Mockito.verify(adClient, times(2)).create(any(), any(), any(), any());
        Mockito.verify(adClient, times(2)).enableUser(any());
        Mockito.verify(adClient, times(2)).changeCatalog(
                any(), any());
        Mockito.verify(wmsClient, times(4)).findByLogin(any());
        Mockito.verify(wmsClient, times(1)).createUser("sof-ag-zhozzhz", "Жожа Жора Жорочкин", null, List.of(), "MALE"
                , null, true);
        Mockito.verify(wmsClient, times(1)).createUser("rst-ag-zhozzhz", "Жожа Жора Жорочкин", null, List.of(), "MALE"
                , null, true);
    }

    @Test
    @DbUnitDataSet(
            before = "WmsUserStateManagerTest.FromInProgressToReadyTransitionTests.happyPath.before.csv",
            after = "WmsUserStateManagerTest.FromInProgressToReadyTransitionTests.withExceptionInAd.after.csv"
    )
    public void fromInProgressToReadyTransitionWithExceptionInAd() {
        doThrow(new RuntimeException("something goes wrong"))
                .when(adClient).create(eq("sof-ivanov"), anyString(), anyString(), anyString());
        Assertions.assertThrows(TplIllegalStateException.class, () -> manager.fromInProgressToReadyTransition());
        Mockito.reset(adClient);
    }

    @Test
    @DbUnitDataSet(
            before = "WmsUserStateManagerTest.FromInProgressToReadyTransitionTests.happyPath.before.csv",
            after = "WmsUserStateManagerTest.FromInProgressToReadyTransitionTests.withExceptionInWms.after.csv"
    )
    public void fromInProgressToReadyTransitionWithExceptionInWms() {
        doThrow(new RuntimeException("something goes wrong"))
                .when(wmsClient).createUser(eq("sof-ivanov"), anyString(), anyString(), anyList(),
                        any(), any(), anyBoolean());
        Assertions.assertThrows(TplIllegalStateException.class, () -> manager.fromInProgressToReadyTransition());
        Mockito.reset(wmsClient);
    }

    @Test
    @DbUnitDataSet(
            before = "WmsUserStateManagerTest.FromReadyToInactiveTransitionTests.happyPath.before.csv",
            after = "WmsUserStateManagerTest.FromReadyToInactiveTransitionTests.happyPath.after.csv"
    )
    public void fromReadyToInactiveHappyPath() {
        var stored = HrmsCollectionUtils.toMapBy(wmsUserRepo.findAll(), WmsUserEntity::getLogin);
        stored.values().forEach(wu -> {
            wmsClient.createUser(
                    wu.getLogin(),
                    wu.getFullName(),
                    wu.getEmail(),
                    List.of("role1", "role2"),
                    null,
                    null,
                    wu.getOutstaffId() != null);
            adClient.create(wu.getLogin(), null, null, null);
        });

        manager.fromReadyToInactiveTransition();

        var accountInfoCaptor = ArgumentCaptor.forClass(AccountInfo.class);
        Mockito.verify(adClient, times(2))
                .changePassword(accountInfoCaptor.capture(), any());
        Mockito.verify(adClient, times(2))
                .changeCatalog(accountInfoCaptor.capture(), any());
        Mockito.verify(adClient, times(2))
                .disableUser(accountInfoCaptor.capture());
        var captured = StreamEx.of(accountInfoCaptor.getAllValues())
                .groupingBy(AccountInfo::samAccountName, Collectors.counting());
        captured.forEach((login, times) -> {
            Assertions.assertTrue(stored.containsKey(login));
            Assertions.assertEquals(3, times,
                    "Суммарное количество вызовов changePassword, changeCatalog, disableUser для логина " + login);
        });

        var userIdCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(wmsClient, times(2))
                .deactivateUser(userIdCaptor.capture());
        Assertions.assertEquals(2, userIdCaptor.getAllValues().size(),
                "Количество уникальных userId, которые передавались в deactivateUser");

        var userLoginsCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(wmsClient, times(2))
                .deleteRoles(userLoginsCaptor.capture(), anyList());
        Assertions.assertEquals(2, userLoginsCaptor.getAllValues().size(),
                "Количество уникальных userLogin, которые передавались в deleteRoles");
    }
}
