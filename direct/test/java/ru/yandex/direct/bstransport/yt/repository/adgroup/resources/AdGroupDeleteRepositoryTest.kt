package ru.yandex.direct.bstransport.yt.repository.adgroup.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.adgroup.AdGroup

class AdGroupDeleteRepositoryTest : AbstractAdGroupYtRepositoryTest(AdGroupDeleteYtRepository::class) {
    @Test
    fun getSchemaWithMappingTest() {
        val resource = AdGroup.newBuilder()
            .setAdGroupId(12L)
            .setOrderId(456L)
            .setIterId(1643L)
            .setUpdateTime(123434242L)
            .setDeleteTime(123434242L)
            .build()

        val expectedColumnNameToValue = mapOf<String, Any?>(
            "AdGroupID" to 12L,
            "OrderID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1643L,
            "DeleteTime" to 123434242L,
        )
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }
}
