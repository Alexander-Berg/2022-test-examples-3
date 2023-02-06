package ru.yandex.market.jmf.logic.def.test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.logic.def.impl.BackTypeUtils;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class BackLinkTest {

    public static final String BACK_ATTR = "backAttr";
    private static final Fqn DIRECT_1 = Fqn.of("d1");
    private static final Fqn BACK_1 = Fqn.of("b1");
    private static final Fqn DIRECT_2 = Fqn.of("d2");
    private static final Fqn BACK_2 = Fqn.of("b2");
    @Inject
    BcpService bcpService;
    @Inject
    BackTypeUtils backTypeUtils;
    @Inject
    DbService dbService;
    @Inject
    EntityService entityService;

    @Test
    public void addToBackLink_object() {
        // настройка системы
        Entity direct = createInTx(DIRECT_1);
        Entity back = createInTx(BACK_1);
        // вызов системы
        int result = link(back, direct);

        // проверка утверждений
        Assertions.assertEquals(1, result);
            Entity d = dbService.get(direct.getGid());
            Entity attr = d.getAttribute("attr");
            Assertions.assertEquals(back, attr, "Должно изменится значение прямого атрибута");
    }

    @Test
    public void addToBackLinkSynchronizesBackLinkValue_object() {
        var direct1 = new AtomicReference<Entity>();
        var back = new AtomicReference<Entity>();
        // настройка системы
            direct1.set(createInTx(DIRECT_1));
            back.set(createInTx(BACK_1));
            link(back.get(), direct1.get());
        // вызов системы
            Entity realBack = dbService.get(back.get().getGid());
            Entity direct2 = createInTx(DIRECT_1);
            int result = link(realBack, direct2);

            // проверка утверждений
            Assertions.assertEquals(1, result);

            Assertions.assertEquals(Set.of(direct1.get(), direct2), realBack.getAttribute(BACK_ATTR),
                    "Значение обратной ссылки должно быть в актуальном состоянии");
    }

    @Test
    public void addToBackLink_objects() {
        // настройка системы
        Entity direct = createInTx(DIRECT_2);
        Entity back = createInTx(BACK_2);
        // вызов системы
        int result = link(back, direct);

        // проверка утверждений
        Assertions.assertEquals(1, result);
            Entity d = dbService.get(direct.getGid());
            Collection<Entity> attr = d.getAttribute("attr");
            Assertions.assertEquals(1, attr.size());
            Assertions.assertTrue(attr.contains(back), "Должно изменится значение прямого атрибута");
    }

    @Test
    public void addToBackLinkSynchronizesBackLinkValue_objects() {
        var direct1 = new AtomicReference<Entity>();
        var back = new AtomicReference<Entity>();
        // настройка системы
            direct1.set(createInTx(DIRECT_2));
            back.set(createInTx(BACK_2));
            link(back.get(), direct1.get());
        // вызов системы
            Entity realBack = dbService.get(back.get().getGid());
            Entity direct2 = createInTx(DIRECT_2);
            int result = link(realBack, direct2);

            // проверка утверждений
            Assertions.assertEquals(1, result);

            Assertions.assertEquals(Set.of(direct1.get(), direct2), realBack.getAttribute(BACK_ATTR),
                    "Значение обратной ссылки должно быть в актуальном состоянии");
    }

    @Test
    public void removeFromBackLink_object() {
        // настройка системы
        Entity direct = createInTx(DIRECT_1);
        Entity back = createInTx(BACK_1);

        bcpService.edit(direct, Maps.of("attr", back));

        // вызов системы
        int result = unlink(back, direct);

        // проверка утверждений
        Assertions.assertEquals(1, result);
            Entity d = dbService.get(direct.getGid());
            Entity attr = d.getAttribute("attr");
            Assertions.assertNull(attr, "Должно изменится значение прямого атрибута");
    }

    @Test
    public void removeDirectLink_object() {
        // настройка системы
        Entity direct = createInTx(DIRECT_1);
        Entity back = createInTx(BACK_1);

        // создание ссылки
        bcpService.edit(direct, Maps.of("attr", back));

        // проверка, что ссылка создалась
        Entity b = dbService.get(back.getGid());
        Set<Entity> backAttr = b.getAttribute(BACK_ATTR);
        Assertions.assertEquals(Set.of(direct), backAttr,
                "Значение обратной ссылки должно быть в актуальном состоянии");

        // удаление ссылающегося объекта
        bcpService.delete(direct);

        // проверка, что ссылка удалилась
        b = dbService.get(back.getGid());
        backAttr = b.getAttribute(BACK_ATTR);
        Assertions.assertTrue(backAttr.isEmpty(), "Значение обратной ссылки должно стать пустым");
    }

    @Test
    public void removeDirectLink_object_sameTx() {
        // настройка системы
        Entity direct = create(DIRECT_1);
        Entity back = create(BACK_1);

        // создание ссылки
        bcpService.edit(direct, Maps.of("attr", back));

        // проверяем, что ссылка создалась
        Set<Entity> backAttr = back.getAttribute(BACK_ATTR);
        Assertions.assertEquals(Set.of(direct), backAttr,
                "Значение обратной ссылки должно быть в актуальном состоянии");

        // удаление ссылающегося объекта
        bcpService.delete(direct);

        // проверка, что ссылка удалилась
        Set<Entity> backAttr2 = back.getAttribute(BACK_ATTR);
        Assertions.assertTrue(backAttr2.isEmpty(), "Значение обратной ссылки должно стать пустым");
    }

    @Test
    public void removeFromBackLinkSynchronizesBackLinkValue_object() {
        var direct1 = new AtomicReference<Entity>();
        var direct2 = new AtomicReference<Entity>();
        var back = new AtomicReference<Entity>();
        // настройка системы
            direct1.set(createInTx(DIRECT_1));
            direct2.set(createInTx(DIRECT_1));
            back.set(createInTx(BACK_1));
            bcpService.edit(direct1.get(), Maps.of("attr", back.get()));
            bcpService.edit(direct2.get(), Maps.of("attr", back.get()));

        // вызов системы
            Entity realBack = dbService.get(back.get().getGid());
            int result = unlink(realBack, direct1.get());

            // проверка утверждений
            Assertions.assertEquals(1, result);

            Assertions.assertEquals(Set.of(direct2.get()), realBack.getAttribute(BACK_ATTR),
                    "Значение обратной ссылки должно быть в актуальном состоянии");
    }

    @Test
    public void removeFromBackLink_objects() {
        // настройка системы
        Entity direct = createInTx(DIRECT_2);
        Entity back = createInTx(BACK_2);

        bcpService.edit(direct, Maps.of("attr", back));

        // вызов системы
        int result = unlink(back, direct);

        // проверка утверждений
        Assertions.assertEquals(1, result);
            Entity d = dbService.get(direct.getGid());
            Collection<Entity> attr = d.getAttribute("attr");
            Assertions.assertTrue(attr.isEmpty(), "Должно изменится значение прямого атрибута");
    }

    @Test
    public void removeFromBackLinkSynchronizesBackLinkValue_objects() {
        var direct1 = new AtomicReference<Entity>();
        var direct2 = new AtomicReference<Entity>();
        var back = new AtomicReference<Entity>();
        // настройка системы
            direct1.set(createInTx(DIRECT_2));
            direct2.set(createInTx(DIRECT_2));
            back.set(createInTx(BACK_2));
            bcpService.edit(direct1.get(), Maps.of("attr", back.get()));
            bcpService.edit(direct2.get(), Maps.of("attr", back.get()));
        // вызов системы
            Entity realBack = dbService.get(back.get().getGid());
            int result = unlink(realBack, direct1.get());

            // проверка утверждений
            Assertions.assertEquals(1, result);

            Assertions.assertEquals(Set.of(direct2.get()), realBack.getAttribute(BACK_ATTR),
                    "Значение обратной ссылки должно быть в актуальном состоянии");
    }

    private Integer link(Entity back, Entity direct) {
        return backTypeUtils.edit(back, BACK_ATTR, List.of(direct.getGid()), List.of());
    }

    private int unlink(Entity back, Entity direct) {
        return backTypeUtils.edit(back, BACK_ATTR, Collections.emptyList(),
                Collections.singletonList(direct.getGid()));
    }

    @Nonnull
    private Entity createInTx(Fqn fqn) {
        return create(fqn);
    }

    @Nonnull
    private Entity create(Fqn fqn) {
        return bcpService.create(fqn, Maps.of("title", Randoms.string()));
    }

    @Import(LogicDefaultTestConfiguration.class)
    public static class Configuration {

        @Bean
        public MetadataProvider provider(MetadataProviders providers) {
            return providers.of("classpath:backLink_metadata.xml");
        }

    }

}
