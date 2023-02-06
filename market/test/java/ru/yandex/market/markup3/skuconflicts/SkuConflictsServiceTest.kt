package ru.yandex.market.markup3.skuconflicts

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.mocks.MboMappingsServiceMock
import ru.yandex.market.markup3.skuconflicts.repository.SkuConflictsAssignmentRepository
import ru.yandex.market.markup3.testutils.BaseAppTest

class SkuConflictsServiceTest : BaseAppTest() {

    @Autowired
    lateinit var mboMappingsService: MboMappingsServiceMock

    @Autowired
    lateinit var skuConflictsService: SkuConflictsService

    @Autowired
    lateinit var skuConflictsAssignmentRepository: SkuConflictsAssignmentRepository

    @Before
    fun setup() {
        mboMappingsService.businessSkuKeyToOfferId = mapOf(
            BusinessSkuKey(1, "sku1") to 1,
            BusinessSkuKey(2, "sku2") to 2,
            BusinessSkuKey(3, "sku3") to 3,
        )
    }

    @Test
    fun `sucsessfully accept new conflicts`() {
        val results = skuConflictsService.addConflicts(listOf(
            ConflictParameter(
                businessSkuKey = BusinessSkuKey(1, "sku1"),
                modelId = null,
                conflictCardId = 1,
                mappingSkuId = null,
                categoryId = 1,
                parameterId = 1,
                parameterValue = listOf("test", "test2")
            ),

            ConflictParameter(
                businessSkuKey = BusinessSkuKey(2, "sku2"),
                modelId = 1,
                conflictCardId = 1,
                mappingSkuId = 2,
                categoryId = 1,
                parameterId = 2,
                parameterValue = listOf("test3")
            ),
        ))

        results shouldContainExactly listOf(AddConflictResult.OK, AddConflictResult.OK)

        val assignments = skuConflictsAssignmentRepository.findAll()

        assignments.map { it.conflictCardId } shouldContainExactlyInAnyOrder listOf(1, 1)
        assignments.map { it.parentModelId } shouldContainExactlyInAnyOrder listOf(null, 1)
        assignments.map { it.mappingSkuId } shouldContainExactlyInAnyOrder listOf(null, 2)
        assignments.map { it.offerId } shouldContainExactlyInAnyOrder listOf(1, 2)
        assignments.map { it.parameterId } shouldContainExactlyInAnyOrder listOf(1, 2)
        assignments.map { it.parameterValue } shouldContainExactlyInAnyOrder listOf(listOf("test", "test2"), listOf("test3"))
    }

    @Test
    fun `fail accept not found offers`() {
        val results = skuConflictsService.addConflicts(listOf(
            ConflictParameter(
                businessSkuKey = BusinessSkuKey(1, "sku1"),
                modelId = null,
                conflictCardId = 1,
                mappingSkuId = null,
                categoryId = 1,
                parameterId = 1,
                parameterValue = listOf("test")
            ),

            ConflictParameter(
                businessSkuKey = BusinessSkuKey(1, "invalid"),
                modelId = null,
                conflictCardId = 1,
                mappingSkuId = null,
                categoryId = 1,
                parameterId = 2,
                parameterValue = listOf("test2")
            ),
        ))

        results shouldContainExactly listOf(AddConflictResult.OK, AddConflictResult.OFFER_NOT_FOUND_FAIL)

        val assignments = skuConflictsAssignmentRepository.findAll()

        assignments.map { it.conflictCardId } shouldContainExactlyInAnyOrder listOf(1)
        assignments.map { it.offerId } shouldContainExactlyInAnyOrder listOf(1)
        assignments.map { it.parameterId } shouldContainExactlyInAnyOrder listOf(1)
        assignments.map { it.parameterValue } shouldContainExactlyInAnyOrder listOf(listOf("test"))
    }

    @Test
    fun `don't accept conflict on same parameter`() {
        var results = skuConflictsService.addConflicts(listOf(
            ConflictParameter(
                businessSkuKey = BusinessSkuKey(1, "sku1"),
                modelId = null,
                conflictCardId = 1,
                mappingSkuId = null,
                categoryId = 1,
                parameterId = 1,
                parameterValue = listOf("test")
            ),

            ConflictParameter(
                businessSkuKey = BusinessSkuKey(2, "sku2"),
                modelId = null,
                conflictCardId = 1,
                mappingSkuId = null,
                categoryId = 1,
                parameterId = 1,
                parameterValue = listOf("test2")
            ),
        ))

        results shouldContainExactly listOf(AddConflictResult.OK, AddConflictResult.OK)
        skuConflictsAssignmentRepository.findAll().size shouldBe 1

        results = skuConflictsService.addConflicts(listOf(
            ConflictParameter(
                businessSkuKey = BusinessSkuKey(2, "sku2"),
                modelId = null,
                conflictCardId = 1,
                mappingSkuId = null,
                categoryId = 1,
                parameterId = 1,
                parameterValue = listOf("wtf")
            ),
        ))

        // maybe someday will be DUPLICATE_FAIL
        results shouldContainExactly listOf(AddConflictResult.OK)

        val assignments = skuConflictsAssignmentRepository.findAll()

        assignments.map { it.conflictCardId } shouldContainExactlyInAnyOrder listOf(1)
        assignments.map { it.offerId } shouldContainExactlyInAnyOrder listOf(1)
        assignments.map { it.parameterId } shouldContainExactlyInAnyOrder listOf(1)
        assignments.map { it.parameterValue } shouldContainExactlyInAnyOrder listOf(listOf("test"))
    }

    @Test
    fun `partially accept conflicts with new and same parameters`() {
        var results = skuConflictsService.addConflicts(listOf(
            ConflictParameter(
                businessSkuKey = BusinessSkuKey(1, "sku1"),
                modelId = null,
                conflictCardId = 1,
                mappingSkuId = null,
                categoryId = 1,
                parameterId = 1,
                parameterValue = listOf("test")
            ),
        ))

        results shouldContainExactly listOf(AddConflictResult.OK)
        skuConflictsAssignmentRepository.findAll().size shouldBe 1

        results = skuConflictsService.addConflicts(listOf(
            ConflictParameter(
                businessSkuKey = BusinessSkuKey(2, "sku2"),
                modelId = null,
                conflictCardId = 1,
                mappingSkuId = null,
                categoryId = 1,
                parameterId = 1,
                parameterValue = listOf("wtf")
            ),

            ConflictParameter(
                businessSkuKey = BusinessSkuKey(3, "sku3"),
                modelId = null,
                conflictCardId = 2,
                mappingSkuId = null,
                categoryId = 1,
                parameterId = 1,
                parameterValue = listOf("omg")
            ),
        ))

        // maybe someday will be DUPLICATE_FAIL
        results shouldContainExactly listOf(AddConflictResult.OK, AddConflictResult.OK)

        val assignments = skuConflictsAssignmentRepository.findAll()

        assignments.map { it.conflictCardId } shouldContainExactlyInAnyOrder listOf(1, 2)
        assignments.map { it.offerId } shouldContainExactlyInAnyOrder listOf(1, 3)
        assignments.map { it.parameterId } shouldContainExactlyInAnyOrder listOf(1, 1)
        assignments.map { it.parameterValue } shouldContainExactlyInAnyOrder listOf(listOf("test"), listOf("omg"))
    }
}
