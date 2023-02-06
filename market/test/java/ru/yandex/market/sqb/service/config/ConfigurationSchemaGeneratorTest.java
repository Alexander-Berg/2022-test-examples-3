package ru.yandex.market.sqb.service.config;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.model.vo.QueryVO;

/**
 * Fake-тест: генератор схемы для модели запроса.
 *
 * @author Vladislav Bauer
 */
@Disabled
final class ConfigurationSchemaGeneratorTest {

    @Test
    void generateSchemaForQueryVO() throws Exception {
        generate(QueryVO.class);
    }


    private void generate(final Class<?> objectClass) throws Exception {
        final JAXBContext context = JAXBContext.newInstance(objectClass);
        final SchemaOutputResolver resolver = new CustomSchemaOutputResolver();

        context.generateSchema(resolver);
    }


    private static class CustomSchemaOutputResolver extends SchemaOutputResolver {

        /**
         * {@inheritDoc}
         */
        @Override
        public Result createOutput(final String namespaceURI, final String suggestedFileName) throws IOException {
            final File file = new File(suggestedFileName);
            final String systemId = file.toURI().toURL().toString();

            final StreamResult result = new StreamResult(file);
            result.setSystemId(systemId);
            return result;
        }

    }

}
