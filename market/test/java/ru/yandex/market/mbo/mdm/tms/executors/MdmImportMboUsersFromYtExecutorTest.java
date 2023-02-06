package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMboUsersRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.services.mbouserstorage.YtMboUserInfo;
import ru.yandex.market.mboc.common.services.mbouserstorage.YtMboUserStorageReaderMock;

/**
 * @author albina-gima
 * @date 24/09/2020
 */
public class MdmImportMboUsersFromYtExecutorTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmMboUsersRepository mdmMboUsersRepository;

    private MdmImportMboUsersFromYtExecutor executor;
    private YtMboUserStorageReaderMock ytReader;

    @Before
    public void setup() {
        ytReader = new YtMboUserStorageReaderMock();
        executor = new MdmImportMboUsersFromYtExecutor(ytReader, mdmMboUsersRepository);
    }

    @Test
    public void testMboUserInfoStored() {
        YtMboUserInfo info1 = new YtMboUserInfo().setId(1).setName("Tom").setLogin("login1")
            .setStaffLogin("staff1").setRoles(List.of("operator", "operator_chief")).setSupervisorsIds(List.of(100500L));
        YtMboUserInfo info2 = new YtMboUserInfo().setId(2).setName("Alice").setLogin("login2")
            .setStaffLogin("staff1").setRoles(List.of()).setSupervisorsIds(List.of());
        YtMboUserInfo info3 = new YtMboUserInfo().setId(3).setName("Mark").setLogin("login3")
            .setStaffLogin("staff1").setRoles(List.of("operator")).setSupervisorsIds(List.of());
        YtMboUserInfo info4 = new YtMboUserInfo().setId(4).setName("Alex").setLogin("login4")
            .setStaffLogin("staff1").setRoles(List.of()).setSupervisorsIds(List.of(100600L, 100700L));

        ytReader.prepareMboUserInfo(Arrays.asList(info1, info2, info3, info4));
        executor.execute();

        Assertions.assertThat(mdmMboUsersRepository.findAll()).usingElementComparatorIgnoringFields("updatedTs")
            .containsExactlyInAnyOrder(
                MdmImportMboUsersFromYtExecutor.convertToMdmMboUser(info1),
                MdmImportMboUsersFromYtExecutor.convertToMdmMboUser(info2),
                MdmImportMboUsersFromYtExecutor.convertToMdmMboUser(info3),
                MdmImportMboUsersFromYtExecutor.convertToMdmMboUser(info4)
            );
    }
}
