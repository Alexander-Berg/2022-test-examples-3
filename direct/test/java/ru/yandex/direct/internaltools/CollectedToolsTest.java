package ru.yandex.direct.internaltools;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.InternalToolProxy;
import ru.yandex.direct.internaltools.core.InternalToolsRegistry;
import ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole;
import ru.yandex.direct.internaltools.core.enums.InternalToolCategory;

import static org.assertj.core.api.Assertions.assertThat;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CollectedToolsTest {
    @Autowired
    private InternalToolsRegistry registry;

    private Map<InternalToolCategory, List<InternalToolProxy>> internalToolsByCategory;

    @Before
    public void before() {
        ClientId operatorClientId = ClientId.fromLong(1L);
        internalToolsByCategory =
                registry.getInternalToolsByCategory(Collections.singleton(InternalToolAccessRole.SUPER),
                        operatorClientId);
    }

    @Test
    public void testRegistryIsFilled() {
        assertThat(internalToolsByCategory)
                .isNotEmpty();
    }

    // TODO(bzzzz): нужно бы тут что-то более осмысленное проверять
    @Test
    public void checkToolsAreCorrect() {
        for (Map.Entry<InternalToolCategory, List<InternalToolProxy>> entry : internalToolsByCategory.entrySet()) {
            assertThat(entry.getKey())
                    .isNotNull();

            for (InternalToolProxy proxy : entry.getValue()) {
                assertThat(proxy.getCategory())
                        .isEqualTo(entry.getKey());
            }
        }
    }
}
