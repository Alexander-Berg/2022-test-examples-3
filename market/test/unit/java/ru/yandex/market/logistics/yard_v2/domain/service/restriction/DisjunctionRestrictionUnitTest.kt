package ru.yandex.market.logistics.yard_v2.domain.service.restriction

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionParamType
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionType
import ru.yandex.market.logistics.yard_v2.domain.entity.EntityParam
import ru.yandex.market.logistics.yard_v2.domain.entity.RestrictionEntity

class DisjunctionRestrictionUnitTest: SoftAssertionSupport() {

    private var restrictionProvider: RestrictionProvider? = null
    private var disjunctionRestriction: DisjunctionRestriction? = null

    @BeforeEach
    fun init() {
        restrictionProvider = mock(RestrictionProvider::class.java)
        disjunctionRestriction = DisjunctionRestriction(restrictionProvider!!)
    }

    @Test
    fun checkOnlyFirstRestrictionIfItIsApplicable() {
        val restrictionEntity = createRestrictionEntity()
        val firstRestriction: Restriction = createRestrictionWithExpectedResult(true, restrictionEntity)
        val secondRestriction: Restriction = createRestrictionWithExpectedResult(false, restrictionEntity)
        `when`(restrictionProvider!!.provide(RestrictionType.EVENT_REQUIRED)).thenReturn(firstRestriction)
        `when`(restrictionProvider!!.provide(RestrictionType.ARRIVAL_TIME_NEAR_SLOT)).thenReturn(secondRestriction)
        val actual = disjunctionRestriction!!.isApplicable(1, restrictionEntity)
        softly.assertThat(actual).isTrue
        verify(firstRestriction).isApplicable(1, restrictionEntity)
        verify(secondRestriction, never()).isApplicable(1, restrictionEntity)
    }

    @Test
    fun fistsRestrictionNotApplicableAndSecondRestrictionIsApplicable() {
        val restrictionEntity = createRestrictionEntity()
        val firstRestriction: Restriction = createRestrictionWithExpectedResult(false, restrictionEntity)
        val secondRestriction: Restriction = createRestrictionWithExpectedResult(true, restrictionEntity)
        `when`(restrictionProvider!!.provide(RestrictionType.EVENT_REQUIRED)).thenReturn(firstRestriction)
        `when`(restrictionProvider!!.provide(RestrictionType.ARRIVAL_TIME_NEAR_SLOT)).thenReturn(secondRestriction)
        val actual = disjunctionRestriction!!.isApplicable(1, restrictionEntity)
        softly.assertThat(actual).isTrue
        verify(firstRestriction).isApplicable(1, restrictionEntity)
        verify(secondRestriction).isApplicable(1, restrictionEntity)
    }

    @Test
    fun allRestrictionsNotApplicable() {
        val restrictionEntity = createRestrictionEntity()
        val firstRestriction: Restriction = createRestrictionWithExpectedResult(false, restrictionEntity)
        val secondRestriction: Restriction = createRestrictionWithExpectedResult(false, restrictionEntity)
        `when`(restrictionProvider!!.provide(RestrictionType.EVENT_REQUIRED)).thenReturn(firstRestriction)
        `when`(restrictionProvider!!.provide(RestrictionType.ARRIVAL_TIME_NEAR_SLOT)).thenReturn(secondRestriction)
        val actual = disjunctionRestriction!!.isApplicable(1, restrictionEntity)
        softly.assertThat(actual).isFalse
        verify(firstRestriction).isApplicable(1, restrictionEntity)
        verify(secondRestriction).isApplicable(1, restrictionEntity)
    }

    private fun createRestrictionEntity(): RestrictionEntity {
        return RestrictionEntity(
            1,
            null,
            RestrictionType.DISJUNCTION,
            listOf(EntityParam(RestrictionParamType.RESTRICTION_TYPES.name, "EVENT_REQUIRED, ARRIVAL_TIME_NEAR_SLOT"))
        )
    }

    private fun createRestrictionWithExpectedResult(result: Boolean,
                                                    restrictionEntity: RestrictionEntity): Restriction {
        val restriction: Restriction = mock(Restriction::class.java)
        `when`(restriction.isApplicable(1, restrictionEntity)).thenReturn(result)
        return restriction
    }
}
