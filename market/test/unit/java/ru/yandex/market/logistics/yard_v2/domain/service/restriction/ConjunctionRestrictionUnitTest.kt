package ru.yandex.market.logistics.yard_v2.domain.service.restriction

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionParamType
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionType
import ru.yandex.market.logistics.yard_v2.domain.entity.EntityParam
import ru.yandex.market.logistics.yard_v2.domain.entity.RestrictionEntity

class ConjunctionRestrictionUnitTest: SoftAssertionSupport() {

    private var restrictionProvider: RestrictionProvider? = null
    private var conjunctionRestriction: ConjunctionRestriction? = null

    @BeforeEach
    fun init() {
        restrictionProvider = Mockito.mock(RestrictionProvider::class.java)
        conjunctionRestriction = ConjunctionRestriction(restrictionProvider!!)
    }

    @Test
    fun checkOnlyFirstRestrictionIfItIsNotApplicable() {
        val restrictionEntity = createRestrictionEntity()
        val firstRestriction: Restriction = createRestrictionWithExpectedResult(false, restrictionEntity)
        val secondRestriction: Restriction = createRestrictionWithExpectedResult(true, restrictionEntity)
        Mockito.`when`(restrictionProvider!!.provide(RestrictionType.EVENT_REQUIRED)).thenReturn(firstRestriction)
        Mockito.`when`(restrictionProvider!!.provide(RestrictionType.ARRIVAL_TIME_NEAR_SLOT)).thenReturn(secondRestriction)
        val actual = conjunctionRestriction!!.isApplicable(1, restrictionEntity)
        softly.assertThat(actual).isFalse
        Mockito.verify(firstRestriction).isApplicable(1, restrictionEntity)
        Mockito.verify(secondRestriction, Mockito.never()).isApplicable(1, restrictionEntity)
    }

    @Test
    fun fistsRestrictionApplicableAndSecondRestrictionIsNotApplicable() {
        val restrictionEntity = createRestrictionEntity()
        val firstRestriction: Restriction = createRestrictionWithExpectedResult(true, restrictionEntity)
        val secondRestriction: Restriction = createRestrictionWithExpectedResult(false, restrictionEntity)
        Mockito.`when`(restrictionProvider!!.provide(RestrictionType.EVENT_REQUIRED)).thenReturn(firstRestriction)
        Mockito.`when`(restrictionProvider!!.provide(RestrictionType.ARRIVAL_TIME_NEAR_SLOT)).thenReturn(secondRestriction)
        val actual = conjunctionRestriction!!.isApplicable(1, restrictionEntity)
        softly.assertThat(actual).isFalse
        Mockito.verify(firstRestriction).isApplicable(1, restrictionEntity)
        Mockito.verify(secondRestriction).isApplicable(1, restrictionEntity)
    }

    @Test
    fun allRestrictionsApplicable() {
        val restrictionEntity = createRestrictionEntity()
        val firstRestriction: Restriction = createRestrictionWithExpectedResult(true, restrictionEntity)
        val secondRestriction: Restriction = createRestrictionWithExpectedResult(true, restrictionEntity)
        Mockito.`when`(restrictionProvider!!.provide(RestrictionType.EVENT_REQUIRED)).thenReturn(firstRestriction)
        Mockito.`when`(restrictionProvider!!.provide(RestrictionType.ARRIVAL_TIME_NEAR_SLOT)).thenReturn(secondRestriction)
        val actual = conjunctionRestriction!!.isApplicable(1, restrictionEntity)
        softly.assertThat(actual).isTrue
        Mockito.verify(firstRestriction).isApplicable(1, restrictionEntity)
        Mockito.verify(secondRestriction).isApplicable(1, restrictionEntity)
    }

    private fun createRestrictionEntity(): RestrictionEntity {
        return RestrictionEntity(
            1,
            null,
            RestrictionType.CONJUNCTION,
            listOf(EntityParam(RestrictionParamType.CONJUNCTION_RESTRICTION_TYPES.name, "EVENT_REQUIRED, ARRIVAL_TIME_NEAR_SLOT"))
        )
    }

    private fun createRestrictionWithExpectedResult(result: Boolean,
                                                    restrictionEntity: RestrictionEntity
    ): Restriction {
        val restriction: Restriction = Mockito.mock(Restriction::class.java)
        Mockito.`when`(restriction.isApplicable(1, restrictionEntity)).thenReturn(result)
        return restriction
    }
}
