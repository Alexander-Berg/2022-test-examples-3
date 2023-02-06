package ru.yandex.market.mbo.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.jetbrains.annotations.NotNull;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.security.MboRoles;
import ru.yandex.market.mbo.tt.model.Operator;

/**
 * @author ayratgdl
 * @date 28.03.18
 */
public class UserManagerMock implements UserManager {
    private Map<Long, MboUser> users = new HashMap<>();
    private List<CategoryUsersInfo> categoryUsersInfos = new ArrayList<>();
    private ArrayListMultimap<Long, Integer> uidToRoles = ArrayListMultimap.create();
    private Map<Long, Set<Long>> superOpsPerCategory = new HashMap<>();
    private Map<Long, Set<Long>> supportOpsPerCategory = new HashMap<>();
    private Multimap<Long, Long> subordinatesByChief = ArrayListMultimap.create();
    private Map<Integer, Role> roles = new HashMap<>();

    {
        roles.put(MboRoles.ADMIN,
            new Role(MboRoles.ADMIN, "admin", "a", "", "Админ"));
        roles.put(MboRoles.OPERATOR,
            new Role(MboRoles.OPERATOR, "operator", "o", "", "Оператор"));
    }

    @Override
    public void addUser(long uid, String name, String login, String email) {
        users.put(uid, new MboUser(login, uid, name, email, login));
    }

    @Override
    public void setCategoryOperators(List<Long> operators, long categoryId) {
        throw new UnsupportedOperationException("setCategoryOperators isn't implemented yet");
    }

    @Override
    public void addUser(MboUser mboUser) {
        users.put(mboUser.getUid(), mboUser);
    }

    public void addUserWithRole(MboUser mboUser, int roleId) {
        users.put(mboUser.getUid(), mboUser);
        addRole(mboUser.getUid(), roleId);
    }

    @Override
    public void setUserRole(long uid, int roleId) {
        uidToRoles.put(uid, roleId);
    }

    @Override
    public void setCategorySuperoperator(long uid, long order, long categoryId) {
        setCategorySuperoperators(Collections.singletonList(uid), categoryId);
    }

    @Override
    public void setCategorySuperoperators(List<Long> selectedOperators, long categoryId) {
        Set<Long> ops = superOpsPerCategory.computeIfAbsent(categoryId, c -> new HashSet<>());
        ops.addAll(selectedOperators);
    }

    @Override
    public void setSuperOperatorCategories(long operatorId, List<Long> categories) {
        throw new UnsupportedOperationException("setSuperOperatorCategories not implemented");
    }

    @Override
    public void setCategorySupportOperators(List<Long> uid, long categoryId) {
        Set<Long> ops = supportOpsPerCategory.computeIfAbsent(categoryId, c -> new HashSet<>());
        ops.addAll(uid);
    }

    @Override
    public void setSupportOperatorCategories(long operatorId, List<Long> categories) {
        throw new UnsupportedOperationException("setSupportOperatorCategories isn't implemented yet");
    }

    @Override
    public void setCategoryResponse(long uid, long categoryId) {
        throw new UnsupportedOperationException("setCategoryResponse isn't implemented yet");
    }

    @Override
    public void removeUserRole(long uid, int roleId) {
        uidToRoles.remove(uid, roleId);
    }

    @Override
    public void removeAllUserRoles(long uid) {
        throw new UnsupportedOperationException("removeAllUserRoles isn't implemented yet");
    }

    @Override
    public void removeCategorySuperoperator(long uid, long categoryId) {
        throw new UnsupportedOperationException("removeCategorySuperoperator isn't implemented yet");
    }

    @Override
    public void removeCategorySupportOperators(long uid, long categoryId) {
        throw new UnsupportedOperationException("removeCategorySupportOperators isn't implemented yet");
    }

    @Override
    public void removeCategoryResponse(long uid, long categoryId) {
        throw new UnsupportedOperationException("removeCategoryResponse isn't implemented yet");
    }

    @Override
    public void removeAllCategoryResponse(long uid) {
        throw new UnsupportedOperationException("removeAllCategoryResponse isn't implemented yet");
    }

    @Override
    public void removeCategoryOperator(long uid, long categoryId) {
        throw new UnsupportedOperationException("removeCategoryOperator isn't implemented yet");
    }

    @Override
    public void removeCategoryVisualOperator(long uid, long categoryId) {
        throw new UnsupportedOperationException("removeCategoryVisualOperator isn't implemented yet");
    }

    @Override
    public void removeCategoryFull(long categoryId) {
        throw new UnsupportedOperationException("removeCategoryFull isn't implemented yet");
    }

    @Override
    public Collection<MboUserWithRoles> getAllRoleUsersWithRoles() {
        return users.values().stream()
            .filter(u -> uidToRoles.containsKey(u.getUid()))
            .map(MboUserWithRoles::new)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<MboUserWithRoles> getAllUsersWithRoles() {
        return users.values().stream()
            .map(MboUserWithRoles::new)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<MboUserWithRoles> getAllUsersWithGlobalRoles() {
        return uidToRoles.asMap().entrySet().stream().map(it -> {
            MboUser mboUser = users.get(it.getKey());
            MboUserWithRoles mboUserWithRoles = new MboUserWithRoles(mboUser);
            it.getValue().stream().forEach(roleId -> {
                Role role = roles.get(roleId);
                if (role != null) {
                    mboUserWithRoles.addRole(role.getId(), role.getName());
                }
            });
            return mboUserWithRoles;
        }).collect(Collectors.toList());
    }

    @Override
    public FullUserInfo getFullUserInfo(long uid) {
        throw new UnsupportedOperationException("getFullUserInfo isn't implemented yet");
    }

    @Override
    public List<Operator> getAllOperators() {
        throw new UnsupportedOperationException("getAllOperators isn't implemented yet");
    }

    @Override
    public List<MboUser> getAllMboUser() {
        return new ArrayList<>(users.values());
    }

    @Override
    public List<MboUser> getMboUsersByIds(Collection<Long> uids) {
        throw new UnsupportedOperationException("getMboUsersByIds isn't implemented yet");
    }

    @Override
    public List<Operator> getOperatorsByRole(int roleId) {
        throw new UnsupportedOperationException("getOperatorsByRole isn't implemented yet");
    }

    @Override
    public List<Long> getOperatorsIdsByRole(int roleId) {
        throw new UnsupportedOperationException("getOperatorsIdsByRole isn't implemented yet");
    }

    @Override
    public List<Long> getOperatorCategories(long userId) {
        throw new UnsupportedOperationException("getOperatorCategories isn't implemented yet");
    }

    @Override
    public MboUser getUserInfoByStaffLogin(String staffLogin) {
        return users.entrySet().
            stream()
            .filter(it -> staffLogin.equals(it.getValue().getStaffLogin()))
            .map(Map.Entry::getValue)
            .findFirst().orElse(MboUser.NULL);
    }

    @Override
    public List<Long> getAssessorIds() {
        throw new UnsupportedOperationException("getAssessorIds isn't implemented yet");
    }

    @Override
    public List<Long> getPartnerContentAdminIds() {
        throw new UnsupportedOperationException("getPartnerContentAdminIds isn't implemented yet");
    }

    @Override
    public void setOperatorCategories(long operatorId, List<Long> categories) {
        throw new UnsupportedOperationException("setOperatorCategories isn't implemented yet");
    }

    @Override
    public List<Long> getChiefs(long userId) {
        Multimap<Long, Long> chiefsBySubordinate = ArrayListMultimap.create();
        Multimaps.invertFrom(subordinatesByChief, chiefsBySubordinate);
        return new ArrayList<>(chiefsBySubordinate.get(userId));
    }

    @Override
    public List<Long> getChiefsInCategory(long userId, long categoryId) {
        throw new UnsupportedOperationException("getChiefsInCategory isn't implemented yet");
    }

    @NotNull
    @Override
    public List<Integer> getRoles(long userId) {
        return uidToRoles.get(userId);
    }

    @NotNull
    @Override
    public Collection<Role> getIdmRoles() {
        return roles.values();
    }

    public void addRole(long uid, int roleId) {
        uidToRoles.put(uid, roleId);
    }

    @Override
    public List<Long> getResponsibleCategories(long userId) {
        throw new UnsupportedOperationException("getResponsibleCategories isn't implemented yet");
    }

    @Override
    public List<Long> getSuperCategories(long userId) {
        throw new UnsupportedOperationException("getSuperCategories isn't implemented yet");
    }

    @Override
    public List<Long> getSupportCategories(long userId) {
        throw new UnsupportedOperationException("getSupportCategories isn't implemented yet");
    }

    @Override
    public List<Long> getSubordinates(long userId) {
        throw new UnsupportedOperationException("getSubordinates isn't implemented yet");
    }

    @Override
    public Map<Long, Collection<Long>> getSubordinatesMap() {
        throw new UnsupportedOperationException("getSubordinatesMap isn't implemented yet");
    }

    @Override
    public Operator getOperator(long operatorId) {
        throw new UnsupportedOperationException("getOperator isn't implemented yet");
    }

    @Override
    public long getCategoryResponsible(long categoryId) {
        throw new UnsupportedOperationException("getCategoryResponsible isn't implemented yet");
    }

    @Override
    public MboUser getCategoryResponsibleUser(long categoryId) {
        throw new UnsupportedOperationException("getCategoryResponsibleUser isn't implemented yet");
    }

    @Override
    public List<Long> getCategorySupers(long categoryId) {
        throw new UnsupportedOperationException("getCategorySupers isn't implemented yet");
    }

    @Override
    public List<OrderedUser> getCategorySuperUsers(long categoryId) {
        throw new UnsupportedOperationException("getCategorySuperUsers isn't implemented yet");
    }

    @Override
    public List<MboUser> getCategorySupportUsers(long categoryId) {
        throw new UnsupportedOperationException("getCategorySupportUsers isn't implemented yet");
    }

    @Override
    public List<Pair<Long, Long>> getSuperUserPerCategory() {
        List<Pair<Long, Long>> result = new ArrayList<>();
        superOpsPerCategory.forEach((categoryId, operators) -> {
            operators.forEach(operator -> result.add(new Pair<>(operator, categoryId)));
        });
        return result;
    }

    @Override
    public List<Pair<Long, Long>> getSupportUserPerCategory() {
        List<Pair<Long, Long>> result = new ArrayList<>();
        supportOpsPerCategory.forEach((categoryId, operators) -> {
            operators.forEach(operator -> result.add(new Pair<>(operator, categoryId)));
        });
        return result;
    }

    @Override
    public void setUserRoles(long uid, Set<Integer> newRoles) {
        uidToRoles.putAll(uid, newRoles);
    }

    @Override
    public void setUserSubordinates(long uid, List<Long> newSubs) {
        subordinatesByChief.removeAll(uid);
        subordinatesByChief.putAll(uid, newSubs);
    }

    @Override
    public void setFullName(long uid, String fullName) {
        if (users.containsKey(uid)) {
            MboUser user = users.get(uid);
            MboUser updatedUser =
                new MboUser(user.getLogin(), user.getUid(), fullName, user.getStaffEmail(), user.getStaffLogin());
            users.put(uid, updatedUser);
        }
    }

    @Override
    public void setStaffLogin(long uid, String login) {
        if (users.containsKey(uid)) {
            MboUser user = users.get(uid);
            MboUser updatedUser =
                new MboUser(user.getLogin(), user.getUid(), user.getFullname(), user.getStaffEmail(), login);
            users.put(uid, updatedUser);
        }
    }

    @Override
    public Set<Role> getEditableRoles() {
        throw new UnsupportedOperationException("getEditableRoles isn't implemented yet");
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Override
    public Set<Role> getAllRoles() {
        return new HashSet<Role>() {{
            add(new Role(1, "super (old)", "S(old)"));
            add(new Role(2, "operator"));
            add(new Role(3, "admin", "A"));
            add(new Role(4, "assesor (old)", "A(old)"));
            add(new Role(5, "mapper-operator", "M"));
            add(new Role(6, "category_superoperator", "S"));
            add(new Role(7, "ResponsibleForCategory", "R"));
            add(new Role(8, "operator_chief", "OC"));
            add(new Role(9, "guest", "G"));
            add(new Role(10, "operator_in_category", "Cat_O"));
            add(new Role(11, "robot-admin"));
            add(new Role(12, "robot-operator"));
            add(new Role(13, "visual-operator", "V"));
            add(new Role(14, "visual-super-operator", "VS"));
            add(new Role(15, "robot-user"));
            add(new Role(16, "classifier-manager", "CM"));
            add(new Role(17, "global-log-user", "GL"));
            add(new Role(18, "assessor", "AS"));
            add(new Role(19, "sku-mapping-operator", "SMO"));
            add(new Role(20, "support-operator", "SUPP"));
            add(new Role(21, "global-log-operator", "GLO"));
            add(new Role(22, "operator-size-measure", "MEA"));
            add(new Role(23, "partner_content_admin", "PCA"));
        }};
    }

    @Override
    public boolean isAdmin(long uid) {
        throw new UnsupportedOperationException("isAdmin isn't implemented yet");
    }

    @Override
    public MboUser getUserInfo(long uid) {
        return users.getOrDefault(uid, MboUser.NULL);
    }

    @Override
    public List<CategoryUsersInfo> getAllCategoriesUsers() {
        return Collections.unmodifiableList(categoryUsersInfos);
    }

    public void addCategoryUsersInfo(CategoryUsersInfo categoryUsersInfo) {
        this.categoryUsersInfos.add(categoryUsersInfo);
    }

    @Override
    public List<Long> getVisualOperatorCategories(long userId) {
        throw new UnsupportedOperationException("getVisualOperatorCategories isn't implemented yet");
    }

    @Override
    public void setVisualOperatorCategories(long userId, List<Long> visualOperatorCategories) {
        throw new UnsupportedOperationException("setVisualOperatorCategories isn't implemented yet");
    }

    @Override
    public void setCategoryVisualOperators(List<Long> uid, long categoryId) {
        throw new UnsupportedOperationException("setCategoryVisualOperators not implemented");
    }

    @Override
    public void deleteCategory(long categoryId) {
        superOpsPerCategory.remove(categoryId);
        categoryUsersInfos.removeIf(ci -> ci.getGuruCategoryId() == categoryId);
    }

    @Override
    public List<Long> getCategoryOperatorsWithRoleOperator(long categoryId) {
        throw new UnsupportedOperationException("getCategoryOperatorsWithRoleOperator isn't implemented yet");
    }

    @Override
    public List<Long> getCategoryOperators(long categoryId) {
        throw new UnsupportedOperationException("getCategoryOperators isn't implemented yet");
    }
}
