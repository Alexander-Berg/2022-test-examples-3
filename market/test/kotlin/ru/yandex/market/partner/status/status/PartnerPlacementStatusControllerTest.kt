package ru.yandex.market.partner.status.status

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.mj.generated.client.wizard_client.model.NeedTestingState
import ru.yandex.mj.generated.client.wizard_client.model.PartnerStatusInfo
import ru.yandex.mj.generated.client.wizard_client.model.PartnerSubStatusInfo
import ru.yandex.mj.generated.client.wizard_client.model.StatusResolverResults
import ru.yandex.mj.generated.client.wizard_client.model.StatusResolverType
import ru.yandex.mj.generated.client.wizard_client.model.StatusResolversRequest
import ru.yandex.mj.generated.client.wizard_client.model.StatusResolversRequestItem
import ru.yandex.mj.generated.client.wizard_client.model.WizardStepStatus

/**
 * Тесты для [PartnerPlacementStatusController].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class PartnerPlacementStatusControllerTest : AbstractFunctionalTest() {

    @Test
    fun `empty request`() {
        val request = StatusResolversRequest()
        val response = partnerPlacementStatusApiClient.getStatusResolvers(request).schedule().join()
        Assertions.assertThat(response.resolvers)
            .isEmpty()
    }

    @Test
    fun `unknown partners`() {
        val request = StatusResolversRequest()
            .addResolversItem(
                StatusResolversRequestItem()
                    .addNamesItem(StatusResolverType.FBS_SORTING_CENTER)
                    .addPartnerIdsItem(100L)
            )
        val response = partnerPlacementStatusApiClient.getStatusResolvers(request).schedule().join()
        Assertions.assertThat(response.resolvers)
            .singleElement()
            .isEqualTo(
                StatusResolverResults()
                    .resolver(StatusResolverType.FBS_SORTING_CENTER)
                    .results(emptyList())
            )
    }

    @Test
    @DbUnitDataSet(before = ["PartnerPlacementStatusControllerTest/nullStatus.before.csv"])
    fun `partner with null status`() {
        val request = StatusResolversRequest()
            .addResolversItem(
                StatusResolversRequestItem()
                    .addNamesItem(StatusResolverType.FBS_SORTING_CENTER)
                    .addPartnerIdsItem(100L)
            )
        val response = partnerPlacementStatusApiClient.getStatusResolvers(request).schedule().join()
        Assertions.assertThat(response.resolvers)
            .singleElement()
            .isEqualTo(
                StatusResolverResults()
                    .resolver(StatusResolverType.FBS_SORTING_CENTER)
                    .addResultsItem(PartnerStatusInfo().partnerId(100L))
            )
    }

    @Test
    @DbUnitDataSet(before = ["PartnerPlacementStatusControllerTest/failedStatus.before.csv"])
    fun `partner with failed status`() {
        val request = StatusResolversRequest()
            .addResolversItem(
                StatusResolversRequestItem()
                    .addNamesItem(StatusResolverType.FBS_SORTING_CENTER)
                    .addPartnerIdsItem(100L)
            )
        val response = partnerPlacementStatusApiClient.getStatusResolvers(request).schedule().join()
        Assertions.assertThat(response.resolvers)
            .singleElement()
            .isEqualTo(
                StatusResolverResults()
                    .resolver(StatusResolverType.FBS_SORTING_CENTER)
                    .addResultsItem(
                        PartnerStatusInfo()
                            .partnerId(100L)
                            .status(WizardStepStatus.FAILED)
                            .enabled(false)
                            .needTestingState(NeedTestingState.NOT_REQUIRED)
                            .addSubStatusesItem(
                                PartnerSubStatusInfo().code("sort_center_not_configured").params(
                                    emptyMap()
                                )
                            )
                    )
            )
    }


}
