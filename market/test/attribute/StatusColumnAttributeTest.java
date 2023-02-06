package ru.yandex.market.jmf.logic.wf.test.attribute;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.lock.LockServiceConfiguration;
import ru.yandex.market.jmf.logic.def.test.LogicDefaultTestConfiguration;
import ru.yandex.market.jmf.logic.wf.DefaultWfConfigurationProvider;
import ru.yandex.market.jmf.logic.wf.LogicWfConfiguration;
import ru.yandex.market.jmf.logic.wf.WfConfigurationProvider;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.script.storage.ScriptStorageConfiguration;
import ru.yandex.market.jmf.security.test.SecurityTestConfiguration;
import ru.yandex.market.jmf.utils.XmlUtils;

@ContextConfiguration(classes = StatusColumnAttributeTest.Configuration.class)
public class StatusColumnAttributeTest extends AbstractAttributeTest {

    public StatusColumnAttributeTest() {
        super(Fqn.parse("wf1"), "status");
    }

    @Override
    protected Object randomAttributeValue() {
        return Randoms.string();
    }

    @org.springframework.context.annotation.Configuration
    @Import({
            LogicDefaultTestConfiguration.class,
            LogicWfConfiguration.class,
            SecurityTestConfiguration.class,
            LockServiceConfiguration.class,
            ScriptStorageConfiguration.class
    })
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:StatusColumnAttributeTest/metadata/wf1.xml",
                    "classpath:StatusColumnAttributeTest/metadata/linkAttrMetaclass_metadata.xml");
        }

        @Bean
        public WfConfigurationProvider wfProvider(XmlUtils xmlUtils) {
            return new DefaultWfConfigurationProvider("classpath:StatusColumnAttributeTest/wf/wf1.xml", xmlUtils);
        }
    }
}
