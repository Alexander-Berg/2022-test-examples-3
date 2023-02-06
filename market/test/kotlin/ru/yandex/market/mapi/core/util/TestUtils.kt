package ru.yandex.market.mapi.core.util

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.json.JSONException
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.slf4j.LoggerFactory
import ru.yandex.market.mapi.core.AbstractNonSpringTest
import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.MapiContextRw
import ru.yandex.market.mapi.core.MapiEnvironment
import ru.yandex.market.mapi.core.UserExpInfo
import ru.yandex.market.mapi.core.UserPassportInfo
import ru.yandex.market.mapi.core.UserPlusInfo
import ru.yandex.market.mapi.core.mapiContext
import ru.yandex.market.request.trace.RequestContextHolder
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.servlet.http.HttpServletRequest
import kotlin.io.path.exists

private val log = LoggerFactory.getLogger(AbstractNonSpringTest::class.java)

private val mapiPath = "market/mapi"
private val modules = listOf("mapi", "clients", "screen-api", "client-fapi-mocks")

fun String.asResource(): String {
    return JsonHelperTest::class.java.getResource(this)?.readText(Charsets.UTF_8)
        ?: throw RuntimeException("Can't find resource: $this")
}

fun mockMapiContext(initFun: (MapiContextRw) -> Unit) :MapiContext {
    val attrMap: MutableMap<String, Any?> = HashMap()

    val mockRequest = mock<HttpServletRequest>()
    whenever(mockRequest.getAttribute(any())).then { attrMap[it.getArgument(0)] }
    whenever(mockRequest.setAttribute(any(), any())).then {
        val key: String = it.getArgument(0)
        val value: Any = it.getArgument(1)
        attrMap[key] = value
        null
    }

    RequestContextHolder.createContext("req_id")
    val mockedContext = MapiContextRw(MapiEnvironment.JUNIT)

    // just in case - mock timer
    mockedContext.crTime = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli()
    initFun(mockedContext)
    MapiContext.set(mockedContext)
    return mockedContext
}

fun mockFlags(vararg flags: String) {
    getMockContextRw()?.flags = flags.toSet()
}

fun mockRearrs(vararg rearrs: String) {
    val rearrsSet = LinkedHashSet(rearrs.toList())
    getMockContextRw()?.userExpInfo = UserExpInfo("test", rearrsSet)
}

fun mockOauth(oauth: String) {
    getMockContextRw()?.oauth = oauth
}

fun mockNoOauth() {
    getMockContextRw()?.oauth = null
}

fun mockApp(appPlatform: String?, version: String?) {
    val mockContext = getMockContextRw()
    mockContext?.appPlatform = appPlatform
    mockContext?.appVersionRaw = version
}

fun mockRegion(region: Long) {
    getMockContextRw()?.regionId = region
}

fun mockUuid(value: String) {
    getMockContextRw()?.uuid = value
}

fun mockUserInfo(user: UserPassportInfo) {
    getMockContextRw()?.oauthInfo = user
}

fun mockPlusInfo(plus: UserPlusInfo) {
    getMockContextRw()?.userPlusInfo = plus
}

fun mockExpInfo(info: UserExpInfo) {
    getMockContextRw()?.userExpInfo = info
}

fun mockQueryParams(value: Map<String, Any>) {
    getMockContextRw()?.queryParams = value
}

fun mockTimezone(tz: String) {
    getMockContextRw()?.timezoneRW = ZoneOffset.of(tz)
}

fun getMockContextRw(): MapiContextRw? {
    return mapiContext() as? MapiContextRw
}

fun buildMockUser(isYandexoid: Boolean = false): UserPassportInfo {
    return UserPassportInfo(
        true, null,
        login = "mock-login",
        111,
        "user_ticket_value",
        isYandexoid
    )
}

fun buildMockPlusInfo(): UserPlusInfo {
    return UserPlusInfo(
        hasPlus = true,
        plusInfo = UserPlusInfo.PlusInfo(
            balance = BigDecimal.valueOf(123123),
            marketCashbackPercent = BigDecimal.valueOf(0.23),
            isCashbackAllowed = true,
        )
    )
}

/**
 * Checks json.
 * Compares object's json representation with file contents (or maybe raw result).
 *
 * Can override file contents when -DoverrideJson=true is set + env=ARCADIA_ROOT is also set.
 */
fun assertJson(
    result: Any,
    expected: String,
    name: String = "Result json",
    сompareMode: JSONCompareMode = JSONCompareMode.NON_EXTENSIBLE,
    isExpectedInFile: Boolean = true
) {
    val resultJson = if (result is String) result else JsonHelper.toString(result)
    log.info("\n\n$name: $resultJson\n")

    val expectedJson = if (isExpectedInFile) {
        expected.asResource()
    } else {
        expected
    }

    try {
        JSONAssert.assertEquals(
            expectedJson,
            resultJson,
            сompareMode
        )
    } catch (cause: Throwable) {
        if (cause !is AssertionError && cause !is JSONException) {
            throw cause
        }

        val arcadiaRoot = System.getenv("ARCADIA_ROOT")
        if (isExpectedInFile && System.getProperty("overrideJson") == "true" && !arcadiaRoot.isNullOrBlank()) {
            // update expected files (and files only) - or throw cause if can't
            writeJson(result, expected, arcadiaRoot) ?: throw cause
        } else {
            throw cause
        }
    }
}

private fun writeJson(result: Any, expectedPath: String, arcadiaRoot: String): Path? {
    // find target file to update (it's hard to predict module, so look in all of them)
    val path = modules.map { module ->
        Paths.get(arcadiaRoot, mapiPath, module, "src/test/resources", expectedPath.substring(1))
    }
        .firstOrNull { path -> path.exists() }
        ?: return null

    // ensure object to write. String input is always a preformatted json
    val jsonObject = if (result is String) {
        JsonHelper.parseTree(result)
    } else {
        result
    }

    val mapper = ObjectMapper().registerKotlinModule()

    // indenter with 2 spaces as indent
    val indenter = DefaultIndenter("  ", DefaultIndenter.SYS_LF)
    val printer = DefaultPrettyPrinter()
    printer.indentObjectsWith(indenter)
    printer.indentArraysWith(indenter)

    // expected path always starts with / -> remove first symbol

    Files.newOutputStream(path).use { stream ->
        mapper.writer(printer).writeValue(stream, jsonObject)
    }

    return path
}
