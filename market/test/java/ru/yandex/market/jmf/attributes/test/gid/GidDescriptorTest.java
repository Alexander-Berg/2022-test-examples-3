package ru.yandex.market.jmf.attributes.test.gid;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.gid.GidDescriptor;
import ru.yandex.market.jmf.attributes.gid.GidType;
import ru.yandex.market.jmf.entity.EntityDtoService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;

public class GidDescriptorTest {

    @Test
    public void unwrap() {
        GidDescriptor descriptor = new GidDescriptor(
                Mockito.mock(EntityStorageService.class),
                Mockito.mock(EntityDtoService.class)
        );
        Map<String, String> expected = Map.of(HasGid.GID, Randoms.string());

        Map actual = descriptor.unwrap(
                Mockito.mock(Attribute.class),
                Mockito.mock(GidType.class),
                expected.get(HasGid.GID),
                Map.class
        );

        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void wrap() {
        GidDescriptor descriptor = new GidDescriptor(
                Mockito.mock(EntityStorageService.class),
                Mockito.mock(EntityDtoService.class)
        );
        String expected = Randoms.string();

        String actual = descriptor.wrap(
                Mockito.mock(Attribute.class),
                Mockito.mock(GidType.class),
                Map.of(HasGid.GID, expected)
        );

        Assertions.assertEquals(expected, actual);
    }
}
