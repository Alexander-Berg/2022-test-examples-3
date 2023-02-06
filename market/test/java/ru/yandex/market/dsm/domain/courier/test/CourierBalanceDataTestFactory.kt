package ru.yandex.market.dsm.domain.courier.test

import org.springframework.stereotype.Service
import ru.yandex.market.dsm.domain.courier.balance.command.CourierBalanceDataBaseCommand
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.mj.generated.server.model.CourierBalanceDataUpsertDto
import java.util.UUID

@Service
class CourierBalanceDataTestFactory(
) {

    fun createCommand(): CourierBalanceDataBaseCommand.Create {
        val result = TestUtil.OBJECT_GENERATOR.nextObject(CourierBalanceDataBaseCommand.Create::class.java)
        result.id = UUID.randomUUID().toString()
        return result
    }

    fun upsertDto() = TestUtil.OBJECT_GENERATOR.nextObject(CourierBalanceDataUpsertDto::class.java)

    fun updateBalanceData() =
        TestUtil.OBJECT_GENERATOR.nextObject(CourierBalanceDataBaseCommand.UpdateBalanceData::class.java)
}
