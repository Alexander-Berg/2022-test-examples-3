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
@ContextConfiguration(classes = RelationTest.Configuration.class)
public class RelationTest {

    private static final Fqn FQN_1 = Fqn.parse("e1");
    private static final Fqn FQN_2 = Fqn.parse("e2");
    private static final Fqn FQN_1_1 = Fqn.parse("e1$t1");
    private static final Fqn FQN_2_1 = Fqn.parse("e2$t1");
    @Inject
    MetadataService metadataService;

    @Test
    public void incomingRelation() {
        checkIncomingRelation(FQN_2);
    }

    @Test
    public void incomingRelation_Type() {
        checkIncomingRelation(FQN_2_1);
    }

    @Test
    public void outgoingRelation() {
        checkOutgoingRelation(FQN_1);
    }

    @Test
    public void outgoingRelation_Type() {
        checkOutgoingRelation(FQN_1_1);
    }

    private void checkIncomingRelation(Fqn fqn) {
        Metaclass metaclass = metadataService.getMetaclassOrError(fqn);
        Assertions.assertEquals(1, metaclass.getIncomingRelations().size(), "У метакласса одна входящая связь");
        Relation relation = Iterables.get(metaclass.getIncomingRelations(), 0);
        Assertions.assertEquals("attr1", relation.getAttribute().getCode(), "Связь установлена атрибутом attr1");
        Assertions.assertEquals(FQN_1, relation.getSource().getFqn(), "Связь установлена из метакласс FQN_1");
        Assertions.assertEquals(FQN_2, relation.getTarget().getFqn(), "Связь установлена на метакласс FQN_2");
    }

    private void checkOutgoingRelation(Fqn fqn) {
        Metaclass metaclass = metadataService.getMetaclassOrError(fqn);
        Assertions.assertEquals(1, metaclass.getOutgoingRelations().size(), "У метакласса одна исходящая связть");
        Relation relation = Iterables.get(metaclass.getOutgoingRelations(), 0);
        Assertions.assertEquals("attr1", relation.getAttribute().getCode(), "Связь установлена атрибутом attr1");
        Assertions.assertEquals(FQN_1, relation.getSource().getFqn(), "Связь установлена из метакласс FQN_1");
        Assertions.assertEquals(FQN_2, relation.getTarget().getFqn(), "Связь установлена на метакласс FQN_2");
    }

    @Import({
            MetadataTestConfiguration.class,
            LockServiceTestConfiguration.class,
            TestAttributeStoreConfiguration.class
    })
    static class Configuration {
        @Bean
        public MetadataProvider provider(MetadataProviders providers) {
            return providers.of("classpath:relation_metadata.xml");
        }

        @Bean
        public MetadataAttributeTypeInitializer string() {
            return MetadataTestHelper.object(FQN_2);
        }

        @Bean
        @Primary
        public SessionFactory hibernateProperties() {
            return Mockito.mock(SessionFactory.class);
        }
    }
}
