package ru.yandex.market.jmf.module.entity.snapshot;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.AttributeTypeService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.GidService;
import ru.yandex.market.jmf.entity.HasFqn;
import ru.yandex.market.jmf.entity.HasId;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.Fqns;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
@SpringJUnitConfig(InternalModuleEntitySnapshotTestConfiguration.class)
public class SnapshottedByStatusLogicTest {

    private static final Fqn SNAPSHOTTED_FQN = Fqn.of("testSnapshottedByStatusMetaclass");
    private static final Fqn SNAPSHOTTED_WITH_ALL_ATTRIBUTES_FQN = Fqn.of("testSnapshottedMetaclassWithAllAttributes");

    private static final String NOT_SNAPSHOTTED_ATTR_CODE = "notSnapshottedAttr";
    private static final String SNAPSHOTTED_ATTR_CODE_1 = "snapshottedAttr1";
    private static final String SNAPSHOTTED_ATTR_CODE_2 = "snapshottedAttr2";

    @Inject
    BcpService bcpService;

    @Inject
    DbService dbService;

    @Inject
    GidService gidService;

    @Inject
    AttributeTypeService attributeTypeService;

    @Test
    public void testCreateWithSnapshotAllAttributes() {
        Entity entity = bcpService.create(SNAPSHOTTED_WITH_ALL_ATTRIBUTES_FQN, Map.of(
                HasWorkflow.STATUS, SnapshottedByStatus.Statuses.ACTIVE,
                SNAPSHOTTED_ATTR_CODE_1, Randoms.string(),
                SNAPSHOTTED_ATTR_CODE_2, Randoms.string()
        ));

        EntitySnapshot snapshot = getSingleSnapshot(SNAPSHOTTED_WITH_ALL_ATTRIBUTES_FQN);

        assertEquals(entity, snapshot.getEntity());
        assertNotNull(snapshot.getCreationTime());
        assertSnapshot(entity, snapshot.getSnapshot());
    }

    @Test
    public void testCreateWithoutSnapshot() {
        bcpService.create(SNAPSHOTTED_FQN, Map.of());
        assertSnapshotsIsEmpty();
    }

    @Test
    public void testCreateWithSnapshot() {
        Entity entity = bcpService.create(SNAPSHOTTED_FQN, Map.of(
                HasWorkflow.STATUS, SnapshottedByStatus.Statuses.ACTIVE
        ));

        EntitySnapshot snapshot = getSingleSnapshot(SNAPSHOTTED_FQN);

        assertEquals(entity, snapshot.getEntity());
        assertNotNull(snapshot.getCreationTime());
        assertSnapshot(entity, snapshot.getSnapshot());
    }

    @Test
    public void testSnapshotOnEdit() {
        Entity entity = bcpService.create(SNAPSHOTTED_FQN, Map.of(
                NOT_SNAPSHOTTED_ATTR_CODE, Randoms.string(),
                SNAPSHOTTED_ATTR_CODE_1, Randoms.string()
        ));
        assertEquals(SnapshottedByStatus.Statuses.DRAFT, entity.getAttribute(HasWorkflow.STATUS));
        assertSnapshotsIsEmpty();

        bcpService.edit(entity, Map.of(HasWorkflow.STATUS, SnapshottedByStatus.Statuses.REVIEW));
        assertSnapshotsIsEmpty();

        bcpService.edit(entity, Map.of(HasWorkflow.STATUS, SnapshottedByStatus.Statuses.ACTIVE));
        EntitySnapshot snapshot = getSingleSnapshot(SNAPSHOTTED_FQN);

        assertEquals(entity, snapshot.getEntity());
        assertNotNull(snapshot.getCreationTime());
        assertSnapshot(entity, snapshot.getSnapshot());
    }

    @Test
    public void testReturnToDraftOnEditSnapshottedAttribute() {
        Entity entity = bcpService.create(SNAPSHOTTED_FQN, Map.of(
                HasWorkflow.STATUS, SnapshottedByStatus.Statuses.ACTIVE
        ));

        bcpService.edit(entity, Map.of(SNAPSHOTTED_ATTR_CODE_1, Randoms.string()));
        assertEquals(SnapshottedByStatus.Statuses.DRAFT, entity.getAttribute(HasWorkflow.STATUS));
        getSingleSnapshot(SNAPSHOTTED_FQN);
    }

    @Test
    public void testNoReturnToDraftOnEditNotSnapshottedAttribute() {
        Entity entity = bcpService.create(SNAPSHOTTED_FQN, Map.of(
                HasWorkflow.STATUS, SnapshottedByStatus.Statuses.ACTIVE
        ));

        bcpService.edit(entity, Map.of(NOT_SNAPSHOTTED_ATTR_CODE, Randoms.string()));
        assertEquals(SnapshottedByStatus.Statuses.ACTIVE, entity.getAttribute(HasWorkflow.STATUS));
        getSingleSnapshot(SNAPSHOTTED_FQN);
    }

    private void assertSnapshotsIsEmpty() {
        List<EntitySnapshot> snapshotList = dbService.list(Query.of(Fqns.snapshotOf(SNAPSHOTTED_FQN)));
        assertEquals(0, snapshotList.size());
    }

    private EntitySnapshot getSingleSnapshot(Fqn fqn) {
        List<EntitySnapshot> snapshotList = dbService.list(Query.of(Fqns.snapshotOf(fqn)));
        assertEquals(1, snapshotList.size());
        return snapshotList.get(0);
    }

    private void assertSnapshot(Entity entity, JsonNode snapshot) {
        Metaclass metaclass = entity.getMetaclass();
        Set<String> attributes = metaclass.hasExtension(SnapshotAttributesExtension.class)
                ? metaclass.getExtension(SnapshotAttributesExtension.class).getAttributes()
                : getAllSnapshottedAttributes(metaclass);
        assertEquals(attributes.size() + 2, snapshot.size());
        assertEquals(gidService.parse(entity.getGid()).getId(), snapshot.get(HasId.ID).longValue());
        assertEquals(entity.getFqn().toString(), snapshot.get(HasFqn.FQN).textValue());
        for (String attr : attributes) {
            Object value = attributeTypeService.wrap(metaclass.getAttributeOrError(attr), snapshot.get(attr));
            assertEquals(entity.getAttribute(attr), value);
        }
    }

    private Set<String> getAllSnapshottedAttributes(Metaclass metaclass) {
        return metaclass.getAttributes().stream()
                .filter(a -> a.getType().isCopyable() && a.getStore().isCopyable())
                .map(Attribute::getCode)
                .collect(Collectors.toSet());
    }
}
