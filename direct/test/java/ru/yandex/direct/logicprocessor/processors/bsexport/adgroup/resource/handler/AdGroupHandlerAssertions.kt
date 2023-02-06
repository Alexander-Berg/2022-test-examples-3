package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler

import org.assertj.core.api.Assertions.assertThat
import ru.yandex.direct.core.entity.adgroup.model.AdGroup

object AdGroupHandlerAssertions {

    fun <T> assertAdGroupHandledCorrectly(
        handler: AdGroupBaseHandler<T>,
        adGroup: AdGroup,
        expectedProto: ru.yandex.adv.direct.adgroup.AdGroup,
    ) {
        val adGroupWithBuilder = AdGroupWithBuilder(adGroup, ru.yandex.adv.direct.adgroup.AdGroup.newBuilder())
        handler.handle(1, mapOf(adGroup.id to adGroupWithBuilder))
        assertThat(adGroupWithBuilder.protoBuilder.buildPartial()).isEqualTo(expectedProto)
    }

    fun <T> assertAdGroupsHandledCorrectly(
        handler: AdGroupBaseHandler<T>,
        adGroups: List<AdGroup>,
        expectedProtos: List<ru.yandex.adv.direct.adgroup.AdGroup>,
    ) {
        val adGroupsWithBuilders: HashMap<Long, AdGroupWithBuilder> = HashMap()
        val adGroupsWithExpectedProtos: HashMap<Long, ru.yandex.adv.direct.adgroup.AdGroup> = HashMap()
        adGroups.zip(expectedProtos).forEach { pair ->
            val (adGroup, expectedProto) = pair
            val adGroupWithBuilder = AdGroupWithBuilder(adGroup, ru.yandex.adv.direct.adgroup.AdGroup.newBuilder())
            adGroupsWithBuilders[adGroup.id] = adGroupWithBuilder
            adGroupsWithExpectedProtos[adGroup.id] = expectedProto
        }

        handler.handle(1, adGroupsWithBuilders)

        assertThat(adGroupsWithBuilders.mapValues { it.value.protoBuilder.buildPartial() })
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*contextId_", ".*memoizedHashCode", ".*bitField0_")
            .isEqualTo(adGroupsWithExpectedProtos)
    }
}
