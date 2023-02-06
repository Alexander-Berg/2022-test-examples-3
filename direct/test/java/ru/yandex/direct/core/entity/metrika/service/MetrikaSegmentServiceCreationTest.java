package ru.yandex.direct.core.entity.metrika.service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaSegmentPreset;
import ru.yandex.direct.core.entity.retargeting.model.RawMetrikaSegmentPreset;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsPpcDictRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.util.ReflectionTranslator;
import ru.yandex.direct.metrika.client.model.response.Counter;
import ru.yandex.direct.metrika.client.model.response.Segment;
import ru.yandex.direct.rbac.RbacService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.ECOM_ABANDONED_CART_EXPRESSION;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.ECOM_PURCHASE_EXPRESSION;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.ECOM_VIEWED_WITHOUT_PURCHASE_EXPRESSION;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.NOT_BOUNCE_EXPRESSION;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.SEGMENT_TANKER_NAME_BY_TYPE;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.SegmentType.ECOM_ABANDONED_CART;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.SegmentType.ECOM_PURCHASE;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.SegmentType.ECOM_VIEWED_WITHOUT_PURCHASE;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaSegmentServiceCreationTest {
    private static final int DEFAULT_ID = 1;
    private static final String DEFAULT_DOMAIN = "ya.ru";

    private static final RawMetrikaSegmentPreset PRESET_1 = new RawMetrikaSegmentPreset()
            .withPresetId(1)
            .withName("Новые посетители")
            .withExpression("ym:s:isNewUser=='Yes'");
    private static final RawMetrikaSegmentPreset PRESET_7 = new RawMetrikaSegmentPreset()
            .withPresetId(7)
            .withName("Неотказы")
            .withExpression(NOT_BOUNCE_EXPRESSION);

    private static final String ECOM_PURCHASE_SEGMENT_NAME = "Покупатели";
    private static final String ECOM_ABANDONED_CART_SEGMENT_NAME = "Брошенные корзины";
    private static final String ECOM_VIEWED_WITHOUT_PURCHASE_SEGMENT_NAME = "Смотрели товары, но не купили";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private Steps steps;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private ReflectionTranslator reflectionTranslator;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Mock
    private RetargetingGoalsPpcDictRepository retargetingGoalsPpcDictRepository;

    private MetrikaSegmentService metrikaSegmentService;

    private UserInfo userInfo;

    @Before
    public void before() {
        metrikaSegmentService = new MetrikaSegmentService(rbacService, metrikaClientStub, reflectionTranslator,
                ppcPropertiesSupport, retargetingGoalsPpcDictRepository);
        userInfo = steps.userSteps().createDefaultUser();

        metrikaClientStub.clearEditableCounters();
        metrikaClientStub.clearSegments();

        metrikaClientStub.addUserCounter(userInfo.getUid(), DEFAULT_ID);

        when(retargetingGoalsPpcDictRepository.getSegmentPresets()).thenReturn(List.of(PRESET_1, PRESET_7));

        var existingSegments = metrikaClientStub.getSegments(DEFAULT_ID, null);
        assertThat(existingSegments).isEmpty();
    }

    @Test
    public void testEmptyRequest() {
        var result = metrikaSegmentService.createMetrikaSegmentsByPresets(Map.of());
        assertThat(result).isEmpty();
    }

    @Test
    public void testSinglePresetForSingleCounter() {
        metrikaClientStub.addEditableCounter(new Counter().withId(DEFAULT_ID).withDomain(DEFAULT_DOMAIN));

        var result = metrikaSegmentService.createMetrikaSegmentsByPresets(Map.of(DEFAULT_ID,
                List.of(new MetrikaSegmentPreset()
                        .withName(PRESET_1.getName())
                        .withCounterId(DEFAULT_ID)
                        .withDomain(DEFAULT_DOMAIN)
                        .withPresetId(PRESET_1.getPresetId()))));
        assertThat(result).hasSize(1);

        var existingSegments = metrikaClientStub.getSegments(DEFAULT_ID, null);
        assertThat(existingSegments).contains(result.get(0));
    }

    @Test
    public void testGetOrCreateNotBounceSegments_GetExisting() {
        Segment existingSegment = metrikaClientStub.createSegment(DEFAULT_ID, PRESET_7.getName(),
                PRESET_7.getExpression(), null);

        List<Segment> notBounceSegments = metrikaSegmentService.getOrCreatePresetSegments(
                userInfo.getClientId(), List.of(DEFAULT_ID), MetrikaSegmentService.SegmentType.NOT_BOUNCE);
        assertThat(notBounceSegments).hasSize(1);
        Segment segment = notBounceSegments.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(segment.getId()).isEqualTo(existingSegment.getId());
            softly.assertThat(segment.getName()).isEqualTo(existingSegment.getName());
            softly.assertThat(segment.getCounterId()).isEqualTo(existingSegment.getCounterId());
            softly.assertThat(segment.getExpression()).isEqualTo(existingSegment.getExpression());
        });
    }

    @Test
    public void testGetOrCreateNotBounceSegments_CreateNew() {
        metrikaClientStub.addEditableCounter(new Counter().withId(DEFAULT_ID).withDomain(DEFAULT_DOMAIN));

        List<Segment> notBounceSegments = metrikaSegmentService.getOrCreatePresetSegments(
                userInfo.getClientId(), List.of(DEFAULT_ID), MetrikaSegmentService.SegmentType.NOT_BOUNCE);
        assertThat(notBounceSegments).hasSize(1);
        Segment segment = notBounceSegments.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(segment.getCounterId()).isEqualTo(DEFAULT_ID);
            softly.assertThat(segment.getExpression()).isEqualTo(PRESET_7.getExpression());
        });
    }

    @Test
    public void testGetOrCreateNotBounceSegments_NoPermission() {
        List<Segment> notBounceSegments = metrikaSegmentService.getOrCreatePresetSegments(
                userInfo.getClientId(), List.of(DEFAULT_ID), MetrikaSegmentService.SegmentType.NOT_BOUNCE);
        assertThat(notBounceSegments).isEmpty();
    }

    // Метод getOrCreateNotBounceSegmentIds использует метод getOrCreateNotBounceSegments, поэтому один тест на него
    @Test
    public void testGetOrCreateNotBounceSegmentIds_GetExisting() {
        Segment existingSegment = metrikaClientStub.createSegment(DEFAULT_ID, PRESET_7.getName(),
                PRESET_7.getExpression(), null);

        Set<Long> notBounceSegmentIds =
                metrikaSegmentService.getOrCreateNotBounceSegmentIds(userInfo.getClientId(), List.of(DEFAULT_ID));
        assertThat(notBounceSegmentIds).hasSize(1);
        assertThat(notBounceSegmentIds).contains(Long.valueOf(existingSegment.getId()));
    }

    @Test
    public void testCheckAvailabilityOfNotBounceSegments_SegmentsAlreadyExist_True() {
        metrikaClientStub.createSegment(DEFAULT_ID, PRESET_7.getName(),
                PRESET_7.getExpression(), null);

        Boolean isNotBounceShortcutsAvailable = metrikaSegmentService.checkAvailabilityOfPresetSegments(
                userInfo.getClientId(), List.of(DEFAULT_ID), MetrikaSegmentService.SegmentType.NOT_BOUNCE);
        assertThat(isNotBounceShortcutsAvailable).isTrue();

    }

    @Test
    public void testCheckAvailabilityOfNotBounceSegments_SegmentsCanBeCreated_True() {
        metrikaClientStub.addEditableCounter(new Counter().withId(DEFAULT_ID).withDomain(DEFAULT_DOMAIN));

        Boolean isNotBounceShortcutsAvailable = metrikaSegmentService.checkAvailabilityOfPresetSegments(
                userInfo.getClientId(), List.of(DEFAULT_ID), MetrikaSegmentService.SegmentType.NOT_BOUNCE);
        assertThat(isNotBounceShortcutsAvailable).isTrue();

    }

    @Test
    public void testCheckAvailabilityOfNotBounceSegments_NoExistingAndNoPermissionToCreateSegments_False() {
        Boolean isNotBounceShortcutsAvailable = metrikaSegmentService.checkAvailabilityOfPresetSegments(
                userInfo.getClientId(), List.of(DEFAULT_ID), MetrikaSegmentService.SegmentType.NOT_BOUNCE);
        assertThat(isNotBounceShortcutsAvailable).isFalse();
    }


    @Test
    public void checkMetrikaSegmentPresetTranslations() {
        List<String> translations = StreamEx.of(MetrikaSegmentPresetTranslations.class.getMethods())
                .map(Method::getName)
                .toList();
        var typesWithoutTranslation = SEGMENT_TANKER_NAME_BY_TYPE.keySet().stream()
                .filter(key -> !translations.contains(SEGMENT_TANKER_NAME_BY_TYPE.get(key)))
                .collect(Collectors.toList());

        assertThat(typesWithoutTranslation).isEmpty();
    }

    @Test
    public void testGetOrCreateSegmentIdsBySegmentType_GetExisting() {
        Segment ecomPurchaseSegment = metrikaClientStub.createSegment(DEFAULT_ID, ECOM_PURCHASE_SEGMENT_NAME,
                ECOM_PURCHASE_EXPRESSION, null);
        Segment ecomAbandonedCartSegment = metrikaClientStub.createSegment(DEFAULT_ID, ECOM_ABANDONED_CART_SEGMENT_NAME,
                ECOM_ABANDONED_CART_EXPRESSION, null);

        var segmentIdsBySegmentType =
                metrikaSegmentService.getOrCreateSegmentIdsBySegmentType(userInfo.getClientId(), List.of(DEFAULT_ID),
                        Set.of(ECOM_PURCHASE, ECOM_ABANDONED_CART));
        assertThat(segmentIdsBySegmentType).hasSize(2);
        assertThat(segmentIdsBySegmentType.get(ECOM_PURCHASE))
                .contains(Long.valueOf(ecomPurchaseSegment.getId()));
        assertThat(segmentIdsBySegmentType.get(ECOM_ABANDONED_CART))
                .contains(Long.valueOf(ecomAbandonedCartSegment.getId()));
    }

    @Test
    public void testGetOrCreateSegmentIdsBySegmentType_GetExistingAndCreateNew() {
        metrikaClientStub.addEditableCounter(new Counter().withId(DEFAULT_ID).withDomain(DEFAULT_DOMAIN));
        metrikaClientStub.addUserCounter(userInfo.getUid(), DEFAULT_ID + 1);
        Segment ecomViewedWithoutPurchaseSegment = metrikaClientStub.createSegment(DEFAULT_ID + 1,
                ECOM_ABANDONED_CART_SEGMENT_NAME, ECOM_VIEWED_WITHOUT_PURCHASE_EXPRESSION, null);

        var segmentIdsBySegmentType = metrikaSegmentService.getOrCreateSegmentIdsBySegmentType(
                userInfo.getClientId(), List.of(DEFAULT_ID, DEFAULT_ID + 1),
                Set.of(ECOM_PURCHASE, ECOM_ABANDONED_CART, ECOM_VIEWED_WITHOUT_PURCHASE));
        assertThat(segmentIdsBySegmentType).hasSize(3);
        assertThat(segmentIdsBySegmentType.get(ECOM_PURCHASE)).hasSize(1);
        assertThat(segmentIdsBySegmentType.get(ECOM_ABANDONED_CART)).hasSize(1);
        assertThat(segmentIdsBySegmentType.get(ECOM_VIEWED_WITHOUT_PURCHASE)).hasSize(2);
        assertThat(segmentIdsBySegmentType.get(ECOM_VIEWED_WITHOUT_PURCHASE))
                .contains(Long.valueOf(ecomViewedWithoutPurchaseSegment.getId()));
    }

    @Test
    public void testGetOrCreateSegmentsBySegmentType_CreateNew() {
        metrikaClientStub.addEditableCounter(new Counter().withId(DEFAULT_ID).withDomain(DEFAULT_DOMAIN));

        Segment existingEcomAbandonedCartSegment = metrikaClientStub.createSegment(DEFAULT_ID,
                ECOM_ABANDONED_CART_SEGMENT_NAME, ECOM_ABANDONED_CART_EXPRESSION, null);

        var segmentsBySegmentType =
                metrikaSegmentService.getOrCreateSegmentsBySegmentType(userInfo.getClientId(), List.of(DEFAULT_ID),
                        Set.of(ECOM_PURCHASE, ECOM_ABANDONED_CART, ECOM_VIEWED_WITHOUT_PURCHASE));
        assertThat(segmentsBySegmentType).hasSize(3);

        assertThat(segmentsBySegmentType.get(ECOM_PURCHASE)).hasSize(1);
        Segment ecomPurchaseSegment = segmentsBySegmentType.get(ECOM_PURCHASE).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(ecomPurchaseSegment.getCounterId()).isEqualTo(DEFAULT_ID);
            softly.assertThat(ecomPurchaseSegment.getExpression()).isEqualTo(ECOM_PURCHASE_EXPRESSION);
            softly.assertThat(ecomPurchaseSegment.getName()).isEqualTo(ECOM_PURCHASE_SEGMENT_NAME);
        });

        assertThat(segmentsBySegmentType.get(ECOM_ABANDONED_CART)).hasSize(1);
        Segment ecomAbandonedCartSegment = segmentsBySegmentType.get(ECOM_ABANDONED_CART).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(ecomAbandonedCartSegment.getCounterId()).isEqualTo(DEFAULT_ID);
            softly.assertThat(ecomAbandonedCartSegment.getExpression()).isEqualTo(ECOM_ABANDONED_CART_EXPRESSION);
            softly.assertThat(ecomAbandonedCartSegment.getName()).isEqualTo(ECOM_ABANDONED_CART_SEGMENT_NAME);
            softly.assertThat(ecomAbandonedCartSegment.getId()).isEqualTo(existingEcomAbandonedCartSegment.getId());
        });

        assertThat(segmentsBySegmentType.get(ECOM_VIEWED_WITHOUT_PURCHASE)).hasSize(1);
        Segment ecomViewedWithoutPurchaseSegment = segmentsBySegmentType.get(ECOM_VIEWED_WITHOUT_PURCHASE).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(ecomViewedWithoutPurchaseSegment.getCounterId()).isEqualTo(DEFAULT_ID);
            softly.assertThat(ecomViewedWithoutPurchaseSegment.getExpression())
                    .isEqualTo(ECOM_VIEWED_WITHOUT_PURCHASE_EXPRESSION);
            softly.assertThat(ecomViewedWithoutPurchaseSegment.getName())
                    .isEqualTo(ECOM_VIEWED_WITHOUT_PURCHASE_SEGMENT_NAME);
        });
    }

    @Test
    public void testGetOrCreateSegmentsBySegmentType_NoPermission() {
        var segmentsBySegmentType =
                metrikaSegmentService.getOrCreateSegmentsBySegmentType(userInfo.getClientId(), List.of(DEFAULT_ID),
                        Set.of(ECOM_PURCHASE, ECOM_ABANDONED_CART));
        assertThat(segmentsBySegmentType).isEmpty();
    }

    @Test
    public void testGetAvailableEcomSegments_SegmentsAlreadyExist() {
        metrikaClientStub.createSegment(DEFAULT_ID, ECOM_ABANDONED_CART_SEGMENT_NAME, ECOM_ABANDONED_CART_EXPRESSION,
                null);

        Set<MetrikaSegmentService.SegmentType> availableSegmentTypes = metrikaSegmentService.getAvailableEcomSegments(
                userInfo.getClientId(), List.of(DEFAULT_ID));
        assertThat(availableSegmentTypes).containsExactly(ECOM_ABANDONED_CART);
    }

    @Test
    public void testGetAvailableEcomSegments_SegmentsCanBeCreated() {
        metrikaClientStub.addEditableCounter(new Counter().withId(DEFAULT_ID).withDomain(DEFAULT_DOMAIN));

        Set<MetrikaSegmentService.SegmentType> availableSegmentTypes = metrikaSegmentService.getAvailableEcomSegments(
                userInfo.getClientId(), List.of(DEFAULT_ID));
        assertThat(availableSegmentTypes)
                .containsExactlyInAnyOrder(ECOM_PURCHASE, ECOM_ABANDONED_CART, ECOM_VIEWED_WITHOUT_PURCHASE);
    }

    @Test
    public void testGetAvailableEcomSegments_NoExistingAndNoPermissionToCreateSegments() {
        Set<MetrikaSegmentService.SegmentType> availableSegmentTypes = metrikaSegmentService.getAvailableEcomSegments(
                userInfo.getClientId(), List.of(DEFAULT_ID));
        assertThat(availableSegmentTypes).isEmpty();
    }
}
