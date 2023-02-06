package ru.yandex.direct.core.entity.addition.callout.repository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.addition.callout.container.CalloutSelection;
import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.model.CalloutDeleted;
import ru.yandex.direct.core.entity.addition.callout.model.CalloutsStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCalloutRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CalloutRepositoryTest {

    private static final CompareStrategy STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath("lastChange"), newPath("createTime"));
    private static final LocalDateTime INITIAL_LASTCHANGE = LocalDateTime.of(2017, 11, 1, 12, 0);

    @Autowired
    private Steps steps;

    @Autowired
    private TestCalloutRepository calloutRepository;

    private ClientInfo clientInfo;
    private Callout defaultCallout;

    private int shard;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        defaultCallout = getDefaultCallout();
    }

    @Test
    public void getExistingCalloutIds() {
        calloutRepository.add(shard, singletonList(defaultCallout));
        assumeThat("после добавления в модели есть id", defaultCallout.getId(), greaterThan(0L));

        Set<Long> actualCallouts = calloutRepository
                .getExistingCalloutIds(shard, clientInfo.getClientId(), singletonList(defaultCallout.getId()),
                        Boolean.TRUE);

        assertThat("полученные уточнения соответстует ожиданию", actualCallouts,
                contains(equalTo(defaultCallout.getId())));
    }


    @Test
    public void getExistingCalloutIds_WithoutDeleted() {
        calloutRepository.add(shard, singletonList(defaultCallout));
        assumeThat("после добавления в модели есть id", defaultCallout.getId(), greaterThan(0L));
        calloutRepository.setDeleted(shard, singletonList(defaultCallout.getId()), Boolean.TRUE);

        Set<Long> actualCallouts = calloutRepository
                .getExistingCalloutIds(shard, clientInfo.getClientId(), singletonList(defaultCallout.getId()),
                        Boolean.FALSE);

        assertThat("полученные уточнения соответстует ожиданию", actualCallouts, empty());
    }

    @Test
    public void getExistingCalloutIds_WithDeleted() {
        calloutRepository.add(shard, singletonList(defaultCallout));
        assumeThat("после добавления в модели есть id", defaultCallout.getId(), greaterThan(0L));
        calloutRepository.setDeleted(shard, singletonList(defaultCallout.getId()), Boolean.TRUE);

        Set<Long> actualCallouts = calloutRepository
                .getExistingCalloutIds(shard, clientInfo.getClientId(), singletonList(defaultCallout.getId()),
                        Boolean.TRUE);

        assertThat("полученные уточнения соответстует ожиданию", actualCallouts,
                contains(equalTo(defaultCallout.getId())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getExistingCalloutsDeleted_TwoCallouts() {
        defaultCallout.withDeleted(Boolean.FALSE);
        Callout callout2 = getDefaultCallout()
                .withDeleted(Boolean.FALSE);
        List<Long> ids = calloutRepository.add(shard, asList(defaultCallout, callout2));
        assumeThat("метод add вернул целые положительные числа", ids, contains(greaterThan(0L), greaterThan(0L)));

        Map<Pair<Long, BigInteger>, CalloutDeleted> calloutDeletedMap =
                calloutRepository.getExistingCallouts(shard, asList(defaultCallout, callout2));

        Map<Pair<Long, BigInteger>, CalloutDeleted> expectedMap = new HashMap<>();
        expectedMap.put(Pair.of(defaultCallout.getClientId(), defaultCallout.getHash()), defaultCallout);
        expectedMap.put(Pair.of(callout2.getClientId(), callout2.getHash()), callout2);
        assertThat("полученная мапа соответствуют ожиданию", calloutDeletedMap,
                beanDiffer(expectedMap).useCompareStrategy(onlyFields(newPath("id"), newPath("deleted"))));
    }

    @Test
    public void getCallouts_OneCallout() {
        calloutRepository.add(shard, singletonList(defaultCallout));
        assumeThat("после добавления в модели есть id", defaultCallout.getId(), greaterThan(0L));

        List<Callout> actualCallouts = calloutRepository.get(shard, singletonList(defaultCallout.getId()));
        defaultCallout.withStatusModerate(CalloutsStatusModerate.READY).withDeleted(Boolean.FALSE);

        assertThat("полученный уточнения соответстует ожиданию", actualCallouts,
                contains(beanDiffer(defaultCallout).useCompareStrategy(STRATEGY)));
    }

    @Test
    public void getCallouts_emptyCalloutIds() {
        List<Callout> actualCallouts = calloutRepository.get(shard, emptyList());
        assertThat("должна быть пустая коллекция уточнений", actualCallouts, empty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getClientsExistingCallouts() {
        Callout callout2 = new Callout()
                .withClientId(clientInfo.getClientId().asLong() + 1)
                .withText(RandomStringUtils.randomAlphanumeric(5))
                .withDeleted(Boolean.FALSE)
                .withLastChange(LocalDateTime.now())
                .withCreateTime(LocalDateTime.now());
        List<Long> ids = calloutRepository.add(shard, asList(defaultCallout, callout2));
        assumeThat("метод add вернул целые положительные числа", ids, contains(greaterThan(0L), greaterThan(0L)));

        Collection<Callout> actualCallouts =
                calloutRepository.getClientExistingCallouts(shard, ClientId.fromLong(defaultCallout.getClientId()));
        defaultCallout.withStatusModerate(CalloutsStatusModerate.READY).withDeleted(Boolean.FALSE);

        assertThat("полученный уточнения соответстует ожиданию", actualCallouts,
                contains(beanDiffer(defaultCallout).useCompareStrategy(STRATEGY)));
    }

    // add
    @Test
    public void add_OneCallout() {
        List<Long> ids = calloutRepository.add(shard, singletonList(defaultCallout));
        assertThat("метод add вернул целое положительное число", ids, contains(greaterThan(0L)));
    }

    @Test
    public void add_OneSameCallout() {
        Callout callout2 = new Callout()
                .withClientId(clientInfo.getClientId().asLong())
                .withText(defaultCallout.getText());
        calloutRepository.add(shard, singletonList(defaultCallout));
        assumeThat("после добавления в модели есть id", defaultCallout.getId(), greaterThan(0L));

        calloutRepository.add(shard, singletonList(callout2));
        assertThat("одинаковые уточнения получили один id", defaultCallout.getId(), equalTo(callout2.getId()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void add_TwoCallouts() {
        Callout callout2 = getDefaultCallout()
                .withDeleted(Boolean.FALSE);
        List<Long> ids = calloutRepository.add(shard, asList(defaultCallout, callout2));
        assertThat("метод add вернул целые положительные числа", ids, contains(greaterThan(0L), greaterThan(0L)));
    }

    @Test
    public void add_CalloutDeleted() {
        calloutRepository.add(shard, singletonList(defaultCallout));
        assumeThat("после добавления в модели есть id", defaultCallout.getId(), greaterThan(0L));

        calloutRepository.setDeleted(shard, singletonList(defaultCallout.getId()), Boolean.TRUE);
        Callout calloutInDB = calloutRepository.get(shard, singletonList(defaultCallout.getId())).get(0);
        assumeThat("уточнение удалено", calloutInDB.getDeleted(), equalTo(Boolean.TRUE));

        Callout newSameCallout = new Callout()
                .withClientId(clientInfo.getClientId().asLong())
                .withText(defaultCallout.getText());
        calloutRepository.add(shard, singletonList(newSameCallout));
        assumeThat("после добавления в модели есть id", newSameCallout.getId(), greaterThan(0L));
        assumeThat("одинаковые уточнения получили один id", defaultCallout.getId(), equalTo(newSameCallout.getId()));

        Callout sameCalloutInDB = calloutRepository.get(shard, singletonList(defaultCallout.getId())).get(0);
        assertThat("уточнение востановлено из удаленных", sameCalloutInDB.getDeleted(), equalTo(Boolean.FALSE));
    }

    @Test
    public void add_Callout_SetModelId() {
        List<Long> ids = calloutRepository.add(shard, singletonList(defaultCallout));
        assumeThat("метод add вернул целое положительное число", ids, contains(greaterThan(0L)));

        assertThat("после добавления в модели есть id", defaultCallout.getId(), greaterThan(0L));
    }

    @Test
    public void add_Callout_SaveCalloutDataCorrectly() {
        calloutRepository.add(shard, singletonList(defaultCallout));

        List<Callout> savedCallout =
                calloutRepository.get(shard, singletonList(defaultCallout.getId()));
        defaultCallout.withStatusModerate(CalloutsStatusModerate.READY).withDeleted(Boolean.FALSE);

        assertThat("данные извлеченного уточнения не соответствуют данным ранее сохраненного",
                savedCallout, contains(beanDiffer(defaultCallout).useCompareStrategy(STRATEGY)));
    }

    //update
    @Test
    public void setDeleted() {
        calloutRepository.add(shard, singletonList(defaultCallout));
        assumeThat("после добавления в модели есть id", defaultCallout.getId(), greaterThan(0L));

        calloutRepository.setDeleted(shard, singletonList(defaultCallout.getId()), Boolean.TRUE);
        List<Callout> actualCallout =
                calloutRepository.get(shard, singletonList(defaultCallout.getId()));
        defaultCallout.withStatusModerate(CalloutsStatusModerate.READY).withDeleted(Boolean.TRUE);

        assertThat("данные извлеченного уточнения не соответствуют данным ранее сохраненного",
                actualCallout, contains(beanDiffer(defaultCallout).useCompareStrategy(STRATEGY)));
    }

    //delete
    @Test
    public void delete_Callout() {
        calloutRepository.add(shard, singletonList(defaultCallout));
        assumeThat("после добавления в модели есть id", defaultCallout.getId(), greaterThan(0L));

        calloutRepository.delete(shard, singletonList(defaultCallout.getId()));

        Collection<Callout> clientCallouts =
                calloutRepository.getClientExistingCallouts(shard, clientInfo.getClientId());

        assertThat("уточнение удалилось",
                clientCallouts, empty());
    }

    @Test
    public void get_CalloutSelection_ByIds() {
        List<Long> ids = calloutRepository.add(shard, singletonList(defaultCallout));
        assumeThat(ids, hasSize(1));
        List<Callout> callouts = calloutRepository.get(
                shard, clientInfo.getClientId(), new CalloutSelection().withIds(ids), LimitOffset.maxLimited());
        assertThat(callouts, contains(hasProperty(Callout.ID.name(), equalTo(ids.get(0)))));
    }

    @Test
    public void get_CalloutSelection_ByIdsWithLimit() {
        List<Long> ids = calloutRepository.add(shard, Arrays.asList(getDefaultCallout(), getDefaultCallout()));
        assumeThat(ids, hasSize(2));
        List<Callout> callouts = calloutRepository.get(
                shard, clientInfo.getClientId(), new CalloutSelection().withIds(ids), LimitOffset.limited(1, 0));
        assertThat(callouts, contains(hasProperty(Callout.ID.name(), equalTo(ids.get(0)))));
        callouts = calloutRepository.get(
                shard, clientInfo.getClientId(), new CalloutSelection().withIds(ids), LimitOffset.limited(1, 1));
        assertThat(callouts, contains(hasProperty(Callout.ID.name(), equalTo(ids.get(1)))));
    }

    @Test
    public void get_CalloutSelection_ByIdsSelectWithoutDeleted() {
        List<Long> ids = calloutRepository.add(shard, Arrays.asList(getDefaultCallout(), getDefaultCallout()));
        Long deletedCalloutId = ids.get(0);
        calloutRepository.setDeleted(shard, singletonList(deletedCalloutId), true);

        List<Callout> callouts = calloutRepository.get(
                shard, clientInfo.getClientId(),
                new CalloutSelection().withIds(ids).withDeleted(false),
                LimitOffset.maxLimited());

        assertThat("в результате не должно быть удалённого уточнения", callouts, contains(
                hasProperty(Callout.ID.name(), not(equalTo(deletedCalloutId)))
        ));
    }

    @Test
    public void get_CalloutSelection_ByIdsSelectWithDeletedOnly() {
        List<Long> ids = calloutRepository.add(shard, Arrays.asList(getDefaultCallout(), getDefaultCallout()));
        Long deletedCalloutId = ids.get(0);
        calloutRepository.setDeleted(shard, singletonList(deletedCalloutId), true);

        List<Callout> callouts = calloutRepository.get(
                shard, clientInfo.getClientId(),
                new CalloutSelection().withIds(ids).withDeleted(true),
                LimitOffset.maxLimited());

        assertThat("в результате должно быть только удалённое уточнение", callouts, contains(
                hasProperty(Callout.ID.name(), equalTo(deletedCalloutId))
        ));
    }

    @Test
    public void get_CalloutSelection_ByIdsSelectBothDeletedAndNot() {
        List<Long> ids = calloutRepository.add(shard, Arrays.asList(getDefaultCallout(), getDefaultCallout()));
        Long deletedCalloutId = ids.get(0);
        calloutRepository.setDeleted(shard, singletonList(deletedCalloutId), true);

        List<Callout> callouts = calloutRepository.get(
                shard, clientInfo.getClientId(),
                new CalloutSelection().withIds(ids),
                LimitOffset.maxLimited());

        //noinspection unchecked
        assertThat("в результате должны быть уточнения и удалённые и нет", callouts, contains(
                hasProperty(Callout.ID.name(), equalTo(ids.get(0))),
                hasProperty(Callout.ID.name(), equalTo(ids.get(1)))
        ));
    }

    @Test
    public void get_CalloutSelection_ByStatuses() {
        List<Long> ids = calloutRepository.add(shard, Arrays.asList(getDefaultCallout(), getDefaultCallout()));
        Long moderatedCalloutId = ids.get(0);
        calloutRepository.setStatusModerate(shard, singletonList(moderatedCalloutId), CalloutsStatusModerate.YES);

        List<Callout> callouts = calloutRepository.get(
                shard, clientInfo.getClientId(),
                new CalloutSelection().withIds(ids).withStatuses(CalloutsStatusModerate.YES),
                LimitOffset.maxLimited());

        //noinspection unchecked
        assertThat("в результате должно быть только промодерированное уточнение", callouts, contains(
                hasProperty(Callout.ID.name(), equalTo(moderatedCalloutId))
        ));
    }

    @Test
    public void get_CalloutSelection_ByLastChange() {
        List<Long> ids = calloutRepository.add(shard, Arrays.asList(getDefaultCallout(), getDefaultCallout()));

        Long modifiedCalloutId = ids.get(0);
        // вместе с изменением статуса модерации должено измениться время последней модификации
        calloutRepository.setStatusModerate(shard, singletonList(modifiedCalloutId), CalloutsStatusModerate.READY);

        List<Callout> callouts = calloutRepository.get(
                shard, clientInfo.getClientId(),
                new CalloutSelection().withIds(ids).withLastChangeGreaterOrEqualThan(INITIAL_LASTCHANGE.plusSeconds(1)),
                LimitOffset.maxLimited());

        //noinspection unchecked
        assertThat("в результате должно быть только модифицированное уточнение", callouts, contains(
                hasProperty(Callout.ID.name(), equalTo(modifiedCalloutId))
        ));
    }

    @Test
    public void add_TwoParallelRequestsWithSameCallout_NoDuplicates() {
        List<Callout> callouts = singletonList(defaultCallout);
        Map<Pair<Long, BigInteger>, CalloutDeleted> existCalloutMap =
                calloutRepository.getExistingCalloutsMap(shard, callouts);

        List<Long> ids = calloutRepository.add(shard, singletonList(copyCallout(defaultCallout)));

        List<Callout> newCallouts = filterList(callouts, sl -> sl.getId() == null);
        int inserted = calloutRepository.insertCallouts(shard, callouts, existCalloutMap);
        List<Long> calloutIds = calloutRepository.getCalloutIds(shard, inserted, callouts, newCallouts);

        assertThat("Должны получиться одинаковые id", ids, is(calloutIds));
    }

    private Callout getDefaultCallout() {
        return new Callout()
                .withClientId(clientInfo.getClientId().asLong())
                .withText(RandomStringUtils.randomAlphanumeric(5))
                .withLastChange(INITIAL_LASTCHANGE)
                .withCreateTime(LocalDateTime.now());
    }

    private Callout copyCallout(Callout callout) {
        return new Callout()
                .withClientId(callout.getClientId())
                .withText(callout.getText())
                .withLastChange(callout.getLastChange())
                .withCreateTime(callout.getCreateTime());
    }
}
