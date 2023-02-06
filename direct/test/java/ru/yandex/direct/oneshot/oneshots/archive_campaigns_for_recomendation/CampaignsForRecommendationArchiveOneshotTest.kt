package ru.yandex.direct.oneshot.oneshots.archive_campaigns_for_recomendation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

@OneshotTest
@RunWith(SpringRunner::class)
class CampaignsForRecommendationArchiveOneshotTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var oneshot: CampaignsForRecommendationArchiveOneshot

    private lateinit var clientId: ClientId

    @Before
    fun setUp() {
        val operatorInfo = steps.clientSteps().createDefaultClient().chiefUserInfo!!
        clientId = operatorInfo.user!!.clientId
    }

    @Test
    fun validate_success() {
        val inputData = InputData(listOf(clientId.asLong()))
        val vr = oneshot.validate(inputData)
        assertThat(vr.flattenErrors()).isEmpty()
    }

    @Test
    fun validate_withInvalidClientId() {
        val invalidClientId = 0L
        val inputData = InputData(listOf(invalidClientId))
        val vr = oneshot.validate(inputData)
        assertThat(vr.flattenErrors()).`is`(
            Conditions.matchedBy(
                org.hamcrest.Matchers.contains(
                    Matchers.validationError(
                        PathHelper.path(
                            PathHelper.field("clientIds"),
                            PathHelper.index(0)
                        ), CommonDefects.validId()
                    )
                )
            )
        )
    }
}
