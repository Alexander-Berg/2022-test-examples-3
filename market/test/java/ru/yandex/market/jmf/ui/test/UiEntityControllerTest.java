package ru.yandex.market.jmf.ui.test;

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
import ru.yandex.market.jmf.logic.def.Bo;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.search.Search;
import ru.yandex.market.jmf.security.SecurityConstants;
import ru.yandex.market.jmf.ui.UiConstants;
import ru.yandex.market.jmf.ui.UiMvcConfiguration;
import ru.yandex.market.jmf.ui.UiUtils;
import ru.yandex.market.jmf.ui.api.content.AbstractLayout;
import ru.yandex.market.jmf.ui.api.content.Card;
import ru.yandex.market.jmf.ui.api.content.Content;
import ru.yandex.market.jmf.ui.api.content.TabBar;
import ru.yandex.market.jmf.ui.api.content.Table;
import ru.yandex.market.jmf.ui.controller.UiEntityController;
import ru.yandex.market.jmf.ui.controller.actions.GetCardResult;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
        @ContextConfiguration(classes = {InternalUiTestConfiguration.class}),
        @ContextConfiguration(classes = {UiMvcConfiguration.class}),
})
@TestPropertySource({"classpath:/do_not_require_getters_for_all_attributes.properties"})
public class UiEntityControllerTest {

    private static final Fqn FQN = Fqn.of("smpl");

    @Inject
    protected BcpService bcpService;
    @Inject
    protected UiEntityController controller;
    @Inject
    protected UiUtils uiUtils;

    /**
     * Проверяем, что возвращается объект и основные его атрибуты
     */
    @Test
    public void getViewCard_entity() {
        Entity entity = bcpService.create(FQN, Maps.of("title", Randoms.string()));

        GetCardResult result = controller.getViewCard(entity.getGid(), UiConstants.VIEW, false);

        Assertions.assertNotNull(result.getEntity());
        Assertions.assertEquals(entity.getGid(), result.getEntity().get(Entity.GID));
        Object title = entity.getAttribute("title");
        Assertions.assertEquals(title, result.getEntity().get("title"));
    }

    /**
     * Проверяем, что возвращается карточка (валидность самой карточки не проверяем)
     */
    @Test
    public void getViewCard_card() {
        Entity entity = bcpService.create(FQN, Maps.of("title", Randoms.string()));

        GetCardResult result = controller.getViewCard(entity.getGid(), UiConstants.VIEW, false);

        Assertions.assertNotNull(result.getCard());
        Assertions.assertNotNull(result.getCard().getContent());
        Assertions.assertNotNull(result.getCard().getToolBar());
    }

    @Test
    public void getCard_exists() {
        String title = createEntity(Randoms.string().replace("-", ""));

        String gid = Search.FQN.gidOf(title);
        GetCardResult result = uiUtils.getCard(gid, UiConstants.VIEW, SecurityConstants.Permissions.VIEW);

        Assertions.assertNotNull(result);

        Card card = result.getCard();
        Assertions.assertEquals("search", card.getHead().getType());

        Content content = card.getContent();
        Assertions.assertTrue(content instanceof TabBar);

        TabBar tabBar = (TabBar) content;
        Assertions.assertEquals(1, tabBar.getTabs().size());

        TabBar.Tab tab = tabBar.getTabs().get(0);
        Assertions.assertEquals("Объект для тестирования UI", tab.getCaption());

        AbstractLayout.Column column = tab.getLayout().getRows().get(0).getColumns().get(0);
        Content tblContent = column.getContents().get(0);
        Assertions.assertTrue(tblContent instanceof Table);
    }

    private String createEntity(String title) {
        bcpService.create(FQN, Maps.of(Bo.TITLE, title));
        return title;
    }

}
