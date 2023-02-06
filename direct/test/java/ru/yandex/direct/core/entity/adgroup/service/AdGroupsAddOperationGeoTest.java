package ru.yandex.direct.core.entity.adgroup.service;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupsAddOperationGeoTest {

    @Autowired
    public GeoTreeFactory geoTreeFactory;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private AdGroupsAddOperationFactory operationFactory;

    @Autowired
    private Steps steps;

    @Test
    public void russianClientAndAdGroupWithGeoRussia_AddToDatabaseRussiaAndCrimea() {
        ClientInfo russianClient =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(RUSSIA_REGION_ID));
        CampaignInfo defaultCampaign = steps.campaignSteps().createActiveTextCampaign(russianClient);
        AdGroup adGroup = TestGroups.activeTextAdGroup(defaultCampaign.getCampaignId())
                .withGeo(Collections.singletonList(RUSSIA_REGION_ID));

        AdGroup actualAdGroup = addAdGroup(adGroup, russianClient);
        assertThat(actualAdGroup.getGeo(), containsInAnyOrder(RUSSIA_REGION_ID, CRIMEA_REGION_ID));
    }

    @Test
    public void ukraineClientAndAdGroupWithGeoUkraine_AddToDatabaseUkraineAndCrimea() {
        ClientInfo ukraineClient =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(UKRAINE_REGION_ID));
        CampaignInfo defaultCampaign = steps.campaignSteps().createActiveTextCampaign(ukraineClient);
        AdGroup adGroup = TestGroups.activeTextAdGroup(defaultCampaign.getCampaignId())
                .withGeo(Collections.singletonList(UKRAINE_REGION_ID));

        AdGroup actualAdGroup = addAdGroup(adGroup, ukraineClient);
        assertThat(actualAdGroup.getGeo(), containsInAnyOrder(UKRAINE_REGION_ID, CRIMEA_REGION_ID));
    }

    @Test
    public void globalClientAndAdGroupWithGeoRussia_AddToDatabaseRussiaAndCrimea() {
        ClientInfo globalClient =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(GLOBAL_REGION_ID));
        CampaignInfo defaultCampaign = steps.campaignSteps().createActiveTextCampaign(globalClient);
        AdGroup adGroup = TestGroups.activeTextAdGroup(defaultCampaign.getCampaignId())
                .withGeo(Collections.singletonList(RUSSIA_REGION_ID));

        AdGroup actualAdGroup = addAdGroup(adGroup, globalClient);
        assertThat(actualAdGroup.getGeo(), containsInAnyOrder(RUSSIA_REGION_ID, CRIMEA_REGION_ID));
    }

    private AdGroup addAdGroup(AdGroup adGroup, ClientInfo clientInfo) {
        GeoTree geoTree = geoTreeFactory.getTranslocalGeoTree(clientInfo.getClient().getCountryRegionId());
        AdGroupsAddOperation addOperation = operationFactory
                .newInstance(Applicability.FULL, Collections.singletonList(adGroup), geoTree,
                        MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE, clientInfo.getUid(),
                        clientInfo.getClientId(), clientInfo.getShard(), true, true);

        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        return adGroupRepository
                .getAdGroups(clientInfo.getShard(), Collections.singletonList(result.get(0).getResult())).get(0);
    }

}
