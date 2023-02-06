package ru.yandex.market.mbo.user;

import com.google.common.base.Functions;
import com.google.common.collect.Multimap;
import org.springframework.util.CollectionUtils;
import ru.yandex.market.mbo.security.MboRole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UserRolesManagerMock implements UserRolesManager {

    private UserManager userManagerMock;
    private List<UserBinding> userBindings;

    public UserRolesManagerMock(UserManager userManagerMock) {
        this.userManagerMock = userManagerMock;
        userBindings = new ArrayList<>();
    }

    @Override
    public Collection<Long> getUsersForGuruCategoryId(long guruId, MboRole mboRole) {
        throw new UnsupportedOperationException("getUsersForGuruCategoryId isn't implemented yet");
    }

    @Override
    public Collection<Long> getUsersForHid(long hid, MboRole mboRole) {
        throw new UnsupportedOperationException("getUsersForHid isn't implemented yet");
    }

    @Override
    public MboUser getUserInfo(long uid) {
        return userManagerMock.getUserInfo(uid);
    }

    @Override
    public Collection<MboUser> getMboUsersByIds(Collection<Long> uids) {
        return userManagerMock.getMboUsersByIds(uids);
    }

    @Override
    public Collection<UserBinding> getUserBindings(UserBindingsFilter filter) {
        return userBindings.stream()
                .filter(userBinding -> filter.getUserIds().isEmpty()
                    || filter.getUserIds().contains(userBinding.getUid()))
                .map(userBinding -> filterCategoryRoles(userBinding, categoryRole ->
                        (filter.getHid() == null || categoryRole.getHid() == filter.getHid())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Multimap<Long, CategoryRole> getUserCategoryRoles(UserBindingsFilter filter) {
        return null;
    }

    private UserBinding filterCategoryRoles(UserBinding userBinding,
                                            Predicate<CategoryRole> filterCategoryRole) {
        List<CategoryRole> categoryRolesFiltered = userBinding.getCategoryRoles().stream()
                .filter(filterCategoryRole)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(categoryRolesFiltered)) {
            return null;
        }
        return new UserBinding(userBinding.getUid(), userBinding.getName(),
            Collections.emptySet(), categoryRolesFiltered);
    }

    @Override
    public List<Long> getGuruCategoryIdsForUser(long uid, MboRole mboRole) {
        throw new UnsupportedOperationException("getGuruCategoryIdsForUser isn't implemented yet");
    }

    @Override
    public List<Long> getCategoryIdsForUser(long uid, MboRole mboRole) {
        throw new UnsupportedOperationException("getCategoryIdsForUser isn't implemented yet");
    }

    @Override
    public void saveUserRoles(List<UserBinding> users) {
        Map<Long, UserBinding> usersMap = users.stream()
                .collect(Collectors.toMap(UserBinding::getUid, Functions.identity()));
        userBindings = userBindings.stream()
                .map(userBinding -> filterCategoryRoles(userBinding, categoryRole ->
                        !deleteOrNotCategoryRole(categoryRole, usersMap.get(userBinding.getUid()))))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        userBindings.addAll(users);
    }

    @Override
    public void updateRoles(Multimap<Long, CategoryRole> rolesByUid) {
        throw new UnsupportedOperationException("updateRoles isn't implemented yet");
    }

    private boolean deleteOrNotCategoryRole(CategoryRole categoryRoleCurrent, UserBinding user) {
        return user != null && user.getCategoryRoles().stream()
                .anyMatch(categoryRole -> categoryRole.getHid() == categoryRoleCurrent.getHid() &&
                        categoryRole.getRole() == categoryRoleCurrent.getRole());
    }

    @Override
    public void bindUserRoleToCategories(long uid, MboRole mboRole, List<Long> guruIds) {
        throw new UnsupportedOperationException("bindUserRoleToCategories isn't implemented yet");
    }

    @Override
    public void bindUserRoleToHids(long uid, MboRole mboRole, List<Long> guruIds) {
        throw new UnsupportedOperationException("bindUserRoleToHids isn't implemented yet");
    }

    @Override
    public void setUsersToCategoryRole(long guruId, MboRole mboRole, List<Long> uids) {
        throw new UnsupportedOperationException("setUsersToCategoryRole isn't implemented yet");
    }

    @Override
    public void removeUserRoleByGuruId(long uid, long guruId, MboRole mboRole) {
        throw new UnsupportedOperationException("removeUserRoleByGuruId isn't implemented yet");
    }

    @Override
    public void removeUserRoleByHid(long uid, long hid, MboRole mboRole) {
        throw new UnsupportedOperationException("removeCategoryUserByHid isn't implemented yet");
    }

    @Override
    public void deleteBindingsByGuruId(long guruId) {
        userManagerMock.deleteCategory(guruId);
    }

    @Override
    public void deleteBindingsByHid(long hid) {
        throw new UnsupportedOperationException("deleteCategoryByHid isn't implemented yet");
    }

    @Override
    public void deleteCategoryFullByGuruId(long guruId) {
        throw new UnsupportedOperationException("deleteCategoryFullByGuruId isn't implemented yet");
    }

    @Override
    public void deleteCategoryFullByHid(long hid) {
        throw new UnsupportedOperationException("deleteCategoryFullByHid isn't implemented yet");
    }

    @Override
    public void removeUser(long uid) {
        userManagerMock.removeAllUserRoles(uid);
        userBindings.removeIf(filter -> filter.getUid() == uid);
    }
}
