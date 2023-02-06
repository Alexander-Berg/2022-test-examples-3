package ru.yandex.market.contentmapping.services.category.info

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass

internal class DataCampGroupIdServiceImplTest : BaseAppTestClass() {
    @Autowired
    private lateinit var dataCampGroupIdService: DataCampGroupIdService

    @Test
    fun `test consistency`() {
        val shopId1 = 1L
        val shopId2 = 2L
        val groupIdA = null
        val groupIdB = "g"
        val groupNameA = null
        val groupNameB = "n"

        val shop1NAGA = dataCampGroupIdService.getGroupIdIntForGroupId(shopId1, groupNameA, groupIdA)
        val shop1NAGB = dataCampGroupIdService.getGroupIdIntForGroupId(shopId1, groupNameA, groupIdB)
        val shop1NBGA = dataCampGroupIdService.getGroupIdIntForGroupId(shopId1, groupNameB, groupIdA)
        val shop1NBGB = dataCampGroupIdService.getGroupIdIntForGroupId(shopId1, groupNameB, groupIdB)

        val shop2NAGA = dataCampGroupIdService.getGroupIdIntForGroupId(shopId2, groupNameA, groupIdA)
        val shop2NAGB = dataCampGroupIdService.getGroupIdIntForGroupId(shopId2, groupNameA, groupIdB)
        val shop2NBGA = dataCampGroupIdService.getGroupIdIntForGroupId(shopId2, groupNameB, groupIdA)
        val shop2NBGB = dataCampGroupIdService.getGroupIdIntForGroupId(shopId2, groupNameB, groupIdB)

        dataCampGroupIdService.getGroupIdIntForGroupId(shopId1, groupNameA, groupIdA) shouldBe shop1NAGA
        dataCampGroupIdService.getGroupIdIntForGroupId(shopId1, groupNameA, groupIdB) shouldBe shop1NAGB
        dataCampGroupIdService.getGroupIdIntForGroupId(shopId1, groupNameB, groupIdA) shouldBe shop1NBGA
        dataCampGroupIdService.getGroupIdIntForGroupId(shopId1, groupNameB, groupIdB) shouldBe shop1NBGB

        dataCampGroupIdService.getGroupIdIntForGroupId(shopId2, groupNameA, groupIdA) shouldBe shop2NAGA
        dataCampGroupIdService.getGroupIdIntForGroupId(shopId2, groupNameA, groupIdB) shouldBe shop2NAGB
        dataCampGroupIdService.getGroupIdIntForGroupId(shopId2, groupNameB, groupIdA) shouldBe shop2NBGA
        dataCampGroupIdService.getGroupIdIntForGroupId(shopId2, groupNameB, groupIdB) shouldBe shop2NBGB
    }
}
