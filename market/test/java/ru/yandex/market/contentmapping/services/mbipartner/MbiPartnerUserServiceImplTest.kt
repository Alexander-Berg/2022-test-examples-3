package ru.yandex.market.contentmapping.services.mbipartner

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

class MbiPartnerUserServiceImplTest : BaseAppTestClass() {
    @Autowired
    private lateinit var mbiPartnerUserService: MbiPartnerUserServiceImpl

    @Test
    fun testParseCampaignsResponse() {
        val response = loadResource("MbiPartnerUserServiceImplTest/mbi-partner-campaigns-success.json")
        val content = BufferedReader(InputStreamReader(response, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"))
        val shops: List<Shop> = mbiPartnerUserService.parseCampaignsResponse(content)
        shops shouldHaveSize 5

        shops[0].asClue {
            val shop: Shop = it
            shop.id shouldBe 10308948
            shop.businessId shouldBe 10535223
            shop.name shouldBe "supplier-gcontent1 (FBY)"
            shop.config shouldBe null
        }

        shops[1].asClue {
            val shop: Shop = it
            shop.id shouldBe 10405856
            shop.businessId shouldBe 10558089
            shop.name shouldBe "Тест alexblinov (FBS)"
            shop.config shouldBe null
        }

        shops[4].asClue {
            val shop: Shop = it
            shop.id shouldBe 10804767
            shop.businessId shouldBe 10804768
            shop.name shouldBe "Для таксономий 3 (FBY)"
            shop.config shouldBe null
        }
    }

    @Test
    fun testParseCampaignsEmptyResponse() {
        val response = loadResource("MbiPartnerUserServiceImplTest/mbi-partner-campaigns-empty.json")
        val content = BufferedReader(InputStreamReader(response, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"))
        val shops: List<Shop> = mbiPartnerUserService.parseCampaignsResponse(content)
        shops.size shouldBe 0
    }

    @Test
    fun testParseBusinessesResponse() {
        val response = loadResource("MbiPartnerUserServiceImplTest/mbi-partner-businesses-success.json")
        val content = BufferedReader(InputStreamReader(response, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"))
        val business: MbiPartnerUserService.MbiBusiness? = mbiPartnerUserService.parseBusinessesResponse(content)
        business shouldNotBe null
        business?.businessId shouldBe 10696615
        business?.name shouldBe "trice.org"
    }
}
