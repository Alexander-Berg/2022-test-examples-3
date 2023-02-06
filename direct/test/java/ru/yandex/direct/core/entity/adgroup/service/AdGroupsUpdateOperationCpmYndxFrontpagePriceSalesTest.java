package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import jdk.jfr.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupUpdateOperationParams;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_DEFAULT;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDefaultAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestGroups.activeSpecificAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.CRIMEA_PROVINCE;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.UKRAINE;
import static ru.yandex.direct.core.testing.data.TestRegions.URYUPINSK_REGION_ID;
import static ru.yandex.direct.regions.Region.KYIV_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationCpmYndxFrontpagePriceSalesTest extends
        AdGroupsUpdateOperationCpmYndxFrontpageTestBase {

    public static final String NEW_NAME = "new adgroup name";

    @Autowired
    protected Steps steps;

    @Autowired
    protected AdGroupRepository adGroupRepository;

    @Autowired
    protected GeoTreeFactory geoTreeFactory;

    private GeoTree geoTree;
    private ClientInfo clientInfo;
    private PricePackage pricePackage;
    private CpmPriceCampaign cpmPriceCampaign;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        geoTree = geoTreeFactory.getGlobalGeoTree();
        pricePackage = createPricePackageWithRussiaGeoForClient(clientInfo);
        cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
    }

    @Test
    public void updateDefaultAdGroupName_Success() {
        CpmYndxFrontpageAdGroup adGroup = createDefaultAdGroup();
        ModelChanges<AdGroup> modelChanges = (ModelChanges) modelChangesWithName(adGroup, NEW_NAME);
        var updateOperation = createUpdateOperation(singletonList(modelChanges));

        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        AdGroup adGroupFromDb =
                adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(result.get(0).getResult())).get(0);
        assertThat(adGroupFromDb.getName(), equalTo(NEW_NAME));
    }

    @Test
    public void updateSpecificAdGroupName_Success() {
        CpmYndxFrontpageAdGroup adGroup = createSpecificAdGroup();
        ModelChanges<AdGroup> modelChanges = (ModelChanges) modelChangesWithName(adGroup, NEW_NAME);
        var updateOperation = createUpdateOperation(singletonList(modelChanges));

        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        AdGroup adGroupFromDb =
                adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(result.get(0).getResult())).get(0);
        assertThat(adGroupFromDb.getName(), equalTo(NEW_NAME));
    }

    @Test
    @Description("в снэпшоте RUSSIA, KYIV не в России, поэтому ошибка")
    public void updateSpecificAdGroup_WrongGeoSpecificGroup_ValidationError() {
        CpmYndxFrontpageAdGroup adGroup = createSpecificAdGroup();
        ModelChanges<AdGroup> modelChanges = (ModelChanges) modelChangesWithGeo(adGroup, List.of(KYIV_REGION_ID));
        var updateOperation = createUpdateOperation(singletonList(modelChanges));

        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(CpmYndxFrontpageAdGroup.GEO)), DefectIds.INVALID_VALUE)));
    }

    @Test
    @Description("в снэпшоте RUSSIA, в специфической группе разрешается сужать аудиторию даже до Урюпинска")
    public void updateSpecificAdGroup_TownInGeoSpecificGroup_Ok() {
        CpmYndxFrontpageAdGroup adGroup = createSpecificAdGroup();
        List<Long> newGeo = List.of(URYUPINSK_REGION_ID, CENTRAL_DISTRICT);
        ModelChanges<AdGroup> modelChanges = (ModelChanges) modelChangesWithGeo(adGroup, newGeo);
        var updateOperation = createUpdateOperation(singletonList(modelChanges));

        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        AdGroup adGroupFromDb =
                adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(result.get(0).getResult())).get(0);
        assertThat(adGroupFromDb.getGeo(), equalTo(newGeo));
    }

    @Test
    @Description("если priority передан - будет ошибка валидации")
    public void priorityInModelChanges_Error() {
        CpmYndxFrontpageAdGroup adGroup = createDefaultAdGroup();
        ModelChanges<AdGroup> modelChanges = (ModelChanges) modelChangesWithPriority(adGroup, PRIORITY_DEFAULT);
        var updateOperation = createUpdateOperation(singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(0),
                field(CpmYndxFrontpageAdGroup.PRIORITY)), DefectIds.FORBIDDEN_TO_CHANGE)));
    }

    @Test
    @Description("при сохранении прайсовых групп всегда используется Российское гео-дерево")
    public void russianGeoTreeUsedForRussianClient() {
        var client = steps.clientSteps().createClient(defaultClient().withCountryRegionId(RUSSIA));
        var pricePackage = createPricePackageWithRussiaGeoForClient(client);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackage);

        var adGroup = activeSpecificAdGroupForPriceSales(campaign)
                .withGeo(List.of(CENTRAL_DISTRICT));
        steps.adGroupSteps().createAdGroupRaw(adGroup, client);

        var changes = ModelChanges.build((AdGroup) adGroup, AdGroup.GEO, List.of(RUSSIA));
        var updateOperation = createUpdateOperation(client, geoTree, List.of(changes));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        var adGroupFromDb = adGroupRepository.getAdGroups(client.getShard(), List.of(adGroup.getId())).get(0);
        assertThat(adGroupFromDb.getGeo(), equalTo(List.of(RUSSIA, CRIMEA_PROVINCE)));
    }

    @Test
    @Description("при сохранении прайсовых групп всегда используется Российское гео-дерево")
    public void russianGeoTreeUsedForNonRussianClient() {
        var client = steps.clientSteps().createClient(defaultClient().withCountryRegionId(UKRAINE));
        var pricePackage = createPricePackageWithRussiaGeoForClient(client);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackage);

        var adGroup = activeSpecificAdGroupForPriceSales(campaign)
                .withGeo(List.of(CENTRAL_DISTRICT));
        steps.adGroupSteps().createAdGroupRaw(adGroup, client);

        var changes = ModelChanges.build((AdGroup) adGroup, AdGroup.GEO, List.of(RUSSIA));
        var updateOperation = createUpdateOperation(client, geoTree, List.of(changes));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        var adGroupFromDb = adGroupRepository.getAdGroups(client.getShard(), List.of(adGroup.getId())).get(0);
        assertThat(adGroupFromDb.getGeo(), equalTo(List.of(RUSSIA, CRIMEA_PROVINCE)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void forceSaveDraftForbidden() {
        CpmYndxFrontpageAdGroup adGroup = createDefaultAdGroup();
        ModelChanges<AdGroup> modelChanges = (ModelChanges) modelChangesWithName(adGroup, NEW_NAME);

        AdGroupsUpdateOperation updateOperation = adGroupsUpdateOperationFactory.newInstance(
                Applicability.FULL,
                List.of(modelChanges),
                AdGroupUpdateOperationParams.builder()
                        .withModerationMode(ModerationMode.FORCE_SAVE_DRAFT)
                        .withValidateInterconnections(true)
                        .build(),
                geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                clientInfo.getUid(),
                clientInfo.getClientId(),
                clientInfo.getShard());
        updateOperation.prepareAndApply();
    }

    private PricePackage createPricePackageWithRussiaGeoForClient(ClientInfo client) {
        var pricePackage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom())
                .withClients(List.of(allowedPricePackageClient(client)));
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_COUNTRY);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        return pricePackage;
    }

    private CpmYndxFrontpageAdGroup createDefaultAdGroup() {
        CpmYndxFrontpageAdGroup adGroup = activeDefaultAdGroupForPriceSales(cpmPriceCampaign);
        adGroup.withGeo(List.of(RUSSIA, CRIMEA_PROVINCE));
        steps.adGroupSteps().createAdGroupRaw(adGroup, clientInfo);
        return adGroup;
    }

    private CpmYndxFrontpageAdGroup createSpecificAdGroup() {
        CpmYndxFrontpageAdGroup adGroup = activeSpecificAdGroupForPriceSales(cpmPriceCampaign);
        adGroup.withGeo(List.of(RUSSIA, CRIMEA_PROVINCE));
        steps.adGroupSteps().createAdGroupRaw(adGroup, clientInfo);
        return adGroup;
    }

    private AdGroupsUpdateOperation createUpdateOperation(List<ModelChanges<AdGroup>> modelChangesList) {
        return super.createUpdateOperation(clientInfo, geoTree, modelChangesList);
    }

}
