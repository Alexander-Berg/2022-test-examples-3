package ru.yandex.market.jmf.attributes;

import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.db.api.relations.MetaclassRelationsService;
import ru.yandex.market.jmf.entity.AttributeTypeService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.metadata.metaclass.Relation;

@Transactional
@ExtendWith(SpringExtension.class)
public abstract class AbstractRelationTest {

    private final Fqn fqn;
    private final String attributeCode;
    private final Fqn relatedFqn;

    @Inject
    protected DbService dbService;
    @Inject
    protected EntityService entityService;
    @Inject
    protected MetadataService metadataService;
    @Inject
    protected MetaclassRelationsService relationService;
    @Inject
    protected AttributeTypeService attributeTypeService;

    private Entity e1_1, e1_2, e1_3, e2_1, e2_2;


    public AbstractRelationTest() {
        this.fqn = Fqn.parse("e1");
        this.attributeCode = "attr";
        this.relatedFqn = Fqn.parse("e2");
    }

    @AfterEach
    public void after() {
        dbService = null;
        entityService = null;
        metadataService = null;
        relationService = null;
    }

    @BeforeEach
    public void setUp() {
        e2_1 = getEntity2();
        e2_2 = getEntity2();

        e1_1 = getEntity1(e2_1);
        e1_2 = getEntity1(e2_1);
        e1_3 = getEntity1(null);
    }

    @Test
    public void outgoing_notEmpty() {
        List<Entity> result = relationService.getOutgoing(e1_1, relation());

        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.contains(e2_1));
    }

    @Test
    public void incoming_notEmpty() {
        List<Entity> result = relationService.getIncoming(e2_1, relation(), null);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(e1_1));
        Assertions.assertTrue(result.contains(e1_2));
    }

    @Test
    public void incoming_empty() {
        List<Entity> result = relationService.getIncoming(e2_2, relation(), null);

        Assertions.assertEquals(0, result.size());
    }

    @Test
    public void outgoing_empty() {
        List<Entity> result = relationService.getOutgoing(e1_3, relation());

        Assertions.assertEquals(0, result.size());
    }

    private Relation relation() {
        Metaclass metaclass = metadataService.getMetaclassOrError(fqn);
        Attribute attribute = metaclass.getAttribute(attributeCode);
        return Iterables.find(metaclass.getOutgoingRelations(), r -> r.attribute().equals(attribute));
    }

    private <T> T doInTx(Supplier<T> action) {
        T result = action.get();
        dbService.flush();
        return result;
    }

    private Entity getEntity1(Entity o) {
        Entity entity = entityService.newInstance(fqn);
        Attribute attribute = metadataService.getMetaclassOrError(fqn).getAttributeOrError(attributeCode);
        Object wrapValue = attributeTypeService.wrap(attribute, o);

        entityService.setAttribute(entity, attributeCode, wrapValue);
        persist(entity);
        return entity;
    }

    private Entity getEntity2() {
        Entity entity = entityService.newInstance(relatedFqn);
        persist(entity);
        return entity;
    }

    private void persist(Entity entity) {
        doInTx(() -> {
            dbService.save(entity);
            return null;
        });
    }

    protected Object extractValue(Object value) {
        return value;
    }
}
