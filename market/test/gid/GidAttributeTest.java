package ru.yandex.market.jmf.attributes.test.gid;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = GidAttributeTest.Configuration.class)
public class GidAttributeTest {

    private static final Fqn FQN = Fqn.parse("e1");
    private static final String ATTRIBUTE_CODE = "gid";
    private static final String ATTRIBUTE = "e1@gid";

    @Inject
    protected EntityService entityService;
    @Inject
    protected DbService dbService;
    @Inject
    protected MetadataService metadataService;

    public static Stream<Arguments> countOfElements() {
        return Stream.of(
                arguments(1),
                arguments(5)
        );
    }

    /**
     * Проверяем, что в базе находим entity по значению атрибута с использованием фильтра EQ
     */
    @Test
    public void filterEq() {
        var value = createPersistedEntity();
        createPersistedEntity();

        List<Entity> result = filter(Filters.eq(ATTRIBUTE, value.getGid()));

        EntityCollectionAssert.assertThat(result)
                .hasSize(1)
                .allHasAttributes(ATTRIBUTE_CODE, value.getGid());
    }

    /**
     * Проверяем, что в базе находим entity по значению атрибута с использованием фильтра IN
     */
    @ParameterizedTest
    @MethodSource("countOfElements")
    public void filterIn(int countOfElements) {
        var values = createPersistedEntities(countOfElements);
        createPersistedEntity();

        List<Entity> result = filter(Filters.in(ATTRIBUTE, values));
        Assertions.assertEquals(countOfElements, result.size(), "Из базы данных должны получить ожидаемое количество " +
                "элементов");
    }

    /**
     * Проверяем, что в базе находим entity по значению атрибута с использованием фильтра NE
     */
    @Test
    public void filterNe() {
        var value = createPersistedEntity();
        createPersistedEntities(10);

        List<Entity> result = filter(Filters.ne(ATTRIBUTE, value.getGid()));

        EntityCollectionAssert.assertThat(result)
                .hasSize(10);
    }

    public List<Entity> filter(Filter filter) {
        Query q = Query.of(FQN).withFilters(filter).withAttributes(ATTRIBUTE_CODE);
        return doInTx(() -> new ArrayList<>(dbService.list(q)));
    }


    protected List<Entity> createPersistedEntities(int count) {
        List<Entity> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(createPersistedEntity());
        }
        return list;
    }

    protected Entity createPersistedEntity() {
        Entity entity = entityService.newInstance(FQN);
        doInTx(() -> {
            dbService.save(entity);
            return null;
        });
        return entity;
    }

    protected <T> T doInTx(Supplier<T> action) {
        dbService.clear();
        T result = action.get();
        dbService.flush();
        return result;
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:gid_attribute_metadata.xml");
        }
    }
}
