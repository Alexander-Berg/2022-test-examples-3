package ru.yandex.direct.core.entity.metrika.service;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaSegmentPreset;
import ru.yandex.direct.core.entity.retargeting.model.RawMetrikaSegmentPreset;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsPpcDictRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.util.ReflectionTranslator;
import ru.yandex.direct.metrika.client.model.response.Counter;
import ru.yandex.direct.metrika.client.model.response.Segment;
import ru.yandex.direct.rbac.RbacService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaSegmentServicePresetsTest {
    private static final int DEFAULT_ID = 1;
    private static final String DEFAULT_DOMAIN = "ya.ru";
    private static final String DEFAULT_OWNER_LOGIN = "I'm owner";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private Steps steps;
    @Autowired
    private TranslationService translationService;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private ReflectionTranslator reflectionTranslator;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Mock
    private RetargetingGoalsPpcDictRepository retargetingGoalsPpcDictRepository;

    private MetrikaSegmentService metrikaSegmentService;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        metrikaSegmentService = new MetrikaSegmentService(rbacService, metrikaClientStub, reflectionTranslator,
                ppcPropertiesSupport, retargetingGoalsPpcDictRepository);

        clientInfo = steps.clientSteps().createDefaultClient();

        metrikaClientStub.clearEditableCounters();
        metrikaClientStub.clearSegments();

        metrikaClientStub.addUserCounter(clientInfo.getUid(), DEFAULT_ID);

        var translations = MetrikaSegmentPresetTranslations.INSTANCE;
        when(retargetingGoalsPpcDictRepository.getSegmentPresets()).thenReturn(List.of(
                new RawMetrikaSegmentPreset().withPresetId(1)
                        .withName(translationService.translate(translations.newUsers()))
                        .withTankerNameKey("newUsers")
                        .withExpression("ym:s:isNewUser=='Yes'"),
                new RawMetrikaSegmentPreset().withPresetId(2)
                        .withName(translationService.translate(translations.oldUser()))
                        .withTankerNameKey("oldUser")
                        .withExpression("ym:s:isNewUser=='No'"),
                new RawMetrikaSegmentPreset().withPresetId(3)
                        .withName(translationService.translate(translations.organicTrafficSource()))
                        .withTankerNameKey("organicTrafficSource")
                        .withExpression("ym:s:trafficSource=='organic'"),
                new RawMetrikaSegmentPreset().withPresetId(4)
                        .withName(translationService.translate(translations.adTrafficSource()))
                        .withTankerNameKey("adTrafficSource")
                        .withExpression("ym:s:trafficSource=='ad'")
        ));
    }

    @Test
    public void testNoEditableCounters() {
        var result = metrikaSegmentService.getSegmentPresets(clientInfo.getClientId());
        assertThat(result).isEmpty();
    }

    @Test
    public void testSingleCounterWithNoSegments() {
        metrikaClientStub.addEditableCounter(
                new Counter()
                        .withId(DEFAULT_ID)
                        .withOwnerLogin(DEFAULT_OWNER_LOGIN)
                        .withDomain(DEFAULT_DOMAIN));
        var result = metrikaSegmentService.getSegmentPresets(clientInfo.getClientId());
        assertThat(result).isEqualTo(fullPresetsSetForDefaultCounter());
    }

    @Test
    public void testSingleCounterWithSegmentCreatedNotFromPreset() {
        metrikaClientStub.addEditableCounter(
                new Counter()
                        .withId(DEFAULT_ID)
                        .withOwnerLogin(DEFAULT_OWNER_LOGIN)
                        .withDomain(DEFAULT_DOMAIN));
        metrikaClientStub.addSegment(DEFAULT_ID, new Segment()
                .withId(100500)
                .withCounterId(DEFAULT_ID)
                .withName("Metrika native segment")
                .withExpression("ym:s:isSomething=='Yes'"));
        var result = metrikaSegmentService.getSegmentPresets(clientInfo.getClientId());
        assertThat(result).isEqualTo(fullPresetsSetForDefaultCounter());
    }

    @Test
    public void testSingleCounterWithSegmentCreatedByPreset() {
        metrikaClientStub.addEditableCounter(
                new Counter()
                        .withId(DEFAULT_ID)
                        .withOwnerLogin(DEFAULT_OWNER_LOGIN)
                        .withDomain(DEFAULT_DOMAIN));
        var fullPresetSet = fullPresetsSetForDefaultCounter();
        var firstPreset = fullPresetSet.get(0);
        metrikaClientStub.addSegment(DEFAULT_ID, new Segment()
                .withId(firstPreset.getPresetId())
                .withCounterId(DEFAULT_ID)
                .withName(firstPreset.getName())
                .withExpression("some:expression:place:holder"));

        var expected = filterList(fullPresetSet, counter -> !counter.getPresetId().equals(firstPreset.getPresetId()));
        var actual = metrikaSegmentService.getSegmentPresets(clientInfo.getClientId());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testSingleCounterWithSegmentCreateWithAnotherCaseOnly() {
        metrikaClientStub.addEditableCounter(
                new Counter()
                        .withId(DEFAULT_ID)
                        .withOwnerLogin(DEFAULT_OWNER_LOGIN)
                        .withDomain(DEFAULT_DOMAIN));
        var fullPresetSet = fullPresetsSetForDefaultCounter();
        var firstPreset = fullPresetSet.get(0);
        metrikaClientStub.addSegment(DEFAULT_ID, new Segment()
                .withId(firstPreset.getPresetId())
                .withCounterId(DEFAULT_ID)
                .withName(firstPreset.getName().toUpperCase())
                .withExpression("some:expression:place:holder"));

        var expected = filterList(fullPresetSet, counter -> !counter.getPresetId().equals(firstPreset.getPresetId()));
        var actual = metrikaSegmentService.getSegmentPresets(clientInfo.getClientId());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testNoPresetForRepresentativeCounterWithNoEditPermission() {
        var representative = steps.userSteps().createRepresentative(clientInfo);

        metrikaClientStub.addUserCounter(representative.getUid(), DEFAULT_ID + 1);
        metrikaClientStub.addEditableCounter(
                new Counter()
                        .withId(DEFAULT_ID)
                        .withOwnerLogin(DEFAULT_OWNER_LOGIN)
                        .withDomain(DEFAULT_DOMAIN));

        var result = metrikaSegmentService.getSegmentPresets(clientInfo.getClientId());

        var counterIds = listToSet(result, MetrikaSegmentPreset::getCounterId);

        assertThat(counterIds).contains(DEFAULT_ID);
        assertThat(counterIds).doesNotContain(DEFAULT_ID + 1);
    }

    @Test
    public void testPresetsForRepresentativeCounterWithEditPermission() {
        var representative = steps.userSteps().createRepresentative(clientInfo);

        metrikaClientStub.addUserCounter(representative.getUid(), DEFAULT_ID + 1);
        metrikaClientStub.addEditableCounter(
                new Counter()
                        .withId(DEFAULT_ID)
                        .withOwnerLogin(DEFAULT_OWNER_LOGIN)
                        .withDomain(DEFAULT_DOMAIN));
        metrikaClientStub.addEditableCounter(
                new Counter()
                        .withId(DEFAULT_ID + 1)
                        .withOwnerLogin(DEFAULT_OWNER_LOGIN)
                        .withDomain(DEFAULT_DOMAIN));

        var result = metrikaSegmentService.getSegmentPresets(clientInfo.getClientId());
        var expected = fullPresetsSetForDefaultCounter();
        expected.addAll(fullPresetsFor(DEFAULT_ID + 1));

        assertThat(result).isEqualTo(expected);
    }

    private List<MetrikaSegmentPreset> fullPresetsSetForDefaultCounter() {
        return fullPresetsFor(DEFAULT_ID);
    }

    private List<MetrikaSegmentPreset> fullPresetsFor(int counterId) {
        return StreamEx.of(retargetingGoalsPpcDictRepository.getSegmentPresets())
                .map(raw -> new MetrikaSegmentPreset()
                        .withCounterId(counterId)
                        .withPresetId(raw.getPresetId())
                        .withName(raw.getName())
                        .withCounterOwner(DEFAULT_OWNER_LOGIN)
                        .withDomain(DEFAULT_DOMAIN))
                .toList();
    }
}
