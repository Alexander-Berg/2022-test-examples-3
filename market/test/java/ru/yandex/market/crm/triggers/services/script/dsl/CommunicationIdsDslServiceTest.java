package ru.yandex.market.crm.triggers.services.script.dsl;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.script.ScriptVariablesPreparationCopyService;
import ru.yandex.market.mcrm.script.impl.ScriptCompilerServiceImpl;
import ru.yandex.market.mcrm.script.impl.ScriptServiceImpl;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author vtarasoff
 * @since 11.05.2021
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        CommunicationIdsDslServiceImpl.class,
        ScriptCompilerServiceImpl.class,
        ScriptVariablesPreparationCopyService.class,
        ScriptServiceImpl.class
})
public class CommunicationIdsDslServiceTest {
    @Autowired
    private CommunicationIdsDslService service;

    @Test
    public void shouldReturnCorrectUid() {
        Map<String, Object> vars = Map.of(
                "object", Map.of(
                        "subObject", Map.of(
                                "email", "example@example.com",
                                "phone", "8 (888) 888-88-88",
                                "puid", "123456789",
                                "uuid", "987654321",
                                "yuid", "123-456-789"
                        )
                )
        );

        Uid email = service.execute("email object.subObject.email", vars);
        assertThat(email, equalTo(Uid.asEmail("example@example.com")));

        Uid phone = service.execute("phone object.subObject.phone", vars);
        assertThat(phone, equalTo(Uid.asPhone("88888888888")));

        Uid puid = service.execute("puid object.subObject.puid", vars);
        assertThat(puid, equalTo(Uid.asPuid("123456789")));

        Uid uuid = service.execute("uuid object.subObject.uuid", vars);
        assertThat(uuid, equalTo(Uid.asUuid("987654321")));

        Uid yuid = service.execute("yuid object.subObject.yuid", vars);
        assertThat(yuid, equalTo(Uid.asYuid("123-456-789")));
    }

    @Test
    public void shouldNullIfEmptyCommunicationId() {
        Uid email = service.execute("email null", Map.of());
        assertThat(email, nullValue());

        email = service.execute("email '   '", Map.of());
        assertThat(email, nullValue());
    }

    @Test
    public void shouldReturnCustomValue() {
        String helloWorld = service.execute("'Hello World!'", Map.of());
        assertThat(helloWorld, equalTo("Hello World!"));
    }
}
