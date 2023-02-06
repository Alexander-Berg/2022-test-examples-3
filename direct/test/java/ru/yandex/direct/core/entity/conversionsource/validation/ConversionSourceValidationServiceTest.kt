package ru.yandex.direct.core.entity.conversionsource.validation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.direct.core.entity.conversionsource.model.ConversionAction
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSource
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSourceSettings
import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceTypeCode
import ru.yandex.direct.core.testing.data.defaultConversionSourceLink
import ru.yandex.direct.core.testing.data.defaultConversionSourceMetrika
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CollectionDefects
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.CommonDefects.invalidValue
import ru.yandex.direct.validation.defect.StringDefects.notEmptyString
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectInfo
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult
import ru.yandex.direct.validation.util.D

private val CLIENT_ID = ClientId.fromLong(23222L)
private const val COUNTER_ID = 64646L
private val VALID_LINK_SETTINGS =
    ConversionSourceSettings.Link("https://valid.ru")
private val VALID_FTP_SETTINGS =
    ConversionSourceSettings.Ftp("host.ru", 22, true, "login", "pwd", "file.txt")
private val VALID_SFTP_SETTINGS =
    ConversionSourceSettings.SFtp("host.ru", 22, "login", "pwd", "file.txt")
private val VALID_GOOGLE_SHEETS_SETTINGS =
    ConversionSourceSettings.GoogleSheets("https://docs.google.com/spreadsheets/some_page")

class ConversionSourceValidationServiceTest {
    private val validationService = ConversionSourceValidationService()

    companion object {
        @JvmStatic
        fun testValidConversionSettingsProvider(): Array<Array<*>> {
            return arrayOf(
                arrayOf("VALID_LINK_SETTINGS", VALID_LINK_SETTINGS),
                arrayOf("VALID_FTP_SETTINGS", VALID_FTP_SETTINGS),
                arrayOf("VALID_SFTP_SETTINGS", VALID_SFTP_SETTINGS),
                arrayOf("VALID_GOOGLE_SHEETS_SETTINGS", VALID_GOOGLE_SHEETS_SETTINGS)
            )
        }

        @JvmStatic
        fun testInvalidConversionSettingsProvider(): Array<Array<*>> {
            return arrayOf(
                arrayOf(
                    "INVALID_LINK_SETTINGS",
                    VALID_LINK_SETTINGS.copy(url = "invalid_url"),
                    invalidValue(),
                    path(field("settings"), field("url"))
                ),
                arrayOf(
                    "INVALID_FTP_SETTINGS",
                    VALID_FTP_SETTINGS.copy(host = ""),
                    notEmptyString(),
                    path(field("settings"), field("host"))
                ),
                arrayOf(
                    "INVALID_FTP_SETTINGS",
                    VALID_FTP_SETTINGS.copy(path = ""),
                    notEmptyString(),
                    path(field("settings"), field("path"))
                ),
                arrayOf(
                    "INVALID_FTP_SETTINGS",
                    VALID_FTP_SETTINGS.copy(encryptedPassword = ""),
                    notEmptyString(),
                    path(field("settings"), field("encryptedPassword"))
                ),
                arrayOf(
                    "INVALID_FTP_SETTINGS",
                    VALID_FTP_SETTINGS.copy(port = -1),
                    invalidValue(),
                    path(field("settings"), field("port"))
                ),
                arrayOf(
                    "INVALID_FTP_SETTINGS",
                    VALID_FTP_SETTINGS.copy(login = ""),
                    notEmptyString(),
                    path(field("settings"), field("login"))
                ),
                arrayOf(
                    "INVALID_SFTP_SETTINGS",
                    VALID_SFTP_SETTINGS.copy(host = ""),
                    notEmptyString(),
                    path(field("settings"), field("host"))
                ),
                arrayOf(
                    "INVALID_SFTP_SETTINGS",
                    VALID_SFTP_SETTINGS.copy(path = ""),
                    notEmptyString(),
                    path(field("settings"), field("path"))
                ),
                arrayOf(
                    "INVALID_SFTP_SETTINGS",
                    VALID_SFTP_SETTINGS.copy(encryptedPassword = ""),
                    notEmptyString(),
                    path(field("settings"), field("encryptedPassword"))
                ),
                arrayOf(
                    "INVALID_SFTP_SETTINGS",
                    VALID_SFTP_SETTINGS.copy(port = -1),
                    invalidValue(),
                    path(field("settings"), field("port"))
                ),
                arrayOf(
                    "INVALID_SFTP_SETTINGS",
                    VALID_SFTP_SETTINGS.copy(login = ""),
                    notEmptyString(),
                    path(field("settings"), field("login"))
                ),
                arrayOf(
                    "INVALID_GOOGLE_SHEETS_SETTINGS",
                    VALID_GOOGLE_SHEETS_SETTINGS.copy(url = "invalid_url"),
                    invalidValue(),
                    path(field("settings"), field("url"))
                )
            )
        }
    }

    @Test
    fun validateConversionSource_MetrikaWithoutId() {
        val source: ConversionSource = validMetrikaConversionSource()
        val context = validationContextWithoutExisting()

        val vr = validationService.validateConversionSource(context, source)

        assertThat(vr).doesNotContainAnyErrors()
    }

    @Test
    fun validateConversionSource_LinkWithoutId() {
        val source: ConversionSource = validLinkConversionSource()
        val context = validationContextWithoutExisting()

        val vr = validationService.validateConversionSource(context, source)

        assertThat(vr).doesNotContainAnyErrors()
    }

    @Test
    fun validateConversionSource_NonExistentId() {
        val source: ConversionSource = validMetrikaConversionSource().copy(id = 22L)
        val context = validationContextWithoutExisting()

        val vr = validationService.validateConversionSource(context, source)

        assertThat(vr).containsExactlyErrors(
            validationError(path(field("id")), CommonDefects.objectNotFound())
        )
    }

    @Test
    fun validateConversionSource_ExistingId() {
        val source: ConversionSource = validMetrikaConversionSource().copy(id = 22L)
        val context = validationContextWithoutExisting().copy(existingConversionSources = mapOf(source.id!! to source))

        val vr = validationService.validateConversionSource(context, source)

        assertThat(vr).doesNotContainAnyErrors()
    }

    @Test
    fun validateConversionSource_WrongSettingType() {
        val source: ConversionSource = validMetrikaConversionSource().copy(
            settings = ConversionSourceSettings.Link(url = "https://ya.ru")
        )
        val context = validationContextWithoutExisting()

        val vr = validationService.validateConversionSource(context, source)

        assertThat(vr).containsExactlyErrors(
            validationError(path(field("settings")), invalidValue())
        )
    }

    @Test
    fun validateConversionSource_WrongLinkSettings() {
        val source: ConversionSource = validLinkConversionSource().copy(
            settings = ConversionSourceSettings.Link(url = "xxx")
        )
        val context = validationContextWithoutExisting()

        val vr = validationService.validateConversionSource(context, source)

        assertThat(vr).containsExactlyErrors(
            validationError(path(field("settings"), field("url")), CommonDefects.invalidValue())
        )
    }

    @Test
    fun validateConversionSource_LinkWithoutActions() {
        val source: ConversionSource = validLinkConversionSource().copy(
            actions = listOf()
        )
        val context = validationContextWithoutExisting()

        val vr = validationService.validateConversionSource(context, source)

        assertThat(vr).containsExactlyErrors(
            validationError(path(field("actions")), CollectionDefects.collectionSizeIsValid(2, 2))
        )
    }

    @Test
    fun validateConversionSource_LinkWithOnlyAction() {
        val source: ConversionSource = validLinkConversionSource().copy(
            actions = listOf(
                ConversionAction(ACTION_NAME_IN_PROGRESS, null, value = null)
            ),
        )
        val context = validationContextWithoutExisting()

        val vr = validationService.validateConversionSource(context, source)

        assertThat(vr).containsExactlyErrors(
            validationError(path(field("actions")), CollectionDefects.collectionSizeIsValid(2, 2))
        )
    }

    @Test
    fun validateConversionSource_LinkWithTooManyActions() {
        val source: ConversionSource = validLinkConversionSource().copy(
            actions = listOf(
                ConversionAction(ACTION_NAME_IN_PROGRESS, null, value = null),
                ConversionAction(ACTION_NAME_PAID, null, value = null),
                ConversionAction("xxxx", null, value = null),
            ),
        )
        val context = validationContextWithoutExisting()

        val vr = validationService.validateConversionSource(context, source)

        assertThat(vr).containsExactlyErrors(
            validationError(path(field("actions")), CollectionDefects.collectionSizeIsValid(2, 2))
        )
    }

    @Test
    fun validateConversionSource_LinkWithWrongActionName() {
        val source: ConversionSource = validLinkConversionSource().copy(
            actions = listOf(
                ConversionAction(ACTION_NAME_IN_PROGRESS, null, value = null),
                ConversionAction("xxxx", null, value = null),
            ),
        )
        val context = validationContextWithoutExisting()

        val vr = validationService.validateConversionSource(context, source)

        assertThat(vr).containsExactlyErrors(
            validationError(
                path(field("actions"), index(1), field("name")),
                invalidValue()
            )
        )
    }

    @Test
    fun validateConversionSource_InaccessibleCounterId() {
        val source: ConversionSource = validLinkConversionSource()
        val context = validationContextWithoutExisting().copy(accessibleCounterIds = setOf())

        val vr = validationService.validateConversionSource(context, source)

        assertThat(vr).containsExactlyErrors(
            validationError(path(field("counterId")), ConversionSourceDefects.counterIsInaccessible(source.counterId))
        )
    }

    @ParameterizedTest(name = "{0} path {3}")
    @MethodSource("testInvalidConversionSettingsProvider")
    fun validateConversionConversionSource_InvalidSettings(
        description: String,
        settings: ConversionSourceSettings,
        defect: Defect<*>,
        path: Path
    ) {
        val conversionSource = validConversionSource(settings)
        val context = validationContextWithoutExisting()

        val vr = validationService.validateConversionSource(context, conversionSource)

        assertThat(vr).containsExactlyErrors(validationError(path, defect))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testValidConversionSettingsProvider")
    fun validateConversionConversionSource_ValidSettings(
        description: String,
        settings: ConversionSourceSettings
    ) {
        val conversionSource = validConversionSource(settings)
        val context = validationContextWithoutExisting()

        val vr = validationService.validateConversionSource(context, conversionSource)

        assertThat(vr).doesNotContainAnyErrors()
    }

    private fun validationContextWithoutExisting() = ConversionSourceValidationContext(
        owner = CLIENT_ID,
        existingConversionSources = mapOf(),
        accessibleCounterIds = setOf(COUNTER_ID),
    )

    private fun validLinkConversionSource() = defaultConversionSourceLink(CLIENT_ID, COUNTER_ID)

    private fun validMetrikaConversionSource() = defaultConversionSourceMetrika(CLIENT_ID, COUNTER_ID)

    private fun validConversionSource(settings: ConversionSourceSettings): ConversionSource {
        val type = when (settings) {
            is ConversionSourceSettings.Link -> ConversionSourceTypeCode.LINK
            is ConversionSourceSettings.Ftp -> ConversionSourceTypeCode.FTP
            is ConversionSourceSettings.SFtp -> ConversionSourceTypeCode.SFTP
            is ConversionSourceSettings.Metrika -> ConversionSourceTypeCode.METRIKA
            is ConversionSourceSettings.GoogleSheets -> ConversionSourceTypeCode.GOOGLE_SHEETS
        }
        return defaultConversionSourceLink(CLIENT_ID, COUNTER_ID)
            .copy(settings = settings, typeCode = type)
    }
}

private fun <ACTUAL : ValidationResult<*, D>> ObjectAssert<ACTUAL>.doesNotContainAnyErrors() {
    extracting { it.flattenErrors() }.asList().isEmpty()
}

private fun <ACTUAL : ValidationResult<*, D>> ObjectAssert<ACTUAL>.containsExactlyErrors(
    vararg matchers: Matcher<DefectInfo<D>>,
) {
    extracting { it.flattenErrors() }.asList().`is`(
        Conditions.matchedBy(Matchers.containsInAnyOrder(*matchers))
    )
}

private fun <ACTUAL : ValidationResult<*, D>> ObjectAssert<ACTUAL>.doesNotContainErrors(
    vararg matchers: Matcher<DefectInfo<Defect<Any>>>,
) {
    extracting { it.flattenErrors() }.asList().`is`(
        Conditions.matchedBy(Matchers.not(Matchers.hasItems(*matchers)))
    )
}
