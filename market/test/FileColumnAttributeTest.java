package ru.yandex.market.jmf.logic.def.test;

import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class FileColumnAttributeTest {

    protected final Fqn fqn = Fqn.of("fileE1");
    protected final String attributeCode = "attr";

    @Inject
    protected EntityService entityService;
    @Inject
    protected MetadataService metadataService;
    @Inject
    protected DbService dbService;
    @Inject
    protected BcpService bcpService;

    protected Object randomAttributeValue() {
        return bcpService.create(Attachment.FQN_DEFAULT, Map.of(
                Attachment.NAME, Randoms.string(),
                Attachment.CONTENT_TYPE, Randoms.string(),
                Attachment.URL, Randoms.url()
        ));
    }

    @BeforeEach
    public void init() {
        for (Entity o : dbService.list(Query.of(fqn))) {
            dbService.delete(o);
        }
        dbService.flush();
    }

    private Entity create(Object value) {
        return bcpService.create(fqn, Maps.of(
                attributeCode, value
        ));
    }

    protected void persist(Entity entity) {
        doInTx(() -> {
            dbService.save(entity);
            return null;
        });
    }

    private <T> T doInTx(Supplier<T> action) {
        dbService.flush();
        T result = action.get();
        dbService.flush();
        return result;
    }

    /**
     * Проверяем, что из базы получаем ранее сохраненное значение
     */
    @Test
    public void get() {
        Object value = randomAttributeValue();
        var entity = create(value);

        Entity result = get(entity);

        Object attributeValue = result.getAttribute(attributeCode);
        Assertions.assertEquals(value, attributeValue, "Из базы данных должны получить ранее сохраненное значение");
    }

    private Entity get(Entity e) {
        return doInTx(() -> dbService.get(e.getGid()));
    }

    @Test
    public void get_null() {
        Entity entity = create(null);

        Entity result = get(entity);

        Entity attributeValue = result.getAttribute(attributeCode);
        Assertions.assertNull(attributeValue, "Из базы данных должны получить ранее сохраненное значение");
    }

    /**
     * Проверяем успешность сохранения entity с заполненным значением атрибута
     */
    @Test
    public void persist() {
        Object value = randomAttributeValue();
        Entity entity = create(value);

        Assertions.assertNotNull(entity.getGid(), "После сохранения у entity должен сформироваться gid");
    }

    /**
     * Проверяем успешность сохранения entity с значением атрибута равным null
     */
    @Test
    public void persist_null() {
        Entity entity = create(null);

        Assertions.assertNotNull(entity.getGid(), "После сохранения у entity должен сформироваться gid");
    }

}
