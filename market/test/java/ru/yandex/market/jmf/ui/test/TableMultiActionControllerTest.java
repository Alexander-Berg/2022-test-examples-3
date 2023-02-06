package ru.yandex.market.jmf.ui.test;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.ui.UiMvcConfiguration;
import ru.yandex.market.jmf.ui.controller.TableMultiActionController;
import ru.yandex.market.jmf.ui.controller.actions.MultiActionEditRequest;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
        @ContextConfiguration(classes = InternalUiTestConfiguration.class),
        @ContextConfiguration(classes = UiMvcConfiguration.class)
})
@TestPropertySource({"classpath:/do_not_require_getters_for_all_attributes.properties"})
public class TableMultiActionControllerTest {

    private static final Fqn FQN = Fqn.of("smpl");

    @Inject
    protected BcpService bcpService;
    @Inject
    protected TableMultiActionController controller;

    @Test
    public void testEmptyParameters() {
        Entity entity1 = bcpService.create(FQN, Maps.of("title", Randoms.string()));
        Entity entity2 = bcpService.create(FQN, Maps.of("title", Randoms.string()));

        MultiActionEditRequest request = new MultiActionEditRequest(
                FQN.toString(),
                List.of(entity1.getGid(), entity2.getGid()),
                Map.of()
        );

        List<Map<String, Object>> result = controller.edit(request);
        Assertions.assertEquals(2, result.size());
    }
}
