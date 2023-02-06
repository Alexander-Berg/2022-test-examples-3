package ru.yandex.market.tsum.clients.startrek;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.startrek.client.utils.StartrekClientModule;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.05.17
 */
public class StartrekApiObjectMapper {
    private StartrekApiObjectMapper() {
    }

    public static ObjectMapper get() {
        return new ObjectMapper()
            .registerModule(new JodaModule())
            .registerModule(new StartrekClientModule(Cf.map()))
            .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setInjectableValues(new InjectableValues() {
                @Override
                public Object findInjectableValue(Object valueId,
                                                  DeserializationContext ctxt,
                                                  BeanProperty forProperty,
                                                  Object beanInstance) {
                    return null;
                }
            });
    }
}
