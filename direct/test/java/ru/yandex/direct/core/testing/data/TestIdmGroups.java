package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;

public final class TestIdmGroups {

    /**
     * Это внешний идентификатор. В проде соответствует идентификатору группы на стафе.
     */
    public static final long DEFAULT_IDM_GROUP_ID = 99L;

    public static final IdmRequiredRole DEFAULT_REQUIRED_ROLE = IdmRequiredRole.MANAGER;

    public static IdmGroup defaultIdmGroup() {
        return new IdmGroup().withIdmGroupId(DEFAULT_IDM_GROUP_ID).withRequiredRole(DEFAULT_REQUIRED_ROLE);
    }

}
