package ru.yandex.market.jmf.metadata.test.unique_group;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UniqueAttributeGroupWithoutAttributesInMetaclassTest.Configuration.class)
public class UniqueAttributeGroupWithoutAttributesInMetaclassTest extends AbstractConfigurationErrorTest {

    @org.springframework.context.annotation.Configuration
    static class Configuration extends UniqueAttributeGroupConfiguration {
        public Configuration() {
            super("classpath:unique_group/unique_group_without_attrs_in_metaclass_metadata.xml");
        }
    }
}
