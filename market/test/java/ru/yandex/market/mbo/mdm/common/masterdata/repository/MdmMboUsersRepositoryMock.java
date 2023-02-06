package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.Objects;
import java.util.Optional;

import ru.yandex.market.mbo.lightmapper.test.EmptyGenericMapperRepositoryMock;

/**
 * @author albina-gima
 * @date 24/09/2020
 */
public class MdmMboUsersRepositoryMock extends EmptyGenericMapperRepositoryMock<MdmMboUser, Long>
    implements MdmMboUsersRepository {

    public MdmMboUsersRepositoryMock() {
        super(MdmMboUser::getUid);
    }

    @Override
    public Optional<MdmMboUser> findByStaffLogin(String staffLogin) {
        return findAll().stream().filter(obj -> Objects.equals(obj.getStaffLogin(), staffLogin)).findFirst();
    }
}
