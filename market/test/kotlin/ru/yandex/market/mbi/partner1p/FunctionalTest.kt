package ru.yandex.market.mbi.partner1p

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.io.InputStreamResource
import org.springframework.test.context.ActiveProfiles
import ru.yandex.market.mbi.partner1p.config.MbiPartner1PFunctionalTestConfig
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * @author lozovskii@yandex-team.ru
 */
@SpringBootTest(
    properties = ["spring.main.allow-bean-definition-overriding=true"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [MbiPartner1PFunctionalTestConfig::class]
)
@ActiveProfiles("functionalTest")
open class FunctionalTest {

    @LocalServerPort
    protected var serverPort = 0

    protected open fun baseUrl(): String {
        return "http://localhost:$serverPort"
    }

    protected fun getInputStreamResource(filename: String): InputStreamResource {
        return InputStreamResource(
            javaClass.getResourceAsStream(
                javaClass.simpleName + filename
            )
        )
    }

    protected fun getStringResource(name: String): String? {
        val cls: Class<out FunctionalTest?> = javaClass
        return readFromFile(cls, cls.simpleName + name)
    }

    private fun readFromFile(contextClass: Class<*>, jsonFileName: String): String? {
        try {
            contextClass.getResourceAsStream(jsonFileName).use { `in` ->
                val resource = IOUtils.toString(`in`, StandardCharsets.UTF_8.name())
                return StringUtils.trimToNull(resource)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
