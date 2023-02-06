package ru.yandex.market.mbo.user;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import ru.yandex.market.dbselector.DbType;
import ru.yandex.market.mbo.configs.MboUserManagerConfig;
import ru.yandex.market.mbo.configs.TestConfiguration;
import ru.yandex.market.mbo.configs.TestPropertiesConfiguration;
import ru.yandex.market.mbo.core.conf.DatabasesConfig;
import ru.yandex.market.mbo.core.conf.databases.DbSelectorManagedDbConfig;
import ru.yandex.market.mbo.core.conf.databases.MboOracleDBConfig;
import ru.yandex.market.mbo.database.configs.PostgresDatabaseConfig;
import ru.yandex.market.mbo.db.MboDbSelector;
import ru.yandex.market.mbo.security.MboRole;
import ru.yandex.market.mbo.statistic.model.TaskType;
import ru.yandex.utils.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        TestConfiguration.class,
        TestPropertiesConfiguration.class,
        MboUserManagerConfig.class,
        DatabasesConfig.class
})
public class UserRolesManagerTest {

    private static final long NOT_REAL_UID_1 = 13L;
    private static final long NOT_REAL_UID_2 = 14L;


    @Autowired
    private UserRolesManager userRolesManager;

    @Autowired
    private UserManager userManager;

    @Autowired
    private MboOracleDBConfig mboOracleDBConfig;

    @Autowired
    private DbSelectorManagedDbConfig dbSelectorManagedDbConfig;

    @Autowired
    private PostgresDatabaseConfig postgresDatabaseConfig;

    @Test
    public void testRewriteSetUsersToCategoryRole() {
        runDbSafe(() -> {
            List<Pair<Long, Long>> hidsAndGurus = getHidAndGuruCategoryIds(2);
            List<Long> uid = getUids(2, MboRole.OPERATOR);
            hidsAndGurus.forEach(pair -> {
                userRolesManager.deleteCategoryFullByHid(pair.getFirst());
                userRolesManager.setUsersToCategoryRole(pair.getSecond(), MboRole.OPERATOR,
                        Collections.singletonList(uid.get(0)));
            });

            hidsAndGurus.forEach(pair -> {
                assertContainsPGUsers(Collections.singletonList(uid.get(0)), pair.getFirst(), MboRole.OPERATOR);
                assertContainsOracleUsers(Collections.singletonList(uid.get(0)), pair.getSecond(),
                        userManager::getCategoryOperatorsWithRoleOperator);
            });

            hidsAndGurus.forEach(pair -> {
                userRolesManager.setUsersToCategoryRole(pair.getSecond(), MboRole.OPERATOR,
                        Collections.singletonList(uid.get(1)));
            });

            hidsAndGurus.forEach(pair -> {
                List<Long> operatorsOracle = userManager.getCategoryOperators(pair.getSecond());
                Collection<Long> operatorPG = userRolesManager.getUsersForHid(pair.getFirst(), MboRole.OPERATOR);
                Assert.assertFalse(operatorsOracle.contains(uid.get(0)));
                Assert.assertFalse(operatorPG.contains(uid.get(0)));
                assertContainsPGUsers(Collections.singletonList(uid.get(1)), pair.getFirst(), MboRole.OPERATOR);
                assertContainsOracleUsers(Collections.singletonList(uid.get(1)), pair.getSecond(),
                        userManager::getCategoryOperatorsWithRoleOperator);
            });
        });
    }

    @Test
    public void testRewriteRoles() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            long uid = getUids(1, MboRole.OPERATOR).get(0);
            userRolesManager.deleteCategoryFullByHid(hid);
            CategoryRole categoryRole = new CategoryRole(MboRole.OPERATOR, hid,
                    Collections.singletonList(TaskType.BLUE_LOGS));
            List<UserBinding> users = Collections.singletonList(
                    new UserBinding(uid, "", Collections.emptyList(), Collections.singletonList(categoryRole))
            );
            userRolesManager.saveUserRoles(users);
            List<Long> uids = users.stream().map(UserBinding::getUid).collect(Collectors.toList());
            assertEqualsPGUsers(uids, hid, MboRole.OPERATOR);
            assertEqualsOracleUsers(uids, guruId, userManager::getCategoryOperatorsWithRoleOperator);

            categoryRole = new CategoryRole(MboRole.SUPER, hid,
                    Collections.singletonList(TaskType.BLUE_LOGS));
            users = Collections.singletonList(
                    new UserBinding(uid, "", Collections.emptyList(), Collections.singletonList(categoryRole))
            );
            userRolesManager.saveUserRoles(users);
            assertEqualsPGUsers(uids, hid, MboRole.SUPER);
            assertEqualsOracleUsers(uids, guruId, userManager::getCategorySupers);
        });
    }

    @Test
    public void testSaveNewUserRoles() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            userRolesManager.deleteCategoryFullByHid(hid);
            addUser(NOT_REAL_UID_1, MboRole.OPERATOR);
            addUser(NOT_REAL_UID_2, MboRole.OPERATOR);
            CategoryRole categoryRole1 = new CategoryRole(MboRole.OPERATOR, hid,
                    Collections.singletonList(TaskType.BLUE_LOGS));
            List<UserBinding> users = Arrays.asList(
                    new UserBinding(NOT_REAL_UID_1, "",
                        Collections.emptyList(), Collections.singletonList(categoryRole1)),
                    new UserBinding(NOT_REAL_UID_2, "",
                        Collections.emptyList(), Collections.singletonList(categoryRole1))
            );

            userRolesManager.saveUserRoles(users);
            List<Long> uids = users.stream().map(UserBinding::getUid).collect(Collectors.toList());
            assertEqualsPGUsers(uids, hid, MboRole.OPERATOR);
            assertEqualsOracleUsers(uids, guruId, userManager::getCategoryOperatorsWithRoleOperator);
        });
    }

    @Test
    public void testUpdateRoles() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            userRolesManager.deleteCategoryFullByHid(hid);
            addUser(NOT_REAL_UID_1, MboRole.OPERATOR);
            addUser(NOT_REAL_UID_2, MboRole.OPERATOR, MboRole.SUPER);
            CategoryRole categoryRole1 = new CategoryRole(MboRole.OPERATOR, hid,
                Collections.singletonList(TaskType.BLUE_LOGS));
            List<UserBinding> users = Arrays.asList(
                new UserBinding(NOT_REAL_UID_1, "",
                    Collections.emptyList(), Collections.singletonList(categoryRole1)),
                new UserBinding(NOT_REAL_UID_2, "",
                    Collections.emptyList(), Collections.singletonList(categoryRole1))
            );

            userRolesManager.saveUserRoles(users);

            Multimap<Long, CategoryRole> toUpdate = ArrayListMultimap.create();
            toUpdate.put(NOT_REAL_UID_1, new CategoryRole(MboRole.OPERATOR, hid, Collections.emptyList())); //deletion
            toUpdate.put(NOT_REAL_UID_2, new CategoryRole(MboRole.SUPER, hid,
                ImmutableList.of(TaskType.BLUE_LOGS, TaskType.MAPPING_MODERATION)));

            userRolesManager.updateRoles(toUpdate);

            assertEqualsPGUsers(Collections.singletonList(NOT_REAL_UID_2), hid, MboRole.OPERATOR);
            assertEqualsOracleUsers(Collections.singletonList(NOT_REAL_UID_2), guruId,
                userManager::getCategoryOperatorsWithRoleOperator);

            assertEqualsPGUsers(ImmutableList.of(NOT_REAL_UID_2), hid, MboRole.SUPER);
            assertEqualsOracleUsers(ImmutableList.of(NOT_REAL_UID_2), guruId, userManager::getCategorySupers);
        });
    }

    @Test
    public void testUserWithoutRolesIsShown() {
        runDbSafe(() -> {
            boolean exist = userRolesManager.getAllUserBindings().stream().filter(u -> u.getUid() == NOT_REAL_UID_1)
                .findFirst().isPresent();
            Assert.assertFalse(exist);
            addUser(NOT_REAL_UID_1, MboRole.OPERATOR);
            exist = userRolesManager.getAllUserBindings().stream().filter(u -> u.getUid() == NOT_REAL_UID_1)
                .findFirst().isPresent();
            Assert.assertTrue(exist);
        });
    }

    @Test
    public void testNotAddingForAbsentGlobalRole() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            long operatorUid = getUids(1, Collections.singletonList(MboRole.OPERATOR),
                Collections.singletonList(MboRole.CATEGORY_SUPPORT_OPERATOR)).get(0);
            userRolesManager.deleteCategoryFullByHid(hid);
            CategoryRole operatorRole = new CategoryRole(MboRole.OPERATOR, hid,
                Collections.singletonList(TaskType.BLUE_LOGS));
            CategoryRole supportRole = new CategoryRole(MboRole.CATEGORY_SUPPORT_OPERATOR, hid,
                Collections.singletonList(TaskType.BLUE_LOGS));

            List<UserBinding> users = Collections.singletonList(
                new UserBinding(operatorUid, "", Collections.emptyList(), Arrays.asList(operatorRole, supportRole))
            );
            userRolesManager.saveUserRoles(users);
            List<Long> uids = users.stream().map(UserBinding::getUid).collect(Collectors.toList());
            assertEqualsPGUsers(uids, hid, MboRole.OPERATOR);
            assertEqualsOracleUsers(uids, guruId, userManager::getCategoryOperatorsWithRoleOperator);
            assertEqualsPGUsers(Collections.emptyList(), hid, MboRole.CATEGORY_SUPPORT_OPERATOR);
            assertEqualsOracleUsers(Collections.emptyList(), guruId, userManager::getSupportCategories);
        });
    }

    @Test
    public void testSuperAvailableToOperatorRole() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            long operatorUid = getUids(1, Collections.singletonList(MboRole.OPERATOR),
                Collections.singletonList(MboRole.SUPER)).get(0);
            userRolesManager.deleteCategoryFullByHid(hid);
            CategoryRole operatorRole = new CategoryRole(MboRole.OPERATOR, hid,
                Collections.singletonList(TaskType.BLUE_LOGS));
            CategoryRole superRole = new CategoryRole(MboRole.SUPER, hid,
                Collections.singletonList(TaskType.BLUE_LOGS));

            List<UserBinding> users = Collections.singletonList(
                new UserBinding(operatorUid, "", Collections.emptyList(), Arrays.asList(operatorRole, superRole))
            );
            userRolesManager.saveUserRoles(users);
            List<Long> uids = users.stream().map(UserBinding::getUid).collect(Collectors.toList());
            assertEqualsPGUsers(uids, hid, MboRole.SUPER);
            assertEqualsOracleUsers(uids, guruId, userManager::getCategorySupers);
            assertEqualsPGUsers(Collections.singletonList(operatorUid), hid, MboRole.SUPER);
            Multimap<Long, CategoryRole> result = userRolesManager.getUserCategoryRoles(
                    UserBindingsFilter.newFilter().setUserId(operatorUid));
            Optional<CategoryRole> superRoleOpt = result.get(operatorUid).stream()
                    .filter(cr -> cr.getHid() == hid)
                    .filter(cr -> cr.getRole() == MboRole.SUPER)
                    .findFirst();
            Assert.assertTrue(superRoleOpt.isPresent());
            Assert.assertEquals(new HashSet<>(superRoleOpt.get().getProjects()),
                    new HashSet<>(Arrays.asList(TaskType.BLUE_LOGS, TaskType.MBO)));
        });
    }

    @Test
    public void testSetUsersToCategoryRole() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            userRolesManager.deleteCategoryFullByHid(hid);
            addUser(NOT_REAL_UID_1, MboRole.OPERATOR);
            addUser(NOT_REAL_UID_2, MboRole.OPERATOR);
            List<Long> uids = Arrays.asList(NOT_REAL_UID_1, NOT_REAL_UID_2);
            userRolesManager.setUsersToCategoryRole(guruId, MboRole.OPERATOR, uids);
            assertEqualsPGUsers(uids, hid, MboRole.OPERATOR);
            assertEqualsOracleUsers(uids, guruId, userManager::getCategoryOperatorsWithRoleOperator);
        });
    }

    @Test
    public void testBindUserRoleToCategories() {
        runDbSafe(() -> {
            List<Pair<Long, Long>> hidsAndGurus = getHidAndGuruCategoryIds(2);
            addUser(NOT_REAL_UID_1, MboRole.OPERATOR);
            userRolesManager.bindUserRoleToCategories(NOT_REAL_UID_1, MboRole.OPERATOR, hidsAndGurus.stream()
                .map(Pair::getSecond)
                .collect(Collectors.toList()));

            hidsAndGurus.forEach(pair -> {
                List<Long> uids = Collections.singletonList(NOT_REAL_UID_1);
                assertContainsPGUsers(uids, pair.getFirst(), MboRole.OPERATOR);
                assertContainsOracleUsers(uids, pair.getSecond(), userManager::getCategoryOperatorsWithRoleOperator);
            });
        });
    }

    @Test
    public void testSaveUserRolesVisualOperators() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            userRolesManager.deleteCategoryFullByHid(hid);
            addUser(NOT_REAL_UID_1, MboRole.VISUAL_OPERATOR);
            addUser(NOT_REAL_UID_2, MboRole.VISUAL_OPERATOR);
            CategoryRole categoryRole1 = new CategoryRole(MboRole.VISUAL_OPERATOR, hid,
                    Collections.singletonList(TaskType.WHITE_LOGS));
            List<UserBinding> users = Arrays.asList(
                    new UserBinding(NOT_REAL_UID_1, "", Collections.emptyList(),
                        Collections.singletonList(categoryRole1)),
                    new UserBinding(NOT_REAL_UID_2, "", Collections.emptyList(),
                        Collections.singletonList(categoryRole1))
            );

            userRolesManager.saveUserRoles(users);

            Collection<Long> uidsPG = userRolesManager.getUsersForGuruCategoryId(guruId,
                    MboRole.VISUAL_OPERATOR);
            Assert.assertEquals(users.size(), uidsPG.size());
            users.forEach(user -> Assert.assertTrue(uidsPG.contains(user.getUid())));

            users.stream()
                    .map(UserBinding::getUid)
                    .forEach(uid -> {
                        List<Long> uidsOracle = userManager.getVisualOperatorCategories(uid);
                        Assert.assertEquals(1, uidsOracle.size());
                        Assert.assertEquals(guruId, (long) uidsOracle.iterator().next());
                    });
        });
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void testUpdateWithDeletion() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            userRolesManager.deleteCategoryFullByHid(hid);
            Set<Integer> roleIds = Arrays.asList(MboRole.OPERATOR, MboRole.SUPER, MboRole.CATEGORY_RESPONSIBLE)
                .stream()
                .map(MboRole::getId)
                .collect(Collectors.toSet());
            long uid = 249661853L;
            userManager.setUserRoles(uid, roleIds);

            CategoryRole categoryRole1 = new CategoryRole(MboRole.OPERATOR, hid,
                Collections.singletonList(TaskType.WHITE_LOGS));
            CategoryRole categoryRole2 = new CategoryRole(MboRole.SUPER, hid,
                Collections.singletonList(TaskType.WHITE_LOGS));

            saveBindings(uid, categoryRole1, categoryRole2);

            List<Long> uids = Collections.singletonList(uid);
            assertContainsPGUsers(uids, hid, MboRole.OPERATOR);
            assertContainsOracleUsers(uids, guruId, userManager::getCategoryOperatorsWithRoleOperator);
            assertContainsPGUsers(uids, hid, MboRole.SUPER);
            assertContainsOracleUsers(uids, guruId, userManager::getCategorySupers);

            CategoryRole categoryRole3 = new CategoryRole(MboRole.CATEGORY_RESPONSIBLE, hid,
                Collections.singletonList(TaskType.WHITE_LOGS));

            saveBindings(uid, categoryRole1, categoryRole3);

            assertContainsPGUsers(uids, hid, MboRole.OPERATOR);
            assertContainsOracleUsers(uids, guruId, userManager::getCategoryOperatorsWithRoleOperator);
            assertContainsPGUsers(Collections.emptyList(), hid, MboRole.SUPER);
            assertContainsOracleUsers(Collections.emptyList(), guruId, userManager::getCategorySupers);
            assertContainsPGUsers(uids, hid, MboRole.CATEGORY_RESPONSIBLE);
            assertContainsOracleUsers(uids, guruId,
                (gId) -> Collections.singletonList(userManager.getCategoryResponsible(gId)));
        });
    }

    @Test
    public void testUpdateProjects() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            userRolesManager.deleteCategoryFullByHid(hid);
            long uid = getUids(1, MboRole.OPERATOR).get(0);

            CategoryRole categoryRole1 = new CategoryRole(MboRole.OPERATOR, hid,
                Arrays.asList(TaskType.WHITE_LOGS));
            CategoryRole categoryRole2 = new CategoryRole(MboRole.SUPER, hid,
                Arrays.asList(TaskType.MSKU_FROM_PSKU_GENERATION));

            saveBindings(uid, categoryRole1, categoryRole2);

            Collection<UserBinding> current =
                userRolesManager.getUserBindings(UserBindingsFilter.newFilter().setUserId(uid));

            //check automatically added MBO
            Map<MboRole, CategoryRole> currentRoles = current.iterator().next().getCategoryRoles().stream()
                .filter(cr -> cr.getHid() == hid)
                .collect(Collectors.toMap(r -> r.getRole(), r -> r));
            Assert.assertEquals(
                new CategoryRole(MboRole.OPERATOR, hid, Arrays.asList(TaskType.WHITE_LOGS, TaskType.MBO)),
                currentRoles.get(MboRole.OPERATOR));

            Assert.assertEquals(
                new CategoryRole(MboRole.SUPER, hid,
                    Arrays.asList(TaskType.MSKU_FROM_PSKU_GENERATION, TaskType.MBO)),
                currentRoles.get(MboRole.SUPER));

            CategoryRole categoryRoleCopy1 = new CategoryRole(categoryRole1.getRole(),
                categoryRole1.getHid(),
                Arrays.asList(TaskType.MBO, TaskType.WHITE_LOGS, TaskType.BLUE_LOGS));

            saveBindings(uid, categoryRoleCopy1, currentRoles.get(MboRole.SUPER));
            current =
                userRolesManager.getUserBindings(UserBindingsFilter.newFilter().setUserId(uid));

            Assert.assertEquals(categoryRoleCopy1,
                current.iterator().next().getCategoryRoles().stream()
                    .filter(r -> r.getRole() == categoryRoleCopy1.getRole() && r.getHid() == hid)
                    .findFirst().orElse(null));
        });
    }

    @Test
    public void testRemoveUser() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            userRolesManager.deleteCategoryFullByHid(hid);
            CategoryRole categoryRole = new CategoryRole(MboRole.SUPER, hid,
                Collections.singletonList(TaskType.BLUE_LOGS));
            UserBinding user = new UserBinding(getUids(1, MboRole.OPERATOR).get(0), "",
                Collections.emptyList(),
                Collections.singletonList(categoryRole));
            userRolesManager.saveUserRoles(Collections.singletonList(user));
            List<Long> uids = Collections.singletonList(user.getUid());
            assertEqualsPGUsers(uids, hid, MboRole.SUPER);
            assertEqualsOracleUsers(uids, guruId, userManager::getCategorySupers);
            userRolesManager.removeUserRoleByHid(user.getUid(), hid, MboRole.SUPER);
            assertEqualsPGUsers(Collections.emptyList(), hid, MboRole.SUPER);
            assertEqualsOracleUsers(Collections.emptyList(), guruId, userManager::getCategorySupers);
        });
    }

    @Test
    public void testDismissUser() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            userRolesManager.deleteCategoryFullByHid(hid);
            addUser(NOT_REAL_UID_1, MboRole.OPERATOR, MboRole.CATEGORY_RESPONSIBLE, MboRole.VISUAL_OPERATOR);
            UserBinding user = new UserBinding(NOT_REAL_UID_1, "",
                    Collections.emptyList(),
                    Arrays.asList(
                            new CategoryRole(MboRole.SUPER, hid, TaskType.BLUE_LOGS),
                            new CategoryRole(MboRole.OPERATOR, hid, TaskType.WHITE_LOGS),
                            new CategoryRole(MboRole.CATEGORY_RESPONSIBLE, hid, TaskType.MBO)
                    ));
            userRolesManager.saveUserRoles(Collections.singletonList(user));
            List<Long> uids = Collections.singletonList(user.getUid());
            assertEqualsPGUsers(uids, hid, MboRole.SUPER);
            assertEqualsPGUsers(uids, hid, MboRole.OPERATOR);
            assertEqualsPGUsers(uids, hid, MboRole.CATEGORY_RESPONSIBLE);
            assertEqualsOracleUsers(uids, guruId, userManager::getCategorySupers);
            assertEqualsOracleUsers(uids, guruId, userManager::getCategoryOperatorsWithRoleOperator);
            Assert.assertEquals(Arrays.asList(guruId), userManager.getResponsibleCategories(user.getUid()));
            userRolesManager.removeUser(user.getUid());
            assertEqualsPGUsers(Collections.emptyList(), hid, MboRole.SUPER);
            assertEqualsPGUsers(Collections.emptyList(), hid, MboRole.OPERATOR);
            assertEqualsPGUsers(Collections.emptyList(), hid, MboRole.CATEGORY_RESPONSIBLE);
            assertEqualsOracleUsers(Collections.emptyList(), guruId, userManager::getCategorySupers);
            assertEqualsOracleUsers(Collections.emptyList(), guruId, userManager::getCategoryOperatorsWithRoleOperator);
            List<Integer> roles = userManager.getRoles(user.getUid());
            Assert.assertTrue(roles.isEmpty());
            Assert.assertEquals(0, userManager.getCategoryResponsible(guruId));
        });
    }

    @Test
    public void testDeleteBindingsByGuruId() {
        runDbSafe(() -> {
            Pair<Long, Long> hidAndGuru = getHidAndGuruCategoryIds(1).get(0);
            long hid = hidAndGuru.getFirst();
            long guruId = hidAndGuru.getSecond();
            long uid = getUids(1, MboRole.OPERATOR).get(0);
            userRolesManager.deleteCategoryFullByHid(hid);
            userRolesManager.bindUserRoleToCategories(uid, MboRole.SUPER, Collections.singletonList(guruId));
            assertContainsPGUsers(Collections.singletonList(uid), hid, MboRole.SUPER);
            assertContainsOracleUsers(Collections.singletonList(uid), guruId, userManager::getCategorySupers);
            userRolesManager.deleteBindingsByGuruId(guruId);
            Assert.assertTrue(userRolesManager.getUsersForGuruCategoryId(guruId, MboRole.SUPER).isEmpty());
            Assert.assertTrue(userRolesManager.getUsersForGuruCategoryId(guruId, MboRole.CATEGORY_RESPONSIBLE)
                .isEmpty());
            Assert.assertTrue(userManager.getCategorySupers(guruId).isEmpty());
            Assert.assertEquals(0L, userManager.getCategoryResponsible(guruId));
        });
    }

    private void saveBindings(long uid, CategoryRole... roles) {
        userRolesManager.saveUserRoles(Collections.singletonList(
            new UserBinding(uid, "", Collections.emptyList(), Arrays.asList(roles))
        ));
    }

    private void assertEqualsPGUsers(List<Long> uids, long hid, MboRole mboRole) {
        Collection<Long> uidsPG = userRolesManager.getUsersForHid(hid, mboRole);
        Assert.assertEquals(uids.size(), uidsPG.size());
        uidsPG.forEach(uid -> Assert.assertTrue(uids.contains(uid)));
    }

    private void assertContainsPGUsers(List<Long> uids, long hid, MboRole mboRole) {
        Collection<Long> uidsPG = userRolesManager.getUsersForHid(hid, mboRole);
        uids.forEach(uid -> Assert.assertTrue(uidsPG.contains(uid)));
    }

    private void addUser(Long uid, MboRole... roles) {
        userManager.addUser(uid, "I.C.Winner", "pitun" + uid, "lololog");
        for (MboRole role : roles) {
            userManager.setUserRole(uid, role.getId());
        }
    }

    private void assertEqualsOracleUsers(List<Long> uids, long guruId, Function<Long, List<Long>> oracleUsersGetter) {
        List<Long> uidsOracle = oracleUsersGetter.apply(guruId);
        Assert.assertEquals(uids.size(), uidsOracle.size());
        uidsOracle.forEach(uid -> Assert.assertTrue(uids.contains(uid)));
    }

    private void assertContainsOracleUsers(List<Long> uids, long guruId, Function<Long, List<Long>> oracleUsersGetter) {
        List<Long> uidsOracle = oracleUsersGetter.apply(guruId);
        uids.forEach(uid -> Assert.assertTrue(uidsOracle.contains(uid)));
    }

    private List<Long> getUids(int count, MboRole... roles) {
        return getUids(count, Arrays.asList(roles), Collections.emptyList());
    }

    private List<Long> getUids(int count, Collection<MboRole> neededRoles, Collection<MboRole> excludedRoles) {
        MapSqlParameterSource parameterSource = new MapSqlParameterSource()
            .addValue("role_ids", toIds(neededRoles))
            .addValue("role_count", neededRoles.size())
            .addValue("count", count);
        String and = "";
        if (excludedRoles.size() > 0) {
            parameterSource.addValue("excluded_role_ids", toIds(excludedRoles));
            and = "and role_id not in (:excluded_role_ids)";
        }
        return mboOracleDBConfig.namedScatJdbcTemplate().queryForList("select user_id from " +
                "(select user_id from (select user_id, role_id from tt_user_role where role_id in (:role_ids) " + and +
                ") group by user_id having count(1) = :role_count) " +
                "where rownum <= :count", parameterSource, Long.class);
    }

    private List<Integer> toIds(Collection<MboRole> roles) {
        return roles.stream().map(MboRole::getId).collect(Collectors.toList());
    }

    private List<Pair<Long, Long>> getHidAndGuruCategoryIds(int count) {
        List<Pair<Long, Long>> hidsAndGurus = new ArrayList<>();

        MboDbSelector dbSelector = dbSelectorManagedDbConfig.userRolesManagerDbSelector();
        JdbcTemplate jdbcTemplate = dbSelector.getProxyingJdbcTemplate();
        if (dbSelector.getDbType() == DbType.ORACLE) {
            jdbcTemplate.query("SELECT guru_category_id, hyper_id from mc_category " +
                        "WHERE guru_category_id is not null AND hyper_id is not null AND ROWNUM <= " + count,
                    rs -> {
                        hidsAndGurus.add(new Pair<>(rs.getLong("hyper_id"), rs.getLong("guru_category_id")));
                    }
                );
        } else if (dbSelector.getDbType() == DbType.POSTGRESQL) {
            jdbcTemplate.query("SELECT guru_category_id, hyper_id from mc_category " +
                        "WHERE guru_category_id is not null AND hyper_id is not null limit " + count,
                    rs -> {
                        hidsAndGurus.add(new Pair<>(rs.getLong("hyper_id"), rs.getLong("guru_category_id")));
                    }
                );
        } else  {
            throw new IllegalStateException(String.format("Unknown database type %s",
                dbSelector.getDbType()));
        }
        return hidsAndGurus;
    }

    private void runDbSafe(Runnable test) {
        mboOracleDBConfig.scatTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                status.setRollbackOnly();
                postgresDatabaseConfig.postgresTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        status.setRollbackOnly();
                        test.run();
                    }
                });
            }
        });
    }

}
