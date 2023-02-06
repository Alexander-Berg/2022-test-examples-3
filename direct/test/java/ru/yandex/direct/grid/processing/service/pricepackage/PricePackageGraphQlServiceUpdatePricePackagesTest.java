package ru.yandex.direct.grid.processing.service.pricepackage;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import jdk.jfr.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageCampaignOptions;
import ru.yandex.direct.core.entity.pricepackage.model.ShowsFrequencyLimit;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdInventoryType;
import ru.yandex.direct.grid.processing.model.cliententity.GdCreativeType;
import ru.yandex.direct.grid.processing.model.pricepackage.GdAvailableAdGroupType;
import ru.yandex.direct.grid.processing.model.pricepackage.GdPriceAllowedCreativeTemplates;
import ru.yandex.direct.grid.processing.model.pricepackage.GdPricePackageBidModifiers;
import ru.yandex.direct.grid.processing.model.pricepackage.GdViewType;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdMutationPriceRetargetingCondition;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdMutationTargetingsCustom;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdMutationTargetingsFixed;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdPricePackageClientInput;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackages;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackagesItem;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackagesPayload;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackagesPayloadItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.creative.model.CreativeType.BANNERSTORAGE;
import static ru.yandex.direct.core.entity.creative.model.CreativeType.CPM_VIDEO_CREATIVE;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.defects.PricePackageDefects.geoExpandedIsEmpty;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_RETARGETING_CONDITION;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.anotherPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.pricepackage.converter.PricePackageDataConverter.toGdAllowedDomains;
import static ru.yandex.direct.grid.processing.service.pricepackage.converter.PricePackageDataConverter.toGdMutationRetargetingCondition;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.regions.Region.SNG_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThan;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PricePackageGraphQlServiceUpdatePricePackagesTest {

    private static final String MUTATION_HANDLE = "updatePricePackages";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    updatedItems {\n"
            + "      id\n"
            + "    }\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "      warnings {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final GdMutationPriceRetargetingCondition GD_MUTATION_PRICE_RETARGETING_CONDITION =
            toGdMutationRetargetingCondition(DEFAULT_RETARGETING_CONDITION);

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private PricePackageRepository repository;

    @Autowired
    private Steps steps;

    @Autowired
    PricePackageDataService pricePackageDataService;

    private ClientInfo clientInfo;
    private User operator;

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createClient(defaultClient(RbacRole.SUPER)
                .withCountryRegionId(RUSSIA));
        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);
        steps.pricePackageSteps().clearPricePackages();
        steps.sspPlatformsSteps().addSspPlatforms(defaultPricePackage().getAllowedSsp());
        steps.sspPlatformsSteps().addSspPlatforms(anotherPricePackage().getAllowedSsp());
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void updatePricePackages_OneValidAndOneInvalidUpdate() {
        PricePackage pricePackage1 = steps.pricePackageSteps().createNewPricePackage().getPricePackage();
        PricePackage pricePackage2 = steps.pricePackageSteps().createNewPricePackage().getPricePackage();
        PricePackage expected = anotherPricePackage()
                .withId(pricePackage1.getId())
                .withAuctionPriority(12345L)
                .withStatusApprove(pricePackage1.getStatusApprove())
                .withIsPublic(!pricePackage1.getIsPublic())
                .withClients(List.of());

        var validUpdate = new GdUpdatePricePackagesItem()
                .withId(pricePackage1.getId())
                .withTitle(expected.getTitle())
                .withTrackerUrl(expected.getTrackerUrl())
                .withPrice(expected.getPrice())
                .withAuctionPriority(expected.getAuctionPriority())
                .withCurrency(expected.getCurrency())
                .withOrderVolumeMin(expected.getOrderVolumeMin())
                .withOrderVolumeMax(expected.getOrderVolumeMax())
                .withTargetingsFixed(pricePackageDataService.toGdMutationTargetingsFixed(expected.getTargetingsFixed()))
                .withTargetingsCustom(pricePackageDataService.toGdMutationTargetingsCustom(expected.getTargetingsCustom()))
                .withDateStart(expected.getDateStart())
                .withDateEnd(expected.getDateEnd())
                .withIsPublic(expected.getIsPublic())
                .withIsSpecial(expected.getIsSpecial())
                .withLastSeenUpdateTime(pricePackage1.getLastUpdateTime())
                .withCampaignAutoApprove(expected.getCampaignAutoApprove())
                .withAllowedPageIds(expected.getAllowedPageIds())
                .withAllowedDomains(toGdAllowedDomains(expected.getAllowedSsp(), expected.getAllowedDomains()))
                .withAvailableAdGroupTypes(Set.of(GdAvailableAdGroupType.CPM_PRICE_VIDEO))
                .withBidModifiers(new GdPricePackageBidModifiers().withBidModifierInventoryAll(Set.of(GdInventoryType.INBANNER)).withBidModifierInventoryFixed(Set.of(GdInventoryType.INBANNER)))
                .withAllowBrandSafety(false)
                .withAllowDisabledVideoPlaces(true)
                .withAllowedCreativeTemplates(new GdPriceAllowedCreativeTemplates().withCreativeTemplateIds(Map.of(
                        GdCreativeType.BANNERSTORAGE, expected.getAllowedCreativeTemplates().get(BANNERSTORAGE),
                        GdCreativeType.CPM_VIDEO_CREATIVE,
                        expected.getAllowedCreativeTemplates().get(CPM_VIDEO_CREATIVE))))
                .withCategoryId(expected.getCategoryId());

        var invalidUpdate = new GdUpdatePricePackagesItem()
                .withId(pricePackage2.getId())
                .withOrderVolumeMax(-1L)
                .withLastSeenUpdateTime(pricePackage2.getLastUpdateTime());
        List<GdUpdatePricePackagesItem> items = asList(invalidUpdate, validUpdate);

        GdUpdatePricePackagesPayload payload = updatePricePackagesGraphQl(items);

        Map<Long, PricePackage> actual = repository.getPricePackages(
                List.of(pricePackage1.getId(), pricePackage2.getId()));
        assertThat(actual.size()).isEqualTo(2);
        assertThat(actual.get(pricePackage1.getId()))
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(getDefaultCompareStrategy())));
        assertThat(actual.get(pricePackage2.getId()))
                .is(matchedBy(beanDiffer(pricePackage2).useCompareStrategy(
                        DefaultCompareStrategies.allFields()
                                .forFields(newPath("eshow")).useDiffer(new BigDecimalDiffer()))));
        GdUpdatePricePackagesPayload expectedPayload = new GdUpdatePricePackagesPayload()
                .withUpdatedItems(asList(
                        null,
                        new GdUpdatePricePackagesPayloadItem().withId(pricePackage1.getId())))
                .withValidationResult(toGdValidationResult(
                        toGdDefect(path(field(GdUpdatePricePackages.UPDATE_ITEMS), index(0),
                                        field(GdUpdatePricePackagesItem.ORDER_VOLUME_MAX)),
                                greaterThan(0))));
        DefaultCompareStrategy validationCompareStrategy = DefaultCompareStrategies
                .allFieldsExcept(newPath("validationResult", "errors", "0", "params"));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload).useCompareStrategy(validationCompareStrategy)));
    }

    /**
     * У заапрувленного пакета должна появиться возможность изменения клиентов
     */
    @Test
    public void updatePricePackages_ClientsCanBeChanged() {
        PricePackage approvedPricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withStatusApprove(StatusApprove.YES)
                .withIsPublic(false)
        ).getPricePackage();
        Long pricePackageId = approvedPricePackage.getId();

        GdUpdatePricePackagesItem update = new GdUpdatePricePackagesItem()
                .withId(pricePackageId)
                .withClients(List.of(new GdPricePackageClientInput()
                        .withLogin(clientInfo.getLogin())
                        .withIsAllowed(true)))
                .withLastSeenUpdateTime(approvedPricePackage.getLastUpdateTime());
        GdUpdatePricePackagesPayload payload = updatePricePackagesGraphQl(List.of(update));

        PricePackage expectedPricePackage = approvedPricePackage
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        Map<Long, PricePackage> actual = repository.getPricePackages(List.of(pricePackageId));
        assertThat(actual.size()).isEqualTo(1);
        assertThat(actual.get(pricePackageId))
                .is(matchedBy(beanDiffer(expectedPricePackage)
                        .useCompareStrategy(getDefaultCompareStrategy())));
        GdUpdatePricePackagesPayload expectedPayload = new GdUpdatePricePackagesPayload()
                .withUpdatedItems(asList(
                        new GdUpdatePricePackagesPayloadItem().withId(pricePackageId)));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void updatePricePackages_ClientsCanBeChangedByClientId() {
        PricePackage approvedPricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withStatusApprove(StatusApprove.YES)
                .withIsPublic(false)
        ).getPricePackage();
        Long pricePackageId = approvedPricePackage.getId();

        GdUpdatePricePackagesItem update = new GdUpdatePricePackagesItem()
                .withId(pricePackageId)
                .withClients(List.of(new GdPricePackageClientInput()
                        .withClientId(clientInfo.getClientId().asLong())
                        .withIsAllowed(true)))
                .withLastSeenUpdateTime(approvedPricePackage.getLastUpdateTime());
        GdUpdatePricePackagesPayload payload = updatePricePackagesGraphQl(List.of(update));

        PricePackage expectedPricePackage = approvedPricePackage
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        Map<Long, PricePackage> actual = repository.getPricePackages(List.of(pricePackageId));
        assertThat(actual.size()).isEqualTo(1);
        assertThat(actual.get(pricePackageId))
                .is(matchedBy(beanDiffer(expectedPricePackage)
                        .useCompareStrategy(getDefaultCompareStrategy())));
        GdUpdatePricePackagesPayload expectedPayload = new GdUpdatePricePackagesPayload()
                .withUpdatedItems(asList(
                        new GdUpdatePricePackagesPayloadItem().withId(pricePackageId)));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void updatePricePackages_NonExistingClientLogin_ValidationError() {
        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withStatusApprove(StatusApprove.YES)
                .withIsPublic(false)
        ).getPricePackage();
        Long pricePackageId = pricePackage.getId();

        GdUpdatePricePackagesItem update = new GdUpdatePricePackagesItem()
                .withId(pricePackageId)
                .withClients(List.of(new GdPricePackageClientInput()
                        .withLogin("non-existing-login")
                        .withIsAllowed(true)))
                .withLastSeenUpdateTime(pricePackage.getLastUpdateTime());
        GdUpdatePricePackagesPayload payload = updatePricePackagesGraphQl(List.of(update));

        GdUpdatePricePackagesPayload expectedPayload = new GdUpdatePricePackagesPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(toGdValidationResult(toGdDefect(
                        path(field(GdUpdatePricePackages.UPDATE_ITEMS), index(0),
                                field(GdUpdatePricePackagesItem.CLIENTS), index(0),
                                field(GdPricePackageClientInput.LOGIN)),
                        notNull())));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    @Description("После разворачивания geo пустой geoExpanded в targetingsFixed - ошибка")
    public void updatePricePackages_TargetingsFixedGeoExpandedNull_Error() {
        PricePackage pricePackage = steps.pricePackageSteps().createNewPricePackage().getPricePackage();
        Long pricePackageId = pricePackage.getId();

        GdUpdatePricePackagesItem update = new GdUpdatePricePackagesItem()
                .withId(pricePackageId)
                .withTargetingsFixed(new GdMutationTargetingsFixed()
                        // В нашем geo деревере СНГ не имеет округов, поэтому geoExpanded будет пустым
                        .withGeo(List.of(SNG_REGION_ID))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(null)
                        .withViewTypes(List.of(GdViewType.DESKTOP, GdViewType.MOBILE, GdViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(new GdMutationTargetingsCustom()
                        .withRetargetingCondition(GD_MUTATION_PRICE_RETARGETING_CONDITION))
                .withLastSeenUpdateTime(pricePackage.getLastUpdateTime());
        GdUpdatePricePackagesPayload payload = updatePricePackagesGraphQl(List.of(update));

        GdUpdatePricePackagesPayload expectedPayload = new GdUpdatePricePackagesPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(toGdValidationResult(
                        toGdDefect(path(field(GdUpdatePricePackages.UPDATE_ITEMS), index(0),
                                        field(PricePackage.TARGETINGS_FIXED),
                                        field(TargetingsFixed.GEO.name())),
                                geoExpandedIsEmpty())));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    @Description("После разворачивания geo пустой geoExpanded в targetingsCustom - ошибка")
    public void updatePricePackages_TargetingsCustomGeoExpandedNull_Error() {
        PricePackage pricePackage = steps.pricePackageSteps().createNewPricePackage().getPricePackage();
        Long pricePackageId = pricePackage.getId();

        GdUpdatePricePackagesItem update = new GdUpdatePricePackagesItem()
                .withId(pricePackageId)
                .withTargetingsFixed(new GdMutationTargetingsFixed()
                        .withViewTypes(List.of(GdViewType.DESKTOP, GdViewType.MOBILE, GdViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(new GdMutationTargetingsCustom()
                        // В нашем geo деревере СНГ не имеет округов, поэтому geoExpanded будет пустым
                        .withGeo(List.of(SNG_REGION_ID))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(null)
                        .withRetargetingCondition(GD_MUTATION_PRICE_RETARGETING_CONDITION))
                .withLastSeenUpdateTime(pricePackage.getLastUpdateTime());
        GdUpdatePricePackagesPayload payload = updatePricePackagesGraphQl(List.of(update));

        GdUpdatePricePackagesPayload expectedPayload = new GdUpdatePricePackagesPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(toGdValidationResult(
                        toGdDefect(path(field(GdUpdatePricePackages.UPDATE_ITEMS), index(0),
                                        field(PricePackage.TARGETINGS_CUSTOM),
                                        field(TargetingsCustom.GEO.name())),
                                geoExpandedIsEmpty())));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void updatePricePackages_Archive() {
        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withIsArchived(false)
        ).getPricePackage();
        Long pricePackageId = pricePackage.getId();

        GdUpdatePricePackagesItem update = new GdUpdatePricePackagesItem()
                .withId(pricePackageId)
                .withIsArchived(true)
                .withLastSeenUpdateTime(pricePackage.getLastUpdateTime());
        GdUpdatePricePackagesPayload payload = updatePricePackagesGraphQl(List.of(update));

        GdUpdatePricePackagesPayload expectedPayload = new GdUpdatePricePackagesPayload()
                .withUpdatedItems(asList(
                        new GdUpdatePricePackagesPayloadItem().withId(pricePackageId)));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));

        PricePackage expectedPricePackage = pricePackage
                .withIsArchived(true);
        Map<Long, PricePackage> actual = repository.getPricePackages(List.of(pricePackageId));
        assertThat(actual.size()).isEqualTo(1);
        assertThat(actual.get(pricePackageId))
                .is(matchedBy(beanDiffer(expectedPricePackage)
                        .useCompareStrategy(getDefaultCompareStrategy())));
    }

    @Test
    public void updatePricePackages_Unarchive() {
        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withIsArchived(true)
        ).getPricePackage();
        Long pricePackageId = pricePackage.getId();

        GdUpdatePricePackagesItem update = new GdUpdatePricePackagesItem()
                .withId(pricePackageId)
                .withIsArchived(false)
                .withLastSeenUpdateTime(pricePackage.getLastUpdateTime());
        GdUpdatePricePackagesPayload payload = updatePricePackagesGraphQl(List.of(update));

        GdUpdatePricePackagesPayload expectedPayload = new GdUpdatePricePackagesPayload()
                .withUpdatedItems(asList(
                        new GdUpdatePricePackagesPayloadItem().withId(pricePackageId)));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));

        PricePackage expectedPricePackage = pricePackage
                .withIsArchived(false);
        Map<Long, PricePackage> actual = repository.getPricePackages(List.of(pricePackageId));
        assertThat(actual.size()).isEqualTo(1);
        assertThat(actual.get(pricePackageId))
                .is(matchedBy(beanDiffer(expectedPricePackage)
                        .useCompareStrategy(getDefaultCompareStrategy())));
    }

    private GdUpdatePricePackagesPayload updatePricePackagesGraphQl(List<GdUpdatePricePackagesItem> items) {
        GdUpdatePricePackages request = new GdUpdatePricePackages().withUpdateItems(items);
        String query = String.format(MUTATION_TEMPLATE, MUTATION_HANDLE, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_HANDLE);
        return GraphQlJsonUtils.convertValue(data.get(MUTATION_HANDLE), GdUpdatePricePackagesPayload.class);
    }

    @Test
    public void updatePricePackagesCampaignOptions() {
        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withIsArchived(false)
                .withCampaignOptions(new PricePackageCampaignOptions().withAllowBrandSafety(true))
        ).getPricePackage();
        Long pricePackageId = pricePackage.getId();
        GdUpdatePricePackagesItem update = new GdUpdatePricePackagesItem()
                .withId(pricePackageId)
                .withIsArchived(true)
                .withLastSeenUpdateTime(pricePackage.getLastUpdateTime());

        updatePricePackagesGraphQl(List.of(update));

        assertThat(repository.getPricePackages(List.of(pricePackageId)).get(pricePackageId)
                .getCampaignOptions().getAllowBrandSafety()).isTrue();
    }

    @Test
    @Description("На обновлении пакета null в поле showsFrequencyLimit трактовать это как удаление ограничения")
    public void delete_showsFrequencyLimit() {
        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withIsArchived(false)
                .withCampaignOptions(new PricePackageCampaignOptions()
                        .withAllowBrandSafety(true)
                        .withShowsFrequencyLimit(
                                new ShowsFrequencyLimit()
                                        .withFrequencyLimit(5)
                        )
                )
        ).getPricePackage();
        Long pricePackageId = pricePackage.getId();
        assertThat(repository.getPricePackages(List.of(pricePackageId)).get(pricePackageId)
                .getCampaignOptions().getShowsFrequencyLimit().getFrequencyLimit()).isEqualTo(5);
        GdUpdatePricePackagesItem update = new GdUpdatePricePackagesItem()
                .withId(pricePackageId)
                .withIsArchived(true)
                .withLastSeenUpdateTime(pricePackage.getLastUpdateTime())
                .withAllowBrandSafety(true)
                .withShowsFrequencyLimit(null);

        updatePricePackagesGraphQl(List.of(update));

        assertThat(repository.getPricePackages(List.of(pricePackageId)).get(pricePackageId)
                .getCampaignOptions().getShowsFrequencyLimit()).isNull();
    }

    private DefaultCompareStrategy getDefaultCompareStrategy() {
        return DefaultCompareStrategies
                .allFields()
                .forFields(newPath("price")).useDiffer(new BigDecimalDiffer())
                .forFields(newPath("eshow")).useDiffer(new BigDecimalDiffer())
                .forFields(newPath("lastUpdateTime")).useMatcher(approximatelyNow(ZoneOffset.UTC));
    }

}
