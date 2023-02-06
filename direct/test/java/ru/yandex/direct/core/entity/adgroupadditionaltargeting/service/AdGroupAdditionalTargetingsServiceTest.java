package ru.yandex.direct.core.entity.adgroupadditionaltargeting.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InternalNetworkAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexUidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.ResultState;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.operation.Applicability.FULL;
import static ru.yandex.direct.operation.Applicability.PARTIAL;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.validation.result.DefectIds.OBJECT_NOT_FOUND;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupAdditionalTargetingsServiceTest {
    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupAdditionalTargetingRepository repository;

    @Autowired
    private AdGroupAdditionalTargetingService service;

    private int shard;
    private AdGroupInfo adGroupInfo1;
    private AdGroupInfo adGroupInfo2;
    private ClientId clientId;

    @Before
    public void setUp() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalFreeCampaign();
        shard = campaignInfo.getShard();
        clientId = campaignInfo.getClientId();
        adGroupInfo1 = steps.adGroupSteps().createDefaultInternalAdGroup().withCampaignInfo(campaignInfo);
        adGroupInfo2 = steps.adGroupSteps().createDefaultInternalAdGroup().withCampaignInfo(campaignInfo);
    }

    @Test
    public void getTargetings_NoIds() {
        new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        List<AdGroupAdditionalTargeting> targetings = service.getTargetings(adGroupInfo1.getClientId(), emptyList());

        assertThat("не должно быть прочитано таргетингов", targetings, is(empty()));
    }

    @Test
    public void getTargetings_ExistingAndNonExistent() {
        InternalNetworkAdGroupAdditionalTargeting targeting = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        repository.add(shard, clientId, singletonList(targeting));

        List<AdGroupAdditionalTargeting> targetings =
                service.getTargetings(adGroupInfo1.getClientId(), asList(6543L, targeting.getId(), 3456L));

        assumeThat("должен быть получен только один созданный таргетинг", targetings, hasSize(1));
        assertThat("должен быть получен именно созданный таргетинг", targetings.get(0), equalTo(targeting));
    }

    @Test
    public void getTargetingsByAdGroupIds_ForDifferentAdGroups() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        YandexUidsAdGroupAdditionalTargeting targeting2 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(singletonList("1637667131551189519"));

        YandexUidsAdGroupAdditionalTargeting targeting3 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(singletonList("1837667131221189811"));

        repository.add(shard, clientId, asList(targeting1, targeting2, targeting3));

        List<AdGroupAdditionalTargeting> targetings =
                service.getTargetingsByAdGroupIds(adGroupInfo1.getClientId(),
                        asList(adGroupInfo1.getAdGroupId(), adGroupInfo2.getAdGroupId()));

        assumeThat("должно быть прочитано три таргетинга", targetings, hasSize(3));
        assertThat("должны быть прочитаны именно созданные таргетинги", targetings,
                containsInAnyOrder(targeting1, targeting2, targeting3));
    }

    @Test
    public void deleteTargetings_NonExistent() {
        InternalNetworkAdGroupAdditionalTargeting targeting = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        repository.add(shard, clientId, singletonList(targeting));

        MassResult<Long> result =
                service.deleteTargetings(adGroupInfo1.getClientId(), singletonList(6642L));

        List<AdGroupAdditionalTargeting> targetings = repository.getByIds(shard, singletonList(targeting.getId()));

        assumeThat("должен быть возвращён один таргетинг", targetings, hasSize(1));

        assertThat("должен быть возвращён созданный ранее таргетинг", targetings.get(0), equalTo(targeting));

        assertThat("операция должна завершиться успешно", result.getState(), equalTo(ResultState.SUCCESSFUL));

        assertThat("удаляемый объект не должен быть найден",
                result.getResult().get(0).getErrors().get(0).getDefect().defectId(), equalTo(OBJECT_NOT_FOUND));
    }

    @Test
    public void deleteTargetings_ExactlyOne() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        YandexUidsAdGroupAdditionalTargeting targeting2 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(asList("1637667131551189519", "1737667131256129520"));

        repository.add(shard, clientId, asList(targeting1, targeting2));

        MassResult<Long> result =
                service.deleteTargetings(adGroupInfo1.getClientId(), singletonList(targeting1.getId()));

        List<AdGroupAdditionalTargeting> targetings = repository
                .getByIds(shard, Stream.of(targeting1, targeting2).map(AdGroupAdditionalTargeting::getId).collect(
                        Collectors.toList()));

        assumeThat("должен быть возвращён один таргетинг", targetings, hasSize(1));

        assertThat("должен быть возвращён созданный ранее таргетинг", targetings.get(0), equalTo(targeting2));

        assertThat("операция должна завершиться успешно", result.getState(), equalTo(ResultState.SUCCESSFUL));
    }

    @Test
    public void createAddOperation_NoTargetings() {
        AdGroupAdditionalTargetingsAddOperation operation =
                service.createAddOperation(FULL, new ArrayList<>(), adGroupInfo1.getClientId());

        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getErrorCount(), is(0));
    }

    @Test
    public void createAddOperation_CreateAndGet() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        YandexUidsAdGroupAdditionalTargeting targeting2 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(asList("1637667131551189519", "1737667131256129520"));

        AdGroupAdditionalTargetingsAddOperation operation =
                service.createAddOperation(FULL, asList(targeting1, targeting2), adGroupInfo1.getClientId());

        MassResult<Long> result = operation.prepareAndApply();

        assumeThat(result, isSuccessful(asList(true, true)));

        List<AdGroupAdditionalTargeting> targetings = service.getTargetingsByAdGroupIds(adGroupInfo1.getClientId(),
                singletonList(adGroupInfo1.getAdGroupId()));

        assumeThat(targetings, hasSize(2));

        assertThat(targetings, containsInAnyOrder(targeting1, targeting2));
    }

    @Test
    public void createAddOperation_CreateAndGetForMultipleAdGroups() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        YandexUidsAdGroupAdditionalTargeting targeting2 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(asList("1637667131551189519", "1737667131256129520"));

        InternalNetworkAdGroupAdditionalTargeting targeting3 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        AdGroupAdditionalTargetingsAddOperation operation =
                service.createAddOperation(FULL, asList(targeting1, targeting2, targeting3),
                        adGroupInfo1.getClientId());

        MassResult<Long> result = operation.prepareAndApply();

        assumeThat(result, isSuccessful(asList(true, true, true)));

        List<AdGroupAdditionalTargeting> targetings = service.getTargetingsByAdGroupIds(adGroupInfo1.getClientId(),
                singletonList(adGroupInfo1.getAdGroupId()));

        assumeThat(targetings, hasSize(2));

        assertThat(targetings, containsInAnyOrder(targeting1, targeting2));

        targetings = service.getTargetingsByAdGroupIds(adGroupInfo1.getClientId(),
                singletonList(adGroupInfo2.getAdGroupId()));

        assumeThat(targetings, hasSize(1));

        assertThat(targetings, containsInAnyOrder(targeting3));
    }

    @Test
    public void createAddOperation_ValidAndInvalid_FullApplicability() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        YandexUidsAdGroupAdditionalTargeting targeting2 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(asList("1637667131551189519", "17376620"));

        AdGroupAdditionalTargetingsAddOperation operation =
                service.createAddOperation(FULL, asList(targeting1, targeting2), adGroupInfo1.getClientId());

        MassResult<Long> result = operation.prepareAndApply();

        assumeThat(result, isSuccessful(asList(true, false)));

        List<AdGroupAdditionalTargeting> targetings = service.getTargetingsByAdGroupIds(adGroupInfo1.getClientId(),
                singletonList(adGroupInfo1.getAdGroupId()));

        assertThat(targetings, empty());
    }

    @Test
    public void createAddOperation_ValidAndInvalid_PartialApplicability() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        YandexUidsAdGroupAdditionalTargeting targeting2 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(asList("1637667131551189519", "17376620"));

        AdGroupAdditionalTargetingsAddOperation operation =
                service.createAddOperation(PARTIAL, asList(targeting1, targeting2),
                        adGroupInfo1.getClientId());

        MassResult<Long> result = operation.prepareAndApply();

        assumeThat(result, isSuccessful(asList(true, false)));

        List<AdGroupAdditionalTargeting> targetings = service.getTargetingsByAdGroupIds(adGroupInfo1.getClientId(),
                singletonList(adGroupInfo1.getAdGroupId()));

        assumeThat(targetings, hasSize(1));

        assertThat(targetings, containsInAnyOrder(targeting1));

        assertThat(targetings, not(hasItem(targeting2)));
    }
}
