package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest
import ru.yandex.market.logistics.cte.client.enums.MatrixType
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeValueType
import ru.yandex.market.logistics.cte.repo.CanonicalCategoryRepository
import ru.yandex.market.logistics.cte.repo.QualityGroupRepository
import ru.yandex.market.logistics.cte.repo.QualityMatrixGroupAttrInclusionEntityRepository

class QualityGroupControllerTest(
    @Autowired private val categoryRepository: CanonicalCategoryRepository,
    @Autowired private val groupRepository: QualityGroupRepository,
    @Autowired
    private val qualityMatrixGroupAttrInclusionEntityRepository: QualityMatrixGroupAttrInclusionEntityRepository
) : MvcIntegrationTest() {


    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/quality-group/group-with-categories/quality_groups.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml")
    )
    fun listGroupsWithCategories() {

        val params = LinkedMultiValueMap<String, String>().apply {
            add("sort", "groupId")
            add("offset", "0")
            add("limit", "10")
        }

        testGetEndpoint(
            "/quality_group/with-categories/list",
            params,
            "controller/quality-group/group-with-categories/response.json",
            HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/quality-group/group-with-categories/quality_groups.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml")
    )
    fun listGroupsWithCategoriesExcel() {

        val params = LinkedMultiValueMap<String, String>().apply {
            add("sort", "groupId")
        }

        testGetEndpointWithExcelFile(
            "/quality_group/with-categories/list/excel",
            params,
            HttpStatus.OK,
            "category_table.xlsx"
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/quality-group/group-with-categories/quality_groups.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml")
    )
    fun listGroupsWithCategoriesWithFilter() {

        val params = LinkedMultiValueMap<String, String>().apply {
            add("sort", "groupId")
            add("offset", "0")
            add("limit", "10")
            add("filter", "(groupName=='default')")
        }

        testGetEndpoint(
            "/quality_group/with-categories/list",
            params,
            "controller/quality-group/group-with-categories-and-filter/response.json",
            HttpStatus.OK)
    }

    @Test
    fun createQualityGroup() {
        val groupId = 14L
        val groupName = "TestGroup"

        testEndpointPostStatus(
            "/quality_group/create",
            "controller/quality-group/create-group/request.json",
            HttpStatus.OK
        )

        val groupEntity = groupRepository.findById(groupId)

        Assert.assertEquals(groupName, groupEntity.get().groupName)
    }

    @Test
    @DatabaseSetup("classpath:controller/quality-group/create-group/before.xml")
    fun createQualityGroupFail() {
        testEndpointPost(
            "/quality_group/create",
            "controller/quality-group/create-group-fail/request.json",
            "controller/quality-group/create-group-fail/response.json",
            HttpStatus.BAD_REQUEST
        )

    }

    @Test
    @DatabaseSetup("classpath:controller/quality-group/create-category/quality_groups.xml")
    fun createCategory() {
        val requestBuilder = MockMvcRequestBuilders.request(
            HttpMethod.POST,
            "/quality_group/create-category")
            .content(readFromFile("controller/quality-group/create-category/request.json"))
            .contentType(MediaType.APPLICATION_JSON)
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))

        val categoryEntity = categoryRepository.findById(1).get()

        Assert.assertEquals(categoryEntity.name, "first category")
        Assert.assertEquals(categoryEntity.groupId, 1L)
    }

    @Test
    fun createCategoryWithEmptyGroup() {

        val requestBuilder = MockMvcRequestBuilders.request(
            HttpMethod.POST,
            "/quality_group/create-category")
            .content(readFromFile("controller/quality-group/create-category-empty-group/request.json"))
            .contentType(MediaType.APPLICATION_JSON)
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))

        val categoryEntity = categoryRepository.findById(1).get()

        Assert.assertEquals(categoryEntity.name, "first category")
        Assert.assertEquals(categoryEntity.groupId, null)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/quality-group/create-category-empty-group/quality_groups.xml"),
        DatabaseSetup("classpath:repository/canonical_category.xml")
    )
    fun createCategoryFail() {
        val requestBuilder = MockMvcRequestBuilders.request(
            HttpMethod.POST,
            "/quality_group/create-category")
            .content(readFromFile("controller/quality-group/create-category-empty-group/request.json"))
            .contentType(MediaType.APPLICATION_JSON)
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.CONFLICT.value()))
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/quality-group/group-with-categories/quality_groups.xml")
    )
    fun listGroups() {
        testGetEndpoint(
            "/quality_group/list",
            LinkedMultiValueMap(),
            "controller/quality-group/group-list/response.json",
            HttpStatus.OK)
    }

    @Deprecated("Delete after MatrixType had become required")
    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
    )
    fun createMatrixAddNewAttribute() {
        testEndpointPutStatus(
            "/quality_group/update",
            "controller/quality-group/create-matrix/add-attribute/request.json",
            HttpStatus.OK)

        val attrInclusions = qualityMatrixGroupAttrInclusionEntityRepository.findAll()
        Assert.assertEquals(1, attrInclusions.size)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
    )
    fun createMatrixAddNewAttributeWithMatrixType() {
        testEndpointPutStatus(
            "/quality_group/update",
            "controller/quality-group/create-matrix/add-attribute/requestV2.json",
            HttpStatus.OK)

        val attrInclusions = qualityMatrixGroupAttrInclusionEntityRepository.findAll()
        Assert.assertEquals(1, attrInclusions.filter { it.matrix?.matrixType == MatrixType.FULFILLMENT }.size)
        Assert.assertEquals(QualityAttributeValueType.COMPENSATION,
            attrInclusions.filter { it.matrix?.matrixType == MatrixType.FULFILLMENT }[0].assessment)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:controller/quality-group/create-matrix/update-attribute-value/qmatrix_group.xml"),
        DatabaseSetup(
            "classpath:controller/quality-group/create-matrix/update-attribute-value/qmatrix_attribute_inclusion.xml"),
    )
    fun createMatrixAfterChangeAttributeValue() {
        testEndpointPutStatus(
            "/quality_group/update",
            "controller/quality-group/create-matrix/update-attribute-value/request.json",
            HttpStatus.OK)

        val attrInclusions = qualityMatrixGroupAttrInclusionEntityRepository.findAll()

        val changedAttr = attrInclusions.filter { (it.matrix?.id == 1L) && (it.attribute?.id == 4L) }.first()

        Assert.assertEquals(4, attrInclusions.size)
        Assert.assertEquals(QualityAttributeValueType.COMPENSATION, changedAttr.assessment)
    }
}
