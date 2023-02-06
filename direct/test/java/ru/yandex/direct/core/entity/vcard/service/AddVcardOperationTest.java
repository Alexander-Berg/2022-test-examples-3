package ru.yandex.direct.core.entity.vcard.service;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.vcard.model.PointOnMap;
import ru.yandex.direct.core.entity.vcard.model.PointPrecision;
import ru.yandex.direct.core.entity.vcard.model.PointType;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.entity.vcard.service.validation.AddVcardValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestVcards;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.geosearch.GeosearchClient;
import ru.yandex.direct.geosearch.model.Address;
import ru.yandex.direct.geosearch.model.GeoObject;
import ru.yandex.direct.geosearch.model.Kind;
import ru.yandex.direct.geosearch.model.Precision;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static java.math.BigDecimal.ROUND_CEILING;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.vcard.service.validation.VcardDefects.vcardIsDuplicated;
import static ru.yandex.direct.core.testing.data.TestVcards.SCALE;
import static ru.yandex.direct.core.testing.data.TestVcards.autoPoint;
import static ru.yandex.direct.core.testing.data.TestVcards.vcardUserFields;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddVcardOperationTest {

    private static final String CITY_NAME_RU = "Москва";
    private static final String CITY_NAME_EN = "Moscow";
    private static final Long CITY_GEO_ID = 213L;
    private static final Long CITY_SOME_METRO_ID = 20490L;

    private static final CompareStrategy STRATEGY_WITHOUT_ADDRESS = DefaultCompareStrategies.allFields()
            .forFields(newPath("lastChange"))
            .useMatcher(approximatelyNow())
            .forFields(newPath("lastDissociation"))
            .useMatcher(approximatelyNow())
            .forFields(newPath("orgDetailsId"))
            .useMatcher(greaterThan(0L));

    private static final CompareStrategy STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("lastChange"))
            .useMatcher(approximatelyNow())
            .forFields(newPath("lastDissociation"))
            .useMatcher(approximatelyNow())
            .forFields(newPath("orgDetailsId"))
            .useMatcher(greaterThan(0L))
            .forFields(newPath("addressId"))
            .useMatcher(greaterThan(0L))
            .forFields(newPath("manualPoint", "id"))
            .useMatcher(greaterThan(0L))
            .forFields(newPath("autoPoint", "id"))
            .useMatcher(greaterThan(0L));

    @Autowired
    private AddVcardValidationService addVcardValidationService;

    @Autowired
    private VcardRepository vcardRepository;

    @Autowired
    private GeoRegionLookup geoRegionLookup;

    private VcardHelper vcardHelper;

    @Autowired
    private BannerCommonRepository bannerRepository;

    private GeosearchClient geosearchClient;

    @Autowired
    private CampaignSteps campaignSteps;

    private int shard;
    private ClientId clientId;
    private long clientUid;
    private long campaignId;

    public AddVcardOperationTest() {
        geosearchClient = mock(GeosearchClient.class);
        setGeocoderResponse(defaultGeocoderResponse());
    }

    // проверка результата - Applicability.PARTIAL

    @Before
    public void before() {
        vcardHelper = new VcardHelper(geosearchClient, geoRegionLookup);

        Region regionRu = geoRegionLookup.getRegionByCity(CITY_NAME_RU);
        Region regionEn = geoRegionLookup.getRegionByCity(CITY_NAME_RU);
        checkState(regionRu != null && regionRu.getId() == CITY_GEO_ID);
        checkState(regionEn != null && regionEn.getId() == CITY_GEO_ID);

        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign();
        shard = campaignInfo.getShard();
        clientId = campaignInfo.getClientId();
        clientUid = campaignInfo.getUid();
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidItem_ResultIsFullySuccessful() {
        AddVcardOperation operation = createOperation(Applicability.PARTIAL, oneValidVcard());
        MassResult<Long> massResult = operation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(notNullValue(Long.class)));
    }

    @Test
    public void prepareAndApply_PartialYes_OneInvalidItem_ResultHasElementError() {
        AddVcardOperation operation = createOperation(Applicability.PARTIAL, oneInvalidVcard());
        MassResult<Long> massResult = operation.prepareAndApply();
        assertThat(massResult, isSuccessful(false));
    }

    @Test
    public void prepareAndApply_PartialYes_TwoValidItems_ResultIsFullySuccessful() {
        AddVcardOperation operation = createOperation(Applicability.PARTIAL, twoValidVcards());
        MassResult<Long> massResult = operation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(notNullValue(Long.class), notNullValue(Long.class)));
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidAndOneInvalidItems_ResultHasElementError() {
        AddVcardOperation operation = createOperation(Applicability.PARTIAL, asList(validVcard(), invalidVcard()));
        MassResult<Long> massResult = operation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(notNullValue(Long.class), null));
    }

    // проверка результата - Applicability.FULL

    @Test
    public void prepareAndApply_PartialYes_TwoInvalidItems_ResultHasBothElementsErrors() {
        AddVcardOperation operation = createOperation(Applicability.PARTIAL, twoInvalidVcards());
        MassResult<Long> massResult = operation.prepareAndApply();
        assertThat(massResult, isSuccessful(false, false));
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidItem_ResultIsFullySuccessful() {
        AddVcardOperation operation = createOperation(Applicability.FULL, oneValidVcard());
        MassResult<Long> massResult = operation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(notNullValue(Long.class)));
    }

    @Test
    public void prepareAndApply_PartialNo_OneInvalidItem_ResultHasElementError() {
        AddVcardOperation operation = createOperation(Applicability.FULL, oneInvalidVcard());
        MassResult<Long> massResult = operation.prepareAndApply();
        assertThat(massResult, isSuccessful(false));
    }

    @Test
    public void prepareAndApply_PartialNo_TwoValidItems_ResultIsFullySuccessful() {
        AddVcardOperation operation = createOperation(Applicability.FULL, twoValidVcards());
        MassResult<Long> massResult = operation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(notNullValue(Long.class), notNullValue(Long.class)));
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItems_ResultHasElementError() {
        AddVcardOperation operation = createOperation(Applicability.FULL, asList(validVcard(), invalidVcard()));
        MassResult<Long> massResult = operation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(nullValue(Long.class), null));
    }

    @Test
    public void prepareAndApply_PartialNo_TwoInvalidItems_ResultHasBothElementsErrors() {
        AddVcardOperation operation = createOperation(Applicability.FULL, twoInvalidVcards());
        MassResult<Long> massResult = operation.prepareAndApply();
        assertThat(massResult, isSuccessful(false, false));
    }

    // Проверка сохраненных данных - Applicability.PARTIAL
    // Здесь же проверяется, что полученные из геокодера данные правильно сохраняются.
    // Кейсы, связанные с особыми данными отдельных объектов,
    // проверяем только на Applicability.PARTIAL и одном объекте.

    @Test
    public void prepareAndApply_PartialYes_OneValidItemWithFoundGeoDataAndKnownCityRu_DataIsSavedCorrectly() {
        checkOneItemDataIsSavedCorrectly(validVcard().withCity(CITY_NAME_RU),
                expectedVcard().withCity(CITY_NAME_RU));
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidItemWithKnownCityEn_DataIsSavedCorrectly() {
        checkOneItemDataIsSavedCorrectly(
                validVcard().withCity(CITY_NAME_EN),
                expectedVcard().withCity(CITY_NAME_EN));
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidItemWithUnknownCity_DataIsSavedCorrectly() {
        String city = "Неизвестный";
        Vcard vcard = validVcard().withCity(city).withMetroId(null);
        Vcard expectedVcard = expectedVcard().withCity(city).withGeoId(Region.GLOBAL_REGION_ID).withMetroId(null);
        checkOneItemDataIsSavedCorrectly(vcard, expectedVcard);
    }

    /*
        Здесь 2 визитки с одинаковыми адресами и одна с другим адресом
        используются для проверки правильности обращения в геокодер с учетом того,
        что обращение в него производится только для уникальных адресов.
        То есть для двух одинаковых адресов будет одно обращение в геокодер,
        и при этом в обеих визитках должны быть установлены полученные из геокодера данные.
        Для того, чтобы проверка срабатывала, визитки с одинаковыми адресами
        должны записываться в базу как разные записи, то есть должны
        иметь отличающиеся поля.
     */
    @Test
    public void prepareAndApply_PartialYes_ThreeDifferentValidItemsWithFoundGeoData_DataIsSavedCorrectly() {
        Vcard vcard1 = validVcard().withHouse("2").withContactPerson("Вася");
        Vcard vcard2 = validVcard().withHouse("1");
        Vcard vcard3 = validVcard().withHouse("2").withContactPerson("Петя");

        // данные, которые мок геокодера вернет для визиток 1 и 3 (данные должны отличаться!!!)
        PointOnMap point1 = TestVcards.autoPoint()
                .withX(BigDecimal.valueOf(55L).setScale(SCALE, ROUND_CEILING));
        PointType pointType1 = PointType.AREA;
        PointPrecision pointPrecision1 = PointPrecision.NEAR;
        GeoObject geoObject1 = geocoderResponse(point1, pointType1, pointPrecision1);

        // данные, которые мок геокодера вернет для визитки 2
        PointOnMap point2 = TestVcards.autoPoint()
                .withX(BigDecimal.valueOf(248L).setScale(SCALE, ROUND_CEILING));
        PointType pointType2 = PointType.BRIDGE;
        PointPrecision pointPrecision2 = PointPrecision.NUMBER;
        GeoObject geoObject2 = geocoderResponse(point2, pointType2, pointPrecision2);

        // настраиваем мок, чтобы он возвращал определенные данные для визиток
        when(geosearchClient.getMostRelevantGeoData(eq(getAddressForGeocoder(vcard1))))
                .thenReturn(geoObject1);
        when(geosearchClient.getMostRelevantGeoData(eq(getAddressForGeocoder(vcard2))))
                .thenReturn(geoObject2);

        // выполняем тестируемое действие - добавляем визитки через сервис
        AddVcardOperation operation = createOperation(Applicability.PARTIAL, asList(vcard1, vcard2, vcard3));
        MassResult<Long> massResult = operation.prepareAndApply();
        assumeThat(massResult, isFullySuccessful());

        long id1 = massResult.getResult().get(0).getResult();
        long id2 = massResult.getResult().get(1).getResult();
        long id3 = massResult.getResult().get(2).getResult();

        // создаем ожидаемые объекты визиток
        Vcard expectedVcard1 = vcard1
                .withId(id1)
                .withAutoPoint(point1)
                .withPointType(pointType1)
                .withPrecision(pointPrecision1);
        Vcard expectedVcard2 = vcard2
                .withId(id2)
                .withAutoPoint(point2)
                .withPointType(pointType2)
                .withPrecision(pointPrecision2);
        Vcard expectedVcard3 = vcard3
                .withId(id3)
                .withAutoPoint(point1)
                .withPointType(pointType1)
                .withPrecision(pointPrecision1);

        // получаем сохраненные визитки
        Vcard actualVcard1 = vcardRepository.getVcards(shard, clientUid, singletonList(id1)).get(0);
        Vcard actualVcard2 = vcardRepository.getVcards(shard, clientUid, singletonList(id2)).get(0);
        Vcard actualVcard3 = vcardRepository.getVcards(shard, clientUid, singletonList(id3)).get(0);

        // проверяем корректность сохраненных данных каждой визитки
        assertThat(actualVcard1, beanDiffer(expectedVcard1).useCompareStrategy(STRATEGY));
        assertThat(actualVcard2, beanDiffer(expectedVcard2).useCompareStrategy(STRATEGY));
        assertThat(actualVcard3, beanDiffer(expectedVcard3).useCompareStrategy(STRATEGY));
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidItemWithWithoutManualPoint_DataIsSavedCorrectlyUsingAutoPoint() {
        Vcard expectedVcard = expectedVcard()
                .withManualPoint(TestVcards.autoPoint())
                .withAutoPoint(TestVcards.autoPoint());

        checkOneItemDataIsSavedCorrectly(validVcard().withManualPoint(null), expectedVcard);
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidItemWithInaccurateAddress_DataIsSavedCorrectlyWithNullAddressId() {
        setGeocoderResponse(new GeoObject.Builder().withPrecision(Precision.OTHER).build());

        Vcard expectedVcard = expectedVcard()
                .withAddressId(null)
                .withAutoPoint(null)
                .withManualPoint(null)
                .withPrecision(null)
                .withPointType(null);

        checkOneItemDataIsSavedCorrectly(
                validVcard().withManualPoint(null), expectedVcard, STRATEGY_WITHOUT_ADDRESS);
    }

    private void checkOneItemDataIsSavedCorrectly(Vcard vcard, Vcard expectedVcard) {
        checkOneItemDataIsSavedCorrectly(vcard, expectedVcard, STRATEGY);
    }

    private void checkOneItemDataIsSavedCorrectly(Vcard vcard, Vcard expectedVcard, CompareStrategy compareStrategy) {
        AddVcardOperation operation = createOperation(Applicability.PARTIAL, singletonList(vcard));
        MassResult<Long> massResult = operation.prepareAndApply();
        assumeThat(massResult, isFullySuccessful());

        long id = massResult.getResult().get(0).getResult();

        expectedVcard.withId(id);
        Vcard actualVcard = vcardRepository.getVcards(shard, clientUid, singletonList(id)).get(0);
        assertThat(actualVcard, beanDiffer(expectedVcard).useCompareStrategy(compareStrategy));
    }

    @Test
    public void prepareAndApply_PartialYes_TwoValidItem_DataIsSavedCorrectly() {
        Vcard vcard1 = validVcard();
        Vcard vcard2 = validVcard().withHouse("999");
        AddVcardOperation operation = createOperation(Applicability.PARTIAL, asList(vcard1, vcard2));
        MassResult<Long> massResult = operation.prepareAndApply();
        assumeThat(massResult, isFullySuccessful());

        long id1 = massResult.getResult().get(0).getResult();
        long id2 = massResult.getResult().get(1).getResult();

        Vcard expectedVcard1 = expectedVcard(id1);
        Vcard expectedVcard2 = expectedVcard(id2).withHouse("999");

        Vcard actualVcard1 = vcardRepository.getVcards(shard, clientUid, singletonList(id1)).get(0);
        Vcard actualVcard2 = vcardRepository.getVcards(shard, clientUid, singletonList(id2)).get(0);
        assertThat(actualVcard1, beanDiffer(expectedVcard1).useCompareStrategy(STRATEGY));
        assertThat(actualVcard2, beanDiffer(expectedVcard2).useCompareStrategy(STRATEGY));
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidAndOneInvalidItems_DataIsSavedCorrectly() {
        Vcard invalidVcard = invalidVcard();
        Vcard validVcard = validVcard();
        AddVcardOperation operation = createOperation(Applicability.PARTIAL, asList(invalidVcard, validVcard));
        MassResult<Long> massResult = operation.prepareAndApply();

        assumeThat(massResult, isSuccessful(false, true));

        long id = massResult.getResult().get(1).getResult();

        Vcard expectedVcard = expectedVcard(id);

        Vcard actualVcard = vcardRepository.getVcards(shard, clientUid, singletonList(id)).get(0);
        assertThat(actualVcard, beanDiffer(expectedVcard).useCompareStrategy(STRATEGY));
    }

    // проверка сохраненных данных - Applicability.FULL
    // здесь же проверяется, что полученные из геокодера данные правильно сохраняются

    @Test
    public void prepareAndApply_PartialYes_OneValidAndOneInvalidItems_CreatedOnlyOneVcard() {
        Vcard invalidVcard = invalidVcard();
        Vcard validVcard = validVcard();
        AddVcardOperation operation = createOperation(Applicability.PARTIAL, asList(invalidVcard, validVcard));
        MassResult<Long> massResult = operation.prepareAndApply();

        assumeThat(massResult, isSuccessful(false, true));

        List<Vcard> allClientVcards = vcardRepository.getVcards(shard, clientUid);
        assertThat("после выполнения операции у клиента должна быть одна карточка", allClientVcards, hasSize(1));
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidItem_DataIsSavedCorrectly() {
        Vcard vcard = validVcard();
        AddVcardOperation operation = createOperation(Applicability.FULL, singletonList(vcard));
        MassResult<Long> massResult = operation.prepareAndApply();
        assumeThat(massResult, isFullySuccessful());

        long id = massResult.getResult().get(0).getResult();

        Vcard expectedVcard = expectedVcard(id);
        Vcard actualVcard = vcardRepository.getVcards(shard, clientUid, singletonList(id)).get(0);
        assertThat(actualVcard, beanDiffer(expectedVcard).useCompareStrategy(STRATEGY));
    }

    @Test
    public void prepareAndApply_PartialNo_TwoValidItem_DataIsSavedCorrectly() {
        Vcard vcard1 = validVcard();
        Vcard vcard2 = validVcard().withHouse("999");
        AddVcardOperation operation = createOperation(Applicability.FULL, asList(vcard1, vcard2));
        MassResult<Long> massResult = operation.prepareAndApply();
        assumeThat(massResult, isFullySuccessful());

        long id1 = massResult.getResult().get(0).getResult();
        long id2 = massResult.getResult().get(1).getResult();

        Vcard expectedVcard1 = expectedVcard(id1);
        Vcard expectedVcard2 = expectedVcard(id2).withHouse("999");

        Vcard actualVcard1 = vcardRepository.getVcards(shard, clientUid, singletonList(id1)).get(0);
        Vcard actualVcard2 = vcardRepository.getVcards(shard, clientUid, singletonList(id2)).get(0);
        assertThat(actualVcard1, beanDiffer(expectedVcard1).useCompareStrategy(STRATEGY));
        assertThat(actualVcard2, beanDiffer(expectedVcard2).useCompareStrategy(STRATEGY));
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItems_OperationIsCancelled() {
        Vcard invalidVcard = invalidVcard();
        Vcard validVcard = validVcard();
        AddVcardOperation operation = createOperation(Applicability.FULL, asList(invalidVcard, validVcard));
        MassResult<Long> massResult = operation.prepareAndApply();

        assumeThat(massResult, isSuccessful(false, true));

        assertThat("id валидного элемента должен быть равен null",
                massResult.getResult().get(1).getResult(), nullValue());
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItems_DoesNotCreateAnything() {
        Vcard invalidVcard = invalidVcard();
        Vcard validVcard = validVcard();
        AddVcardOperation operation = createOperation(Applicability.FULL, asList(invalidVcard, validVcard));
        MassResult<Long> massResult = operation.prepareAndApply();

        assumeThat(massResult, isSuccessful(false, true));

        List<Vcard> allClientVcards = vcardRepository.getVcards(shard, clientUid);
        assertThat("после выполнения операции у клиента не должно быть карточек", allClientVcards, hasSize(0));
    }

    // проверка обращения к геокодеру

    @Test
    public void prepareAndApply_CallsGeocoderWithValidAddress() {
        createOperation(Applicability.FULL, singletonList(validVcard()))
                .prepareAndApply();

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        verify(geosearchClient).getMostRelevantGeoData(addressCaptor.capture());

        assertThat("адрес, переданный в геокодер, соответствует ожидаемому",
                addressCaptor.getValue(),
                beanDiffer(expectedAddressForGeocoder()));
    }

    @Test
    public void prepareAndApply_CallsGeocoderWithValidAddressOnly() {
        createOperation(Applicability.PARTIAL, asList(validVcard(), invalidVcard()))
                .prepareAndApply();

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        verify(geosearchClient, times(1)).getMostRelevantGeoData(addressCaptor.capture());

        assertThat("адрес, переданный в геокодер, соответствует ожидаемому",
                addressCaptor.getValue(),
                beanDiffer(expectedAddressForGeocoder()));
    }

    @Test
    public void prepareAndApply_CallsGeocoderWithOnlyOneUniqueCopyOfValidAddress() {
        createOperation(Applicability.PARTIAL, asList(validVcard(), validVcard()))
                .prepareAndApply();

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        verify(geosearchClient, times(1)).getMostRelevantGeoData(addressCaptor.capture());

        assertThat("адрес, переданный в геокодер, соответствует ожидаемому",
                addressCaptor.getValue(),
                beanDiffer(expectedAddressForGeocoder()));
    }

    @Test
    public void prepareAndApply_AddSameVcardTwice_ExpectDuplicationWarning() {
        MassResult<Long> result = createOperation(Applicability.PARTIAL, asList(validVcard(), validVcard()))
                .prepareAndApply();

        assertThat(
                result.get(0).getValidationResult(), hasNoDefectsDefinitions());
        assertThat(result.get(1).isSuccessful(), is(true));
        assertThat(
                result.get(1).getValidationResult(),
                hasDefectDefinitionWith(validationError(path(), vcardIsDuplicated())));
    }

    @Test
    public void prepareAndApply_AddExistingVcardTwice_ExpectDuplicationWarning() {
        createOperation(Applicability.PARTIAL, singletonList(validVcard()))
                .prepareAndApply();

        MassResult<Long> result = createOperation(Applicability.PARTIAL, singletonList(validVcard()))
                .prepareAndApply();

        assertThat(result.get(0).isSuccessful(), is(true));
        assertThat(
                result.get(0).getValidationResult(),
                hasDefectDefinitionWith(validationError(path(), vcardIsDuplicated())));
    }

    @Test
    public void prepareAndApply_AddNewVcardWithChangedCompanyAndManualPoint_ExpectNoErrorsAndWarnings() {
        createOperation(Applicability.PARTIAL, singletonList(validVcard()))
                .prepareAndApply();
        MassResult<Long> result = createOperation(Applicability.PARTIAL, singletonList(validVcard()
                .withCompanyName("another company")
                .withManualPoint(autoPoint())))
                .prepareAndApply();

        assertThat("Должна быть добавлена новая визитка", result.get(0).getValidationResult(),
                hasNoErrorsAndWarnings());
    }

    private void setGeocoderResponse(@Nullable GeoObject geocoderResponse) {
        when(geosearchClient.getMostRelevantGeoData(any()))
                .thenReturn(geocoderResponse);
    }

    private GeoObject defaultGeocoderResponse() {
        return geocoderResponse(TestVcards.autoPoint(), TestVcards.DEFAULT_POINT_TYPE,
                TestVcards.DEFAULT_PRECISION);
    }

    private GeoObject geocoderResponse(PointOnMap pointOnMap, PointType pointType, PointPrecision precision) {
        Kind geocoderKind = Kind.valueOf(pointType.name());
        Precision geocoderPrecision = Precision.valueOf(precision.name());
        return new GeoObject.Builder()
                .withX(pointOnMap.getX()).withY(pointOnMap.getY())
                .withX1(pointOnMap.getX1()).withY1(pointOnMap.getY1())
                .withX2(pointOnMap.getX2()).withY2(pointOnMap.getY2())
                .withPrecision(geocoderPrecision)
                .withKind(geocoderKind)
                .build();
    }

    private Vcard validVcard() {
        return vcardUserFields(campaignId).withCity(CITY_NAME_RU)
                .withMetroId(CITY_SOME_METRO_ID);
    }

    private Vcard invalidVcard() {
        return vcardUserFields(campaignId).withOgrn("123");
    }

    private List<Vcard> oneValidVcard() {
        return singletonList(validVcard());
    }

    private List<Vcard> twoValidVcards() {
        return asList(validVcard(), validVcard().withHouse("999"));
    }

    private List<Vcard> oneInvalidVcard() {
        return singletonList(invalidVcard());
    }

    private List<Vcard> twoInvalidVcards() {
        return asList(invalidVcard(), invalidVcard().withHouse("999"));
    }

    private Vcard expectedVcard() {
        return validVcard()
                .withUid(clientUid)
                .withGeoId(CITY_GEO_ID)
                .withAutoPoint(TestVcards.autoPoint())
                .withPointType(TestVcards.DEFAULT_POINT_TYPE)
                .withPrecision(TestVcards.DEFAULT_PRECISION);
    }

    private Vcard expectedVcard(long id) {
        return expectedVcard().withId(id);
    }

    private Address expectedAddressForGeocoder() {
        return getAddressForGeocoder(validVcard());
    }

    private Address getAddressForGeocoder(Vcard vcard) {
        return new Address()
                .withCountry(vcard.getCountry())
                .withCity(vcard.getCity())
                .withStreet(vcard.getStreet())
                .withHouse(vcard.getHouse())
                .withBuilding(vcard.getBuild());
    }

    private AddVcardOperation createOperation(Applicability applicability, List<Vcard> vcards) {
        return new AddVcardOperation(applicability, false, vcards, addVcardValidationService,
                vcardRepository, vcardHelper, clientUid,
                UidClientIdShard.of(clientUid, clientId, shard), bannerRepository);
    }
}
