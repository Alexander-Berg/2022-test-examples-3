package ru.yandex.direct.core.entity.clientphone

import org.assertj.core.api.Assertions.entry
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository
import ru.yandex.direct.core.entity.organization.model.Organization
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.randomPositiveInt
import ru.yandex.direct.test.utils.randomPositiveLong

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientPhoneRepositoryOrganizationsTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var clientPhoneRepository: ClientPhoneRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    private var shard: Int = -1
    private var bid: Long = -1

    private lateinit var clientId: ClientId
    private lateinit var clientInfo: ClientInfo
    private lateinit var organization: Organization
    private lateinit var adGroupInfo: AdGroupInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        shard = clientInfo.shard
        clientId = clientInfo.clientId!!

        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo)
        bid = steps.bannerSteps().createActiveTextBanner(adGroupInfo).bannerId
        organization = steps.organizationSteps().createClientOrganization(clientId, randomPositiveLong())
    }

    @Test
    fun unlinkBannerPhonesByBannerId() {
        val banners = listOf(bid)
        val phoneId = randomPositiveInt().toLong()

        steps.organizationSteps().linkOrganizationToBanner(clientId, organization.permalinkId, bid)
        steps.clientPhoneSteps().linkPhoneIdToBanner(shard, bid, phoneId)

        clientPhoneRepository.unlinkBannerPhonesByBannerId(shard, banners)

        val organizationsByAdGroupId =
                organizationRepository.getOrganizationsByBannerIds(shard, clientId, banners)
        val bannerPhonesByBids =
                clientPhoneRepository.getPhoneIdsByBannerIds(shard, banners)

        assertSoftly { softly ->
            softly.assertThat(organizationsByAdGroupId).containsOnly(entry(bid, organization))

            softly.assertThat(bannerPhonesByBids).doesNotContainKey(bid)
        }
    }
}
