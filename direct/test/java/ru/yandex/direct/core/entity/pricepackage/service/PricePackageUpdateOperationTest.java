package ru.yandex.direct.core.entity.pricepackage.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.jfr.Description;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageClient;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestFullGoals;
import ru.yandex.direct.core.testing.data.TestPricePackages;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.creative.model.CreativeType.CPM_VIDEO_CREATIVE;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.defects.PricePackageDefects.creativeTemplatesCanOnlyExpand;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.defects.PricePackageDefects.dateEndCanOnlyExpand;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.anotherPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.CRIMEA_PROVINCE;
import static ru.yandex.direct.core.testing.data.TestRegions.FAR_EASTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTHWESTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTH_CAUCASIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.SOUTH_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.UKRAINE;
import static ru.yandex.direct.core.testing.data.TestRegions.URAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.operation.Applicability.PARTIAL;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_PROVINCE;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PricePackageUpdateOperationTest {

    private static final String NEW_TITLE = "NEW_TITLE";
    private static final long NEW_AUCTION_PRIORITY = 98765L;
    private static final BigDecimal NEW_PRICE = BigDecimal.valueOf(10L);
    private static final BigDecimal NEW_ESHOW = BigDecimal.valueOf(1.98);
    private static final List<Long> NEW_ALLOWED_PAGE_IDS = List.of(12345L, 3456L);
    private static final LocalDate NEW_DATE_END = LocalDate.of(2030, 1, 1).plusDays(5);
    private static final Boolean NEW_IS_DRAFT_APPROVE_ALLOWED = true;

    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private PricePackageRepository repository;
    @Autowired
    private PricePackageUpdateOperationFactory updateOperationFactory;

    private Long pricePackageId;
    private Long pricePackageId2;
    private Long approvedPricePackageId;
    private PricePackage pricePackage;
    private PricePackage pricePackage2;
    private PricePackage approvedPricePackage;

    private User operator;

    @Before
    public void before() {
        var allGoals = TestFullGoals.defaultCryptaGoals();
        testCryptaSegmentRepository.addAll(allGoals);
        steps.sspPlatformsSteps().addSspPlatforms(defaultPricePackage().getAllowedSsp());
        steps.sspPlatformsSteps().addSspPlatforms(anotherPricePackage().getAllowedSsp());

        pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage())
                .getPricePackage();
        pricePackage2 = steps.pricePackageSteps().createPricePackage(anotherPricePackage())
                .getPricePackage();
        approvedPricePackage = steps.pricePackageSteps().createPricePackage(
                        approvedPricePackage()
                                .withCurrency(CurrencyCode.RUB))
                .getPricePackage();
        pricePackageId = pricePackage.getId();
        pricePackageId2 = pricePackage2.getId();
        approvedPricePackageId = approvedPricePackage.getId();
    }

    @Test
    public void changeOnePackageOk() {
        useSuper();
        ModelChanges<PricePackage> modelChanges = modelChangesWithValidTitle(pricePackageId);

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(pricePackage.getLastUpdateTime()));

        assertThat("результат операции должен быть положительный", massResult, isFullySuccessful());
        verifyTitleChanged(massResult.getResult().get(0), pricePackage);
    }

    @Test
    public void titleIsTrimmed() {
        useSuper();
        ModelChanges<PricePackage> modelChanges = ModelChanges.build(pricePackageId, PricePackage.class,
                PricePackage.TITLE, " ABC DEF ");

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(pricePackage.getLastUpdateTime()));

        assertThat("результат операции должен быть положительный", massResult, isFullySuccessful());
        verifyPricePackageChanged(massResult.getResult().get(0), pricePackage.withTitle("ABC DEF"));
    }

    @Test
    public void cmp_video_frontpage_field() {
        useSuper();
        PricePackage clientPricePackage = defaultPricePackage();
        clientPricePackage
                .withIsFrontpage(true)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withAllowedCreativeTemplates(null)
                .withAllowedOrderTags(List.of("mytag"));
        clientPricePackage.getTargetingsFixed().withAllowExpandedDesktopCreative(false);
        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(
                clientPricePackage).getPricePackage();
        ModelChanges<PricePackage> modelChanges = ModelChanges.build(pricePackage.getId(), PricePackage.class,
                PricePackage.ALLOWED_ORDER_TAGS, List.of("newtag"));
        modelChanges.process(List.of("newtag"), PricePackage.ALLOWED_TARGET_TAGS);
        modelChanges.process(null, PricePackage.ALLOWED_CREATIVE_TEMPLATES);

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(pricePackage.getLastUpdateTime()));

        assertThat("результат операции должен быть положительный", massResult, isFullySuccessful());
        PricePackage fromDb = repository.getPricePackages(List.of(pricePackage.getId())).get(pricePackage.getId());
        Assertions.assertThat(fromDb.getAllowedTargetTags()).contains("portal-trusted", "newtag");
        Assertions.assertThat(fromDb.getAllowedOrderTags()).contains("portal-trusted", "newtag");
        Assertions.assertThat(fromDb.getAllowedCreativeTemplates().get(CPM_VIDEO_CREATIVE)).contains(406L);
    }

    @Test
    public void changeTwoPackagesOk() {
        useSuper();
        ModelChanges<PricePackage> modelChanges = modelChangesWithValidTitle(pricePackageId);
        ModelChanges<PricePackage> modelChanges2 = modelChangesWithValidPrice(pricePackageId2);

        var massResult = prepareAndApplyOperation(List.of(modelChanges, modelChanges2),
                List.of(pricePackage.getLastUpdateTime(), pricePackage2.getLastUpdateTime()));

        assertThat("результат операции должен быть положительный", massResult, isFullySuccessful());
        verifyTitleChanged(massResult.getResult().get(0), pricePackage);
        verifyPriceChanged(massResult.getResult().get(1), pricePackage2);
    }

    @Test
    public void changeClientIdsOk() {
        useSuper();
        var client = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        ModelChanges<PricePackage> modelChanges = modelChangesWithClients(approvedPricePackageId,
                List.of(allowedPricePackageClient(client)));

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(approvedPricePackage.getLastUpdateTime()));

        assertThat("результат операции должен быть положительный", massResult, isFullySuccessful());
        verifyPricePackageChanged(massResult.getResult().get(0),
                approvedPricePackage.withClients(List.of(allowedPricePackageClient(client))));
    }

    @Test
    public void changeFieldWhichCanBeChangedApprovedOk() {
        useSuper();
        var client = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        ModelChanges<PricePackage> modelChanges =
                modelChangesWithValidForChangeFieldsOnApprovedPackage(approvedPricePackageId);

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(approvedPricePackage.getLastUpdateTime()));

        assertThat("результат операции должен быть положительный", massResult, isFullySuccessful());
        verifyPricePackageChanged(massResult.getResult().get(0),
                updatePricePackageWithValidForChangeFieldsOnApprovedPackage(approvedPricePackage));
    }

    @Test
    public void invalidChangeDateEndInApprovedPricePackageOk() {
        useSuper();
        ModelChanges<PricePackage> modelChanges =
                modelChangesWithValidForChangeFieldsOnApprovedPackage(approvedPricePackageId);
        modelChanges.processNotNull(approvedPricePackage.getDateEnd().minusDays(3), PricePackage.DATE_END);

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(approvedPricePackage.getLastUpdateTime()));

        assertThat("результат операции должен быть положительный с ошибкой валидации",
                massResult, isSuccessful(false));

        var result = massResult.getResult().get(0);
        Assertions.assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(PricePackage.DATE_END)), dateEndCanOnlyExpand()))));

    }

    @Test
    public void invalidChangeIsCpdInApprovedPricePackageOk() {
        useSuper();
        ModelChanges<PricePackage> modelChanges =
                modelChangesWithValidForChangeFieldsOnApprovedPackage(approvedPricePackageId);
        modelChanges.processNotNull(!approvedPricePackage.getIsCpd(), PricePackage.IS_CPD);

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(approvedPricePackage.getLastUpdateTime()));

        assertThat("результат операции должен быть положительный с ошибкой валидации",
                massResult, isSuccessful(false));

        var result = massResult.getResult().get(0);
        Assertions.assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                validationError(DefectIds.FORBIDDEN_TO_CHANGE))));
    }

    @Test
    public void invalidChangeCreativeTemplatesInApprovedPricePackageOk() {
        useSuper();

        Map<CreativeType, List<Long>> newCreativeTypes =
                new HashMap<>(TestPricePackages.DEFAULT_ALLOWED_CREATIVE_TYPES);
        // оставляем только первый элемент, остальные удаляем, тем самым сужаем список шаблонов
        newCreativeTypes.put(CreativeType.BANNERSTORAGE,
                List.of(newCreativeTypes.get(CreativeType.BANNERSTORAGE).get(0)));

        ModelChanges<PricePackage> modelChanges =
                modelChangesWithValidForChangeFieldsOnApprovedPackage(approvedPricePackageId);
        modelChanges.processNotNull(newCreativeTypes, PricePackage.ALLOWED_CREATIVE_TEMPLATES);

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(approvedPricePackage.getLastUpdateTime()));

        assertThat("результат операции должен быть положительный с ошибкой валидации",
                massResult, isSuccessful(false));

        var result = massResult.getResult().get(0);
        Assertions.assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(PricePackage.ALLOWED_CREATIVE_TEMPLATES)),
                        creativeTemplatesCanOnlyExpand()))));

    }

    @Test
    public void approvedPackageShouldNotBeChanged() {
        useSuper();
        var modelChanges = modelChangesWithValidPrice(approvedPricePackageId);

        var massResult = prepareAndApplyOperation(List.of(modelChanges),
                List.of(approvedPricePackage.getLastUpdateTime()));
        assertThat("результат операции должен быть положительный с ошибкой валидации",
                massResult, isSuccessful(false));

        var result = massResult.getResult().get(0);
        Assertions.assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                validationError(path(), forbiddenToChange()))));
    }

    @Test
    public void approvedPackageTitleCanBeChanged() {
        useSuper();
        var modelChanges = modelChangesWithValidTitle(approvedPricePackageId);

        var massResult = prepareAndApplyOperation(List.of(modelChanges),
                List.of(approvedPricePackage.getLastUpdateTime()));
        assertThat("результат операции должен быть положительный", massResult, isFullySuccessful());

        verifyTitleChanged(massResult.getResult().get(0), approvedPricePackage);
    }

    @Test
    public void approvedAuctionPriorityCanBeChanged() {
        useSuper();
        var modelChanges = modelChangesWithValidAuctionPriority(approvedPricePackageId);

        var massResult = prepareAndApplyOperation(List.of(modelChanges),
                List.of(approvedPricePackage.getLastUpdateTime()));
        assertThat("результат операции должен быть положительный", massResult, isFullySuccessful());

        verifyAuctionPriorityChanged(massResult.getResult().get(0), approvedPricePackage);
    }

    @Test
    public void geoExpandedUpdatedOnGeoChange() {
        useSuper();
        assumeThat(pricePackage.getTargetingsFixed().getGeoExpanded(), notNullValue());
        assumeThat(pricePackage.getTargetingsCustom().getGeoExpanded(), nullValue());

        var modelChanges = new ModelChanges<>(pricePackageId, PricePackage.class)
                .process(pricePackage.getTargetingsFixed()
                        .withGeo(null)
                        .withGeoType(null), PricePackage.TARGETINGS_FIXED)
                .process(pricePackage.getTargetingsCustom()
                        .withGeo(List.of(RUSSIA, -URAL_DISTRICT))
                        .withGeoType(REGION_TYPE_DISTRICT), PricePackage.TARGETINGS_CUSTOM);

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(pricePackage.getLastUpdateTime()));
        var pricePackageAfterUpdate = repository.getPricePackages(List.of(pricePackageId)).get(pricePackageId);

        assertThat("результат операции должен быть положительный", massResult, isFullySuccessful());
        Assertions.assertThat(pricePackageAfterUpdate.getTargetingsFixed().getGeo()).isNull();
        Assertions.assertThat(pricePackageAfterUpdate.getTargetingsFixed().getGeoExpanded()).isNull();
        Assertions.assertThat(pricePackageAfterUpdate.getTargetingsCustom().getGeo()).containsExactlyInAnyOrder(
                RUSSIA, -URAL_DISTRICT);
        Assertions.assertThat(pricePackageAfterUpdate.getTargetingsCustom().getGeoExpanded()).containsExactlyInAnyOrder(
                NORTHWESTERN_DISTRICT, CENTRAL_DISTRICT, SOUTH_DISTRICT,
                SIBERIAN_DISTRICT, FAR_EASTERN_DISTRICT, VOLGA_DISTRICT, NORTH_CAUCASIAN_DISTRICT);
    }

    @Test
    public void russianGeoTreeUsed() {
        useSuper();
        assumeThat(pricePackage.getTargetingsFixed().getGeoExpanded(), notNullValue());
        assumeThat(pricePackage.getTargetingsCustom().getGeoExpanded(), nullValue());

        var modelChanges = new ModelChanges<>(pricePackageId, PricePackage.class)
                .process(pricePackage.getTargetingsFixed()
                        .withGeo(List.of(RUSSIA))
                        .withGeoType(REGION_TYPE_PROVINCE), PricePackage.TARGETINGS_FIXED);

        var client = steps.clientSteps().createClient(defaultClient()
                .withRole(RbacRole.SUPER)
                .withCountryRegionId(UKRAINE));

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(pricePackage.getLastUpdateTime()), client.getChiefUserInfo().getUser());
        var pricePackageAfterUpdate = repository.getPricePackages(List.of(pricePackageId)).get(pricePackageId);

        assertThat("результат операции должен быть положительный", massResult, isFullySuccessful());
        Assertions.assertThat(pricePackageAfterUpdate.getTargetingsFixed().getGeoExpanded()).contains(CRIMEA_PROVINCE);
    }

    @Test
    @Description("Проверяем что клиент, у которого getCanManagePricePackage=true может работать с пакетом, даже если " +
            "не супер")
    public void clientCanManagePricePackage() {
        operator = steps.userSteps().createUser(generateNewUser()
                        .withCanManagePricePackages(true))
                .getUser();
        ModelChanges<PricePackage> modelChanges = modelChangesWithValidTitle(pricePackageId);

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(pricePackage.getLastUpdateTime()));

        assertThat("результат операции должен быть положительный", massResult, isFullySuccessful());
        verifyTitleChanged(massResult.getResult().get(0), pricePackage);
    }

    @Test
    @Description("Проверяем что клиент, у которого getCanManagePricePackage=false не может работать с пакетом")
    public void clientCanNotManagePricePackage() {
        operator = steps.userSteps().createUser(generateNewUser()
                        .withCanManagePricePackages(false))
                .getUser();
        ModelChanges<PricePackage> modelChanges = modelChangesWithValidPrice(pricePackageId);

        var massResult = prepareAndApplyOperation(singletonList(modelChanges),
                singletonList(pricePackage.getLastUpdateTime()));

        var result = massResult.getResult().get(0);
        Assertions.assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                validationError(path(), forbiddenToChange()))));
    }

    private void useSuper() {
        operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER).getChiefUserInfo().getUser();
    }

    private void verifyTitleChanged(Result<Long> result, PricePackage basePricePackage) {
        verifyPricePackageChanged(result, basePricePackage.withTitle(NEW_TITLE));
    }

    private void verifyAuctionPriorityChanged(Result<Long> result, PricePackage basePricePackage) {
        verifyPricePackageChanged(result, basePricePackage.withAuctionPriority(NEW_AUCTION_PRIORITY));
    }

    private void verifyPriceChanged(Result<Long> result, PricePackage basePricePackage) {
        verifyPricePackageChanged(result, basePricePackage.withPrice(NEW_PRICE));
    }

    private void verifyPricePackageChanged(Result<Long> result, PricePackage expectedPricePackage) {
        assertThat("результат обновления элемента не соответствует ожидаемому",
                result.getErrors(), is(empty()));

        var pricePackageId = expectedPricePackage.getId();
        PricePackage fromDb = repository.getPricePackages(List.of(pricePackageId))
                .get(pricePackageId);
        Assertions.assertThat(fromDb).is(matchedBy(
                beanDiffer(expectedPricePackage
                        .withLastUpdateTime(LocalDateTime.now(ZoneOffset.UTC))
                ).useCompareStrategy(pricePackagesCompareStrategy())
        ));
    }

    private ModelChanges<PricePackage> modelChangesWithValidTitle(long pricePackageId) {
        return ModelChanges.build(pricePackageId, PricePackage.class, PricePackage.TITLE, NEW_TITLE);
    }

    private ModelChanges<PricePackage> modelChangesWithValidPrice(long pricePackageId) {
        return ModelChanges.build(pricePackageId, PricePackage.class, PricePackage.PRICE, NEW_PRICE);
    }

    private ModelChanges<PricePackage> modelChangesWithValidAuctionPriority(long pricePackageId) {
        return ModelChanges.build(pricePackageId, PricePackage.class, PricePackage.AUCTION_PRIORITY,
                NEW_AUCTION_PRIORITY);
    }

    private ModelChanges<PricePackage> modelChangesWithClients(long pricePackageId, List<PricePackageClient> clients) {
        return ModelChanges.build(pricePackageId, PricePackage.class, PricePackage.CLIENTS, clients);
    }

    private ModelChanges<PricePackage> modelChangesWithValidDateEnd(long pricePackageId) {
        return ModelChanges.build(pricePackageId, PricePackage.class, PricePackage.DATE_END, NEW_DATE_END);
    }

    private ModelChanges<PricePackage> modelChangesWithValidForChangeFieldsOnApprovedPackage(long pricePackageId) {
        var modelChanges = new ModelChanges<>(pricePackageId, PricePackage.class)
                .processNotNull(NEW_TITLE, PricePackage.TITLE)
                .processNotNull(NEW_ALLOWED_PAGE_IDS, PricePackage.ALLOWED_PAGE_IDS)
                .processNotNull(NEW_DATE_END, PricePackage.DATE_END)
                .processNotNull(NEW_IS_DRAFT_APPROVE_ALLOWED, PricePackage.IS_DRAFT_APPROVE_ALLOWED);
        return modelChanges;
    }

    private PricePackage updatePricePackageWithValidForChangeFieldsOnApprovedPackage(PricePackage pricePackage) {
        pricePackage.withTitle(NEW_TITLE)
                .withAllowedPageIds(NEW_ALLOWED_PAGE_IDS)
                .withDateEnd(NEW_DATE_END)
                .withIsDraftApproveAllowed(NEW_IS_DRAFT_APPROVE_ALLOWED);
        return pricePackage;
    }

    private MassResult<Long> prepareAndApplyOperation(List<ModelChanges<PricePackage>> modelChangesList,
                                                      List<LocalDateTime> userTimestamps) {
        return prepareAndApplyOperation(modelChangesList, userTimestamps, operator);
    }

    private MassResult<Long> prepareAndApplyOperation(List<ModelChanges<PricePackage>> modelChangesList,
                                                      List<LocalDateTime> userTimestamps, User operator) {
        return updateOperationFactory.newInstance(PARTIAL, modelChangesList, userTimestamps, operator)
                .prepareAndApply();
    }

    private static DefaultCompareStrategy pricePackagesCompareStrategy() {
        return DefaultCompareStrategies
                .allFields()
                .forFields(newPath("price")).useDiffer(new BigDecimalDiffer())
                .forFields(newPath("lastUpdateTime")).useMatcher(approximatelyNow(ZoneOffset.UTC));
    }
}
