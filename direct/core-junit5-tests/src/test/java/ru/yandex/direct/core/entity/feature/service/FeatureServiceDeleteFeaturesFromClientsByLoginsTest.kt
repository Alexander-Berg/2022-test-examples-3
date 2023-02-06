package ru.yandex.direct.core.entity.feature.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.feature.container.LoginClientIdChiefLoginWithFeature
import ru.yandex.direct.core.entity.feature.model.ClientFeature
import ru.yandex.direct.core.entity.feature.model.FeatureState
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.FeatureSteps

@CoreTest
@ExtendWith(SpringExtension::class)
class FeatureServiceDeleteFeaturesFromClientsByLoginsTest @Autowired constructor(
    private val featureManagingService: FeatureManagingService,
    private val featureSteps: FeatureSteps,
    private val clientSteps: ClientSteps
) {
    @Test
    fun `correct result on success`() {
        val client = clientSteps.createDefaultClient()
        val feature = featureSteps.addDefaultFeature()
        featureSteps.addClientFeature(
            ClientFeature()
                .withClientId(client.clientId)
                .withId(feature.id)
                .withState(FeatureState.ENABLED)
        )

        val result = featureManagingService.deleteFeaturesFromClientIdsByLogins(
            listOf(feature.featureTextId),
            listOf(client.login)
        )

        Assertions.assertThat(result.isSuccessful).isTrue
        Assertions.assertThat(result.errors).isEmpty()
        Assertions.assertThat(result.result).containsExactly(
            LoginClientIdChiefLoginWithFeature()
                .withClientId(client.clientId?.asLong())
                .withLogin(client.login)
                .withChiefLogin(client.chiefUserInfo?.login)
                .withFeatureTextId(feature.featureTextId)
                .withFeatureState(FeatureState.DISABLED)
        )
    }

    @Test
    fun `validation error when feature not exist`() {
        val client = clientSteps.createDefaultClient()
        val feature = featureSteps.addDefaultFeature()
        featureSteps.addClientFeature(
            ClientFeature()
                .withClientId(client.clientId)
                .withId(feature.id)
                .withState(FeatureState.ENABLED)
        )

        val result = featureManagingService.deleteFeaturesFromClientIdsByLogins(
            listOf("nonexistent_feature"),
            listOf(client.login)
        )

        Assertions.assertThat(result.isSuccessful).isFalse
        Assertions.assertThat(result.errors).isNotEmpty
        Assertions.assertThat(featureSteps.getClientsWithFeatures(listOf(feature.id))[feature.id])
            .hasSize(1)
    }

    @Test
    fun `validation error when login not exist`() {
        val client = clientSteps.createDefaultClient()
        val feature = featureSteps.addDefaultFeature()
        featureSteps.addClientFeature(
            ClientFeature()
                .withClientId(client.clientId)
                .withId(feature.id)
                .withState(FeatureState.ENABLED)
        )

        val result = featureManagingService.deleteFeaturesFromClientIdsByLogins(
            listOf(feature.featureTextId),
            listOf("nonexistent_login")
        )

        Assertions.assertThat(result.isSuccessful).isFalse
        Assertions.assertThat(result.errors).isNotEmpty
        Assertions.assertThat(featureSteps.getClientsWithFeatures(listOf(feature.id))[feature.id])
            .hasSize(1)
    }

    @Test
    fun `delete feature from one client when feature enabled`() {
        val client = clientSteps.createDefaultClient()
        val feature = featureSteps.addDefaultFeature()
        featureSteps.addClientFeature(
            ClientFeature()
                .withClientId(client.clientId)
                .withId(feature.id)
                .withState(FeatureState.ENABLED)
        )

        val result = featureManagingService.deleteFeaturesFromClientIdsByLogins(
            listOf(feature.featureTextId),
            listOf(client.login)
        )

        Assertions.assertThat(result.isSuccessful).isTrue
        Assertions.assertThat(featureSteps.getClientsWithFeatures(listOf(feature.id))[feature.id])
            .isNullOrEmpty()
    }

    @Test
    fun `delete feature from one client when feature disabled`() {
        val client = clientSteps.createDefaultClient()
        val feature = featureSteps.addDefaultFeature()
        featureSteps.addClientFeature(
            ClientFeature()
                .withClientId(client.clientId)
                .withId(feature.id)
                .withState(FeatureState.DISABLED)
        )

        val result = featureManagingService.deleteFeaturesFromClientIdsByLogins(
            listOf(feature.featureTextId),
            listOf(client.login)
        )

        Assertions.assertThat(result.isSuccessful).isTrue
        Assertions.assertThat(featureSteps.getClientsWithFeatures(listOf(feature.id))[feature.id])
            .isNullOrEmpty()
    }

    @Test
    fun `delete one of several features`() {
        val client = clientSteps.createDefaultClient()
        val feature1 = featureSteps.addDefaultFeature()
        val feature2 = featureSteps.addDefaultFeature()
        featureSteps.addClientFeature(
            ClientFeature()
                .withClientId(client.clientId)
                .withId(feature1.id)
                .withState(FeatureState.ENABLED)
        )
        featureSteps.addClientFeature(
            ClientFeature()
                .withClientId(client.clientId)
                .withId(feature2.id)
                .withState(FeatureState.ENABLED)
        )

        val result = featureManagingService.deleteFeaturesFromClientIdsByLogins(
            listOf(feature1.featureTextId),
            listOf(client.login)
        )

        Assertions.assertThat(result.isSuccessful).isTrue
        val clientsWithFeatures = featureSteps.getClientsWithFeatures(listOf(feature1.id, feature2.id))
        Assertions.assertThat(clientsWithFeatures[feature1.id])
            .isNullOrEmpty()
        Assertions.assertThat(clientsWithFeatures[feature2.id])
            .hasSize(1)
    }

    @Test
    fun `delete feature from one of several clients`() {
        val client1 = clientSteps.createDefaultClient()
        val client2 = clientSteps.createDefaultClient()
        val feature = featureSteps.addDefaultFeature()
        featureSteps.addClientFeature(
            ClientFeature()
                .withClientId(client1.clientId)
                .withId(feature.id)
                .withState(FeatureState.ENABLED)
        )
        featureSteps.addClientFeature(
            ClientFeature()
                .withClientId(client2.clientId)
                .withId(feature.id)
                .withState(FeatureState.ENABLED)
        )

        val result = featureManagingService.deleteFeaturesFromClientIdsByLogins(
            listOf(feature.featureTextId),
            listOf(client1.login)
        )

        Assertions.assertThat(result.isSuccessful).isTrue
        Assertions.assertThat(featureSteps.getClientsWithFeatures(listOf(feature.id))[feature.id])
            .containsExactly(
                ClientFeature()
                    .withClientId(client2.clientId)
                    .withId(feature.id)
                    .withState(FeatureState.ENABLED)
            )
    }

    @Test
    fun `delete feature from several clients`() {
        val client1 = clientSteps.createDefaultClient()
        val client2 = clientSteps.createDefaultClient()
        val feature = featureSteps.addDefaultFeature()
        featureSteps.addClientFeature(
            ClientFeature()
                .withClientId(client1.clientId)
                .withId(feature.id)
                .withState(FeatureState.ENABLED)
        )
        featureSteps.addClientFeature(
            ClientFeature()
                .withClientId(client2.clientId)
                .withId(feature.id)
                .withState(FeatureState.ENABLED)
        )

        val result = featureManagingService.deleteFeaturesFromClientIdsByLogins(
            listOf(feature.featureTextId),
            listOf(client1.login, client2.login)
        )

        Assertions.assertThat(result.isSuccessful).isTrue
        Assertions.assertThat(featureSteps.getClientsWithFeatures(listOf(feature.id))[feature.id])
            .isNullOrEmpty()
    }
}
