package ru.yandex.market.b2bcrm.module.ticket.test.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.b2bcrm.module.config.B2bAccountTestConfig;
import ru.yandex.market.b2bcrm.module.ticket.ModuleB2bcrmTicketConfiguration;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.mds.test.MdsSupportTestConfiguration;
import ru.yandex.market.jmf.module.chat.ChatClientService;
import ru.yandex.market.jmf.module.comment.test.ModuleCommentTestConfiguration;
import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.module.geo.ModuleGeoConfiguration;
import ru.yandex.market.jmf.module.mail.test.ModuleMailTestConfiguration;
import ru.yandex.market.jmf.module.ou.test.ModuleOuTestConfiguration;
import ru.yandex.market.jmf.module.relation.test.ModuleRelationTestConfiguration;
import ru.yandex.market.jmf.module.startrek.StartrekServiceTestConfig;
import ru.yandex.market.jmf.module.ticket.ModuleTicketConfiguration;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;
import ru.yandex.market.jmf.script.ScriptServiceApi;
import ru.yandex.market.jmf.telephony.voximplant.test.VoximplantControllerTestConfiguration;
import ru.yandex.market.jmf.utils.LinkUtils;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        ModuleTicketTestConfiguration.class,
        ModuleB2bcrmTicketConfiguration.class,
        ModuleCommentTestConfiguration.class,
        ModuleDefaultTestConfiguration.class,
        B2bAccountTestConfig.class,
        ModuleMailTestConfiguration.class,
        ModuleTicketConfiguration.class,
        VoximplantControllerTestConfiguration.class,
        StartrekServiceTestConfig.class,
        ModuleRelationTestConfiguration.class,
        ModuleGeoConfiguration.class,
        ModuleOuTestConfiguration.class,
        MdsSupportTestConfiguration.class
})
@ComponentScan({
        "ru.yandex.market.b2bcrm.module.ticket.test.utils"
})
public class B2bTicketTestConfig {
    @Bean
    public ChatClientService chatClientService() {
        return mock(ChatClientService.class);
    }

    @Bean
    ScriptServiceApi linkScriptServiceApi() {
        return new MockLinkScriptServiceApi();
    }

    public static class MockLinkScriptServiceApi implements ScriptServiceApi {
        @Autowired
        private LinkUtils linkUtils;

        public String viewCard(HasGid hasGid, String label) {
            return "test";
        }

        public String viewCardRelative(HasGid hasGid) {
            return "test";
        }

        public String viewCard(HasGid hasGid) {
            return "test";
        }

        public String link(String path, String label) {
            return "test";
        }

        public String externalLink(String path, String label, String defaultLabel) {
            return "test";
        }

        public String externalLink(String path, String label) {
            return linkUtils.externalLink(path, label);
        }

        public String encode(String data) {
            return "test";
        }

        public String decode(String data) {
            return "test";
        }
    }
}
