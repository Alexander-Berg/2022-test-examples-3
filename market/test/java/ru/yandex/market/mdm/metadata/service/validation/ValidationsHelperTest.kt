package ru.yandex.market.mdm.metadata.service.validation

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.lib.model.mdm.MdmPathSegment

class ValidationsHelperTest {
    @Test
    fun `checkMdmPathNotEmpty should return error on null path`() {
        // given
        val notExistingPath: MdmPath? = null

        //when
        val notExistingPathCheckResult = ValidationsHelper.checkMdmPathNotEmpty(notExistingPath)

        // then
        notExistingPathCheckResult!!.code shouldBe ValidationsHelper.EMPTY_PATH_CODE
    }

    @Test
    fun `checkMdmPathNotEmpty should return error on empty path`() {
        // given
        val emptyPath = MdmPath()

        //when
        val emptyPathCheckResult = ValidationsHelper.checkMdmPathNotEmpty(emptyPath)

        // then
        emptyPathCheckResult!!.code shouldBe ValidationsHelper.EMPTY_PATH_CODE
    }

    @Test
    fun `checkMdmPathNotEmpty should return no error on not empty path`() {
        // given
        val notEmptyPath = MdmPath(
            listOf(
                MdmPathSegment(
                    mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE,
                    mdmId = 123
                )
            )
        )

        //when
        val notEmptyPathCheckResult = ValidationsHelper.checkMdmPathNotEmpty(notEmptyPath)

        // then
        notEmptyPathCheckResult shouldBe null
    }

    @Test
    fun `checkMdmPathNotContainDuplicateIds should return error when path have duplicate ids`() {
        // given
        val pathWithDuplicates = MdmPath(
            listOf(
                MdmPathSegment(
                    mdmId = 18,
                    mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = 321,
                    mdmMetaType = MdmMetaType.MDM_ATTR
                ),
                MdmPathSegment(
                    mdmId = 19,
                    mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = 18,
                    mdmMetaType = MdmMetaType.MDM_ATTR
                ),
            )
        )

        //when
        val pathWithDuplicatesCheckResult = ValidationsHelper.checkMdmPathNotContainDuplicateIds(pathWithDuplicates)

        // then
        pathWithDuplicatesCheckResult!!.code shouldBe ValidationsHelper.CIRCLED_PATH_CODE
    }

    @Test
    fun `checkMdmPathNotContainDuplicateIds should return no error when path is null`() {
        // given
        val notExistingPath: MdmPath? = null

        //when
        val notExistingPathCheckResult = ValidationsHelper.checkMdmPathNotContainDuplicateIds(notExistingPath)

        // then
        notExistingPathCheckResult shouldBe null
    }

    @Test
    fun `checkFullPathHaveCorrectOrder should return no error when path is null`() {
        // given
        val notExistingPath: MdmPath? = null

        //when
        val notExistingPathCheckResult = ValidationsHelper.checkFullPathHaveCorrectOrder(notExistingPath)

        // then
        notExistingPathCheckResult shouldBe null
    }

    @Test
    fun `checkFullPathHaveCorrectOrder should return error when path contains undefined segment`() {
        // given
        val pathWithUndefinedSegment = MdmPath(
            listOf(
                MdmPathSegment(
                    mdmId = 123,
                    mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = 456
                )
            )
        )

        //when
        val pathWithUndefinedSegmentCheckResult =
            ValidationsHelper.checkFullPathHaveCorrectOrder(pathWithUndefinedSegment)

        // then
        pathWithUndefinedSegmentCheckResult!!.code shouldBe ValidationsHelper.INVALID_PATH_ORDER
    }

    @Test
    fun `checkFullPathHaveCorrectOrder should return error when path without of alternation of attr and entity`() {
        // given
        val pathWithoutAlternation = MdmPath(
            listOf(
                MdmPathSegment(
                    mdmId = 123,
                    mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = 456,
                    mdmMetaType = MdmMetaType.MDM_ATTR
                ),
                MdmPathSegment(
                    mdmId = 457,
                    mdmMetaType = MdmMetaType.MDM_ATTR
                )
            )
        )

        //when
        val pathWithoutAlternationCheckResult = ValidationsHelper.checkFullPathHaveCorrectOrder(pathWithoutAlternation)

        // then
        pathWithoutAlternationCheckResult!!.code shouldBe ValidationsHelper.INVALID_PATH_ORDER
    }

    @Test
    fun `checkFullPathHaveCorrectOrder should return no error when given path is correct path to bool`() {
        // given
        val correctPathToBool = MdmPath(
            listOf(
                MdmPathSegment(
                    mdmId = 123,
                    mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = 456,
                    mdmMetaType = MdmMetaType.MDM_ATTR
                ),
                MdmPathSegment(
                    mdmId = 457,
                    mdmMetaType = MdmMetaType.MDM_BOOL
                )
            )
        )

        //when
        val correctPathToBoolCheckResult = ValidationsHelper.checkFullPathHaveCorrectOrder(correctPathToBool)

        // then
        correctPathToBoolCheckResult shouldBe null
    }
}
