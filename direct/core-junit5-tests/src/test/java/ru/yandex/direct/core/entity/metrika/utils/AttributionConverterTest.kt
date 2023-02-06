package ru.yandex.direct.core.entity.metrika.utils

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.metrika.client.internal.Attribution

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttributionConverterTest {
    @ParameterizedTest
    @EnumSource(CampaignAttributionModel::class)
    fun coreToMetrika_canConvert(param: CampaignAttributionModel) {
        Assertions.assertThatCode { AttributionConverter.coreToMetrika(param) }.doesNotThrowAnyException()
    }

    @ParameterizedTest
    @EnumSource(value = Attribution::class, mode = EnumSource.Mode.EXCLUDE, names = ["UNKNOWN"])
    fun metrikaToCore_canConvert(param: Attribution) {
        Assertions.assertThatCode { AttributionConverter.metrikaToCore(param) }.doesNotThrowAnyException()
    }
}
