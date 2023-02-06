package ru.yandex.market.abo.core.premod.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.common.framework.message.MessageService;
import ru.yandex.common.framework.message.MessageTemplate;
import ru.yandex.market.abo.core.message.Messages;
import ru.yandex.market.abo.core.premod.helper.clone.ClusterStateStorer;
import ru.yandex.market.abo.core.premod.helper.clone.model.DecisionParams;
import ru.yandex.market.abo.core.premod.helper.clone.model.ShopInfoSnapshot;
import ru.yandex.market.abo.core.premod.model.CloneType;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author mixey, kukabara
 */
public class CloneItemHelperTest extends EmptyTest {
    private static final Logger log = Logger.getLogger(CloneItemHelperTest.class);
    @Autowired
    private CloneItemHelper cloneItemHelper;
    @Spy
    private CloneItemHelper cloneItemHelperSpy;
    @Autowired
    private ClusterStateStorer storer;
    @Autowired
    private MessageService messageService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testXmlBody() {
        ShopInfoService mock = mock(ShopInfoService.class);
        doAnswer(invocation -> {
            long shopId = (Long) invocation.getArguments()[0];
            ShopInfo s = new ShopInfo();
            s.setId(shopId);
            s.setName("ShopName-" + shopId);
            return s;
        }).when(mock).getShopInfo(anyLong());
        cloneItemHelperSpy.setShopInfoService(mock);

        doAnswer(invocation -> {
            Collection<Long> shopIds = (Collection<Long>) invocation.getArguments()[0];
            return shopIds.stream()
                    .map(shopId -> {
                        ShopInfo shop = new ShopInfo();
                        shop.setId(shopId);
                        shop.setName("ShopName-" + shopId);
                        return shop;
                    })
                    .collect(Collectors.toList());
        }).when(mock).loadShopInfosById(anyList());
        cloneItemHelperSpy.setShopInfoService(mock);

        String xmlBody = cloneItemHelperSpy.getXmlBody(774L, "123, 456", "Совпадения информации", "890, 764",
                "Прочих причинам");
        assertEquals("<abo-info>\n" +
                " <shop-name>ShopName-774</shop-name>\n" +
                " <blocks>\n" +
                "  <block>\n" +
                "   <leaders>\n" +
                "    <leader>ShopName-456</leader>\n" +
                "    <leader>ShopName-123</leader>\n" +
                "   </leaders>\n" +
                "   <reason>Совпадения информации</reason>\n" +
                "  </block>\n" +
                "  <block>\n" +
                "   <leaders>\n" +
                "    <leader>ShopName-890</leader>\n" +
                "    <leader>ShopName-764</leader>\n" +
                "   </leaders>\n" +
                "   <reason>Прочих причинам</reason>\n" +
                "  </block>\n" +
                " </blocks>\n" +
                "</abo-info>\n", xmlBody);

        xmlBody = cloneItemHelperSpy.getXmlBody(774L, "123", "Совпадения информации", null, null);
        assertEquals("<abo-info>\n" +
                " <shop-name>ShopName-774</shop-name>\n" +
                " <blocks>\n" +
                "  <block>\n" +
                "   <leaders>\n" +
                "    <leader>ShopName-123</leader>\n" +
                "   </leaders>\n" +
                "   <reason>Совпадения информации</reason>\n" +
                "  </block>\n" +
                " </blocks>\n" +
                "</abo-info>\n", xmlBody);
    }

    //	@Test
    public void testCanDuplicateDecision() throws Exception {
        storer.storeStatus(new DecisionParams(651366, CloneType.NO_CLONE, 0, 0L,
                "leader 1, 2, 3", "reason 1", "leader 4, 5, 6", "reason 2"), 155);
        assertTrue(cloneItemHelper.canDuplicateDecision(155));
        assertFalse(cloneItemHelper.canDuplicateDecision(0));
    }

    //	@Test // TODO brokenTest
    public void testClusterStateStorer() throws Exception {
        storer.storeStatus(new DecisionParams(651366, CloneType.NO_CLONE, 0, 0L,
                "leader 1, 2, 3", "reason 1", "leader 4, 5, 6", "reason 2"), 155);
        storer.storeStatus(new DecisionParams(654009, CloneType.CLONE_FOR_ABO, 0, 0L,
                "leaders 1, 2, 3", "reason 11", "leaders 4, 5, 6", "reason 22"), 155);

        ShopInfoSnapshot currentInfo = storer.getCurrentShopInfo(155);
        ShopInfoSnapshot snapshotInfo = storer.getLastShopInfoSnapshot(155);
        assertEquals(currentInfo, snapshotInfo);

        DecisionParams params = storer.getLastDecision(155);
        assertEquals(params.getItemId(), 654009);
        assertEquals(params.getCloneType(), CloneType.CLONE_FOR_ABO);
        assertEquals(params.getUserId(), 0);
        assertEquals((long) params.getSessionId(), 0L);
        assertEquals(params.getClusterLeaders(), "leaders 1, 2, 3");
        assertEquals(params.getReason(), "reason 11");
        assertEquals(params.getClusterLeaders2(), "leaders 4, 5, 6");
        assertEquals(params.getReason2(), "reason 22");
    }

    //	@Test // TODO brokenTest
    public void testClusterStateStorer2() throws Exception {
        DecisionParams params = storer.getLastDecision(0);
        assertNull(params);
        assertNull(storer.getLastShopInfoSnapshot(0));
        assertNull(storer.getLastShopInfoSnapshot(1));

        storer.storeStatus(new DecisionParams(0, CloneType.NO_CLONE, 0, 0L, null, null, null, null), 0);
        assertNull(params);

        ShopInfoSnapshot snapshotInfo = storer.getLastShopInfoSnapshot(0);
        assertNull(snapshotInfo);
    }


    /**
     * Проверить на клоновость в премодерации
     */
    @Test
    @Disabled
    public void testPremodClone() {
        cloneItemHelper.checkCloneItems();
        log.info("Finish clone check processing premod items");
    }

    //	@Test // TODO brokenTest
    public void testCreateParam() {
        ShopInfoService mockInfoService = mock(ShopInfoService.class);
        when(mockInfoService.getShopInfo(anyLong())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ShopInfo info = new ShopInfo();
            info.setName("Shop_" + args[0]);
            return info;
        });
        cloneItemHelper.setShopInfoService(mockInfoService);

        long shopId = 1L;
        Long leader = 2L;
        Long leader2 = 3L;
        List<Long> shops = new ArrayList<>(Arrays.asList(shopId, leader));
        String clusterLeaders = leader.toString();
        String reason = "совокупности данных";
        String clusterLeaders2 = leader2.toString();
        String reason2 = "юридической информации";

        try {
            cloneItemHelper.createParams(shopId, "");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            cloneItemHelper.createParams(shopId, "");
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            cloneItemHelper.createParams(shopId, "");
            fail();
        } catch (IllegalArgumentException e) {
        }

        // один магазин
        shops = new ArrayList<>(Arrays.asList(shopId, leader));
        showMessage(shopId, shops, clusterLeaders, reason, "", "");
        showMessage(shopId, shops, clusterLeaders, reason, clusterLeaders2, reason2);
        Long anotherShop = 123456L;
        shops.add(anotherShop);
        showMessage(shopId, shops, clusterLeaders, reason, clusterLeaders2, reason2);

        // несколько магазинов
        Long newLeader = 11L;
        Long newLeader2 = 22L;
        clusterLeaders += "," + newLeader.toString();
        clusterLeaders2 += "," + newLeader2.toString();
        shops = new ArrayList<>(Arrays.asList(shopId, leader, newLeader, newLeader2));

        showMessage(shopId, shops, clusterLeaders, reason, "", "");
        showMessage(shopId, shops, clusterLeaders, reason, clusterLeaders2, reason2);
        shops.add(anotherShop);
        showMessage(shopId, shops, clusterLeaders, reason, clusterLeaders2, reason2);

    }

    public void showMessage(final long shopId, final List<Long> shopsFromSession,
                            final String clusterLeaders, final String reason, final String clusterLeaders2, final String reason2) {
        Map<String, Object> params;
        MessageTemplate message;
        params = cloneItemHelper.createParams(shopId, "");
        message = messageService.createMessageTemplate(Messages.PREMODERATION_CLONE_CAN_NOT_TURN, params);
        System.out.println(message.getText());
    }
}
