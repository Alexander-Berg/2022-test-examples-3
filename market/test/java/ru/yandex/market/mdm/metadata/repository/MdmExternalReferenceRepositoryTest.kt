package ru.yandex.market.mdm.metadata.repository

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeExternalReferenceDetails
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeExternalReferenceTypeHint
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalReference
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalSystem
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.lib.model.mdm.MdmPathSegment
import ru.yandex.market.mdm.metadata.filters.MdmExternalReferenceSearchFilter
import ru.yandex.market.mdm.metadata.filters.MdmPathFilter
import ru.yandex.market.mdm.metadata.filters.MdmPathMatchingType
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass

class MdmExternalReferenceRepositoryTest : BaseAppTestClass() {
    @Autowired
    lateinit var mdmExternalReferenceRepository: MdmExternalReferenceRepository

    @Before
    fun setUp() {
        mdmExternalReferenceRepository.insertBatch(
            listOf(
                EXTERNAL_REFERENCE1,
                EXTERNAL_REFERENCE2,
                EXTERNAL_REFERENCE3,
                EXTERNAL_REFERENCE4,
                EXTERNAL_REFERENCE5,
                EXTERNAL_REFERENCE6,
                EXTERNAL_REFERENCE7
            )
        )
    }

    @Test
    fun `test search by exact path`() {
        // given
        val mdmExternalReferenceSearchFilter = MdmExternalReferenceSearchFilter(
            mdmPathFilters = listOf(
                MdmPathFilter(
                    mdmPath = MdmPath(listOf(ENTITY1, ATTRIBUTE3, ENTITY4, ATTRIBUTE8, OPTION9)),
                    mdmPathMatchingType = MdmPathMatchingType.EXACT
                )
            )
        )

        // when
        val result = mdmExternalReferenceRepository.findBySearchFilter(mdmExternalReferenceSearchFilter)

        // then
        result shouldContainExactlyInAnyOrder listOf(EXTERNAL_REFERENCE1, EXTERNAL_REFERENCE6)
    }

    @Test
    fun `test search by prefix`() {
        // given
        val mdmExternalReferenceSearchFilter = MdmExternalReferenceSearchFilter(
            mdmPathFilters = listOf(
                MdmPathFilter(
                    mdmPath = MdmPath(listOf(ENTITY1)),
                    mdmPathMatchingType = MdmPathMatchingType.PREFIX
                )
            )
        )

        // when
        val result = mdmExternalReferenceRepository.findBySearchFilter(mdmExternalReferenceSearchFilter)

        // then
        result shouldContainExactlyInAnyOrder
            listOf(EXTERNAL_REFERENCE1, EXTERNAL_REFERENCE2, EXTERNAL_REFERENCE3, EXTERNAL_REFERENCE6)
    }

    @Test
    fun `test search by postfix`() {
        // given
        val mdmExternalReferenceSearchFilter = MdmExternalReferenceSearchFilter(
            mdmPathFilters = listOf(
                MdmPathFilter(
                    mdmPath = MdmPath(listOf(OPTION9)),
                    mdmPathMatchingType = MdmPathMatchingType.POSTFIX
                )
            )
        )

        // when
        val result = mdmExternalReferenceRepository.findBySearchFilter(mdmExternalReferenceSearchFilter)

        // then
        result shouldContainExactlyInAnyOrder listOf(EXTERNAL_REFERENCE1, EXTERNAL_REFERENCE6)
    }

    @Test
    fun `test search by sub path`() {
        // given
        val mdmExternalReferenceSearchFilter = MdmExternalReferenceSearchFilter(
            mdmPathFilters = listOf(
                MdmPathFilter(
                    mdmPath = MdmPath(listOf(ATTRIBUTE3, ENTITY4)),
                    mdmPathMatchingType = MdmPathMatchingType.SUB_PATH
                )
            )
        )

        // when
        val result = mdmExternalReferenceRepository.findBySearchFilter(mdmExternalReferenceSearchFilter)

        // then
        result shouldContainExactlyInAnyOrder listOf(EXTERNAL_REFERENCE1, EXTERNAL_REFERENCE2, EXTERNAL_REFERENCE6)
    }

    @Test
    fun `when path in reversed order should return nothing`() {
        // given
        val mdmExternalReferenceSearchFilter = MdmExternalReferenceSearchFilter(
            mdmPathFilters = listOf(
                MdmPathFilter(
                    mdmPath = MdmPath(listOf(ENTITY4, ATTRIBUTE3)),
                    mdmPathMatchingType = MdmPathMatchingType.SUB_PATH
                )
            )
        )

        // when
        val result = mdmExternalReferenceRepository.findBySearchFilter(mdmExternalReferenceSearchFilter)

        // then
        result shouldHaveSize 0
    }

    @Test
    fun `should return all paths when search by empty prefix`() {
        // given
        val emptyPrefix = MdmExternalReferenceSearchFilter(
            mdmPathFilters = listOf(
                MdmPathFilter(
                    mdmPath = MdmPath(),
                    mdmPathMatchingType = MdmPathMatchingType.PREFIX
                )
            )
        )

        // when
        val result = mdmExternalReferenceRepository.findBySearchFilter(emptyPrefix)

        // then
        listOf(
            EXTERNAL_REFERENCE1,
            EXTERNAL_REFERENCE2,
            EXTERNAL_REFERENCE3,
            EXTERNAL_REFERENCE4,
            EXTERNAL_REFERENCE5,
            EXTERNAL_REFERENCE6,
            EXTERNAL_REFERENCE7
        ).forEach { result shouldContain it }
    }

    @Test
    fun `when search by several path filters should return union`() {
        // given
        val filter = MdmExternalReferenceSearchFilter(
            mdmPathFilters = listOf(
                MdmPathFilter(
                    mdmPath = MdmPath(listOf(ENTITY2, ATTRIBUTE7)),
                    mdmPathMatchingType = MdmPathMatchingType.PREFIX
                ),
                MdmPathFilter(
                    mdmPath = MdmPath(listOf(OPTION9)),
                    mdmPathMatchingType = MdmPathMatchingType.POSTFIX
                )
            )
        )

        // when
        val result = mdmExternalReferenceRepository.findBySearchFilter(filter)

        // then
        result shouldContainExactlyInAnyOrder listOf(
            EXTERNAL_REFERENCE7,
            EXTERNAL_REFERENCE4,
            EXTERNAL_REFERENCE1,
            EXTERNAL_REFERENCE6,
        )
    }

    @Test
    fun `when search by path and id should return intersection`() {
        // given
        val filter = MdmExternalReferenceSearchFilter(
            mdmPathFilters = listOf(
                MdmPathFilter(
                    mdmPath = MdmPath(listOf(ENTITY1)),
                    mdmPathMatchingType = MdmPathMatchingType.PREFIX
                )
            ),
            mdmIds = listOf(EXTERNAL_REFERENCE1.mdmId, EXTERNAL_REFERENCE7.mdmId)
        )

        // when
        val result = mdmExternalReferenceRepository.findBySearchFilter(filter)

        // then
        result shouldContainExactly listOf(EXTERNAL_REFERENCE1)
    }

    companion object {
        private val ENTITY1 = MdmPathSegment(mdmId = 1, mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE)
        private val ENTITY2 = MdmPathSegment(mdmId = 2, mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE)
        private val ATTRIBUTE3 = MdmPathSegment(mdmId = 3, mdmMetaType = MdmMetaType.MDM_ATTR)
        private val ENTITY4 = MdmPathSegment(mdmId = 4, mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE)
        private val ATTRIBUTE5 = MdmPathSegment(mdmId = 5, mdmMetaType = MdmMetaType.MDM_ATTR)
        private val ATTRIBUTE6 = MdmPathSegment(mdmId = 6, mdmMetaType = MdmMetaType.MDM_ATTR)
        private val ATTRIBUTE7 = MdmPathSegment(mdmId = 7, mdmMetaType = MdmMetaType.MDM_ATTR)
        private val ATTRIBUTE8 = MdmPathSegment(mdmId = 8, mdmMetaType = MdmMetaType.MDM_ATTR)
        private val OPTION9 = MdmPathSegment(mdmId = 9, mdmMetaType = MdmMetaType.MDM_ATTR)
        private val BOOL10 = MdmPathSegment(mdmId = 10, mdmMetaType = MdmMetaType.MDM_BOOL)

        private val EXTERNAL_REFERENCE1 = MdmExternalReference(
            mdmPath = MdmPath(listOf(ENTITY1, ATTRIBUTE3, ENTITY4, ATTRIBUTE8, OPTION9)),
            externalSystem = MdmExternalSystem.OLD_MDM,
            externalId = 10001,
        )
        private val EXTERNAL_REFERENCE2 = MdmExternalReference(
            mdmPath = MdmPath(listOf(ENTITY1, ATTRIBUTE3, ENTITY4, ATTRIBUTE8)),
            externalSystem = MdmExternalSystem.OLD_MDM,
            externalId = 10002,
            mdmAttributeExternalReferenceDetails = MdmAttributeExternalReferenceDetails(
                externalName = "Liberté",
                auxExternalData = "Égalité".toByteArray(),
                externalRuTitle = "Fraternité",
                externalTypeHint = MdmAttributeExternalReferenceTypeHint.MBO_STRING
            )
        )
        private val EXTERNAL_REFERENCE3 = MdmExternalReference(
            mdmPath = MdmPath(listOf(ENTITY1, ATTRIBUTE5)),
            externalSystem = MdmExternalSystem.OLD_MDM,
            externalId = 10003
        )
        private val EXTERNAL_REFERENCE4 = MdmExternalReference(
            mdmPath = MdmPath(listOf(ENTITY2, ATTRIBUTE7)),
            externalSystem = MdmExternalSystem.OLD_MDM,
            externalId = 10004
        )
        private val EXTERNAL_REFERENCE5 = MdmExternalReference(
            mdmPath = MdmPath(listOf(ENTITY2, ATTRIBUTE6)),
            externalSystem = MdmExternalSystem.OLD_MDM,
            externalId = 10005
        )
        private val EXTERNAL_REFERENCE6 = MdmExternalReference(
            mdmPath = MdmPath(listOf(ENTITY1, ATTRIBUTE3, ENTITY4, ATTRIBUTE8, OPTION9)),
            externalSystem = MdmExternalSystem.MBO,
            externalId = 10006
        )
        private val EXTERNAL_REFERENCE7 = MdmExternalReference(
            mdmPath = MdmPath(listOf(ENTITY2, ATTRIBUTE7, BOOL10)),
            externalSystem = MdmExternalSystem.MBO,
            externalId = 10007
        )
    }
}
