package ru.yandex.market.jmf.metadata.test;

import javax.inject.Inject;

import com.google.common.collect.Iterables;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.jmf.lock.LockServiceTestConfiguration;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataAttributeTypeInitializer;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.metadata.metaclass.Relation;

/**
 * Проверяем правильность инициализации {@link Relation связей}.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RelationAbstract2Test.Configuration.class)
public class RelationAbstract2Test {

    private static final Fqn FQN_0 = Fqn.parse("a1");
    private static final Fqn FQN_1 = Fqn.parse("e1");
    private static final Fqn FQN_2 = Fqn.parse("e2");
    private static final Fqn FQN_3 = Fqn.parse("e3");
    @Inject
    MetadataService metadataService;

    @Test
    public void incomingRelation1() {
        checkIncomingRelation(FQN_1);
    }

    @Test
    public void incomingRelation2() {
        checkIncomingRelation(FQN_2);
    }

    @Test
    public void outgoingRelation() {
        Metaclass metaclass = metadataService.getMetaclassOrError(FQN_3);
        Assertions.assertEquals(2, metaclass.getOutgoingRelations().size(), "У метакласса две связи: с e1 и e2");
    }

    private void checkIncomingRelation(Fqn fqn) {
        Metaclass metaclass = metadataService.getMetaclassOrError(fqn);
        Assertions.assertEquals(1, metaclass.getIncomingRelations().size(), "У метакласса одна входящая связь");
        Relation relation = Iterables.get(metaclass.getIncomingRelations(), 0);
        Assertions.assertEquals("attr1", relation.attribute().getCode(), "Связь установлена атрибутом attr1");
        Assertions.assertEquals(FQN_3, relation.source().getFqn(), "Связь установлена из метакласс FQN_1");
        Assertions.assertEquals(fqn, relation.target().getFqn(), "Связь установлена на метакласс FQN_2");
    }

    @Import({
            MetadataTestConfiguration.class,
            LockServiceTestConfiguration.class,
            TestAttributeStoreConfiguration.class
    })
    static class Configuration {
        @Bean
        public MetadataProvider provider(MetadataProviders providers) {
            return providers.of("classpath:relation_abstract_2_metadata.xml");
        }

        @Bean
        public MetadataAttributeTypeInitializer string() {
            return MetadataTestHelper.object(FQN_0);
        }

        @Bean
        @Primary
        public SessionFactory hibernateProperties() {
            return Mockito.mock(SessionFactory.class);
        }
    }
}
