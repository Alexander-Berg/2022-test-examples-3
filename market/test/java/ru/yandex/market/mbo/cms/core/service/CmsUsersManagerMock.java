package ru.yandex.market.mbo.cms.core.service;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ru.yandex.market.mbo.cms.core.json.idm.entities.RoleWithUser;
import ru.yandex.market.mbo.cms.core.service.user.User;
import ru.yandex.market.mbo.cms.core.service.user.UserRole;
import ru.yandex.market.mbo.cms.core.service.user.UsersManager;

public class CmsUsersManagerMock implements UsersManager {
    public static final String DEFAULT_NAME = "test user";
    public static final String DEFAULT_LOGIN = "testuser";
    private final long userId;

    public CmsUsersManagerMock(long userId) {
        this.userId = userId;
    }

    @Override
    public String getUserName(long id) {
        return id == userId ? DEFAULT_NAME : null;
    }

    @Override
    public String getUserStaffLogin(long id) {
        return id == userId ? DEFAULT_LOGIN : null;
    }

    @Override
    public boolean isUserExists(long id) {
        return false;
    }

    @Override
    public Collection<UserRole> getUserRoles(long id) {
        return id == userId ? EnumSet.allOf(UserRole.class) : EnumSet.noneOf(UserRole.class);
    }

    @Override
    public Collection<Long> getAllIds() {
        return Collections.singleton(userId);
    }

    @Override
    public boolean havePermission(long id, UserRole userRole) {
        return true;
    }

    @Override
    public void throwIfUserNotHavePermission(long userId, UserRole role) {
    }

    @Override
    public boolean addUser(long uid, String regname, String staffLogin, Long staffId) {
        return false;
    }

    @Override
    public boolean addUsers(List<User> users) {
        return false;
    }

    @Override
    public void setUserRoles(Long userId, Set<UserRole> newRoles) {
        throw new UnsupportedOperationException("setUserRoles is not supported by CmsUsersManagerMock");
    }

    @Override
    public List<User> findUsers(String query, int limit, int offset) {
        throw new UnsupportedOperationException("Not supported by CmsUsersManagerMock");
    }

    @Override
    public Integer findUsersCount(String query) {
        throw new UnsupportedOperationException("Not supported by CmsUsersManagerMock");
    }

    @Override
    public User getUser(long id) {
        throw new UnsupportedOperationException("Not supported by CmsUsersManagerMock");
    }

    @Override
    public Long getUserIdByStaffLogin(String staffLogin) {
        throw new UnsupportedOperationException("Not supported by CmsUsersManagerMock");
    }

    @Override
    public List<RoleWithUser> getAllUsersWithRoles(int limit, long offset) {
        throw new UnsupportedOperationException("Not supported by CmsUsersManagerMock");
    }

    @Override
    public long getAllUsersWithRolesCount() {
        throw new UnsupportedOperationException("Not supported by CmsUsersManagerMock");
    }
}
