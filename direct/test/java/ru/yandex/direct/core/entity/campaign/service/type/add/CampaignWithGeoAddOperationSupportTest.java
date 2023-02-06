package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithGeo;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithGeoAddOperationSupportTest {
    private static final long UID = RandomNumberUtils.nextPositiveLong();
    private static final ClientId CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final Integer GEO = RandomNumberUtils.nextPositiveInteger();
    private static final int SHARD = RandomNumberUtils.nextPositiveInteger();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    public ClientService clientService;

    @Mock
    public ClientGeoService clientGeoService;

    @InjectMocks
    public CampaignWithGeoAddOperationSupport campaignWithGeoAddOperationSupport;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void init() {
        doReturn(null).when(clientGeoService).getClientTranslocalGeoTree(eq(CLIENT_ID));
        doReturn(List.of(GEO.longValue())).when(clientGeoService).convertForSave(eq(List.of(GEO.longValue())),
                isNull());
    }

    @Test
    public void campaignWithGeoAddOperationSupport_checkEnrichWithClientGeo() {
        doReturn(GEO.longValue()).when(clientService).getCountryRegionIdByClientIdStrict(eq(CLIENT_ID));

        List<CampaignWithGeo> campaigns = List.of(
                (CampaignWithGeo) TestCampaigns.newCampaignByCampaignType(campaignType),
                (CampaignWithGeo) TestCampaigns.newCampaignByCampaignType(campaignType));

        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(SHARD,
                UID,
                CLIENT_ID, UID,
                UID);
        campaignWithGeoAddOperationSupport.onPreValidated(addCampaignParametersContainer, campaigns);

        assertThat(campaigns).allMatch(campaign -> campaign.getGeo().equals(Set.of(GEO)));
    }
}
