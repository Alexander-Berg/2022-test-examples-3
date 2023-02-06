package ru.yandex.direct.core.entity.vcard.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.entity.vcard.repository.internal.AddressesRepository;
import ru.yandex.direct.core.entity.vcard.repository.internal.MapsRepository;
import ru.yandex.direct.core.entity.vcard.repository.internal.OrgDetailsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.AddedModelId;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Проверки корректности сохранённых данных при добавлении визиток
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VcardRepositoryAddDataTest {

    private static final CompareStrategy STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("lastChange"))
            .useMatcher(approximatelyNow())
            .forFields(newPath("lastDissociation"))
            .useMatcher(approximatelyNow());

    @Autowired
    private VcardRepository vcardRepository;

    @Autowired
    private MapsRepository mapsRepository;

    @Autowired
    private OrgDetailsRepository orgDetailsRepository;

    @Autowired
    private AddressesRepository addressesRepository;

    @Autowired
    private CampaignSteps campaignSteps;

    private int shard;
    private long clientUid;
    private ClientId clientId;
    private long campaignId;

    @Before
    public void before() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign();
        shard = campaignInfo.getShard();
        clientUid = campaignInfo.getUid();
        clientId = campaignInfo.getClientId();
        campaignId = campaignInfo.getCampaignId();
    }

    // addVcards - проверка сохраненных данных - один полностью новый элемент

    @Test
    public void addVcards_OneNewFullItemWithNewSubItems_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId);

        saveOneItemAndCheckData(vcard);
    }

    @Test
    public void addVcards_OneNewItemWithoutManualPoint_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId).withManualPoint(null);
        saveOneItemAndCheckData(vcard);
    }

    @Test
    public void addVcards_OneNewItemWithoutAutoPoint_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId).withAutoPoint(null);
        saveOneItemAndCheckData(vcard);
    }

    @Test
    public void addVcards_OneNewItemWithoutOgrn_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId).withOgrn(null);
        saveOneItemAndCheckData(vcard);
    }

    @Test
    public void addVcards_OneNewItemWithoutPhone_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId).withPhone(null);
        saveOneItemAndCheckData(vcard);
    }

    @Test
    public void addVcards_OneNewItemWithoutPhoneExtension_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId);
        vcard.getPhone().withExtension(null);
        saveOneItemAndCheckData(vcard);
    }

    @Test
    public void addVcards_OneNewItemWithoutInstantMessenger_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId).withInstantMessenger(null);
        saveOneItemAndCheckData(vcard);
    }

    @Test
    public void addVcards_OneNewItemWithoutOptionalSubElements_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId)
                .withManualPoint(null)
                .withAutoPoint(null)
                .withOgrn(null);
        saveOneItemAndCheckData(vcard);
    }

    // addVcards - проверка сохраненных данных - один новый элемент с существующими субэлементами

    @Test
    public void addVcards_OneNewItemWithExistingManualPoint_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId);
        List<Long> pointsIds = mapsRepository.getOrCreatePointOnMap(shard, singletonList(vcard.getManualPoint()));
        checkState(pointsIds.size() == 1);

        vcard.getManualPoint().withId(null);
        Vcard actualVcard = saveOneItemAndCheckData(vcard);
        assertThat("id ручной точки не соответствует ожидаемому",
                actualVcard.getManualPoint().getId(), equalTo(pointsIds.get(0)));
    }

    @Test
    public void addVcards_OneNewItemWithExistingAutoPoint_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId);
        List<Long> pointsIds = mapsRepository.getOrCreatePointOnMap(shard, singletonList(vcard.getAutoPoint()));
        checkState(pointsIds.size() == 1);

        vcard.getAutoPoint().withId(null);
        Vcard actualVcard = saveOneItemAndCheckData(vcard);
        assertThat("id автоматической точки не соответствует ожидаемому",
                actualVcard.getAutoPoint().getId(), equalTo(pointsIds.get(0)));
    }

    @Test
    public void addVcards_OneNewItemWithExistingOgrn_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId);
        List<Long> orgDetailsIds =
                orgDetailsRepository.getOrCreateOrgDetails(shard, clientUid, clientId, singletonList(vcard));
        checkState(orgDetailsIds.size() == 1);

        vcard.withOrgDetailsId(null);
        Vcard actualVcard = saveOneItemAndCheckData(vcard);
        assertThat("id записи в таблице org_details не соответствует ожидаемому",
                actualVcard.getOrgDetailsId(), equalTo(orgDetailsIds.get(0)));
    }

    @Test
    public void addVcards_OneNewItemWithExistingFullyIdenticalAddress_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId);

        List<Long> mapsIds = mapsRepository
                .getOrCreatePointOnMap(shard, asList(vcard.getManualPoint(), vcard.getAutoPoint()));
        checkState(mapsIds.size() == 2);

        List<Long> orgDetailsIds =
                orgDetailsRepository.getOrCreateOrgDetails(shard, clientUid, clientId, singletonList(vcard));
        checkState(orgDetailsIds.size() == 1);

        List<Long> addressesIds =
                addressesRepository.getOrCreateAddresses(shard, clientId, singletonList(vcard)).stream()
                        .map(AddedModelId::getId)
                        .collect(toList());
        checkState(addressesIds.size() == 1);

        vcard.getManualPoint().withId(null);
        vcard.getAutoPoint().withId(null);
        vcard.withOrgDetailsId(null)
                .withAddressId(null);

        Vcard actualVcard = saveOneItemAndCheckData(vcard);
        assertThat("id записи в таблице addresses должен быть равен id созданной ранее записи",
                actualVcard.getAddressId(), equalTo(addressesIds.get(0)));
    }

    @Test
    public void addVcards_OneNewItemWithExistingAddressWithChangedManualPoint_DataSavedCorrectly() {
        Vcard vcard = fullVcard(clientUid, campaignId);

        List<Long> mapsIds = mapsRepository
                .getOrCreatePointOnMap(shard, asList(vcard.getManualPoint(), vcard.getAutoPoint()));
        checkState(mapsIds.size() == 2);

        List<Long> addressesIds =
                addressesRepository.getOrCreateAddresses(shard, clientId, singletonList(vcard)).stream()
                        .map(AddedModelId::getId)
                        .collect(toList());
        checkState(addressesIds.size() == 1);

        vcard.getManualPoint().withX1(BigDecimal.valueOf(23L).setScale(6, RoundingMode.CEILING));
        Vcard actualVcard = saveOneItemAndCheckData(vcard);
        assertThat("id записи в таблице addresses должен быть равен id созданной ранее записи",
                actualVcard.getAddressId(), equalTo(addressesIds.get(0)));
    }

    private Vcard saveOneItemAndCheckData(Vcard vcard) {
        List<AddedModelId> ids = vcardRepository.addVcards(shard, clientUid, clientId, singletonList(vcard));
        assertThat("количество возвращенных id не соответствует количеству сохраняемых объектов", ids, hasSize(1));

        Vcard actualVcard = vcardRepository.getVcards(shard, clientUid, mapList(ids, AddedModelId::getId)).get(0);

        assertThat("полученный объект должен соответствовать сохраненному",
                actualVcard, beanDiffer(vcard).useCompareStrategy(STRATEGY));

        return actualVcard;
    }

    // addVcards - проверка сохраненных данных - два новых элемента

    @Test
    public void addVcards_TwoDifferentNewItems_DataSavedCorrectly() {
        Vcard vcard1 = fullVcard(clientUid, campaignId);
        Vcard vcard2 = fullVcard(clientUid, campaignId).withApart("999");

        List<AddedModelId> ids = vcardRepository.addVcards(shard, clientUid, clientId, asList(vcard1, vcard2));
        checkState(ids.size() == 2,
                "количество возвращенных id должно соответствовать количеству сохраняемых элементов");

        List<Vcard> vcards = vcardRepository.getVcards(shard, clientUid, mapList(ids, AddedModelId::getId));
        checkState(ids.size() == 2, "обе ранее созданные визитки должны быть найдены");

        assertThat("данные первой визитки не соответствуют ожидаемым",
                vcards.get(0), beanDiffer(vcard1).useCompareStrategy(STRATEGY));
        assertThat("данные первой визитки не соответствуют ожидаемым",
                vcards.get(1), beanDiffer(vcard2).useCompareStrategy(STRATEGY));
    }
}
