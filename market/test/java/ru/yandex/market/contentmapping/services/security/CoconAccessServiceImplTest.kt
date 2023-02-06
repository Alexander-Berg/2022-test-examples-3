package ru.yandex.market.contentmapping.services.security

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

class CoconAccessServiceImplTest : BaseAppTestClass() {
    @Autowired
    private lateinit var coconAccessService: CoconAccessServiceImpl

    @Test
    fun testIsInUnitedCatalog() {
        val responseJson = loadResource("CoconAccessServiceImplTest/cocon-response-in-united-catalog.json")
        val content = BufferedReader(InputStreamReader(responseJson, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"))
        val pageResponse = coconAccessService.parseCoconResponse(content)
        pageResponse shouldNotBe null
        coconAccessService.isInUnitedCatalog(pageResponse!!) shouldBe true
    }

    @Test
    fun testIsNotInUnitedCatalog() {
        val responseJson = loadResource("CoconAccessServiceImplTest/cocon-response-not-in-united-catalog.json")
        val content = BufferedReader(InputStreamReader(responseJson, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"))
        val pageResponse = coconAccessService.parseCoconResponse(content)
        pageResponse shouldNotBe null
        coconAccessService.isInUnitedCatalog(pageResponse!!) shouldBe false
    }

    @Test
    fun testHasNoWriteAccess() {
        val responseJson = loadResource("CoconAccessServiceImplTest/cocon-response-no-role.json")
        val content = BufferedReader(InputStreamReader(responseJson, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"))
        val pageResponse = coconAccessService.parseCoconResponse(content)
        pageResponse shouldNotBe null
        coconAccessService.hasWriteAccess(pageResponse!!) shouldBe false
    }

    @Test
    fun testHasWriteAccess() {
        val responseJson = loadResource("CoconAccessServiceImplTest/cocon-response-not-in-united-catalog.json")
        val content = BufferedReader(InputStreamReader(responseJson, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"))
        val pageResponse = coconAccessService.parseCoconResponse(content)
        pageResponse shouldNotBe null
        coconAccessService.hasWriteAccess(pageResponse!!) shouldBe true
    }
}
