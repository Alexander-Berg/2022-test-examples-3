package ru.yandex.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.DefaultDataTableEntryTransformer;
import io.cucumber.java.DefaultParameterTransformer;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import ru.yandex.calendar.frontend.webNew.WebNewContextConfiguration;
import ru.yandex.calendar.frontend.webNew.actions.WebNewActionsContextConfiguration;
import ru.yandex.calendar.micro.MicroCoreContext;
import ru.yandex.calendar.support.SupportConfiguration;
import ru.yandex.calendar.test.generic.TestBaseContextConfiguration;
import ru.yandex.misc.db.embedded.ActivateEmbeddedPg;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.Map;

@ActivateEmbeddedPg
@ContextConfiguration(classes = {
    TestBaseContextConfiguration.class,
    WebNewContextConfiguration.class,
    WebNewActionsContextConfiguration.class,
    SupportConfiguration.class
})
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class
})
public class CommonDefinitions {
    @Inject
    MicroCoreContext microContext;

    @SneakyThrows
    @DefaultParameterTransformer
    @DefaultDataTableEntryTransformer
    public Object transformer(Object fromValue, Type toValueType) {
        val objectMapper = microContext.findBean(ObjectMapper.class);
        val type = objectMapper.constructType(toValueType);
        if (fromValue instanceof Map) {
            return objectMapper.convertValue(fromValue, type);
        } else {
            return objectMapper.readValue(fromValue.toString(), type);
        }
    }
}
