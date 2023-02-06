package ru.yandex.direct.ess.router.rules.bsexport.bids

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbschema.ppc.enums.BidsBaseBidType
import ru.yandex.direct.ess.logicobjects.bsexport.bids.BidObjectType
import ru.yandex.direct.ess.logicobjects.bsexport.bids.BsExportBidsObject
import ru.yandex.direct.ess.router.rules.bsexport.bids.BsExportBiddableShowConditionsRule.BID_TYPE_BY_BIDS_BASE_TYPE
import ru.yandex.direct.ess.router.rules.bsexport.bids.BsExportBiddableShowConditionsRule.mapBidsBaseChangeObject
import ru.yandex.direct.ess.router.utils.ProceededChange
import ru.yandex.direct.test.utils.randomPositiveLong

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BsExportBiddableShowConditionsRuleTest {

    @Mock
    private lateinit var proceededChange: ProceededChange

    @BeforeEach
    fun initTestData() {
        doReturn(randomPositiveLong())
            .`when`(proceededChange)
            .getBeforeOrAfter<Long, Long>(eq(Tables.BIDS_BASE.CID))
        doReturn(randomPositiveLong())
            .`when`(proceededChange)
            .getBeforeOrAfter<Long, Long>(eq(Tables.BIDS_BASE.PID))
        doReturn(randomPositiveLong())
            .`when`(proceededChange)
            .getPrimaryKey<Long, Long>(eq(Tables.BIDS_BASE.BID_ID))
    }

    @Test
    fun `check getExpectedBsExportBidsObject for allowed bids_base_types`() {
        BID_TYPE_BY_BIDS_BASE_TYPE.forEach { (bidBaseType, bidType) ->
            doReturn(bidBaseType)
                .`when`(proceededChange)
                .getBeforeOrAfter<String, BidsBaseBidType>(eq(Tables.BIDS_BASE.BID_TYPE))

            val expected = getExpectedBsExportBidsObject(bidType)
            assertEquals(expected, mapBidsBaseChangeObject(proceededChange))
        }
    }

    @Test
    fun `check getExpectedBsExportBidsObject for NOT allowed bids_base_types`() {
        BidsBaseBidType.values()
            .filter { !BID_TYPE_BY_BIDS_BASE_TYPE.containsKey(it.literal) }
            .forEach { bidType ->
                doReturn(bidType.literal)
                    .`when`(proceededChange)
                    .getBeforeOrAfter<String, BidsBaseBidType>(eq(Tables.BIDS_BASE.BID_TYPE))

                val expected = getExpectedBsExportBidsObject(null)
                assertEquals(expected, mapBidsBaseChangeObject(proceededChange))
            }
    }

    private fun getExpectedBsExportBidsObject(bidObjectType: BidObjectType?): BsExportBidsObject {
        return BsExportBiddableShowConditionsRule.createBidObject(
            proceededChange,
            Tables.BIDS_BASE.CID,
            Tables.BIDS_BASE.PID,
            Tables.BIDS_BASE.BID_ID,
            bidObjectType
        )
    }
}
