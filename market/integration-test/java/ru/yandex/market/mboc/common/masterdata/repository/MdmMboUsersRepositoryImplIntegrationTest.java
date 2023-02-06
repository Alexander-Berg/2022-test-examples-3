package ru.yandex.market.mboc.common.masterdata.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMboUser;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMboUsersRepository;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;

/**
 * @author albina-gima
 * @date 24/09/2020
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MdmMboUsersRepositoryImplIntegrationTest extends MdmBaseIntegrationTestClass {
    @Autowired
    private MdmMboUsersRepository mdmMboUsersRepository;

    @Test
    public void testInsertWorks() {
        MdmMboUser mboUser = createMdmMboUser(100500L, "Vera", "vera_login", "vera_staff");

        mdmMboUsersRepository.insert(mboUser);

        List<MdmMboUser> found = mdmMboUsersRepository.findAll();
        Assertions.assertThat(found).hasSize(1);
        Assertions.assertThat(found.get(0)).isEqualToIgnoringGivenFields(mboUser, "updatedTs");
    }

    @Test
    public void testMdmMboUserCanBeFoundByStaffLogin() {
        MdmMboUser mboUser = createMdmMboUser(100500L, "Vera", "vera_login", "vera_staff");

        mdmMboUsersRepository.insert(mboUser);

        Optional<MdmMboUser> mboUserFound = mdmMboUsersRepository.findByStaffLogin("vera_staff");
        Assertions.assertThat(mboUserFound).isPresent();
        Assertions.assertThat(mboUserFound.get()).isEqualToIgnoringGivenFields(mboUser, "updatedTs");
    }

    @Test
    public void testInsertUsersWithSameKeyShouldThrowException() {
        MdmMboUser mdmMboUser1 = createMdmMboUser(100500L, "Vera", "vera_login", "vera_staff");
        MdmMboUser mdmMboUser2 = createMdmMboUser(100501L, "Tom", "tom_login", "tom_staff");
        MdmMboUser mdmMboUser3 = createMdmMboUser(100501L, "Alex", "alex_login", "alex_staff");

        Assertions.assertThatExceptionOfType(DuplicateKeyException.class)
            .isThrownBy(() -> mdmMboUsersRepository.insertBatch(List.of(mdmMboUser1, mdmMboUser2, mdmMboUser3)));
    }

    private MdmMboUser createMdmMboUser(long mboUserId, String name, String login, String staffLogin) {
        MdmMboUser user = new MdmMboUser();
        user.setUid(mboUserId);
        user.setFullName(name);
        user.setYandexLogin(login);
        user.setStaffLogin(staffLogin);
        user.setUpdatedTs(LocalDateTime.now());

        return user;
    }
}
