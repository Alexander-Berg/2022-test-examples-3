package ru.yandex.market.abo.cpa;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionTreePlainTextBuilder;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.order.UserGroup;

/**
 * MARKETASSESSOR-2344.
 * Сделать выгрузку отмененных заказов за последнюю неделю
 * Не учитывать отмененные системой, тестовые заказы
 * 1. ID заказа
 * 2. ID пользователя
 * 3. Номер телефона и ФИО (если есть)
 * 4. id магазина
 * 5. Название магазина
 * 6. Дата покупки
 * 7. Список заказанного, можно через запятую в одной колонке.
 * 8. Кто отменил заказ (пользователь или магазин)
 * 9. Причина отмены (USER_UNREACHABLE, USER_CHANGED_MIND и тп.)
 *
 * @author kukabara
 */

public class CpaOrdersForPhoneCalls extends EmptyTest {
    private static final Logger log = Logger.getLogger(CpaOrdersForPhoneCalls.class);
    public static final int pageSize = 50;
    private static final Collection<UserGroup> USEFUL_USER_GROUPS = Arrays.asList(UserGroup.DEFAULT, UserGroup.UNKNOWN);

    @Autowired
    private CheckouterAPI checkouterClient;
    @Autowired
    private ShopInfoService shopInfoService;

    private RegionTree<Region> rt;

    @Test
    public void testCheckouter() throws Exception {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -1);
        OrderStatus[] statuses = new OrderStatus[]{OrderStatus.CANCELLED};
        checkouterClient.getOrders(c.getTime(), new Date(), statuses, 1, 5);
    }

    public void test() throws Exception {
        prepareRegions();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -7);

        int page = 1;

        OrderStatus[] statuses = new OrderStatus[]{OrderStatus.CANCELLED};
        PagedOrders pagedObject = null;
        StringBuilder sb = new StringBuilder();
        sb.append("Id\tUid\tPhone\tName\tMiddlename\tLastname\tRegionId\tRegion\tShopId\tShopName\tCreationDate\t" +
                "WhoCancelled\tBeforeStatus\tBeforeSubstatus\tSubstatus\tofferId\tOfferName\tdescription\tcount\t" +
                "price\n");
        while (pagedObject == null || pagedObject.getPager().getTo() < pagedObject.getPager().getTotal()) {
            try {
                pagedObject = checkouterClient.getOrders(c.getTime(), new Date(), statuses, page++, pageSize);

                if (pagedObject == null) {
                    return;
                }
                for (Order o : pagedObject.getItems()) {
                    if (o.getBuyer() == null) {
                        continue;
                    }
                    OrderHistoryEvent event = getWhoCancelled(o.getId());
                    ClientRole clientRole = event.getAuthor().getRole();
                    if (clientRole == null || clientRole.equals(ClientRole.SYSTEM)) {
                        continue;
                    }
                    // отмененные системой
                    if (o.getSubstatus().equals(OrderSubstatus.RESERVATION_EXPIRED) ||
                            o.getSubstatus().equals(OrderSubstatus.USER_NOT_PAID) ||
                            o.getSubstatus().equals(OrderSubstatus.PROCESSING_EXPIRED)) {
                        continue;
                    }
                    // тестовый заказы не нужны
                    if (o.isFake() || !USEFUL_USER_GROUPS.contains(o.getUserGroup())) {
                        continue;
                    }

                    sb.append(o.getId()).append("\t");
                    sb.append(o.getBuyer().getUid()).append("\t");
                    sb.append(o.getBuyer().getPhone()).append("\t");
                    sb.append(o.getBuyer().getFirstName()).append("\t");
                    sb.append(o.getBuyer().getMiddleName()).append("\t");
                    sb.append(o.getBuyer().getLastName()).append("\t");
                    long regionId = o.getBuyer().getRegionId();
                    sb.append(regionId).append("\t");
                    sb.append(rt.getRegion((int) regionId).getName()).append("\t");
                    sb.append(o.getShopId()).append("\t");
                    sb.append(shopInfoService.getShopInfo(o.getShopId()).getName()).append("\t");
                    sb.append(o.getCreationDate()).append("\t");
                    sb.append(clientRole).append("\t");
                    sb.append(event.getOrderBefore().getStatus()).append("\t");
                    sb.append(event.getOrderBefore().getSubstatus()).append("\t");
                    sb.append(o.getSubstatus()).append("\t");

                    for (OrderItem it : o.getItems()) {
                        sb.append(it.getOfferId()).append("\t");
                        sb.append(it.getOfferName()).append("\t");
                        sb.append(it.getDescription()).append("\t");
                        sb.append(it.getCount()).append("\t");
                        sb.append(it.getBuyerPrice()).append("\t");
                    }

                    sb.append("\n");
                    System.out.println(sb.toString());
                }
            } catch (Exception e) {
                log.error("Exception when getOrders", e);
            }
        }
        writeToFile(sb);
    }

    public void writeToFile(StringBuilder sb) {
        Writer out = null;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("cpaOrders.txt");
            out = new OutputStreamWriter(fileOutputStream, "UTF-8");
            out.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void prepareRegions() {
        try {
            RegionTreePlainTextBuilder builder = new RegionTreePlainTextBuilder();
            builder.setPlainTextURL(new URL("file:///C:\\Users\\kukabara\\Downloads\\MARKETPERS-1683\\geobase.txt"));
            builder.setTimeoutMillis(2000);
            rt = builder.buildRegionTree();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public OrderHistoryEvent getWhoCancelled(long orderId) {
        int page = 1;

        PagedEvents pagedObject = null;

        while (pagedObject == null || pagedObject.getPager().getTo() < pagedObject.getPager().getTotal()) {
            try {
                pagedObject = checkouterClient.orderHistoryEvents().getOrderHistoryEvents(
                        orderId, ClientRole.SYSTEM, null, page++, pageSize);
                if (pagedObject == null) {
                    return null;
                }
                for (OrderHistoryEvent e : pagedObject.getItems()) {
                    if (e.getOrderAfter().getStatus().equals(OrderStatus.CANCELLED)) {
                        return e;
                    }
                }
            } catch (Exception e) {
                log.error("Exception when getOrderHistoryEvents", e);
            }
        }
        return null;
    }
}
