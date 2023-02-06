package ru.yandex.market.vendors.analytics.core.utils;

import java.io.Serializable;
import java.util.Properties;

import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;

import com.google.auto.service.AutoService;
import org.hibernate.HibernateException;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.IdGeneratorInterpreterImpl;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuilderFactory;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * @author antipov93.
 */
@AutoService(MetadataBuilderFactory.class)
public class TestMetadataBuilderFactory implements MetadataBuilderFactory {

    @Override
    public MetadataBuilderImplementor getMetadataBuilder(
            MetadataSources metadatasources,
            MetadataBuilderImplementor defaultBuilder
    ) {
        defaultBuilder.applyIdGenerationTypeInterpreter(new RepeatableIdGeneratorInterpreter());
        return defaultBuilder;
    }

    public static void reset() {
        RepeatableSequenceGenerator.reset();
    }

    public static class RepeatableIdGeneratorInterpreter extends IdGeneratorInterpreterImpl {

        @Override
        public String determineGeneratorName(GenerationType generationType, GeneratorNameDeterminationContext context) {
            if (generationType == GenerationType.SEQUENCE) {
                return RepeatableSequenceGenerator.class.getName();
            }
            return super.determineGeneratorName(generationType, context);
        }

        @Override
        public void interpretSequenceGenerator(
                SequenceGenerator sequenceGeneratorAnnotation,
                IdentifierGeneratorDefinition.Builder definitionBuilder
        ) {
            definitionBuilder.setStrategy(RepeatableSequenceGenerator.class.getName());
        }
    }

    public static class RepeatableSequenceGenerator extends SequenceStyleGenerator {
        private static int CYCLE = 0;
        private int instanceCycle = CYCLE;

        private Type type = null;
        private Properties params = null;
        private ServiceRegistry registry = null;

        private Database database = null;

        @Override
        public void configure(
                Type type,
                Properties params,
                ServiceRegistry serviceRegistry
        ) throws MappingException {
            this.type = type;
            this.params = params;
            registry = serviceRegistry;

            super.configure(type, params, serviceRegistry);
        }

        @Override
        public Serializable generate(
                SharedSessionContractImplementor session,
                Object object
        ) throws HibernateException {
            if (instanceCycle != CYCLE) {
                super.configure(type, params, registry);
                super.registerExportables(database);
                instanceCycle = CYCLE;
            }
            return super.generate(session, object);
        }

        @Override
        public void registerExportables(Database database) {
            this.database = database;
            super.registerExportables(database);
        }

        private static void reset() {
            CYCLE++;
        }
    }

}
