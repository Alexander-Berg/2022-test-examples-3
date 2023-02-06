package ru.yandex.market.mbo.cms.core.permission

import org.junit.Test
import ru.yandex.market.mbo.cms.core.dao.permission.EntityTypeDao
import ru.yandex.market.mbo.cms.core.dao.permission.EntityWithoutPermissionInheritanceDao
import ru.yandex.market.mbo.cms.core.json.service.exceptions.JsonPageGenericException
import ru.yandex.market.mbo.cms.core.models.permission.ActionType
import ru.yandex.market.mbo.cms.core.models.permission.EntityType
import ru.yandex.market.mbo.cms.core.permission.mock.EntityTypeDaoMock
import ru.yandex.market.mbo.cms.core.permission.mock.EntityWithoutPermissionInheritanceDaoMock
import ru.yandex.market.mbo.cms.core.permission.mock.PermissionServiceMock
import ru.yandex.market.mbo.cms.core.permission.mock.RoleServiceMock
import ru.yandex.market.mbo.cms.core.permission.mock.TvmServiceMock
import ru.yandex.market.mbo.cms.core.service.permission.PermissionService
import ru.yandex.market.mbo.cms.core.service.permission.PermissionsCheckService
import ru.yandex.market.mbo.cms.core.service.permission.PermissionsCheckServiceImpl
import ru.yandex.market.mbo.cms.core.service.permission.RoleService
import ru.yandex.market.mbo.cms.core.service.tvm.TvmService

class PermissionsCheckTest {
    private val permissionService: PermissionService = PermissionServiceMock()
    private val roleService: RoleService = RoleServiceMock()
    private val entityTypeDao: EntityTypeDao = EntityTypeDaoMock()
    private val entityWithoutPermissionInheritanceDao:
            EntityWithoutPermissionInheritanceDao = EntityWithoutPermissionInheritanceDaoMock()
    private val tvmService: TvmService = TvmServiceMock()

    private val permissionsCheckService: PermissionsCheckService = PermissionsCheckServiceImpl(
        permissionService,
        roleService,
        entityTypeDao,
        entityWithoutPermissionInheritanceDao,
        tvmService
    )

    @Test
    fun checkUserRightsSuccessDefaultCasesTest() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "0",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.VIEW
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "1",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.VIEW
        )
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "1",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.EDIT
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "2",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.VIEW
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "2",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.EDIT
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "1",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.EDIT
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "0",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.VIEW
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "1",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.EDIT
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "1",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.EDIT
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "2",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.PUBLISH
        )
    }

    @Test(expected = JsonPageGenericException::class)
    fun checkUserRightsExceptionDefaultCasesTest2() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "1",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.PUBLISH
        )
    }

    @Test(expected = JsonPageGenericException::class)
    fun checkUserRightsExceptionDefaultCasesTest3() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "0",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.EDIT
        )
    }

    @Test(expected = JsonPageGenericException::class)
    fun checkUserRightsExceptionDefaultCasesTest4() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "1",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.PUBLISH
        )
    }

    @Test(expected = JsonPageGenericException::class)
    fun checkUserRightsExceptionDefaultCasesTest5() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "0",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.EDIT
        )
    }

    @Test(expected = JsonPageGenericException::class)
    fun checkUserRightsExceptionDefaultCasesTest7() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.SCHEMA,
            mapOf(
                EntityType.SCHEMA to "1",
                EntityType.NAMESPACE_SCHEMA to "n1"
            ),
            ActionType.PUBLISH
        )
    }

    @Test
    fun checkUserRightsSuccessInheritanceTest() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.DOCUMENT,
            mapOf(
                EntityType.DOCUMENT to "0",
                EntityType.DOCUMENT_TYPE to "1",
                EntityType.NAMESPACE_CONTENT to "n1"
            ),
            ActionType.EDIT
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.DOCUMENT,
            mapOf(
                EntityType.DOCUMENT to "0",
                EntityType.DOCUMENT_TYPE to "0",
                EntityType.NAMESPACE_CONTENT to "n1"
            ),
            ActionType.VIEW
        )
    }

    @Test(expected = JsonPageGenericException::class)
    fun checkUserRightsExceptionInheritanceTest1() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.DOCUMENT,
            mapOf(
                EntityType.DOCUMENT to "0",
                EntityType.DOCUMENT_TYPE to "1",
                EntityType.NAMESPACE_CONTENT to "n1"
            ),
            ActionType.PUBLISH
        )
    }

    @Test(expected = JsonPageGenericException::class)
    fun checkUserRightsExceptionInheritanceTest2() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.DOCUMENT,
            mapOf(
                EntityType.DOCUMENT to "0",
                EntityType.DOCUMENT_TYPE to "0",
                EntityType.NAMESPACE_CONTENT to "n1"
            ),
            ActionType.EDIT
        )
    }

    @Test
    fun checkUserRightsSuccessEntityWithoutInheritanceTest() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.DOCUMENT,
            mapOf(
                EntityType.DOCUMENT to "12",
                EntityType.DOCUMENT_TYPE to "dt1",
                EntityType.NAMESPACE_CONTENT to "n1"
            ),
            ActionType.VIEW
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.DOCUMENT,
            mapOf(
                EntityType.DOCUMENT to "13",
                EntityType.DOCUMENT_TYPE to "dt1",
                EntityType.NAMESPACE_CONTENT to "n1"
            ),
            ActionType.EDIT
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.DOCUMENT,
            mapOf(
                EntityType.DOCUMENT to "14",
                EntityType.DOCUMENT_TYPE to "dt1",
                EntityType.NAMESPACE_CONTENT to "n1"
            ),
            ActionType.EDIT
        )
    }

    @Test(expected = JsonPageGenericException::class)
    fun checkUserRightsExceptionEntityWithoutInheritanceTest1() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.DOCUMENT,
            mapOf(
                EntityType.DOCUMENT to "13",
                EntityType.DOCUMENT_TYPE to "dt1",
                EntityType.NAMESPACE_CONTENT to "n1"
            ),
            ActionType.PUBLISH
        )
    }

    @Test(expected = JsonPageGenericException::class)
    fun checkUserRightsExceptionEntityWithoutInheritanceTest2() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.DOCUMENT,
            mapOf(
                EntityType.DOCUMENT to "12",
                EntityType.DOCUMENT_TYPE to "dt1",
                EntityType.NAMESPACE_CONTENT to "n1"
            ),
            ActionType.EDIT
        )
    }

    @Test
    fun checkUserRightsSuccessAllowAllInheritanceTest() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.NAMESPACE_CONTENT,
            mapOf(
                EntityType.NAMESPACE_CONTENT to "0",
            ),
            ActionType.VIEW
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.NAMESPACE_CONTENT,
            mapOf(
                EntityType.NAMESPACE_CONTENT to "1",
            ),
            ActionType.EDIT
        )

        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.NAMESPACE_CONTENT,
            mapOf(
                EntityType.NAMESPACE_CONTENT to "2",
            ),
            ActionType.PUBLISH
        )
    }

    @Test(expected = JsonPageGenericException::class)
    fun checkUserRightsExceptionAllowAllInheritanceTest1() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.NAMESPACE_CONTENT,
            mapOf(
                EntityType.NAMESPACE_CONTENT to "0",
            ),
            ActionType.EDIT
        )

    }

    @Test(expected = JsonPageGenericException::class)
    fun checkUserRightsExceptionAllowAllInheritanceTest3() {
        permissionsCheckService.checkUserRights(
            "abc",
            1,
            EntityType.NAMESPACE_CONTENT,
            mapOf(
                EntityType.NAMESPACE_CONTENT to "1",
            ),
            ActionType.PUBLISH
        )
    }
}
