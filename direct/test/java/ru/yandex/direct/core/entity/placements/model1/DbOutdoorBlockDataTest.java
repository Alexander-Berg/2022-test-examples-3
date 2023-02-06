package ru.yandex.direct.core.entity.placements.model1;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class DbOutdoorBlockDataTest {

    @Test
    public void serializeDeserialize() throws IOException {
        // jackson имеет завязки на имена филдов и методов
        // например boolean field должен называться property, а метод isProperty
        // если назвать field isProperty, то метод нужно называть isIsProperty
        // есть смысл попробовать сериализовать объект, а потом его десериализовать
        // если не было экепшенов - Jackson аннотации раставлены верно
        ObjectMapper mapper = new ObjectMapper();
        DbOutdoorBlockData obj = new DbOutdoorBlockData()
                .withHidden(true);
        String serialized = mapper.writeValueAsString(obj);
        DbOutdoorBlockData deserialized = mapper.readerFor(DbOutdoorBlockData.class).readValue(serialized);
        assertThat(deserialized.getHidden()).isTrue();
    }

}
