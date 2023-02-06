package ru.yandex.direct.core.entity.vcard.repository.internal;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.vcard.model.PointPrecision;
import ru.yandex.direct.core.entity.vcard.model.PointType;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestVcards;
import ru.yandex.direct.core.testing.info.ClientInfo;
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
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddressesRepositoryAddDeduplicationTest {

    @Autowired
    private AddressesRepository addressesRepository;

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

    // getOrCreateAddresses - проверка уникализации - элемент присутствует в базе

    @Test
    public void getOrCreateAddresses_OneMatchingItemExactlyMatched_ReturnsExistingAddressId() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemDiffersByManualPoint_ReturnsExistingAddressId() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);
        vcard.getManualPoint().withX(BigDecimal.valueOf(12L));
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemDiffersByAutoPoint_ReturnsExistingAddressId() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);
        vcard.getAutoPoint().withX(BigDecimal.valueOf(13L));
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemDiffersByMetroId_ReturnsExistingAddressId() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);

        vcard.withMetroId(1239L);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemDiffersByPointType_ReturnsExistingAddressId() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);

        vcard.withPointType(PointType.CEMETERY);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_OneMatchingItemDiffersByPrecision_ReturnsExistingAddressId() {
        Vcard vcard = fullVcard();
        List<Long> ids = create(vcard);

        vcard.withPrecision(PointPrecision.NEAR);
        createOneMatchingItemAndCheckReturnedId(vcard, ids.get(0));
    }

    @Test
    public void getOrCreateAddresses_TwoDifferentItemsMatchingWithExistingInDatabase_ReturnsExistingAddressIds() {
        Vcard vcard1 = fullVcard();
        Vcard vcard2 = fullVcard().withHouse("999");
        Vcard vcard11 = fullVcard();
        Vcard vcard21 = fullVcard().withHouse("999");
        List<Long> oldIds = create(vcard1, vcard2);

        List<Long> ids = addressesRepository.getOrCreateAddresses(shard, clientId, asList(vcard11, vcard21)).stream()
                .map(AddedModelId::getId)
                .collect(toList());

        assertThat("возвращенный id элемента должен соответствовать существующему в базе",
                ids.get(0), equalTo(oldIds.get(0)));
        assertThat("возвращенный id элемента должен соответствовать существующему в базе",
                ids.get(1), equalTo(oldIds.get(1)));
    }

    @Test
    public void getOrCreateAddresses_TwoEqualItemsMatchingWithExistingInDatabase_ReturnsExistingAddressId() {
        Vcard vcard1 = fullVcard();
        Vcard vcard11 = fullVcard();
        Vcard vcard2 = fullVcard();
        List<Long> oldIds = create(vcard1);

        List<Long> ids = addressesRepository.getOrCreateAddresses(shard, clientId, asList(vcard11, vcard2)).stream()
                .map(AddedModelId::getId)
                .collect(toList());

        assertThat("возвращенные id одинаковых элементов должны быть одинаковыми",
                ids.get(0), equalTo(ids.get(1)));
        assertThat("возвращенный id элемента должен соответствовать существующему в базе",
                ids.get(0), equalTo(oldIds.get(0)));
    }

    @Test
    public void getOrCreateAddresses_TwoDifferentItemsMatchingWithExistingInDatabase_ReturnsExistingAddressId() {
        Vcard vcard1 = fullVcard();
        Vcard vcard11 = fullVcard().withPointType(PointType.CEMETERY);
        Vcard vcard2 = fullVcard().withPrecision(PointPrecision.OTHER);
        List<Long> oldIds = create(vcard1);

        List<Long> ids = addressesRepository.getOrCreateAddresses(shard, clientId, asList(vcard11, vcard2)).stream()
                .map(AddedModelId::getId)
                .collect(toList());

        assertThat("возвращенные id одинаковых элементов должны быть одинаковыми",
                ids.get(0), equalTo(ids.get(1)));
        assertThat("возвращенный id элемента должен соответствовать существующему в базе",
                ids.get(0), equalTo(oldIds.get(0)));
    }

    // getOrCreateAddresses - проверка уникализации - элемент отсутствует в базе

    @Test
    public void getOrCreateAddresses_OneNewItemDiffersByAddressAndHash_CreatesNewAddress() {
        Vcard vcard = fullVcard();
        List<Long> ids1 = create(vcard);

        vcard.withCity("Лондон");
        List<Long> ids2 = create(vcard);

        assertThat("id второго создаваемого объекта должен отличаться от первого",
                ids1.get(0), not(equalTo(ids2.get(0))));
    }

    @Test
    public void getOrCreateAddresses_TwoDifferentNewAddressesInOneCallMatchesToEachOther() {
        Vcard vcard1 = fullVcard();
        Vcard vcard2 = fullVcard().withPrecision(PointPrecision.RANGE);

        List<Long> actualIds = addressesRepository.getOrCreateAddresses(shard, clientId, asList(vcard1, vcard2)).stream()
                .map(AddedModelId::getId)
                .collect(toList());

        assertThat("метод должен вернуть 2 id", actualIds, hasSize(2));
        assertThat("id должны быть одинаковые", actualIds.get(0), equalTo(actualIds.get(1)));
    }

    // проверка, что уникализация работает в рамках одного клиента
    @Test
    public void getOrCreateAddresses_OneAddressesSameAsOfOtherClient_CreatesNewAddressForThisClient() {
        ClientInfo otherClientInfo = clientSteps.createClient(new ClientInfo().withShard(clientInfo.getShard()));

        Vcard vcard = fullVcard();

        long addrId = addressesRepository.getOrCreateAddresses(shard, clientInfo.getClientId(), singletonList(vcard)).get(0).getId();

        long otherClientAddrId = addressesRepository.getOrCreateAddresses(shard, otherClientInfo.getClientId(),
                singletonList(vcard))
                .get(0).getId();

        assertThat(addrId, not(equalTo(otherClientAddrId)));
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

    // не должен возвращать рандомных полей
    private static Vcard fullVcard() {
        return TestVcards.fullVcard((Long) null, null);
    }
}
