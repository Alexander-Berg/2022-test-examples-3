package ru.yandex.market.jmf.module.mail.test;

import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.mail.ContentType;
import ru.yandex.market.jmf.module.mail.SendMailService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;

@Transactional
@SpringJUnitConfig(InternalModuleMailTestConfiguration.class)
public class SimpleTriggerTest {

    private static final Fqn FQN = Fqn.parse("simple$type1");
    private static final String ATTR_0 = "attr0";

    @Inject
    BcpService bcpService;
    @Inject
    SendMailService sendMailService;

    @Test
    public void sendMail() {
        Mockito.verifyNoInteractions(sendMailService);
        bcpService.create(FQN, properties(Randoms.string()));
        Mockito.verify(sendMailService, Mockito.times(1)).send(
                anyString(), nullable(String.class), anyList(), anyString(),
                anyList(), nullable(String.class), anyString(), any(ContentType.class), anyString(),
                anyCollection());
    }

    private Map<String, Object> properties(String attr0Value) {
        return ImmutableMap.of(ATTR_0, attr0Value);
    }
}
