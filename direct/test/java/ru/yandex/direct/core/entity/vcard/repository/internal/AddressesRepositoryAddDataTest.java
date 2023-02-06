package ru.yandex.direct.core.entity.vcard.repository.internal;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.vcard.model.PointPrecision;
import ru.yandex.direct.core.entity.vcard.model.PointType;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestVcards;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestAddressesRepository;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.AddedModelId;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddressesRepositoryAddDataTest {

    private static final CompareStrategy STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("lastChange"))
            .useMatcher(approximatelyNow());

    @Autowired
    private AddressesRepository addressesRepository;

    @Autowired
    private TestAddressesRepository testAddressesRepository;

    @Autowired
    private ClientSteps clientSteps;

    private ClientInfo clientInfo;
    private int shard;
    private ClientId clientId;

    @Before
    public void before() {
        clientInfo = clientSteps.createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
    }

    // getOrCreateAddresses - проверка сохраненных данных - один новый элемент

    @Test
    public void getOrCreateAddresses_OneNewItem_ReturnsOneId() {
        List<Long> ids = addressesRepository.getOrCreateAddresses(shard, clientId, singletonList(fullVcard())).stream()
                .map(AddedModelId::getId)
                .collect(toList());
        assertThat(ids, hasSize(1));
    }

    @Test
    public void getOrCreateAddresses_OneNewFullItem_CreatesValidAddress() {
        Vcard vcard = fullVcard();
        createOneNewItemAndCheckSavedData(vcard);
    }

    @Test
    public void getOrCreateAddresses_OneNewItemWithoutManualPoint_CreatesValidAddress() {
        Vcard vcard = fullVcard().withManualPoint(null);
        createOneNewItemAndCheckSavedData(vcard);
    }

    @Test
    public void getOrCreateAddresses_OneNewItemWithoutAutoPoint_CreatesValidAddress() {
        Vcard vcard = fullVcard().withAutoPoint(null);
        createOneNewItemAndCheckSavedData(vcard);
    }

    @Test
    public void getOrCreateAddresses_OneNewItemWithoutBothPoints_CreatesValidAddress() {
        Vcard vcard = fullVcard().withAutoPoint(null).withManualPoint(null);
        createOneNewItemAndCheckSavedData(vcard);
    }

    @Test
    public void getOrCreateAddresses_OneNewItemWithoutBuild_CreatesValidAddress() {
        Vcard vcard = fullVcard().withBuild(null);
        createOneNewItemAndCheckSavedData(vcard);
    }

    @Test
    public void getOrCreateAddresses_OneNewItemWithoutStreetAndHouse_CreatesValidAddress() {
        Vcard vcard = fullVcard().withStreet(null).withHouse(null);
        createOneNewItemAndCheckSavedData(vcard);
    }

    private void createOneNewItemAndCheckSavedData(Vcard vcard) {
        List<Long> ids = create(vcard);
        long id = ids.get(0);
        DbAddress dbAddress = testAddressesRepository.getAddresses(shard, clientId, ids).get(id);
        DbAddress expectedDbAddress = convertVcardToDbAddress(vcard, id, clientId);

        assertThat(dbAddress, beanDiffer(expectedDbAddress).useCompareStrategy(STRATEGY));
    }

    // getOrCreateAddresses - проверка сохраненных данных - два элемента

    @Test
    public void getOrCreateAddresses_TwoNewItems_CreatesValidAddresses() {
        Vcard vcard1 = fullVcard();
        Vcard vcard2 = fullVcard().withHouse("999");

        List<Long> ids = create(vcard1, vcard2);
        checkState(!ids.get(0).equals(ids.get(1)), "полученные id созданных элементов не должны совпадать");

        Map<Long, DbAddress> dbAddresses = testAddressesRepository.getAddresses(shard, clientId, ids);

        DbAddress expectedDbAddress1 = convertVcardToDbAddress(vcard1, ids.get(0), clientId);
        DbAddress expectedDbAddress2 = convertVcardToDbAddress(vcard2, ids.get(1), clientId);

        assertThat("сохраненный адрес не соответствует ожидаемому",
                dbAddresses.get(ids.get(0)),
                beanDiffer(expectedDbAddress1).useCompareStrategy(STRATEGY));
        assertThat("сохраненный адрес не соответствует ожидаемому",
                dbAddresses.get(ids.get(1)),
                beanDiffer(expectedDbAddress2).useCompareStrategy(STRATEGY));
    }

    @Test
    public void getOrCreateAddresses_OneNewAndOneMatchingItem_CreatesOneNewItemAndReturnsIdOfExistingItem() {
        Vcard vcard1 = fullVcard();
        Vcard vcard11 = fullVcard();
        Vcard vcard2 = fullVcard().withHouse("999");

        create(vcard1);

        List<Long> ids = create(vcard11, vcard2);

        Map<Long, DbAddress> dbAddresses = testAddressesRepository.getAddresses(shard, clientId, ids);

        DbAddress expectedDbAddress1 = convertVcardToDbAddress(vcard11, ids.get(0), clientId);
        DbAddress expectedDbAddress2 = convertVcardToDbAddress(vcard2, ids.get(1), clientId);

        assertThat("полученный адрес не соответствует ожидаемому",
                dbAddresses.get(ids.get(0)),
                beanDiffer(expectedDbAddress1).useCompareStrategy(STRATEGY));
        assertThat("сохраненный адрес не соответствует ожидаемому",
                dbAddresses.get(ids.get(1)),
                beanDiffer(expectedDbAddress2).useCompareStrategy(STRATEGY));
    }

    // getOrCreateAddresses - проверка обновления существующих адресов

    @Test
    public void getOrCreateAddresses_OneMatchingItemWithoutChanges_ExistingItemIsNotUpdated() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);
        long id = ids.get(0);
        DbAddress dbAddressAfterCreating = testAddressesRepository.getAddresses(shard, clientId, ids).get(id);

        createOneMatchingItemAndCheckReturnedId(vcard, id);
        DbAddress expectedDbAddress = convertVcardToDbAddress(vcard, id, clientId)
                .withLastChange(dbAddressAfterCreating.getLastChange());

        DbAddress dbAddressAfterGet = testAddressesRepository.getAddresses(shard, clientId, ids).get(id);

        assertThat("существующий адрес после получения без обновления не соответствует ожидаемому",
                dbAddressAfterGet, beanDiffer(expectedDbAddress));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemWithChangedManualPointId_ExistingItemUpdatedCorrectly() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);

        vcard.getManualPoint().withId(1234512L);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
        checkUpdatedItem(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemWithChangedManualPointIdToNull_ExistingItemUpdatedCorrectly() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);

        vcard.withManualPoint(null);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
        checkUpdatedItem(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemWithChangedAutoPointId_ExistingItemUpdatedCorrectly() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);

        vcard.getAutoPoint().withId(1234512L);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
        checkUpdatedItem(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemWithChangedAutoPointIdToNull_ExistingItemUpdatedCorrectly() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);

        vcard.withAutoPoint(null);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
        checkUpdatedItem(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemWithChangedPointType_ExistingItemUpdatedCorrectly() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);

        vcard.withPointType(PointType.AREA);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
        checkUpdatedItem(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemWithChangedPointTypeToNull_ExistingItemUpdatedCorrectly() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);

        vcard.withPointType(null);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
        checkUpdatedItem(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemWithChangedPrecision_ExistingItemUpdatedCorrectly() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);

        vcard.withPrecision(PointPrecision.NEAR);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
        checkUpdatedItem(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneExistingItemWithChangedPrecisionToNull_ExistingItemUpdatedCorrectly() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);

        vcard.withPrecision(null);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
        checkUpdatedItem(vcard, ids.get(0));
    }

    private void checkUpdatedItem(Vcard vcard, long vcardId) {
        DbAddress expectedDbAddress = convertVcardToDbAddress(vcard, vcardId, clientId);

        DbAddress dbAddress = testAddressesRepository
                .getAddresses(shard, clientId, singletonList(vcardId)).get(vcardId);

        assertThat("существующий адрес после обновления не соответствует ожидаемому",
                dbAddress, beanDiffer(expectedDbAddress).useCompareStrategy(STRATEGY));
    }

    // getOrCreateAddresses - сложный кейс с разными проверками - разные виды действий над объектами в одном вызове

    @Test
    public void getOrCreateAddresses_DifferentCasesInOneCall() {
        Vcard vcardToBeNotChanged = fullVcard().withHouse("123");
        Vcard vcardToBeChanged = fullVcard().withHouse("124");
        Vcard vcardToBeNew = fullVcard().withHouse("125");
        long idOfAddressToBeNotChanged = create(vcardToBeNotChanged).get(0);
        long idOfAddressToBeChanged = create(vcardToBeChanged).get(0);

        vcardToBeChanged.withPrecision(PointPrecision.RANGE);

        // в этом вызове должно вернуться 3 id:
        // id существующей неизменённой записи
        // id существующей изменённой записи
        // id новой записи
        List<Long> actualIds = addressesRepository.getOrCreateAddresses(shard, clientId,
                asList(vcardToBeNotChanged, vcardToBeChanged, vcardToBeNew)).stream()
                .map(AddedModelId::getId)
                .collect(toList());

        assertThat("метод должен вернуть 3 id", actualIds, hasSize(3));

        // проверяем, что для существующих адресов возвращаются существующие id
        assertThat("первый id должен соответствовать созданной ранее записи", actualIds.get(0),
                equalTo(idOfAddressToBeNotChanged));
        assertThat("второй id должен соответствовать созданной ранее записи", actualIds.get(1),
                equalTo(idOfAddressToBeChanged));

        // проверяем, что новый адрес создан корректно
        long idOfNewVcard = actualIds.get(2);
        DbAddress newDbAddress = testAddressesRepository
                .getAddresses(shard, clientId, singletonList(idOfNewVcard)).get(idOfNewVcard);
        DbAddress expectedDbAddress = convertVcardToDbAddress(vcardToBeNew, idOfNewVcard, clientId);
        assertThat("новый адрес соответствует ожидаемому",
                newDbAddress, beanDiffer(expectedDbAddress).useCompareStrategy(STRATEGY));

        // проверяем, что существующий адрес, не имеющий изменений, остался прежним
        DbAddress existingDbAddressNotChanged = testAddressesRepository
                .getAddresses(shard, clientId, singletonList(idOfAddressToBeNotChanged)).get(idOfAddressToBeNotChanged);
        DbAddress expectedExistingDbAddressNotChanged =
                convertVcardToDbAddress(vcardToBeNotChanged, idOfAddressToBeNotChanged, clientId);
        assertThat("существующий адрес, который не должен был измениться, соответствует ожидаемому",
                existingDbAddressNotChanged,
                beanDiffer(expectedExistingDbAddressNotChanged).useCompareStrategy(STRATEGY));

        // проверяем, что существующий адрес, который должен был измениться, изменился
        DbAddress existingDbAddressChanged = testAddressesRepository
                .getAddresses(shard, clientId, singletonList(idOfAddressToBeChanged)).get(idOfAddressToBeChanged);
        DbAddress expectedExistingDbAddressChanged =
                convertVcardToDbAddress(vcardToBeChanged, idOfAddressToBeChanged, clientId);
        assertThat("существующий адрес, который должен был измениться, соответствует ожидаемому",
                existingDbAddressChanged,
                beanDiffer(expectedExistingDbAddressChanged).useCompareStrategy(STRATEGY));
    }

    @Test
    public void vcardToDbAddress_correctStrip() {
        Vcard vcard = fullVcard().withCountry("Россия  ") // Должен остаться один пробел
                .withCity("Москва")
                .withStreet("Красная    пресня") // Должен остаться один пробел
                .withHouse("  `10`  ") // Должен остаться один пробел + конвертация кавычек
                .withBuild("1    "); // Пробелов быть не должно
        DbAddress dbAddress = addressesRepository.vcardToDbAddress(vcard, clientId);
        assertEquals("Неправильная конвертация адреса в формат базы",
                "россия ,москва,красная пресня, '10' ,1",
                dbAddress.getAddress());
    }

    private List<Long> create(Vcard... vcards) {
        List<Long> ids = addressesRepository.getOrCreateAddresses(shard, clientId, asList(vcards)).stream()
                .map(AddedModelId::getId)
                .collect(toList());
        checkState(ids != null && ids.size() == vcards.length);
        return ids;
    }

    private void createOneMatchingItemAndCheckReturnedId(Vcard vcard, long existingIdToCheck) {
        List<Long> newIds = addressesRepository.getOrCreateAddresses(shard, clientId, singletonList(vcard)).stream()
                .map(AddedModelId::getId)
                .collect(toList());
        assertThat("при повторном сохранении адреса метод должен вернуть существующий id",
                newIds, contains(existingIdToCheck));
    }

    private static Vcard fullVcard() {
        return TestVcards.fullVcard((Long) null, null);
    }

    private DbAddress convertVcardToDbAddress(Vcard vcard, long addressId, ClientId clientId) {
        return addressesRepository.vcardToDbAddress(vcard, clientId).withId(addressId);
    }
}
