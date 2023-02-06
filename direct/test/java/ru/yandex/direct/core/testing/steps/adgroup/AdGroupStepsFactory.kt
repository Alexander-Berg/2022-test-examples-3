package ru.yandex.direct.core.testing.steps.adgroup

import org.springframework.stereotype.Component
import ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo

@Component
class AdGroupStepsFactory(
    private val adGroupSteps: Collection<AdGroupSteps<*, out AdGroupInfo<*>>>
) {

    fun createAdGroup(adGroupInfo: AdGroupInfo<*>): AdGroupInfo<*> {
        val step = resolveSteps(adGroupInfo)
        return step.createAdGroup(adGroupInfo)
    }

    private fun resolveSteps(adGroupInfo: AdGroupInfo<*>): AdGroupSteps<*, AdGroupInfo<*>> {
        return (adGroupSteps.find {
            adGroupInfo::class == it.getAdGroupInfoClass()
        } ?: throw UnsupportedOperationException("cannot find step for adgroup class " + adGroupInfo::class))
            as AdGroupSteps<*, AdGroupInfo<*>>
    }
}
