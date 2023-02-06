package ru.yandex.market.markup3.users.profile.service

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.users.profile.CategoryGroupUserRoleRow
import ru.yandex.market.markup3.users.profile.CategoryRoleRow
import ru.yandex.market.markup3.users.profile.repository.CategoryGroupUserRolesRepository
import ru.yandex.market.markup3.users.profile.repository.CategoryGroupsRepository
import ru.yandex.market.markup3.users.profile.repository.CategoryRoleRepository
import ru.yandex.market.markup3.users.profile.service.GroupRolesService.Companion.GROUP_PROJECTS
import ru.yandex.market.mboc.http.MboCategory

/**
 * @author shadoff
 * created on 03.07.2022
 */
class GroupRolesServiceTest : CommonTaskTest()  {

    @Autowired
    lateinit var groupRolesService : GroupRolesService

    @Autowired
    lateinit var categoryGroupsRepository : CategoryGroupsRepository

    @Autowired
    lateinit var categoryRoleRepository: CategoryRoleRepository

    @Autowired
    lateinit var categoryGroupUserRolesRepository: CategoryGroupUserRolesRepository

    @Autowired
    lateinit var mboCategoryService: MboCategoryServiceMock

    @Before
    fun setUp() {
        mboCategoryService.groupsResponse = mboCategoryService.defaultCategoryGroupsResponse()
    }

    @Test
    fun `test category groups are imported successfully`() {
        groupRolesService.update()

        val allGroups = categoryGroupsRepository.findAll().associateBy { it.groupId }
        allGroups.keys shouldContainExactlyInAnyOrder listOf(1L, 2L, 3L)

        allGroups[1L]!!.categoryIds shouldContainExactlyInAnyOrder listOf(11L, 12L)
        allGroups[2L]!!.categoryIds shouldContainExactlyInAnyOrder listOf(21L, 22L, 23L)
        allGroups[3L]!!.categoryIds shouldContainExactlyInAnyOrder listOf(31L)
    }

    @Test
    fun `test add group user roles`() {
        categoryRoleRepository.insertBatch(listOf(
            CategoryRoleRow(uid = 1L, categoryId = 11L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS)
        ))

        groupRolesService.update()

        val groupRoles = categoryGroupUserRolesRepository.findAll().associateBy { it.uid }
        groupRoles.keys shouldContainExactlyInAnyOrder listOf(1L)
        groupRoles[1L]!!.categoryGroupIds shouldContainExactly listOf(1L)
    }

    @Test
    fun `test remove group user roles`() {
        categoryGroupUserRolesRepository.insert(
            CategoryGroupUserRoleRow(uid = 1L, projectType = GROUP_PROJECTS.first(), categoryGroupIds = listOf(1L)))

        groupRolesService.update()

        val groupRoles = categoryGroupUserRolesRepository.findAll().associateBy { it.uid }
        groupRoles.keys shouldHaveSize 0
    }

    @Test
    fun `test add group user a lot of roles`() {
        categoryRoleRepository.insertBatch(listOf(
            CategoryRoleRow(uid = 1L, categoryId = 11L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS),
            CategoryRoleRow(uid = 1L, categoryId = 12L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS),
            CategoryRoleRow(uid = 1L, categoryId = 22L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS),
            CategoryRoleRow(uid = 1L, categoryId = 31L, mboRole = "SUPER", projectTypes = GROUP_PROJECTS),
            CategoryRoleRow(uid = 2L, categoryId = 31L, mboRole = "SUPER", projectTypes = GROUP_PROJECTS),
        ))

        groupRolesService.update()

        val groupRoles = categoryGroupUserRolesRepository.findAll().associateBy { it.uid }
        groupRoles.keys shouldContainExactlyInAnyOrder listOf(1L, 2L)
        groupRoles[1L]!!.categoryGroupIds shouldContainExactly listOf(1L, 2L, 3L)
        groupRoles[2L]!!.categoryGroupIds shouldContainExactly listOf(3L)
    }

    @Test
    fun `test update group user roles`() {
        categoryRoleRepository.insertBatch(listOf(
            CategoryRoleRow(uid = 1L, categoryId = 11L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS),
            CategoryRoleRow(uid = 1L, categoryId = 12L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS),
            CategoryRoleRow(uid = 1L, categoryId = 22L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS),
        ))

        groupRolesService.update()

        val groupRoles = categoryGroupUserRolesRepository.findAll().associateBy { it.uid }
        groupRoles.keys shouldContainExactlyInAnyOrder listOf(1L)
        groupRoles[1L]!!.categoryGroupIds shouldContainExactly listOf(1L, 2L)

        categoryRoleRepository.deleteAll()
        categoryRoleRepository.insertBatch(listOf(
            CategoryRoleRow(uid = 1L, categoryId = 11L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS),
            CategoryRoleRow(uid = 1L, categoryId = 12L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS),
        ))

        groupRolesService.update()

        val groupRolesUpdated = categoryGroupUserRolesRepository.findAll().associateBy { it.uid }
        groupRolesUpdated.keys shouldContainExactlyInAnyOrder listOf(1L)
        groupRolesUpdated[1L]!!.categoryGroupIds shouldContainExactly listOf(1L)
    }

    @Test
    fun `test update group user roles category groups changed`() {
        categoryRoleRepository.insertBatch(listOf(
            CategoryRoleRow(uid = 1L, categoryId = 11L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS),
            CategoryRoleRow(uid = 1L, categoryId = 12L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS),
            CategoryRoleRow(uid = 1L, categoryId = 22L, mboRole = "OPERATOR", projectTypes = GROUP_PROJECTS),
        ))

        groupRolesService.update()

        var groupRoles = categoryGroupUserRolesRepository.findAll().associateBy { it.uid }
        groupRoles.keys shouldContainExactlyInAnyOrder listOf(1L)
        groupRoles[1L]!!.categoryGroupIds shouldContainExactly listOf(1L, 2L)


        mboCategoryService.groupsResponse = MboCategory.GetCategoryGroupsResponse.newBuilder()
            .addCategoryGroups(
                MboCategory.GetCategoryGroupsResponse.CategoryGroup.newBuilder()
                .addCategories(11L)
                .addCategories(12L)
                .setId(1L)
            )
            .build()

        groupRolesService.update()

        groupRoles = categoryGroupUserRolesRepository.findAll().associateBy { it.uid }
        groupRoles.keys shouldContainExactlyInAnyOrder listOf(1L)
        groupRoles[1L]!!.categoryGroupIds shouldContainExactly listOf(1L)
    }

    @Test
    fun `test update group user roles category groups remove project`() {
        categoryGroupUserRolesRepository.insert(
            CategoryGroupUserRoleRow(uid = 1L, projectType = "qwer", categoryGroupIds = listOf(1L)))

        groupRolesService.update()

        val groupRoles = categoryGroupUserRolesRepository.findAll().associateBy { it.uid }
        groupRoles.keys shouldHaveSize 0
    }
}
