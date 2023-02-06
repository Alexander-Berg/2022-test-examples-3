package ru.yandex.market.replenishment.autoorder.service

import org.apache.ibatis.session.SqlSession
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mboc.http.MboCategoryOfferChanges
import ru.yandex.market.mboc.http.MboCategoryOfferChanges.FindChangesResponse
import ru.yandex.market.mboc.http.MboCategoryOfferChanges.SimpleBaseOffer
import ru.yandex.market.mboc.http.MboCategoryOfferChangesService
import ru.yandex.market.mboc.http.SupplierOffer.SupplierType
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SskuInfoFastIncrementalLoader

class SskuInfoFastIncrementalLoaderTest : FunctionalTest() {

    @Autowired
    private lateinit var batchSqlSession: SqlSession

    @Autowired
    private lateinit var environmentService: EnvironmentService

    private lateinit var offerChangesService: MboCategoryOfferChangesService
    private lateinit var loader: SskuInfoFastIncrementalLoader

    @Before
    fun setUp() {
        offerChangesService = Mockito.mock(MboCategoryOfferChangesService::class.java)
        loader = SskuInfoFastIncrementalLoader(
            offerChangesService,
            batchSqlSession,
            environmentService
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["SskuInfoFastIncrementalLoaderTest_load.before.csv"],
        after = ["SskuInfoFastIncrementalLoaderTest_load.after.csv"]
    )
    fun load() {
        val responseBuilderOne = FindChangesResponse.newBuilder()
            .setLastModifiedSeqId(2)
            .addOffers(getSimpleBaseOffer("004444.004001", 123, 465852, SupplierType.TYPE_FIRST_PARTY))
            .addOffers(getSimpleBaseOffer("002222.00was-before", 1111, 465852, SupplierType.TYPE_REAL_SUPPLIER))
            .addOffers(getSimpleBaseOffer("004444.not-mapped", 0, 465852, SupplierType.TYPE_FIRST_PARTY))
            .build()
        val responseBuilderTwo = FindChangesResponse.newBuilder()
            .setLastModifiedSeqId(4)
            .addOffers(getSimpleBaseOffer("004444.004001", 456, 465852, SupplierType.TYPE_FIRST_PARTY))
            .addOffers(getSimpleBaseOffer("004242.004242", 42, 465852, SupplierType.TYPE_REAL_SUPPLIER))
            .addOffers(getSimpleBaseOffer("112233", 234, 10000, SupplierType.TYPE_THIRD_PARTY))
            .build()
        val responseBuilderThree = FindChangesResponse.newBuilder()
            .setLastModifiedSeqId(4)
            .build()
        Mockito.`when`(offerChangesService.findChanges(ArgumentMatchers.any()))
            .thenReturn(responseBuilderOne)
            .thenReturn(responseBuilderTwo)
            .thenReturn(responseBuilderThree)

        loader.load()
        Mockito.verify(offerChangesService, Mockito.times(3)).findChanges(ArgumentMatchers.any())
    }
}

private fun getSimpleBaseOffer(
    ssku: String,
    msku: Long,
    supplierId: Int,
    supplierType: SupplierType
) = SimpleBaseOffer.newBuilder()
    .setApprovedMappingMskuId(msku)
    .setShopSku(ssku)
    .addServiceOffers(
        MboCategoryOfferChanges.SimpleServiceOffer.newBuilder()
            .setSupplierId(supplierId)
            .setSupplierType(supplierType)
            .build()
    )
    .build()
