package ru.yandex.market.crm.campaign.services.segments;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.crm.campaign.services.segments.plan.ApplyLinksNode;
import ru.yandex.market.crm.campaign.services.segments.plan.ChainNode;
import ru.yandex.market.crm.campaign.services.segments.plan.CriterionNode;
import ru.yandex.market.crm.campaign.services.segments.plan.GroupNode;
import ru.yandex.market.crm.campaign.services.segments.plan.Node;
import ru.yandex.market.crm.campaign.services.segments.plan.NodeType;
import ru.yandex.market.crm.campaign.services.segments.plan.ResolvePassportIdsNode;
import ru.yandex.market.crm.core.domain.segment.Condition;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.SegmentAlgorithmPart;
import ru.yandex.market.crm.core.domain.segment.SegmentGroupPart;
import ru.yandex.market.crm.core.domain.segment.SegmentPart;
import ru.yandex.market.crm.core.suppliers.TestSubscriptionsTypesSupplier;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author apershukov
 */
public class PlanGeneratorTest {

    private static class TestIdTypesResolver implements IdTypesResolver {

        private final Map<String, Set<UidType>> algorithm2Types;

        TestIdTypesResolver(Algorithm... algorithms) {
            this.algorithm2Types = Stream.of(algorithms)
                    .collect(Collectors.toMap(x -> x.id, x -> x.idTypes));
        }

        @Nonnull
        @Override
        public Set<UidType> resolveIdTypes(SegmentAlgorithmPart part) {
            return algorithm2Types.get(part.getAlgorithmId());
        }
    }

    private static class Algorithm {
        final String id;
        final Set<UidType> idTypes;

        Algorithm(String id, Set<UidType> idTypes) {
            this.id = id;
            this.idTypes = idTypes;
        }
    }

    private static Algorithm algorithm(String id, Set<UidType> idTypes) {
        return new Algorithm(id, idTypes);
    }

    private static Algorithm emailAlgorithm(String id) {
        return algorithm(id, Set.of(UidType.EMAIL));
    }

    private static Algorithm puidAlgorithm(String id) {
        return algorithm(id, Set.of(UidType.PUID));
    }

    private static Algorithm yuidAlgorithm(String id) {
        return algorithm(id, Set.of(UidType.YUID));
    }

    private static Algorithm uuidALgorithm(String id) {
        return algorithm(id, Set.of(UidType.UUID));
    }

    private static Algorithm subscribedAlgorithm() {
        return algorithm(SUBSCRIBED, Set.of(UidType.EMAIL));
    }

    private static SegmentAlgorithmPart algorithmPart(String algorithmId) {
        return new SegmentAlgorithmPart()
                .setAlgorithmId(algorithmId);
    }

    private static SegmentAlgorithmPart subscribedAlgorithmPart(Collection<String> subscriptionTypes) {
        return algorithmPart(SUBSCRIBED)
                .setProperties(Map.of("subscription_types", subscriptionTypes));
    }

    private static SegmentPart not(SegmentPart part) {
        return part.setNot(true);
    }

    private static SegmentPart group(Condition condition, SegmentPart... parts) {
        return new SegmentGroupPart()
                .setCondition(condition)
                .setParts(List.of(parts));
    }

    private static SegmentPart all(SegmentPart... parts) {
        return group(Condition.ALL, parts);
    }

    private static SegmentPart any(SegmentPart... parts) {
        return group(Condition.ANY, parts);
    }

    private static PlanGenerator prepareGenerator(Algorithm... algorithms) {
        TestIdTypesResolver idTypesResolver = new TestIdTypesResolver(algorithms);
        return new PlanGenerator(idTypesResolver, new TestSubscriptionsTypesSupplier());
    }

    private static void assertNodeType(NodeType expectedType, Node node) {
        assertNotNull(node);
        assertEquals(expectedType, node.getType());
    }

    @FunctionalInterface
    private interface NodeChecker extends Consumer<Node> {
    }

    @FunctionalInterface
    private interface ChildChecker extends Consumer<GroupNode.Child> {
    }

    private static void assertChain(Node node, NodeChecker... checkers) {
        pipeline(checkers).accept(node);
    }

    private static NodeChecker pipeline(NodeChecker... checkers) {
        return node -> {
            Node currentNode = node;
            for (int i = 0; i < checkers.length; ++i) {
                checkers[i].accept(currentNode);

                if (i < checkers.length - 1) {
                    if (currentNode instanceof ChainNode) {
                        currentNode = ((ChainNode) currentNode).getInput();
                    } else {
                        throw new AssertionError("No input for node " + currentNode.getType());
                    }
                } else if (currentNode instanceof ChainNode) {
                    throw new AssertionError("Node " + currentNode.getType() + " has its own input");
                }
            }
        };
    }

    private static NodeChecker criterion(String algorithmId) {
        return node -> {
            assertNodeType(NodeType.CRITERION, node);
            CriterionNode criterionNode = (CriterionNode) node;

            SegmentAlgorithmPart part = criterionNode.getPart();
            assertNotNull(part);
            assertEquals(algorithmId, part.getAlgorithmId());
        };
    }

    private static NodeChecker resolvePassportIds(Set<UidType> takeFromInput,
                                                  boolean resolveEmails,
                                                  boolean resolveUuids,
                                                  Boolean usePlatform) {
        return node -> {
            assertNodeType(NodeType.RESOLVE_PASSPORT_IDS, node);
            ResolvePassportIdsNode passportIdsNode = (ResolvePassportIdsNode) node;
            assertEquals(takeFromInput, passportIdsNode.getTakeFromInput());
            assertEquals(resolveEmails, passportIdsNode.resolveEmails());
            assertEquals(resolveUuids, passportIdsNode.resolveUuids());
            assertEquals(usePlatform, passportIdsNode.usePlatform());
        };
    }

    private static NodeChecker resolveEmails(Set<UidType> takeFromInput, boolean usePlatform) {
        return resolvePassportIds(takeFromInput, true, false, usePlatform);
    }

    public static NodeChecker resolveEmails(Set<UidType> takeFromInput) {
        return resolveEmails(takeFromInput, true);
    }

    private static NodeChecker resolveUuidsNode(Set<UidType> takeFromInput) {
        return resolvePassportIds(takeFromInput, false, true, null);
    }

    private static NodeChecker applyLinks(Set<UidType> inputIdTypes,
                                          Set<UidType> takeFromInput,
                                          Set<UidType> takeFromLinks,
                                          LinkingMode mode) {
        return node -> {
            assertNodeType(NodeType.APPLY_LINKS, node);
            ApplyLinksNode linksNode = (ApplyLinksNode) node;
            assertEquals(mode, linksNode.getLinkingMode());
            assertEquals("Unexpected input id types", inputIdTypes, linksNode.getInputIdTypes());
            assertEquals("Unexpected take from input", takeFromInput, linksNode.getTakeFromInput());
            assertEquals("Unexpected take from links", takeFromLinks, linksNode.getTakeFromLinks());
        };
    }

    private static NodeChecker group(Condition condition, boolean isSimple, ChildChecker... checkers) {
        return node -> {
            assertNodeType(NodeType.GROUP, node);
            GroupNode groupNode = (GroupNode) node;
            assertEquals(condition, groupNode.getCondition());
            assertEquals("Group uses unexpected algorithm", isSimple, groupNode.isSimple());

            List<GroupNode.Child> children = groupNode.getChildren();
            assertThat(children, hasSize(checkers.length));

            Set<Integer> matchedIndexes = new HashSet<>();

            for (int i = 0; i < checkers.length; ++i) {
                boolean matched = false;

                for (int j = 0; j < children.size(); ++j) {
                    if (matchedIndexes.contains(j)) {
                        continue;
                    }
                    try {
                        checkers[i].accept(children.get(j));
                        matchedIndexes.add(j);
                        matched = true;
                        break;
                    } catch (AssertionError ignored) {
                    }
                }

                if (!matched) {
                    fail("No node matches matcher with index " + i);
                }
            }
        };
    }

    private static NodeChecker group(Condition condition, boolean isSimple, NodeChecker... checkers) {
        ChildChecker[] childCheckers = Stream.of(checkers)
                .map(checker -> part(false, checker))
                .toArray(ChildChecker[]::new);

        return group(condition, isSimple, childCheckers);
    }

    private static ChildChecker part(boolean isNot, NodeChecker nodeChecker) {
        return child -> {
            assertEquals("Not particle value is unexpected", isNot, child.isNot());
            nodeChecker.accept(child.getNode());
        };
    }

    private static ChildChecker part(NodeChecker nodeChecker) {
        return part(false, nodeChecker);
    }

    private static ChildChecker notPart(NodeChecker nodeChecker) {
        return part(true, nodeChecker);
    }

    private static final String ALGORITHM_ID_1 = "first_algorithm";
    private static final String ALGORITHM_ID_2 = "second_algorithm";
    private static final String ALGORITHM_ID_3 = "third_algorithm";
    private static final String ALGORITHM_ID_4 = "fourth_algorithm";

    private static final String SUBSCRIBED = "subscribed";

    /**
     * Если в сегменте настроено только одно условие типы идентификаторов которого сходятся с
     * типами для которых вычисляется сегмент, план будет состоять только из одного узла
     */
    @Test
    public void testSingleSegmentPlanOfSingleNode() {
        PlanGenerator generator = prepareGenerator(
                emailAlgorithm(ALGORITHM_ID_1)
        );

        Map<String, Object> props = Map.of("criterion_property", "test_value");

        SegmentPart config = algorithmPart(ALGORITHM_ID_1)
                .setProperties(props);

        Node plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.EMAIL));
        assertNotNull(plan);

        assertEquals(NodeType.CRITERION, plan.getType());

        CriterionNode criterionNode = (CriterionNode) plan;
        SegmentAlgorithmPart part = criterionNode.getPart();
        assertNotNull(part);
        assertEquals(ALGORITHM_ID_1, part.getAlgorithmId());
        assertEquals(props, part.getProperties());
    }

    /**
     * Если в сегменте есть условие, работающие по PUID, при вычислении этого сегмента
     * для EMAIL в план добавляется узел с резолвингом паспортных EMAIL
     */
    @Test
    public void testUsePassportEmailsIdIfNecessary() {
        PlanGenerator generator = prepareGenerator(
                puidAlgorithm(ALGORITHM_ID_1)
        );

        SegmentPart config = algorithmPart(ALGORITHM_ID_1);

        Node plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.EMAIL));

        assertChain(plan,
                resolveEmails(Set.of()),
                criterion(ALGORITHM_ID_1)
        );
    }

    /**
     * Если типы идентификаторов условия (YUID) отличаются от тех для которых вычисляется сегмент (PUID)
     * и при этом включена склейка в план добавляется узел её использования.
     *
     * При этом узел резолвинга паспортных идентификаторов отсутствует в плане т. к. целевые типы идентификаторов не
     * входят EMAIL или UUID
     */
    @Test
    public void testUseLinksToConvertIdTypes() {
        PlanGenerator generator = prepareGenerator(
                yuidAlgorithm(ALGORITHM_ID_1)
        );

        SegmentPart config = algorithmPart(ALGORITHM_ID_1);

        Node plan = generator.generate(config, LinkingMode.DIRECT_ONLY, Set.of(UidType.PUID));

        assertChain(plan,
                applyLinks(Set.of(UidType.YUID), Set.of(), Set.of(UidType.PUID), LinkingMode.DIRECT_ONLY),
                criterion(ALGORITHM_ID_1)
        );
    }

    /**
     * Если типы идентификаторов условия не сходятся с теми для которых нужно собрать сегмент и при
     * этом на выходе из сегмента ожидается EMAIL происходит как применение связей так и резолвинг
     * паспорных id.
     */
    @Test
    public void testUseLinksAlongWithPassportIds() {
        PlanGenerator generator = prepareGenerator(
                yuidAlgorithm(ALGORITHM_ID_1)
        );

        SegmentPart config = algorithmPart(ALGORITHM_ID_1);

        Node plan = generator.generate(config, LinkingMode.ALL, Set.of(UidType.EMAIL));

        assertChain(plan,
                resolveEmails(Set.of(UidType.EMAIL)),
                applyLinks(Set.of(UidType.YUID), Set.of(), Set.of(UidType.EMAIL, UidType.PUID), LinkingMode.ALL),
                criterion(ALGORITHM_ID_1)
        );
    }

    /**
     * Если сегмент состоит из группы условий "Каждому" в которой есть два условия, работающие с типом
     * идентификаторов, соответствующим тому для которого вычисляется сегмент, план вычисления состоит
     * только из узла пересечения результатов вычисления узлов условий (в простом режиме).
     *
     * При этом в плане нет ни узлов применения склеек ни узлов резолвинга паспортных идентификаторов,
     * на смотря на то что склейка при вычислении включена.
     */
    @Test
    public void testUseSimpleGroupAlgorithmOnSameIdTypeParts() {
        PlanGenerator generator = prepareGenerator(
                emailAlgorithm(ALGORITHM_ID_1),
                emailAlgorithm(ALGORITHM_ID_2)
        );

        SegmentPart config = all(
                algorithmPart(ALGORITHM_ID_1),
                algorithmPart(ALGORITHM_ID_2)
        );

        Node plan = generator.generate(config, LinkingMode.ALL, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ALL, true,
                        criterion(ALGORITHM_ID_1),
                        criterion(ALGORITHM_ID_2)
                )
        );
    }

    /**
     * Если сегмент состоит из группы условий одно из которых работает с типом идентификаторов
     * который отличается от целевого для вычисления к результату вычисления этого условия перед
     * пересечением будет применена склейка в результате чего будут пересекаться множества
     * идентификаторов одного типа.
     */
    @Test
    public void testUseLinksForSingleChildOfGroupPart() {
        PlanGenerator generator = prepareGenerator(
                puidAlgorithm(ALGORITHM_ID_1),
                yuidAlgorithm(ALGORITHM_ID_2)
        );

        SegmentPart config = all(
                algorithmPart(ALGORITHM_ID_1),
                algorithmPart(ALGORITHM_ID_2)
        );

        Node plan = generator.generate(config, LinkingMode.DIRECT_ONLY, Set.of(UidType.PUID));

        assertChain(plan,
                group(Condition.ALL, true,
                        criterion(ALGORITHM_ID_1),
                        pipeline(
                                applyLinks(Set.of(UidType.YUID), Set.of(), Set.of(UidType.PUID), LinkingMode.DIRECT_ONLY),
                                criterion(ALGORITHM_ID_2)
                        )
                )
        );
    }

    /**
     * Применение склеек при вычислении алгоритма откладывается на максимально поздний этап.
     */
    @Test
    public void testUseLinkingAfterGroupNode() {
        PlanGenerator generator = prepareGenerator(
                yuidAlgorithm(ALGORITHM_ID_1),
                yuidAlgorithm(ALGORITHM_ID_2)
        );

        SegmentPart config = all(
                algorithmPart(ALGORITHM_ID_1),
                algorithmPart(ALGORITHM_ID_2)
        );

        Node plan = generator.generate(config, LinkingMode.ALL, Set.of(UidType.PUID));

        assertChain(plan,
                applyLinks(Set.of(UidType.YUID), Set.of(), Set.of(UidType.PUID), LinkingMode.ALL),
                group(Condition.ALL, true,
                        criterion(ALGORITHM_ID_1),
                        criterion(ALGORITHM_ID_2)
                )
        );
    }

    /**
     * Если в процессе вычисления одного из условий группы был произведен резолвинг
     * паспортных идентификаторов простой алгоритм вычисления группы не используется.
     */
    @Test
    public void testDoNotUseSimpleGroupAlgorithmAfterPassportIdsResolving() {
        PlanGenerator generator = prepareGenerator(
                emailAlgorithm(ALGORITHM_ID_1),
                puidAlgorithm(ALGORITHM_ID_2)
        );

        SegmentPart config = all(
                algorithmPart(ALGORITHM_ID_1),
                algorithmPart(ALGORITHM_ID_2)
        );

        Node plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ALL, false,
                        criterion(ALGORITHM_ID_1),
                        pipeline(
                                resolveEmails(Set.of()),
                                criterion(ALGORITHM_ID_2)
                        )
                )
        );
    }

    /**
     * Если одно условией из группы "Любому" работает с идентификаторами тип которых отличается от целевого типа
     * для сегмента, перед вычислением результата группы идентификаторы из этого условия посредством склейки
     * приводится к типам, ожидаемым от сегмента.
     */
    @Test
    public void testUseLinksForPartOfAnyGroup() {
        PlanGenerator generator = prepareGenerator(
                puidAlgorithm(ALGORITHM_ID_1),
                yuidAlgorithm(ALGORITHM_ID_2)
        );

        SegmentPart config = any(
                algorithmPart(ALGORITHM_ID_1),
                algorithmPart(ALGORITHM_ID_2)
        );

        Node plan = generator.generate(config, LinkingMode.ALL, Set.of(UidType.PUID));

        assertChain(plan,
                group(Condition.ANY, true,
                        criterion(ALGORITHM_ID_1),
                        pipeline(
                                applyLinks(Set.of(UidType.YUID), Set.of(), Set.of(UidType.PUID), LinkingMode.ALL),
                                criterion(ALGORITHM_ID_2)
                        )
                )
        );
    }

    /**
     * Если одно из условий в группе вычитается это его свойство передается
     * узлу в плане вычисления
     */
    @Test
    public void testNotPartInAllGroup() {
        PlanGenerator generator = prepareGenerator(
                emailAlgorithm(ALGORITHM_ID_1),
                emailAlgorithm(ALGORITHM_ID_2)
        );

        SegmentPart config = all(
                algorithmPart(ALGORITHM_ID_1),
                not(algorithmPart(ALGORITHM_ID_2))
        );

        Node plan = generator.generate(config, LinkingMode.ALL, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ALL, true,
                        part(criterion(ALGORITHM_ID_1)),
                        notPart(criterion(ALGORITHM_ID_2))
                )
        );
    }

    /**
     * Если в составе сегмента есть группа, состоящая из условий, работающих по типам идентификаторов,
     * отличающихся от типов, ожидаемых на выходе, склейка применяется к группе целиком, а не к
     * каждому условию, входящему в неё.
     */
    @Test
    public void testUseLinksInEnclosedGroup() {
        PlanGenerator generator = prepareGenerator(
                puidAlgorithm(ALGORITHM_ID_1),
                yuidAlgorithm(ALGORITHM_ID_2),
                yuidAlgorithm(ALGORITHM_ID_3)
        );

        SegmentPart config = all(
                algorithmPart(ALGORITHM_ID_1),
                all(
                        algorithmPart(ALGORITHM_ID_2),
                        algorithmPart(ALGORITHM_ID_3)
                )
        );


        Node plan = generator.generate(config, LinkingMode.ALL, Set.of(UidType.PUID));

        assertChain(plan,
                group(Condition.ALL, true,
                        criterion(ALGORITHM_ID_1),
                        pipeline(
                                applyLinks(Set.of(UidType.YUID), Set.of(), Set.of(UidType.PUID), LinkingMode.ALL),
                                group(Condition.ALL, true,
                                        criterion(ALGORITHM_ID_2),
                                        criterion(ALGORITHM_ID_3)
                                )
                        )
                )
        );
    }

    /**
     * При использовании условия, работающего по uuid, для вычисления email
     * используется сначала склейка, при которой вычисляются email'ы и puid'ы
     * а затем делается резолвинг паспортных email'ов для учеток, полученных на
     * предыдущем шаге
     */
    @Test
    public void testUseLinksCombinedWithPassportIds() {
        PlanGenerator generator = prepareGenerator(
                uuidALgorithm(ALGORITHM_ID_1)
        );

        SegmentPart config = algorithmPart(ALGORITHM_ID_1);

        Node plan = generator.generate(config, LinkingMode.ALL, Set.of(UidType.EMAIL));

        assertChain(plan,
                resolveEmails(Set.of(UidType.EMAIL)),
                applyLinks(Set.of(UidType.UUID), Set.of(), Set.of(UidType.PUID, UidType.EMAIL), LinkingMode.ALL),
                criterion(ALGORITHM_ID_1)
        );
    }

    /**
     * Если на выходе из сегментатора при включенной склейке ожидаются все типы идентификаторов и при этом
     * сегмент состоит из условия, работающего по email, для получения идентификаторов остальных типов
     * используется склейка, которая не добавляет в результат связанные email (все подходящие email уже должны
     * были попасть в результат работы условия)
     *
     * При этом получение email из паспортных учеток так же не используется.
     */
    @Test
    public void testDoNotTakeEmailFromLinksIfCriterionProvidesEmail() {
        PlanGenerator generator = prepareGenerator(
                emailAlgorithm(ALGORITHM_ID_1)
        );

        SegmentPart config = algorithmPart(ALGORITHM_ID_1);

        Node plan = generator.generate(config, LinkingMode.ALL, UidType.ALL);

        assertChain(plan,
                resolveUuidsNode(Set.of(UidType.PUID, UidType.UUID, UidType.YUID, UidType.EMAIL)),
                applyLinks(Set.of(UidType.EMAIL), Set.of(UidType.EMAIL), Set.of(UidType.PUID, UidType.UUID, UidType.YUID), LinkingMode.ALL),
                criterion(ALGORITHM_ID_1)
        );
    }

    /**
     * Если на выходе сегмента, состоящего из условия, работающего по puid'ам, ожидается uuid'ы
     * план вычисления такого сегмента без склеек содержит узел вычисления uuid по паспорным учеткам
     */
    @Test
    public void testResolveUuidsFromPuids() {
        PlanGenerator generator = prepareGenerator(
                puidAlgorithm(ALGORITHM_ID_1)
        );

        SegmentPart config = algorithmPart(ALGORITHM_ID_1);

        Node plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.UUID));

        assertChain(plan,
                resolveUuidsNode(Set.of()),
                criterion(ALGORITHM_ID_1)
        );
    }

    /**
     * Если во вложенной группе условий есть условие, которое работает по EMAIL и PUID, в случае если на выходе
     * из сегмента ожидается EMAIL, резолвинг паспортных EMAIL не применяется ни к результату работы
     * этого условия ни к группе в которую оно входит (иначе есть риск притянуть EMAIL'ы, противоречащие изначальному
     * условию)
     */
    @Test
    public void testDoNotResolvePassportEmailsForGroupContainingEmailCriterion() {
        PlanGenerator generator = prepareGenerator(
                emailAlgorithm(ALGORITHM_ID_1),
                algorithm(ALGORITHM_ID_2, Set.of(UidType.PUID, UidType.EMAIL)),
                puidAlgorithm(ALGORITHM_ID_3)
        );

        SegmentPart config = all(
                algorithmPart(ALGORITHM_ID_1),
                any(
                        algorithmPart(ALGORITHM_ID_2),
                        algorithmPart(ALGORITHM_ID_3)
                )
        );

        Node plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ALL, false,
                        criterion(ALGORITHM_ID_1),
                        group(Condition.ANY, false,
                                criterion(ALGORITHM_ID_2),
                                pipeline(
                                        resolveEmails(Set.of()),
                                        criterion(ALGORITHM_ID_3)
                                )
                        )
                )
        );
    }

    /**
     * Условия, работающие по одинаковым типам идентификаторов, объединяются в группы (ANY)
     */
    @Test
    public void testGroupSameIdTypeCriterionsAny() {
        PlanGenerator generator = prepareGenerator(
                emailAlgorithm(ALGORITHM_ID_1),
                puidAlgorithm(ALGORITHM_ID_2),
                puidAlgorithm(ALGORITHM_ID_3)
        );

        SegmentPart config = any(
                algorithmPart(ALGORITHM_ID_1),
                algorithmPart(ALGORITHM_ID_2),
                algorithmPart(ALGORITHM_ID_3)
        );

        Node plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ANY, false,
                        criterion(ALGORITHM_ID_1),
                        pipeline(
                                resolveEmails(Set.of()),
                                group(Condition.ANY, true,
                                        criterion(ALGORITHM_ID_2),
                                        criterion(ALGORITHM_ID_3)
                                )
                        )
                )
        );
    }

    /**
     * Условия, работающие по одинаковым типам идентификаторов, объединяются в группы (ALL)
     */
    @Test
    public void testGroupSameIdTypeCriterionsAll() {
        PlanGenerator generator = prepareGenerator(
                emailAlgorithm(ALGORITHM_ID_1),
                puidAlgorithm(ALGORITHM_ID_2),
                puidAlgorithm(ALGORITHM_ID_3)
        );

        SegmentPart config = all(
                algorithmPart(ALGORITHM_ID_1),
                algorithmPart(ALGORITHM_ID_2),
                algorithmPart(ALGORITHM_ID_3)
        );

        Node plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ALL, false,
                        criterion(ALGORITHM_ID_1),
                        pipeline(
                                resolveEmails(Set.of()),
                                group(Condition.ALL, true,
                                        criterion(ALGORITHM_ID_2),
                                        criterion(ALGORITHM_ID_3)
                                )
                        )
                )
        );
    }

    /**
     * Вычитаемые условия, работающие по одинаковым типам идентификаторов, объединяются в группы (ANY)
     */
    @Test
    public void testGroupSameIdTypesCriterionsWithNotAny() {
        PlanGenerator generator = prepareGenerator(
                emailAlgorithm(ALGORITHM_ID_1),
                puidAlgorithm(ALGORITHM_ID_2),
                puidAlgorithm(ALGORITHM_ID_3)
        );

        SegmentPart config = any(
                algorithmPart(ALGORITHM_ID_1),
                not(algorithmPart(ALGORITHM_ID_2)),
                not(algorithmPart(ALGORITHM_ID_3))
        );

        Node plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ANY, false,
                        part(criterion(ALGORITHM_ID_1)),
                        notPart(pipeline(
                                resolveEmails(Set.of()),
                                group(Condition.ALL, true,
                                        criterion(ALGORITHM_ID_2),
                                        criterion(ALGORITHM_ID_3)
                                )
                        ))
                )
        );
    }

    /**
     * Вычитаемые условия, работающие по одинаковым типам идентификаторов, объединяются в группы (ALL)
     */
    @Test
    public void testGroupSameIdTypesCriterionsWithNotAll() {
        PlanGenerator generator = prepareGenerator(
                emailAlgorithm(ALGORITHM_ID_1),
                puidAlgorithm(ALGORITHM_ID_2),
                puidAlgorithm(ALGORITHM_ID_3)
        );

        SegmentPart config = all(
                algorithmPart(ALGORITHM_ID_1),
                not(algorithmPart(ALGORITHM_ID_2)),
                not(algorithmPart(ALGORITHM_ID_3))
        );

        Node plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ALL, false,
                        part(criterion(ALGORITHM_ID_1)),
                        notPart(pipeline(
                                resolveEmails(Set.of()),
                                group(Condition.ANY, true,
                                        criterion(ALGORITHM_ID_2),
                                        criterion(ALGORITHM_ID_3)
                                )
                        ))
                )
        );
    }

    /**
     * Если в группу "ЛЮБОМУ" входят условия, работающие по отличающимся типам идентификаторов
     * перед обединением они приводятся к единому типу идентификатора.
     * Тут есть простор для оптимизации.
     */
    @Test
    public void testAnyGroupEmailResolving() {
        PlanGenerator generator = prepareGenerator(
                yuidAlgorithm(ALGORITHM_ID_1),
                algorithm(ALGORITHM_ID_2, Set.of(UidType.PUID, UidType.YUID))
        );

        SegmentPart config = any(
                algorithmPart(ALGORITHM_ID_1),
                algorithmPart(ALGORITHM_ID_2)
        );

        Node plan = generator.generate(config, LinkingMode.DIRECT_ONLY, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ANY, false,
                        pipeline(
                                resolveEmails(Set.of(UidType.EMAIL)),
                                applyLinks(Set.of(UidType.PUID, UidType.YUID), Set.of(UidType.PUID), Set.of(UidType.EMAIL), LinkingMode.DIRECT_ONLY),
                                criterion(ALGORITHM_ID_2)
                        ),
                        pipeline(
                                resolveEmails(Set.of(UidType.EMAIL)),
                                applyLinks(Set.of(UidType.YUID), Set.of(), Set.of(UidType.EMAIL, UidType.PUID), LinkingMode.DIRECT_ONLY),
                                criterion(ALGORITHM_ID_1)
                        )
                )
        );
    }

    /**
     * Случае если условие, работающее по нескольким типам идентификаторов, уже возвращает
     * идентификатор нужного типа склейна к его результату не применяется.
     */
    @Test
    public void testDoNotApplyLinksIfResultAlreadyHasIdOfRequiredType() {
        PlanGenerator generator = prepareGenerator(
                algorithm(ALGORITHM_ID_1, Set.of(UidType.PUID, UidType.UUID, UidType.EMAIL))
        );

        SegmentPart config = algorithmPart(ALGORITHM_ID_1);

        Node plan = generator.generate(config, LinkingMode.DIRECT_ONLY, Set.of(UidType.UUID));

        assertChain(plan,
                criterion(ALGORITHM_ID_1)
        );
    }

    /**
     * Если сегмент ограничен условием наличия подписки с классической логикой, Платформа для резолвинга
     * паспортных адресов не используется т. к. еще до вычисления понятно что все интересующие пары puid-email
     * уже есть в соответствующей таблице на YT
     */
    @Test
    public void testDoNotUsePlatformIfSegmentsContainsSubscribedEmailsOnly() {
        var generator = prepareGenerator(
                subscribedAlgorithm(),
                algorithm(ALGORITHM_ID_1, Set.of(UidType.PUID))
        );

        var config = all(
                subscribedAlgorithmPart(List.of("ADVERTISING")),
                algorithmPart(ALGORITHM_ID_1)
        );

        var plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ALL, false,
                        criterion(SUBSCRIBED),
                        pipeline(
                                resolveEmails(Set.of(), false),
                                criterion(ALGORITHM_ID_1)
                        )
                )
        );
    }

    /**
     * Если сегмент не ограничен условием наличия подписки с классической логикой для резолвинга
     * паспортных адресов используется Платформа
     */
    @Test
    public void testUsePlatformIfSegmentIsNotFullyRestrictedWithSubscriptions() {
        var generator = prepareGenerator(
                subscribedAlgorithm(),
                algorithm(ALGORITHM_ID_1, Set.of(UidType.PUID))
        );

        var config = any(
                subscribedAlgorithmPart(List.of("ADVERTISING")),
                algorithmPart(ALGORITHM_ID_1)
        );

        var plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ANY, false,
                        criterion(SUBSCRIBED),
                        pipeline(
                                resolveEmails(Set.of()),
                                criterion(ALGORITHM_ID_1)
                        )
                )
        );
    }

    /**
     * Если в условии наличия подписки, ограничивающем сегмент, встречается хотя бы одна подписка,
     * работающая по логике "подписан пока не отписался" для резолвинга паспортных адресов используется
     * Платформа
     */
    @Test
    public void testUsePlatformIfSegmentsContainsEmailsSubscribedWithNotUnsubLogicOnly() {
        var generator = prepareGenerator(
                subscribedAlgorithm(),
                puidAlgorithm(ALGORITHM_ID_1)
        );

        var config = all(
                subscribedAlgorithmPart(List.of("ADVERTISING", "WISHLIST")),
                algorithmPart(ALGORITHM_ID_1)
        );

        var plan = generator.generate(config, LinkingMode.NONE, Set.of(UidType.EMAIL));

        assertChain(plan,
                group(Condition.ALL, false,
                        criterion(SUBSCRIBED),
                        pipeline(
                                resolveEmails(Set.of(), true),
                                criterion(ALGORITHM_ID_1)
                        )
                )
        );
    }

    /**
     * Если у сегмента есть несколько вложенных подгрупп, у каждой из которых отсутствуют общие типы идентификаторов
     * вычисление идентификаторов целевого типа происходит сразу внутри этих групп после вычисления условий
     *
     * По мотивам LILUCRM-5171
     */
    @Test
    public void testApplyLinksToAllEnclosedNodes() {
        var generator = prepareGenerator(
                puidAlgorithm(ALGORITHM_ID_1),
                emailAlgorithm(ALGORITHM_ID_2),
                puidAlgorithm(ALGORITHM_ID_3),
                uuidALgorithm(ALGORITHM_ID_4)
        );

        var config = all(
                all(algorithmPart(ALGORITHM_ID_1), algorithmPart(ALGORITHM_ID_2)),
                all(algorithmPart(ALGORITHM_ID_3), algorithmPart(ALGORITHM_ID_4))
        );

        var plan = generator.generate(config, LinkingMode.DIRECT_ONLY, Set.of(UidType.YUID));

        assertChain(plan,
                group(Condition.ALL, true,
                        group(Condition.ALL, true,
                                pipeline(
                                        applyLinks(Set.of(UidType.PUID), Set.of(), Set.of(UidType.YUID), LinkingMode.DIRECT_ONLY),
                                        criterion(ALGORITHM_ID_1)
                                ),
                                pipeline(
                                        applyLinks(Set.of(UidType.EMAIL), Set.of(), Set.of(UidType.YUID), LinkingMode.DIRECT_ONLY),
                                        criterion(ALGORITHM_ID_2)
                                )
                        ),
                        group(Condition.ALL, true,
                                pipeline(
                                        applyLinks(Set.of(UidType.PUID), Set.of(), Set.of(UidType.YUID), LinkingMode.DIRECT_ONLY),
                                        criterion(ALGORITHM_ID_3)
                                ),
                                pipeline(
                                        applyLinks(Set.of(UidType.UUID), Set.of(), Set.of(UidType.YUID), LinkingMode.DIRECT_ONLY),
                                        criterion(ALGORITHM_ID_4)
                                )
                        )
                )
        );
    }
}
