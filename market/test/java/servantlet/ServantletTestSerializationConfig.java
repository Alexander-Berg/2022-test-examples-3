package servantlet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import ru.yandex.market.common.test.SerializationChecker;
import ru.yandex.market.core.xml.Converter;
import ru.yandex.market.core.xml.ConverterFinder;
import ru.yandex.market.core.xml.HierarchicalWriter;
import ru.yandex.market.core.xml.MarshallingContext;
import ru.yandex.market.core.xml.Pope;
import ru.yandex.market.core.xml.finders.ChainConverterFinder;
import ru.yandex.market.core.xml.finders.ClassMapConverterFinder;
import ru.yandex.market.core.xml.impl.DefaultHierarchicalWriter;
import ru.yandex.market.core.xml.impl.MbiMarshallingContext;
import ru.yandex.market.core.xml.popes.LazyPope;
import ru.yandex.market.partner.orginfo.OrganizationInfoConverter;

/**
 * Конфиг для тестов сериализации старых сервантлет-ручек
 *
 * @author fbokovikov
 */
@Configuration
public class ServantletTestSerializationConfig {

    @Bean
    public Map<String, Converter<?>> converters() {
        Map<String, Converter<?>> converterMap = new HashMap<>();
        converterMap.put("ru.yandex.market.core.orginfo.model.OrganizationInfo", new OrganizationInfoConverter());

        return converterMap;
    }

    @Bean
    public ClassMapConverterFinder classMapConverterFinder() {
        ClassMapConverterFinder classMapConverterFinder = new ClassMapConverterFinder();
        classMapConverterFinder.setConvertersMap(converters());
        return classMapConverterFinder;
    }

    @Bean
    public ConverterFinder converterFinder() {
        ChainConverterFinder converterFinder = new ChainConverterFinder();
        converterFinder.setConverterFinders(
                Arrays.asList(
                        classMapConverterFinder()
                )
        );
        return converterFinder;
    }

    @Bean
    public Pope pope() {
        LazyPope lazyPope = new LazyPope();
        lazyPope.setPopeConverterFinder(converterFinder());
        return lazyPope;
    }

    @Bean
    @Scope("prototype")
    public HierarchicalWriter writer() {
        return new DefaultHierarchicalWriter();

    }

    @Bean
    @Scope("prototype")
    public MarshallingContext marshallingContext() {
        MbiMarshallingContext context = new MbiMarshallingContext();
        context.setHierarchicalWriter(writer());
        context.setConverterFinder(converterFinder());
        context.setPope(pope());
        return context;
    }

    @Bean
    public SerializationChecker serializationChecker() {
        MarshallingContext marshallingContext = marshallingContext();
        return new SerializationChecker(
                obj -> SerializationChecker.JSON_STUB,
                null,
                obj -> {
                    marshallingContext.marshalAnother(obj);
                    return marshallingContext.getMarshalled();
                },
                null);
    }

}
