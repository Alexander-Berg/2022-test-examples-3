package ru.yandex.market.jmf.attributes.test.array;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.metadata.Fqn;

public class ArrayOfGidsColumnAttributeTest extends AbstractArrayAttributeTest<String> {
    public ArrayOfGidsColumnAttributeTest() {
        super("FROM e1 e WHERE true = array_overlap(array_of_text(:v), e.attr)");
    }

    @Override
    protected Collection<String> randomAttributeValue() {
        return Stream.generate(() -> entityService.<Entity>newInstance(Fqn.parse("e2")))
                .limit(2)
                .peek(this::persist)
                .map(HasGid::getGid)
                .collect(Collectors.toList());
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:array_of_gids_column_attribute_metadata.xml",
                    "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
