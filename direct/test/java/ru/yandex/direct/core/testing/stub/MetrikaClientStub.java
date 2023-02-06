package ru.yandex.direct.core.testing.stub;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterPermission;
import ru.yandex.direct.core.entity.retargeting.converter.GoalTypeConverter;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.testing.data.TestFullGoals;
import ru.yandex.direct.metrika.client.asynchttp.MetrikaAsyncHttpClient;
import ru.yandex.direct.metrika.client.model.request.GetExistentCountersRequest;
import ru.yandex.direct.metrika.client.model.request.GetGoalsRequest;
import ru.yandex.direct.metrika.client.model.request.GrantAccessRequestStatusesRequest;
import ru.yandex.direct.metrika.client.model.request.RequestGrantsRequest;
import ru.yandex.direct.metrika.client.model.request.RetargetingGoalGroup;
import ru.yandex.direct.metrika.client.model.request.UserCountersExtendedFilter;
import ru.yandex.direct.metrika.client.model.response.Counter;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect;
import ru.yandex.direct.metrika.client.model.response.GetExistentCountersResponse;
import ru.yandex.direct.metrika.client.model.response.GetExistentCountersResponseItem;
import ru.yandex.direct.metrika.client.model.response.GetGoalsResponse;
import ru.yandex.direct.metrika.client.model.response.GoalConversionInfo;
import ru.yandex.direct.metrika.client.model.response.GrantAccessRequestStatus;
import ru.yandex.direct.metrika.client.model.response.GrantAccessRequestStatusesResponse;
import ru.yandex.direct.metrika.client.model.response.RequestGrantsResponse;
import ru.yandex.direct.metrika.client.model.response.RetargetingCondition;
import ru.yandex.direct.metrika.client.model.response.Segment;
import ru.yandex.direct.metrika.client.model.response.TurnOnCallTrackingResponse;
import ru.yandex.direct.metrika.client.model.response.UserCounters;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtended;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtendedResponse;
import ru.yandex.direct.metrika.client.model.response.UserCountersResponse;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class MetrikaClientStub extends MetrikaAsyncHttpClient {
    public MetrikaClientStub() {
        super(mock(PpcPropertiesSupport.class));
    }

    private static final long DEFAULT_ESTIMATE_USERS = 315L;
    private final HashMap<Long, Set<RetargetingCondition>> goalsByUid = new HashMap<>();
    private final HashMap<Long, UserCountersExtended> userCountersExtendedByUid = new HashMap<>();
    private final HashMap<Integer, List<CounterGoal>> counterGoalsByCounters = new HashMap<>();
    private final List<Counter> editableCounters = new ArrayList<>();
    private final List<GetExistentCountersResponseItem> unavailableCounters = new ArrayList<>();
    private final Multimap<Integer, Segment> segmentsByCounter = LinkedHashMultimap.create();

    private final HashMap<Integer, List<Long>> goalIdWithConversionByCounterId = new HashMap<>();
    private final HashMap<Long, Long> conversionVisitsCountByGoalId = new HashMap<>();
    private final Multimap<Long, Long> accessRequestedCounterIdsByUid = HashMultimap.create();

    public void addGoals(Long uid, Set<Goal> retargetingConditionGoals) {
        var goals = goalsByUid.computeIfAbsent(uid, t -> new HashSet<>());
        Map<Long, RetargetingCondition> goalById = listToMap(goals, RetargetingCondition::getId);

        retargetingConditionGoals.forEach((x) -> {
            if (goalById.containsKey(x.getId())) {
                return;
            }
            var cond = new RetargetingCondition()
                    .withId(x.getId())
                    .withName(x.getName())
                    .withGoalSubtype(x.getSubtype())
                    .withType(GoalTypeConverter.toSource(x.getType()))
                    .withCounterId(nvl(x.getCounterId(), 0))
                    .withSectionId(x.getSectionId())
                    .withCounterName(x.getCounterName());
            goals.add(cond);
        });
    }

    public void addConversionVisitsCountToGoalIdForTwoWeeks(Integer counterId, Long goalId, Long visitsCount) {
        goalIdWithConversionByCounterId.put(counterId, List.of(goalId));
        conversionVisitsCountByGoalId.put(goalId, visitsCount);
    }

    public void addUserCounters(Long uid, List<CounterInfoDirect> counters) {
        userCountersExtendedByUid.putIfAbsent(uid, emptyUserCounters(uid));
        UserCountersExtended userCountersExtended = userCountersExtendedByUid.get(uid);

        Set<CounterInfoDirect> resultingCounters = StreamEx.of(counters)
                .append(userCountersExtended.getCounters())
                .toSet();

        userCountersExtended
                .withCounters(mapList(resultingCounters, identity()))
                .withCountersCnt(resultingCounters.size());
    }

    public void addUserCounter(Long uid, CounterInfoDirect counter) {
        addUserCounters(uid, singletonList(counter));
    }

    public void addUserCounter(Long uid, Integer counterId) {
        addUserCounter(uid, buildCounter(counterId));
    }

    public void addUserCounter(Long uid, Integer counterId, String source) {
        addUserCounter(uid, buildCounter(counterId, source));
    }

    public static CounterInfoDirect buildCounter(Integer counterId) {
        return buildCounter(counterId, null);
    }

    public void addUserCounterIds(Long uid, List<Integer> counterIds) {
        addUserCounters(uid, mapList(counterIds, MetrikaClientStub::buildCounter));
    }

    public void addUserCounter(Long uid, Map<Integer, String> sourceById) {
        List<CounterInfoDirect> counters = EntryStream.of(sourceById)
                .mapKeyValue(MetrikaClientStub::buildCounter)
                .toList();
        addUserCounters(uid, counters);
    }

    public void clearUserCounters(Long uid) {
        userCountersExtendedByUid.remove(uid);
    }

    public static CounterInfoDirect buildCounter(Integer counterId, String source) {
        return buildCounter(counterId, source, MetrikaCounterPermission.OWN);
    }

    public static CounterInfoDirect buildCounter(Integer counterId, String name, String domain) {
        return new CounterInfoDirect()
                .withId(counterId)
                .withCounterPermission(MetrikaCounterPermission.OWN.name().toLowerCase())
                .withName(name)
                .withSitePath(domain);
    }

    public static CounterInfoDirect buildCounter(Integer counterId, String source,
                                                 MetrikaCounterPermission counterPermission) {
        return new CounterInfoDirect()
                .withId(counterId)
                .withCounterSource(source)
                .withCounterPermission(counterPermission != null ? counterPermission.name().toLowerCase() : "view")
                .withName(RandomStringUtils.randomAlphabetic(5))
                .withSitePath(RandomStringUtils.randomAlphabetic(5));
    }

    private static GetExistentCountersResponseItem buildExistentCounter(long counterId) {
        return new GetExistentCountersResponseItem().withCounterId(counterId);
    }

    public static UserCountersExtended emptyUserCounters(Long uid) {
        return new UserCountersExtended()
                .withOwner(uid)
                .withCounters(emptyList())
                .withCountersCnt(0);
    }

    public void requestAccess(Long uid, Long counterId) {
        accessRequestedCounterIdsByUid.put(uid, counterId);
    }

    @Override
    public Map<Long, List<RetargetingCondition>> getGoalsByUids(Collection<Long> uids) {
        return StreamEx.of(uids)
                .filter(goalsByUid::containsKey)
                .mapToEntry(goalsByUid::get)
                .mapValues(this::buildList)
                .toMap();
    }

    @Override
    public GetGoalsResponse getGoals(GetGoalsRequest request) {
        var uidToConditions = StreamEx.of(request.getUids())
                .filter(goalsByUid::containsKey)
                .mapToEntry(goalsByUid::get)
                .mapValues(this::buildList)
                //фильтруем по типу цели
                .mapValues(goals -> filterGoalType(request, goals))
                //фильтруем по идентификатору целей
                .mapValues(goals -> filterGoalIds(request, goals))
                //фильтруем по префиксу
                .mapValues(goals -> filterGoalNamePrefix(request, goals))
                .filter(e -> !e.getValue().isEmpty())
                .toMap();
        return new GetGoalsResponse()
                .withUidToConditions(uidToConditions)
                .withHasMoreConditions(false);
    }

    private static RetargetingCondition.Type fromRequestType(ru.yandex.direct.metrika.client.model.request.GoalType goalType) {
        switch (goalType) {
            case GOAL:
                return RetargetingCondition.Type.GOAL;
            case AB_SEGMENT:
                return RetargetingCondition.Type.AB_SEGMENT;
            case SEGMENT:
                return RetargetingCondition.Type.SEGMENT;
            case CDP_SEGMENT:
                return RetargetingCondition.Type.CDP_SEGMENT;
            case AUDIENCE:
                return RetargetingCondition.Type.AUDIENCE;
            case ECOMMERCE:
                return RetargetingCondition.Type.ECOMMERCE;
        }
        throw new IllegalArgumentException("not found type for " + goalType);
    }

    private static List<RetargetingCondition> filterGoalType(GetGoalsRequest request,
                                                             List<RetargetingCondition> conditions) {
        if (request.getGoalType() == null) {
            return conditions;
        }
        return filterList(conditions, goal -> fromRequestType(request.getGoalType()) == goal.getType());
    }

    private static List<RetargetingCondition> filterGoalIds(GetGoalsRequest request,
                                                            List<RetargetingCondition> conditions) {
        if (request.getIds() == null) {
            return conditions;
        }
        var idsSet = new HashSet<>(request.getIds());
        return filterList(conditions, goal -> idsSet.contains(goal.getId()));
    }

    private static List<RetargetingCondition> filterGoalNamePrefix(GetGoalsRequest request,
                                                                   List<RetargetingCondition> conditions) {
        if (request.getPrefix() == null) {
            return conditions;
        }
        return filterList(conditions, goal -> goal.getName().startsWith(request.getPrefix()));
    }

    @Override
    public long estimateUsersByCondition(List<RetargetingGoalGroup> condition) {
        return DEFAULT_ESTIMATE_USERS;
    }

    @Override
    public List<UserCounters> getUsersCountersNum(List<Long> uids) {
        return EntryStream.of(userCountersExtendedByUid)
                .filterKeys(uids::contains)
                .values()
                .map(v -> new UserCounters()
                        .withOwner(v.getOwner())
                        .withCounterIds(mapList(v.getCounters(), CounterInfoDirect::getId))
                        .withCountersCnt(v.getCountersCnt()))
                .toList();
    }

    @Override
    public UserCountersResponse getUsersCountersNum2(List<Long> uids, Collection<Long> counterIds) {
        Set<Long> inputCounterIds = new HashSet<>(counterIds);
        var counters = EntryStream.of(userCountersExtendedByUid)
                .filterKeys(uids::contains)
                .values()
                .map(v -> {
                    var counterList = StreamEx.of(v.getCounters())
                            .map(CounterInfoDirect::getId)
                            .filter(c -> inputCounterIds.contains((long) c))
                            .toList();
                    if (counterList.isEmpty()) {
                        return null;
                    }
                    return new UserCounters()
                            .withOwner(v.getOwner())
                            .withCounterIds(counterList)
                            .withCountersCnt(v.getCountersCnt());
                })
                .nonNull()
                .toList();
        return new UserCountersResponse()
                .withUsers(counters)
                .withHasMoreCounters(false);
    }

    @Override
    public List<UserCountersExtended> getUsersCountersNumExtended(List<Long> uids) {
        return EntryStream.of(userCountersExtendedByUid)
                .filterKeys(uids::contains)
                .values()
                .toList();
    }

    @Override
    public UserCountersExtendedResponse getUsersCountersNumExtended2(List<Long> uids,
                                                                     UserCountersExtendedFilter filter) {
        var counterIds = new HashSet<>(filter.getCounterIds());
        var userCounters = EntryStream.of(userCountersExtendedByUid)
                .filterKeys(uids::contains)
                .mapValues(u -> {
                    var counters = filterList(u.getCounters(), c -> counterIds.contains((long) c.getId()));
                    if (counters.isEmpty()) {
                        return null;
                    }
                    return new UserCountersExtended()
                            .withOwner(u.getOwner())
                            .withCounters(counters)
                            .withCountersCnt(counters.size());
                })
                .nonNullValues()
                .values()
                .toList();
        return new UserCountersExtendedResponse()
                .withUsers(userCounters)
                .withHasMoreCounters(false);
    }

    @Override
    public Map<Integer, List<CounterGoal>> getMassCountersGoalsFromMetrika(Set<Integer> counterIds) {
        return EntryStream.of(counterGoalsByCounters)
                .filterKeys(counterIds::contains)
                .mapValues(x -> mapList(x, Function.identity()))
                .toMap();
    }

    public void addCounterGoal(int counterId, int goalId) {
        addCounterGoal(counterId, new CounterGoal()
                .withId(goalId)
                .withType(CounterGoal.Type.URL));
    }

    public void addCounterGoal(int counterId, CounterGoal goal) {
        List<CounterGoal> counterGoals = counterGoalsByCounters.computeIfAbsent(counterId, id -> new ArrayList<>());
        if (counterGoals.stream()
                .map(CounterGoal::getId)
                .noneMatch(id -> goal.getId() == id)) {
            counterGoals.add(goal);
        }
    }

    @Override
    public Map<Long, Double> getProductImpressionsByCounterId(Set<Long> counterIds, int days) {
        return listToMap(counterIds, Function.identity(), id -> RandomNumberUtils.nextPositiveDouble());
    }

    private List<RetargetingCondition> buildList(Set<RetargetingCondition> v) {
        return new ArrayList<>(v);
    }

    public void addEditableCounter(Counter counter) {
        editableCounters.add(counter);
    }

    public void clearEditableCounters() {
        editableCounters.clear();
    }

    @Override
    public List<Counter> getEditableCounters(@Nullable String tvmUserTicket) {
        return List.copyOf(editableCounters);
    }

    public void addUnavailableCounter(long counterId) {
        addUnavailableCounter(counterId, null);
    }

    public void addUnavailableCounter(long counterId, boolean allowUseGoalsWithoutAccess) {
        unavailableCounters.add(new GetExistentCountersResponseItem()
                .withCounterId(counterId)
                .withAllowUseGoalsWithoutAccess(allowUseGoalsWithoutAccess));
    }

    public void addUnavailableEcommerceCounter(long counterId) {
        unavailableCounters.add(new GetExistentCountersResponseItem()
                .withCounterId(counterId)
                .withEcommerce(true));
    }

    public void addUnavailableCounter(long counterId, String source) {
        unavailableCounters.add(new GetExistentCountersResponseItem()
                .withCounterId(counterId)
                .withCounterSource(source));
    }

    public void addUnavailableCounters(List<Integer> counterIds) {
        unavailableCounters.addAll(mapList(counterIds, c -> buildExistentCounter(c.longValue())));
    }

    public void clearUnavailableCounters() {
        unavailableCounters.clear();
    }

    public void addSegment(int counterId, Segment segment) {
        segmentsByCounter.put(counterId, segment);
    }

    public void clearSegments() {
        segmentsByCounter.clear();
    }

    @Override
    public List<Segment> getSegments(int counterId, @Nullable String tvmUserTicket) {
        return List.copyOf(segmentsByCounter.get(counterId));
    }

    @Override
    public Segment createSegment(int counterId, String name, String expression, @Nullable String tvmUserTicket) {
        var segment = new Segment()
                .withId((int) TestFullGoals.generateGoalId(GoalType.SEGMENT))
                .withCounterId(counterId)
                .withName(name)
                .withExpression(expression);
        segmentsByCounter.put(counterId, segment);
        return segment;
    }

    @Override
    public Map<Long, GoalConversionInfo> getGoalsConversionInfoByCounterIds(Collection<Integer> counterIds, int days) {
        return StreamEx.of(counterIds)
                .map(goalIdWithConversionByCounterId::get)
                .nonNull()
                .flatMap(Collection::stream)
                .mapToEntry(id -> new GoalConversionInfo(id, conversionVisitsCountByGoalId.get(id), null))
                .nonNullValues()
                .toMap();

    }

    @Override
    public Map<Long, GoalConversionInfo> getGoalsConversionInfoByCounterIds(
            Collection<Integer> counterIds, LocalDate dateFrom, LocalDate dateTo) {
        return StreamEx.of(counterIds)
                .map(goalIdWithConversionByCounterId::get)
                .nonNull()
                .flatMap(Collection::stream)
                .mapToEntry(id -> new GoalConversionInfo(id, conversionVisitsCountByGoalId.get(id), null))
                .nonNullValues()
                .toMap();

    }

    @Override
    public GetExistentCountersResponse getExistentCounters(GetExistentCountersRequest request) {
        List<GetExistentCountersResponseItem> responseItems = EntryStream.of(userCountersExtendedByUid)
                .values()
                .flatMap(userCounters -> StreamEx.of(userCounters.getCounters()))
                .map(counter -> new GetExistentCountersResponseItem()
                        .withCounterId((long) counter.getId())
                        .withCounterSource(counter.getCounterSource())
                        .withEcommerce(counter.getEcommerce()))
                .append(unavailableCounters)
                .filter(counter -> request.getCounterIds().contains(counter.getCounterId()))
                .distinct(GetExistentCountersResponseItem::getCounterId)
                .toList();

        return new GetExistentCountersResponse()
                .withResponseItems(responseItems);
    }

    @Override
    public Map<Long, Long> getGoalsStatistics(List<Integer> counterIds, LocalDate dateFrom, LocalDate dateTo) {
        return Map.of();
    }

    @Override
    public GrantAccessRequestStatusesResponse getGrantAccessRequestStatuses(Long uid,
                                                                            GrantAccessRequestStatusesRequest request) {
        Set<Long> accessRequestedCounterIds = StreamEx.of(accessRequestedCounterIdsByUid.get(uid)).toSet();

        List<GrantAccessRequestStatus> responseItems = StreamEx.of(request.getCounterIds())
                .map(counterId -> new GrantAccessRequestStatus()
                        .withCounterId(counterId)
                        .withAccessRequested(accessRequestedCounterIds.contains(counterId)))
                .toList();

        return new GrantAccessRequestStatusesResponse()
                .withGrantAccessRequestStatuses(responseItems);
    }

    @Override
    public RequestGrantsResponse requestCountersGrants(RequestGrantsRequest request) {
        return new RequestGrantsResponse();
    }

    @Override
    public TurnOnCallTrackingResponse turnOnCallTracking(Long counterId) {
        return new TurnOnCallTrackingResponse().withGoal(new CounterGoal());
    }
}
