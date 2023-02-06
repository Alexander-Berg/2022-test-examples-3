package ru.yandex.market.contentmapping.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.repository.ShopRepository
import ru.yandex.market.contentmapping.services.security.audit.AuditContext
import ru.yandex.market.contentmapping.testutils.BaseWebTestClass
import ru.yandex.market.mbo.pgaudit.PgAuditChangeType
import ru.yandex.market.mbo.pgaudit.PgAuditRepository

class AuditConfigTest : BaseWebTestClass() {
    @Autowired
    lateinit var shopRepository: ShopRepository

    @Autowired
    lateinit var auditRepository: PgAuditRepository

    @Autowired
    lateinit var auditContext: AuditContext

    @Before
    fun prepare() {
        auditRepository.clearAll()
    }

    @Test
    fun `it writes audit and uses context for it`() {
        assertThat(auditRepository.findAll()).isEmpty()

        var shop = Shop(0, "First shop")
        shopRepository.insert(shop)

        val records1 = auditRepository.findAll()
        assertThat(records1).hasSize(1)
        records1[0].let {
            assertThat(it.context).matches(":\\w+:")
            assertThat(it.changeType).isEqualTo(PgAuditChangeType.INSERT)
            assertThat(it.entityKey).isEqualTo("${shop.id}")
            assertThat(it.entityType).isEqualTo("shop")
            assertThat(it.userLogin).isNull() // We're out of request here
        }

        shop = shop.copy(name = "The First Shop ever")
        val context = "add-some-context"
        auditContext.setContext(context)

        shopRepository.update(shop)
        val records2 = auditRepository.findAll()
        assertThat(records2).hasSize(2)

        records2[0].let { // returns in reverse order by default
            assertThat(it.context).matches(":\\w+:${context}")
            assertThat(it.changeType).isEqualTo(PgAuditChangeType.UPDATE)
            assertThat(it.changes).isEqualTo(
                    mapOf("name" to listOf("First shop", "The First Shop ever")))
        }
    }

    @Test
    @Ignore("Now there is other problem, with some coverter configuration...")
    fun `test web request`() {
        mockMvc.perform(post("/api/shop?shopId=123&name=NewShop").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)

        val records = auditRepository.findAll()
        assertThat(records).hasSize(1)
        records[0].let {
            assertThat(it.context).matches(":\\w+:dummy")
            assertThat(it.changeType).isEqualTo(PgAuditChangeType.INSERT)
            assertThat(it.entityKey).isEqualTo("123")
            assertThat(it.entityType).isEqualTo("shop")
        }
    }
}
