package ru.yandex.market.jmf.metadata.test.unique_group;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UniqueAttributeGroupWithOneAttributeTest.Configuration.class)
public class UniqueAttributeGroupWithOneAttributeTest extends AbstractConfigurationErrorTest {

    @org.springframework.context.annotation.Configuration
    static class Configuration extends UniqueAttributeGroupConfiguration {
        public Configuration() {
            super("classpath:unique_group/unique_group_with_one_attribute_metadata.xml");
        }
    }
}
