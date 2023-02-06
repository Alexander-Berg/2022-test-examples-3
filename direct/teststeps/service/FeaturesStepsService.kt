package ru.yandex.direct.teststeps.service

import org.springframework.stereotype.Service
import ru.yandex.direct.core.entity.feature.model.ClientFeature
import ru.yandex.direct.core.entity.feature.model.Feature
import ru.yandex.direct.core.entity.feature.model.FeatureState
import ru.yandex.direct.core.testing.steps.FeatureSteps
import ru.yandex.direct.dbutil.model.ClientId

@Service
class FeaturesStepsService(
    private val featureSteps: FeatureSteps,
    private val infoHelper: InfoHelper
) {
    private lateinit var allFeatures: MutableList<Feature>

    /**
     * Степ принимает логин и мапу "Фича - Состояние", и приводит состояние фичей на логин в базе к запрошенному
     */
    fun setFeatureForLogin(login: String, targetFeaturesStates: Map<String, Boolean>) {
        allFeatures = featureSteps.getFeatures()
        val clientId = infoHelper.getUserInfo(login).user!!.clientId
        val clientFeatures =
            targetFeaturesStates.mapNotNull { (featureName, state) -> toClientFeature(clientId, featureName, state) }
        featureSteps.addClientFeatures(clientFeatures)
    }

    private fun toClientFeature(clientId: ClientId, featureName: String, state: Boolean): ClientFeature? {
        //noinspection OptionalGetWithoutIsPresent
        val feature = allFeatures.toList()
            .find { it.featureTextId == featureName }
        return feature?.let {
            ClientFeature()
                .withClientId(clientId)
                .withId(it.id)
                .withState(if (state) FeatureState.ENABLED else FeatureState.DISABLED)
        }
    }
}
