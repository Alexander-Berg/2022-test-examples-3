package ru.yandex.market.security.data;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.id.IdModel;
import ru.yandex.market.security.data.kampfer.PermissionModel;
import ru.yandex.market.security.data.kampfer.impl.simple.SimpleKampfer;
import ru.yandex.market.security.data.kampfer.impl.simple.sql.SqlDialect;
import ru.yandex.market.security.data.kampfer.model.OperationPermissionRow;
import ru.yandex.market.security.data.kampfer.model.OperationRow;
import ru.yandex.market.security.data.kampfer.model.PermissionAuthorityRow;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class SimpleKampferTest extends CachedKampferTest {

    @Test
    void addAuth() {
        tester.insertDataSet("domain.single.csv");
        Kampfer kampfer = getKampfer("test");
        kampfer.authority().addAuth("AUTH", "auth description", "checker");
        tester.assertDataSet("addAuth.after.csv");
    }

    @Test
    void linkAuthsOr() {
        tester.insertDataSet("domain.single.csv");
        Kampfer kampfer = getKampfer("test");
        kampfer.authority().addAuth("AUTH1", null, "checker1");
        kampfer.authority().addAuth("AUTH2", null, "checker2");
        kampfer.authority().linkAuthsOr(
                kampfer.authority().getAuthId("AUTH1"),
                kampfer.authority().getAuthId("AUTH2"),
                "param"
        );
        tester.assertDataSet("linkAuthsOr.after.csv");
    }

    @Test
    void linkAuthsAnd() {
        tester.insertDataSet("domain.single.csv");
        Kampfer kampfer = getKampfer("test");
        kampfer.authority().addAuth("AUTH1", null, "checker1");
        kampfer.authority().addAuth("AUTH2", null, "checker2");
        kampfer.authority().linkAuthsAnd(
                kampfer.authority().getAuthId("AUTH1"),
                kampfer.authority().getAuthId("AUTH2"),
                "param"
        );
        tester.assertDataSet("linkAuthsAnd.after.csv");
    }

    @Test
    void removeAuthLink() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        kampfer.authority().removeAuthLink(
                kampfer.authority().getAuthId("auth4"),
                kampfer.authority().getAuthId("auth5")
        );
        tester.assertDataSet("removeAuthLink.after.csv");
    }

    @Test
    void removeAuth() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        kampfer.authority().removeAuth("auth5");
        tester.assertDataSet("removeAuth.after.csv");
    }

    @Test
    void setAuthChecker() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        kampfer.authority().setAuthChecker(kampfer.authority().getAuthId("auth4"), "checker5");
        tester.assertDataSet("setAuthChecker.after.csv");
    }

    @Test
    void addOperation() {
        tester.insertDataSet("domain.single.csv");
        Kampfer kampfer = getKampfer("test");
        long opId = kampfer.operation().addOperation("/test", "тестовая операция");
        assertEquals(1L, opId);
        tester.assertDataSet("addOperation.after.csv");
    }

    @Test
    void addOperations() {
        Kampfer kampfer = getKampfer("test");
        List<OperationRow> operations = List.of(
                new OperationRow("/test1", "тестовая операция1", "test1", 1),
                new OperationRow("/test2", "тестовая операция2", "test2", 2),
                new OperationRow("/test3", "тестовая операция3", "test3", 3)
        );
        final List<Long> ids = kampfer.operation()
                .addOperations(operations)
                .stream()
                .map(IdModel::getId)
                .collect(Collectors.toList());
        assertEquals(List.of(1L, 2L, 3L), ids);
        tester.assertDataSet("addOperations.after.csv");
    }

    @Test
    void renameOperation() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        kampfer.operation().renameOperation("op", "newop");
        tester.assertDataSet("renameOperation.after.csv");
    }

    @Test
    void renameOperationById() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        kampfer.operation().renameOperation(3, "newop");
        tester.assertDataSet("renameOperation.after.csv");
    }

    @Test
    void removeOperation() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        kampfer.operation().removeOperation("op_child");
        tester.assertDataSet("removeOperation.after.csv");
    }

    @Test
    void removeOperationById() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        kampfer.operation().removeOperation(10);
        tester.assertDataSet("removeOperation.after.csv");
    }

    @Test
    void resetDomain() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer1 = getKampfer("test1");
        Kampfer kampfer2 = getKampfer("test2");
        kampfer1.domain().resetDomain();
        kampfer2.domain().resetDomain();
        tester.assertDataSet("domain.reset.after.csv");
    }

    @Test
    void addPermission() {
        tester.insertDataSet("addPermission.before.csv");
        Kampfer kampfer = getKampfer("test");
        kampfer.permission().addPermission("op", "AUTH", "param");
        tester.assertDataSet("addPermission.after.csv");
    }

    @Test
    void addPermissionById() {
        tester.insertDataSet("addPermission.before.csv");
        Kampfer kampfer = getKampfer("test");
        kampfer.permission().addPermission(2, "AUTH", "param");
        tester.assertDataSet("addPermission.after.csv");
    }

    @Test
    void addPermissionGroup() {
        tester.insertDataSet("addPermission.before.csv");
        Kampfer kampfer = getKampfer("test");
        kampfer.permission().addPermissionGroup(2,
                Collections.singletonList(new PermissionModel("AUTH", "param")));
        tester.assertDataSet("addPermission.after.csv");
    }

    @Test
    void addPermissions() {
        tester.insertDataSet("addPermissions.before.csv");
        Kampfer kampfer = getKampfer("test");
        List<OperationPermissionRow> permissions = Stream.of(1, 2, 3, 4, 5)
                .map((Integer operationId) -> new OperationPermissionRow(operationId, 1, "param"))
                .collect(Collectors.toList());
        final List<Long> actual = kampfer.permission().addPermissions(permissions).stream()
                .map(IdModel::getId)
                .collect(Collectors.toList());
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), actual);
        tester.assertDataSet("addPermissions.after.csv");
    }

    @Test
    void addPermissionAuthorities() {
        tester.insertDataSet("addPermissionAuthorities.before.csv");
        Kampfer kampfer = getKampfer("test");
        List<PermissionAuthorityRow> permissionAuthorities = List.of(
                new PermissionAuthorityRow(1, "auth1", "param1", 11),
                new PermissionAuthorityRow(2, "auth2", "param2", 22),
                new PermissionAuthorityRow(3, "auth3", "param3", 33),
                new PermissionAuthorityRow(4, "auth4", "param4", 44),
                new PermissionAuthorityRow(5, "auth5", "param5", 55)
        );
        final List<Long> actual = kampfer.permission().addPermissionAuthorities(permissionAuthorities).stream()
                .map(IdModel::getId)
                .collect(Collectors.toList());
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), actual);
        tester.assertDataSet("addPermissionAuthorities.after.csv");
    }

    @Test
    void removePermission() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        kampfer.permission().removePermission("op_child", "auth5");
        tester.assertDataSet("removePermission.after.csv");
    }

    @Test
    void removePermissionById() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        kampfer.permission().removePermission(10, "auth5");
        tester.assertDataSet("removePermission.after.csv");
    }

    @Test
    void addStaticAuthUser() {
        tester.insertDataSet("domain.single.csv");
        Kampfer kampfer = getKampfer(null);
        kampfer.staticAuthority().addStaticAuthUser("auth", 123);
        tester.assertDataSet("addStaticAuthUser.after.csv");
    }

    @Test
    void removeStaticAuthUser() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer(null);
        kampfer.staticAuthority().removeStaticAuthUser("auth5", 1234);
        tester.assertDataSet("removeStaticAuthUser.after.csv");
    }

    @Test
    void purgeStaticAuthUser() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer(null);
        kampfer.staticAuthority().purgeStaticAuthUser(1234);
        tester.assertDataSet("removeStaticAuthUser.after.csv");
    }

    @Test
    void addStaticDomainAuthUser() {
        tester.insertDataSet("domain.single.csv");
        Kampfer kampfer = getKampfer("test");
        kampfer.staticAuthority().addStaticDomainAuthUser("auth", 123);
        tester.assertDataSet("addStaticDomainAuthUser.after.csv");
    }

    @Test
    void removeStaticDomainAuthUser() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        kampfer.staticAuthority().removeStaticDomainAuthUser("auth5", 12345);
        tester.assertDataSet("removeStaticDomainAuthUser.after.csv");
    }

    @Test
    void purgeStaticDomainAuthUser() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer(null);
        kampfer.staticAuthority().purgeStaticDomainAuthUser(12345);
        tester.assertDataSet("removeStaticDomainAuthUser.after.csv");
    }

    @Override
    Kampfer getKampfer(String domain) {
        return new SimpleKampfer(dataSource, domain, SqlDialect.ORACLE);
    }
}
