package ru.yandex.market.mdm.lib.model.mdm

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType.MDM_ATTR
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType.MDM_BOOL
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType.MDM_ENTITY_TYPE
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType.MDM_ENUM_OPTION

class MdmPathTest {
    @Test
    fun `test convert to short string and back`() {
        // given
        val initialPath = MdmPath(
            listOf(
                MdmPathSegment(
                    mdmId = 1,
                    mdmMetaType = MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = 2,
                    mdmMetaType = MDM_ATTR
                ),
                MdmPathSegment(
                    mdmId = 3,
                    mdmMetaType = MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = 4,
                    mdmMetaType = MDM_ATTR
                ),
                MdmPathSegment(
                    mdmId = 5,
                    mdmMetaType = MDM_ENUM_OPTION
                )
            )
        )

        // when
        val shortString = initialPath.toString()
        val pathFromShortString = MdmPath.fromShortString(shortString)

        //then
        pathFromShortString shouldBe initialPath
    }

    @Test
    fun `should create path from longs for attribute`() {
        // given
        val longPath = listOf(1L, 2L, 3L, 4L)

        // when
        val path = MdmPath.fromLongs(longPath, MDM_ATTR)

        // then
        path shouldBe MdmPath(
            listOf(
                MdmPathSegment(
                    mdmId = 1,
                    mdmMetaType = MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = 2,
                    mdmMetaType = MDM_ATTR
                ),
                MdmPathSegment(
                    mdmId = 3,
                    mdmMetaType = MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = 4,
                    mdmMetaType = MDM_ATTR
                ),
            )
        )
    }

    @Test
    fun `should create path from longs for bool`() {
        // given
        val longPath = listOf(1L, 2L)

        // when
        val path = MdmPath.fromLongs(longPath, MDM_BOOL)

        // then
        path shouldBe MdmPath(
            listOf(
                MdmPathSegment(
                    mdmId = 1,
                    mdmMetaType = MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = 2,
                    mdmMetaType = MDM_ATTR
                ),
                MdmPathSegment(
                    mdmId = 0,
                    mdmMetaType = MDM_BOOL
                ),
            )
        )
    }

    @Test
    fun `should create path from longs for enum option`() {
        // given
        val longPath = listOf(1L, 2L, 3L)

        // when
        val path = MdmPath.fromLongs(longPath, MDM_ENUM_OPTION)

        // then
        path shouldBe MdmPath(
            listOf(
                MdmPathSegment(
                    mdmId = 1,
                    mdmMetaType = MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = 2,
                    mdmMetaType = MDM_ATTR
                ),
                MdmPathSegment(
                    mdmId = 3,
                    mdmMetaType = MDM_ENUM_OPTION
                ),
            )
        )
    }

    @Test
    fun `should create path from attributes`() {
        // given
        val attribute1 = mdmAttribute()
        val attribute2 = mdmAttribute()
        val attributes = listOf(attribute1, attribute2)

        // when
        val path = MdmPath.fromAttributes(attributes)

        // then
        path shouldBe MdmPath(
            listOf(
                MdmPathSegment(
                    mdmId = attribute1.mdmEntityTypeId,
                    mdmMetaType = MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = attribute1.mdmId,
                    mdmMetaType = MDM_ATTR
                ),
                MdmPathSegment(
                    mdmId = attribute2.mdmEntityTypeId,
                    mdmMetaType = MDM_ENTITY_TYPE
                ),
                MdmPathSegment(
                    mdmId = attribute2.mdmId,
                    mdmMetaType = MDM_ATTR
                ),
            )
        )
    }
}
