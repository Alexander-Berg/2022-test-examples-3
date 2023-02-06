package ru.yandex.direct.core.entity.landing

import java.lang.reflect.Field
import maps_adv.geosmb.landlord.proto.internal.landing_details.LandingDetailsOuterClass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

/**
 * Названия полей, про которые мы уже знаем, что их либо нет возможности обновить
 * (их нет в [LandingDetailsInput][maps_adv.geosmb.landlord.proto.internal.landing_details.LandingDetailsOuterClass.LandingDetailsInput]),
 * либо мы перезаписываем это поле как есть
 *
 * Дочерние поля указываются через точку от названия родительского
 */
val SKIP_FIELDS = setOf(
    // Системные поля
    "serialVersionUID",
    "memoizedIsInitialized",

    // Эти поля сейчас нельзя обновить через API
    "contacts_.geo_",
    "contacts_.isSubstitutionPhone_",

    // Перезаписываем эти поля как есть
    "preferences_",
    "extras_",
    "blocksOptions_",
)

/**
 * Поля, которые мы обновляем с помощью
 * [ru.yandex.direct.core.entity.landing.BizLandingService.buildLandingDetailsInput]
 */
val DIRECT_INPUT_FIELDS = setOf(
    "name_",
    "categories_",
    "description_",
    "logo_.templateUrl_",
    "cover_.templateUrl_",
    "preferences_",
    "contacts_.email_",
    "contacts_.phone_",
    "contacts_.website_",
    "contacts_.phones_",
    "contacts_.instagram_",
    "contacts_.facebook_",
    "contacts_.vkontakte_",
    "contacts_.twitter_",
    "contacts_.telegram_",
    "contacts_.viber_",
    "contacts_.whatsapp_",
)

val SIMPLE_TYPES = listOf(
    "boolean",
    "byte",
    "int",
    "long",
    "java.lang.Object",
    "com.google.protobuf.LazyStringList",
)

/**
 * В [ru.yandex.direct.core.entity.landing.BizLandingService.updateBizLanding] делается обновление лендинга:
 * вычитывается текущее состояние
 * [LandingDetails][maps_adv.geosmb.landlord.proto.internal.landing_details.LandingDetailsOuterClass.LandingDetails],
 * мерджится с входящим с фронтенда input'ом и отправляется в Landlord в виде
 * [LandingDetailsInput][maps_adv.geosmb.landlord.proto.internal.landing_details.LandingDetailsOuterClass.LandingDetailsInput]
 *
 * Если в текущем состоянии `LandingDetailsInput` добавятся новые поля, то сейчас от нас в запросе придет пустое
 * значение для этого поля, то есть мы затрем старое значение
 *
 * Поля, которые реально обновляются в запросе от Директа, перечислены в [DIRECT_INPUT_FIELDS]
 *
 * В этом тесте будут отслеживаться изменения в `LandingDetailsInput`,
 * чтобы своевременно дописать обновление нового поля
 */
class LandingDetailsInputUpdateFieldsTest {

    @Test
    fun diff_landing_vs_directInput_shouldBeEmpty() {
        val landingInputFields = mutableSetOf<String>()
        mapFieldsTo(landingInputFields, LandingDetailsOuterClass.LandingDetailsInput::class.java.declaredFields)

        if ((landingInputFields - DIRECT_INPUT_FIELDS).isEmpty()) {
            return
        }
        fail {
            """Директ не знает про некоторые поля лендинга: ${landingInputFields - DIRECT_INPUT_FIELDS}
            |Пожалуйста, проверь, что указанные поля учтены в ru.yandex.direct.core.entity.landing.BizLandingService#buildLandingDetailsInput.
            |После этого поправь значения SKIP_FIELDS и DIRECT_INPUT_FIELDS в этом тесте.
            |
            |Тест не использует хитрых эвристик и полагается только на добросовестность разработчика. Спасибо!
        """.trimMargin()
        }
    }

    /**
     * Собрать в `result` список всех полей `fields` и всех их дочерних полей и тд
     *
     * Рекурсивно, пока не дойдем до простых полей, перечисленных в [SIMPLE_TYPES]
     */
    private fun mapFieldsTo(result: MutableSet<String>, fields: Array<out Field>, namePrefix: String = "") {
        val meaningfulFields = getMeaningfulFields(fields, namePrefix)
        for (field in meaningfulFields) {
            val name = field.key
            val type = field.value
            if (type in SIMPLE_TYPES) {
                result.add(name)
            } else {
                mapFieldsTo(result, Class.forName(type).declaredFields, namePrefix = "$name.")
            }
        }
    }

    private fun getMeaningfulFields(fields: Array<out Field>, namePrefix: String = ""): Map<String, String> {
        val isSystemField: (Field) -> Boolean = { it.name.endsWith("0_") }
        val isConstantField: (Field) -> Boolean = { it.name.none { c -> c.isLowerCase() } }
        return fields
            .asSequence()
            .filterNot { it.name in SKIP_FIELDS }
            .filterNot { "$namePrefix${it.name}" in SKIP_FIELDS }
            .filterNot(isSystemField)
            .filterNot(isConstantField)
            .associate { "$namePrefix${it.name}" to it.type.name }
    }
}
