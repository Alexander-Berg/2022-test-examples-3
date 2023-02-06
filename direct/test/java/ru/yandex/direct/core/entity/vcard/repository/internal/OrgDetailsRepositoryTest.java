package ru.yandex.direct.core.entity.vcard.repository.internal;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.model.ClientId;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OrgDetailsRepositoryTest {

    @Autowired
    private OrgDetailsRepository orgDetailsRepository;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private UserSteps userSteps;

    private int shard;
    private ClientId clientId;
    private long clientUid;

    @Before
    public void before() {
        ClientInfo clientInfo = clientSteps.createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        clientUid = clientInfo.getUid();
    }

    // getOrCreateOrgDetails - проверка сохраненных данных - новые элементы

    @Test
    public void getOrCreateOrgDetails_OneNewItem_ReturnsOneId() {
        Vcard vcard = miniVcard1();
        List<Long> ids = orgDetailsRepository.getOrCreateOrgDetails(shard, clientUid, clientId, singletonList(vcard));
        assertThat("метод должен вернуть список, состоящий из одного id", ids, contains(greaterThan(0L)));
    }

    @Test
    public void getOrCreateOrgDetails_OneNewItem_CreatesValidEntry() {
        Vcard vcard = miniVcard1();
        List<Long> ids = create(vcard);
        Map<Long, DbOrgDetails> orgDetailsMap = get(ids);
        DbOrgDetails expectedOrgDetails = createExpectedOrgDetails(vcard, ids.get(0));
        assertThat(orgDetailsMap.get(ids.get(0)), beanDiffer(expectedOrgDetails));
    }

    @Test
    public void getOrCreateOrgDetails_TwoDifferentNewItems_CreatesValidEntries() {
        Vcard vcard1 = miniVcard1();
        Vcard vcard2 = miniVcard2();
        List<Long> ids = create(vcard1, vcard2);
        Map<Long, DbOrgDetails> orgDetailsMap = get(ids);

        DbOrgDetails expectedOrgDetails1 = createExpectedOrgDetails(vcard1, ids.get(0));
        DbOrgDetails expectedOrgDetails2 = createExpectedOrgDetails(vcard2, ids.get(1));
        assertThat(orgDetailsMap.get(ids.get(0)), beanDiffer(expectedOrgDetails1));
        assertThat(orgDetailsMap.get(ids.get(1)), beanDiffer(expectedOrgDetails2));
    }

    @Test
    public void getOrCreateOrgDetails_TwoEqualNewItems_CreatesValidEntry() {
        Vcard vcard1 = miniVcard1();
        Vcard vcard2 = miniVcard1();
        List<Long> ids = create(vcard1, vcard2);

        checkState(ids.get(0).equals(ids.get(1)),
                "для обоих создаваемых элементов возвращаемые id должны быть одинаковы");

        Map<Long, DbOrgDetails> vcards = orgDetailsRepository.getOrgDetails(shard, clientUid, ids);
        DbOrgDetails expectedOrgDetails1 = createExpectedOrgDetails(vcard1, ids.get(0));
        assertThat(vcards.get(ids.get(0)), beanDiffer(expectedOrgDetails1));
    }

    @Test
    public void getOrCreateOrgDetails_OneNewAndOneMatchingItem_CreatesValidEntryAndReturnIdOfExistingEntry() {
        Vcard vcard1 = miniVcard1();
        List<Long> idsOld = create(vcard1);

        Vcard vcard11 = miniVcard1();
        Vcard vcard2 = miniVcard2();
        List<Long> ids = create(vcard11, vcard2);

        checkState(ids.get(0).equals(idsOld.get(0)),
                "возвращенный id для элемента, соответствующего существующему в базе, "
                        + "должен быть равен существующему id");

        Map<Long, DbOrgDetails> orgDetailsMap = get(ids);
        DbOrgDetails expectedOrgDetails1 = createExpectedOrgDetails(vcard1, ids.get(0));
        DbOrgDetails expectedOrgDetails2 = createExpectedOrgDetails(vcard2, ids.get(1));
        assertThat(orgDetailsMap.get(ids.get(0)), beanDiffer(expectedOrgDetails1));
        assertThat(orgDetailsMap.get(ids.get(1)), beanDiffer(expectedOrgDetails2));
    }

    // getOrCreateOrgDetails - проверка уникализации - существующие элементы

    @Test
    public void getOrCreateOrgDetails_OneMatchingItem_ReturnsExistingId() {
        Vcard vcard1 = miniVcard1();
        Vcard vcard11 = miniVcard1();
        List<Long> oldIds = create(vcard1);
        List<Long> ids = create(vcard11);
        assertThat("возвращаемый id элемента, соответствующего существующему в базе, "
                        + "должен быть равен существующему id",
                ids.get(0), equalTo(oldIds.get(0)));
    }

    @Test
    public void getOrCreateOrgDetails_TwoEqualItemsMatchingItemsInDatabase_ReturnsExistingId() {
        Vcard vcard1 = miniVcard1();
        Vcard vcard11 = miniVcard1();
        Vcard vcard12 = miniVcard1();

        List<Long> oldIds = create(vcard1);
        List<Long> ids = create(vcard11, vcard12);

        assertThat("возвращаемые id для одинаковых элементов должны быть равны",
                ids.get(0), equalTo(ids.get(1)));
        assertThat("возвращаемый id элемента, соответствующего существующему в базе, "
                        + "должен быть равен существующему id",
                ids.get(0), equalTo(oldIds.get(0)));
    }

    @Test
    public void getOrCreateOrgDetails_TwoDifferentItemsMatchingItemsInDatabase_ReturnsExistingId() {
        Vcard vcard1 = miniVcard1();
        Vcard vcard2 = miniVcard2();
        Vcard vcard11 = miniVcard1();
        Vcard vcard21 = miniVcard2();

        List<Long> oldIds = create(vcard1, vcard2);
        List<Long> ids = create(vcard11, vcard21);

        checkState(!ids.get(0).equals(ids.get(1)),
                "возвращаемые id разных элементов не должны быть равны между собой");

        assertThat("возвращаемый id элемента, соответствующего существующему в базе, "
                        + "должен быть равен существующему id",
                ids.get(0), equalTo(oldIds.get(0)));
        assertThat("возвращаемый id элемента, соответствующего существующему в базе, "
                        + "должен быть равен существующему id",
                ids.get(1), equalTo(oldIds.get(1)));
    }

    // getOrCreateOrgDetails - проверка уникализации - отличающиеся элементы

    // проверка, что uid участвует в уникализации
    @Test
    public void getOrCreateOrgDetails_OneNewItemWithDifferentUid_ReturnsNewId() {
        ClientInfo clientInfo = clientSteps.createDefaultClient();

        Vcard vcard1 = miniVcard1();
        Vcard vcard2 = miniVcard1();

        List<Long> oldIds = orgDetailsRepository
                .getOrCreateOrgDetails(shard, clientUid, clientId, singletonList(vcard1));
        List<Long> ids = orgDetailsRepository
                .getOrCreateOrgDetails(shard, clientInfo.getUid(), clientId, singletonList(vcard2));

        assertThat("возвращаемый id элемента, не соответствующего существующему в базе, "
                        + "не должен быть равен существующему id",
                ids.get(0), not(equalTo(oldIds.get(0))));
    }

    @Test
    public void getOrCreateOrgDetails_OneNewItemWithDifferentOgrn_ReturnsNewId() {
        Vcard vcard1 = miniVcard1();
        Vcard vcard2 = miniVcard2();

        List<Long> oldIds = orgDetailsRepository
                .getOrCreateOrgDetails(shard, clientUid, clientId, singletonList(vcard1));
        List<Long> ids = orgDetailsRepository
                .getOrCreateOrgDetails(shard, clientUid, clientId, singletonList(vcard2));

        assertThat("возвращаемый id элемента, не соответствующего существующему в базе, "
                        + "не должен быть равен существующему id",
                ids.get(0), not(equalTo(oldIds.get(0))));
    }

    private List<Long> create(Vcard... vcards) {
        List<Long> ids = orgDetailsRepository.getOrCreateOrgDetails(shard, clientUid, clientId, asList(vcards));
        checkState(ids != null && ids.size() == vcards.length,
                "количество возвращаемых id должно быть равно количеству элементов, переданных в метод");
        return ids;
    }

    private Map<Long, DbOrgDetails> get(List<Long> ids) {
        Map<Long, DbOrgDetails> orgDetails = orgDetailsRepository.getOrgDetails(shard, clientUid, ids);

        checkState(orgDetails.keySet().containsAll(ids),
                "в возвращаемой мапе должны присутствовать ключи - id запрашиваемых элементов");
        checkState(orgDetails.size() == ids.size(),
                "размер возвращаемой мапы должен соответствовать количеству запрошенных id");
        return orgDetails;
    }

    private DbOrgDetails createExpectedOrgDetails(Vcard vcard, long orgDetailsId) {
        return new DbOrgDetails()
                .withId(orgDetailsId)
                .withUid(clientUid)
                .withOgrn(vcard.getOgrn());
    }

    private static Vcard miniVcard1() {
        return new Vcard()
                .withOgrn("123");
    }

    private static Vcard miniVcard2() {
        return new Vcard()
                .withOgrn("1234");
    }
}
