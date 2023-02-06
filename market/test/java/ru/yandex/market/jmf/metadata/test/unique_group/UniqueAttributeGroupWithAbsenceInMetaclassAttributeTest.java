package ru.yandex.market.jmf.metadata.test.unique_group;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UniqueAttributeGroupWithAbsenceInMetaclassAttributeTest.Configuration.class)
public class UniqueAttributeGroupWithAbsenceInMetaclassAttributeTest extends AbstractConfigurationErrorTest {

    @org.springframework.context.annotation.Configuration
    static class Configuration extends UniqueAttributeGroupConfiguration {
        public Configuration() {
            super("classpath:unique_group/unique_group_with_absence_in_metaclass_attr_metadata.xml");
        }
    }
}
