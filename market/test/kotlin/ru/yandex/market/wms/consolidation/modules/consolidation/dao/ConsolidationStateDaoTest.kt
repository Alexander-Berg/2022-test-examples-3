package ru.yandex.market.wms.consolidation.modules.consolidation.dao

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.consolidation.modules.consolidation.dao.entity.ConsolidationState
import ru.yandex.market.wms.consolidation.modules.consolidation.dao.entity.UitTask

class ConsolidationStateDaoTest : IntegrationTest() {

    @Autowired
    private lateinit var consolidationStateDao: ConsolidationStateDao

    @Test
    fun createAndModify() {
        val state = ConsolidationState(
            "user123", "S01",
            UitTask(
                "UIT123456789",
                "CART10",
                "ORD0001",
                "00001",
                listOf("S01-01", "S01-02"),
                listOf("S01-03", "S01-04")
            )
        )

        consolidationStateDao.insert(state)
        assertThat(consolidationStateDao.getByUser(state.userKey)).isEqualTo(state)

        val update = state.copy(state = ConsolidationState.State())
        consolidationStateDao.update(update)
        assertThat(consolidationStateDao.getByUser(state.userKey)).isEqualTo(update)

        consolidationStateDao.deleteByUser(state.userKey)
        assertThat(consolidationStateDao.getByUser(state.userKey)).isNull()
    }
}
