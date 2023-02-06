package ru.yandex.market.pers.notify;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.InputSource;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.pers.notify.entity.OfferModel;
import ru.yandex.market.pers.notify.external.report.WantedModel;
import ru.yandex.market.pers.notify.templates.EmailSnippetGenerator;
import ru.yandex.market.pers.notify.templates.EmailSnippetType;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;
import ru.yandex.market.report.model.Category;
import ru.yandex.market.report.model.Model;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         19.08.15
 */
public class EmailSnippetGeneratorTest extends MarketMailerMockedDbTest {
    private static final String GET_PREPAID_SHOP = "<paymentType>PREPAID</paymentType>" +
        "<paymentMethod>SHOP_PREPAID</paymentMethod>";
    public static final String htmlBefore = "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"ru\" xml:lang=\"ru\">\n" +
        "<head>\n" +
        "    <meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\"/>\n" +
        "    <title>Яндекс.Маркет</title><style type=\"text/css\">\n" +
        "html {\n" +
        "    -webkit-text-size-adjust: none;\n" +
        "    -ms-text-size-adjust: none;\n" +
        "}\n" +
        "\n" +
        "@media screen and (max-device-width: 500px), screen and (max-width: 500px) {\n" +
        "    .mob-center {\n" +
        "        margin: 0 auto !important;\n" +
        "        text-align: center !important;\n" +
        "        float: none !important;\n" +
        "    }\n" +
        "    .mob-w100p {\n" +
        "        width: 100% !important;\n" +
        "    }\n" +
        "}\n" +
        "</style>\n" +
        "</head>\n" +
        "<body bgcolor=\"#f6f5f3\">";
    public static final String htmlAfter = "</body></html>";
    private static String PREFIX = "sample_";
    private static String GET_SHOP = "<shopPhone>+79112223456</shopPhone>\n" +
        "            <shopName>[Some Shop Name]</shopName>\n" +
        "            <shopSite>http://yandex.ru</shopSite>\n";
    private static String GET_PREPAID = "<paymentType>PREPAID</paymentType>\n" +
        "                <paymentMethod>YANDEX</paymentMethod>\n";
    private static String GET_POSTPAID = "<paymentType>POSTPAID</paymentType>\n" +
        "                <paymentMethod>CASH_ON_DELIVERY</paymentMethod>\n";
    private static String GET_ENTRY_1 = "                    <entry>\n" +
        "                        <feed-order-id>\n" +
        "                            <id>2</id>\n" +
        "                            <feedId>2928</feedId>\n" +
        "                        </feed-order-id>\n" +
        "                        <item>\n" +
        "                            <modelId>-1</modelId>\n" +
        "                            <description>nodescrfortovar-2</description>\n" +
        "                            <pictures class=\"list\">\n" +
        "                                <offer-picture>\n" +
        "                                    <url>http://cs-ellpic.yandex.net/market_HqCmXaNUHY5Qf-NvCbU7RA_</url>\n" +
        "                                </offer-picture>\n" +
        "                            </pictures>\n" +
        "                            <buyerPrice>200</buyerPrice>\n" +
        "                            <fee>0.05</fee>\n" +
        "                            <feeSum>2</feeSum>\n" +
        "                            <feedOfferId reference=\"../../feed-order-id\"/>\n" +
        "                            <offerName>баночка</offerName>\n" +
        "                            <categoryId>7683824</categoryId>\n" +
        "                            <feedCategoryId>111</feedCategoryId>\n" +
        "                            <price>200</price>\n" +
        "                            <count>1</count>\n" +
        "                        </item>\n" +
        "                    </entry>\n";
    private static String GET_ENTRY_2 = "                <entry>\n" +
        "                    <feed-order-id>\n" +
        "                        <id>74373467</id>\n" +
        "                        <feedId>313791</feedId>\n" +
        "                    </feed-order-id>\n" +
        "                    <item>\n" +
        "                        <modelId>1687972944</modelId>\n" +
        "                        <description>умные часы,влагозащищенные,экран, 1.26\", 144x168,уведомление о входящем\n" +
        "                            звонке,совместимость с Android, iOS\n" +
        "                        </description>\n" +
        "                        <pictures class=\"list\">\n" +
        "                            <offer-picture>\n" +
        "                                <url>//cs-ellpic.yandex.net/market_g8FnYXVLPWwI2Xw8zbvjYA_</url>\n" +
        "                            </offer-picture>\n" +
        "                        </pictures>\n" +
        "                        <buyerPrice>22590</buyerPrice>\n" +
        "                        <fee>0.01</fee>\n" +
        "                        <feeSum>7.53</feeSum>\n" +
        "                        <feedOfferId>\n" +
        "                            <id>74373467</id>\n" +
        "                            <feedId>313791</feedId>\n" +
        "                        </feedOfferId>\n" +
        "                        <wareMd5>xBblCv95H_qn4XcLGV-x9w</wareMd5>\n" +
        "                        <offerName>Умные часы Pebble SmartWatch Steel Black Mattу (Черные стальные со стальным ремешком)\n" +
        "                        </offerName>\n" +
        "                        <categoryId>91498</categoryId>\n" +
        "                        <feedCategoryId>3407432</feedCategoryId>\n" +
        "                        <price>22590</price>\n" +
        "                        <count>1</count>\n" +
        "                        <availability>ON_STOCK</availability>\n" +
        "                    </item>\n" +
        "                </entry>\n" ;
    private static String GET_ENTRY_3 = "                <entry>\n" +
        "                    <feed-order-id>\n" +
        "                        <id>67654786</id>\n" +
        "                        <feedId>313791</feedId>\n" +
        "                    </feed-order-id>\n" +
        "                    <item>\n" +
        "                        <modelId>11153480</modelId>\n" +
        "                        <description>Настолько хорош, что вы не захотите выпускать его из рук. Настолько лёгок, что вам и не\n" +
        "                            придётся расставаться\n" +
        "                        </description>\n" +
        "                        <pictures class=\"list\">\n" +
        "                            <offer-picture>\n" +
        "                                <url>//cs-ellpic.yandex.net/market_5obhmVSj9AKdcqcL7mplow_</url>\n" +
        "                            </offer-picture>\n" +
        "                        </pictures>\n" +
        "                        <buyerPrice>30490</buyerPrice>\n" +
        "                        <fee>0.01</fee>\n" +
        "                        <feeSum>10.16</feeSum>\n" +
        "                        <feedOfferId>\n" +
        "                            <id>67654786</id>\n" +
        "                            <feedId>313791</feedId>\n" +
        "                        </feedOfferId>\n" +
        "                        <wareMd5>tjInTqXwThWfUMAB8FWT-g</wareMd5>\n" +
        "                        <offerName>Планшет Apple iPad Air 2 16Gb Wi-Fi + Cellular Gold (Золотой)</offerName>\n" +
        "                        <categoryId>6427100</categoryId>\n" +
        "                        <feedCategoryId>209570</feedCategoryId>\n" +
        "                        <price>30490</price>\n" +
        "                        <count>1</count>\n" +
        "                        <availability>ON_STOCK</availability>\n" +
        "                    </item>\n" +
        "                </entry>\n";
    private static String GET_DELIVERY_FREE = getDelivery("DELIVERY", 0);
    private static String GET_DELIVERY = getDelivery("DELIVERY");
    private static String GET_POST = getDelivery("POST");
    private static String GET_PICKUP = "<delivery>\n" +
        "                    <type>PICKUP</type>\n" +
        "                    <serviceName>own service</serviceName>\n" +
        "                    <price>350</price>\n" +
        "                    <buyerPrice>350</buyerPrice>\n" +
        "                    <deliveryDates>\n" +
        "                        <toDate>2013-07-03 00:00:00.0 MSK</toDate>\n" +
        "                        <fromDate>2013-07-03 00:00:00.0 MSK</fromDate>\n" +
        "                        <reservedUntil>2013-07-03 18:40:00.0 MSK</reservedUntil>\n" +
        "                    </deliveryDates>\n" +
        "                    <regionId>213</regionId>\n" +
        "                    <outletId>666</outletId>\n" +
        "                    <outlet>\n" +
        "                        <shopId>0</shopId>\n" +
        "                        <name>Спортмастер</name>\n" +
        "                        <regionId>213</regionId>\n" +
        "                        <city>Москва</city>\n" +
        "                        <street>Варшавское ш.</street>\n" +
        "                        <house>124</house>\n" +
        "                        <gps>37.615219,55.627321</gps>\n" +
        "                        <phones>\n" +
        "                            <shop-outlet-phone>\n" +
        "                                <countryCode>7</countryCode>\n" +
        "                                <cityCode>383</cityCode>\n" +
        "                                <number>3365731</number>\n" +
        "                                <extNumber>1234</extNumber>\n" +
        "                            </shop-outlet-phone>\n" +
        "                            <shop-outlet-phone>\n" +
        "                                <countryCode>7</countryCode>\n" +
        "                                <cityCode>383</cityCode>\n" +
        "                                <number>336456</number>\n" +
        "                                <extNumber>555</extNumber>\n" +
        "                            </shop-outlet-phone>\n" +
        "                        </phones>\n" +
        "                        <schedule>\n" +
        "                            <date-time-range>\n" +
        "                                <dayFrom>1</dayFrom>\n" +
        "                                <dayTo>5</dayTo>\n" +
        "                                <timeFrom>10:00</timeFrom>\n" +
        "                                <timeTo>20:00</timeTo>\n" +
        "                            </date-time-range>\n" +
        "                            <date-time-range>\n" +
        "                                <dayFrom>6</dayFrom>\n" +
        "                                <dayTo>7</dayTo>\n" +
        "                                <timeFrom>12:00</timeFrom>\n" +
        "                                <timeTo>18:00</timeTo>\n" +
        "                            </date-time-range>\n" +
        "                        </schedule>\n" +
        "                    </outlet>\n" +
        "                </delivery>\n";
    private static String GET_BUYER = "<buyer>" +
        "                    <uid>2382392</uid>\n" +
        "                    <lastName>Паровозов</lastName>\n" +
        "                    <firstName>Антон</firstName>\n" +
        "                    <phone>89162727272</phone>\n" +
        "                    <email>aparovoz@yandex.ru</email>\n" +
        "                    <bindKey>abcdef</bindKey>\n" +
        "                </buyer>";
    static String UNPAID = constructModel(OrderStatus.UNPAID, GET_PICKUP, GET_PREPAID,
        getEntry(), false, false, false
    );
    static String UNPAID_CPA20 = constructModel(OrderStatus.UNPAID, GET_PICKUP, GET_PREPAID,
        getEntry(), false, false, true
    );
    static String PROCESSING_DELIVERY = constructModel(OrderStatus.PROCESSING, GET_DELIVERY, GET_PREPAID,
        getEntry(), false, false, false
    );
    static String PROCESSING_DELIVERY_CPA20_FREE = constructModel(OrderStatus.PROCESSING, GET_DELIVERY_FREE, GET_PREPAID,
        getEntry(), false, false, true
    );
    static String PROCESSING_DELIVERY_NOAUTH = constructModel(OrderStatus.PROCESSING, GET_DELIVERY, GET_PREPAID,
        getEntry(), true, false, false
    );
    static String PROCESSING_MIXED = constructAggModel(
        constructModel(OrderStatus.PROCESSING, GET_PICKUP, GET_PREPAID,
            Arrays.asList(GET_ENTRY_1, GET_ENTRY_2, GET_ENTRY_3), false, false, false),
        constructModel(OrderStatus.PROCESSING, GET_POST, GET_POSTPAID,
            getEntry(), false, false, false),
        constructModel(OrderStatus.PROCESSING, GET_DELIVERY, GET_PREPAID_SHOP,
            Arrays.asList(GET_ENTRY_3), false, false, false)
    );
    static String PROCESSING_PICKUP =constructModel(OrderStatus.PROCESSING, GET_PICKUP, GET_PREPAID,
        getEntry(), false, false, false
    );
    static String PROCESSING_PICKUP_POSTPAID =constructModel(OrderStatus.PROCESSING, GET_PICKUP, GET_POSTPAID,
        getEntry(), false, false, false
    );
    static String PROCESSING_BOOKNOW = constructModel(OrderStatus.PROCESSING, GET_PICKUP, GET_POSTPAID,
        getEntry(), false, true, false
    );
    static String CANCELLED = constructAggModel(
        constructModel(OrderStatus.CANCELLED, GET_DELIVERY, GET_POSTPAID,
            getEntry(), false, false, false),
        constructModel(OrderStatus.CANCELLED, GET_POST, GET_PREPAID,
            getEntry(), false, false, false)
    );
    static String CANCELLED_PREPAID_SHOP = constructModel(OrderStatus.CANCELLED, GET_DELIVERY, GET_PREPAID_SHOP,
        getEntry(), false, false, false
    );
    static String CANCELLED_PREPAID_YANDEX = constructModel(OrderStatus.CANCELLED, GET_DELIVERY, GET_PREPAID,
        getEntry(), false, false, false
    );
    static String CANCELLED_PREPAID_CPA20 = constructModel(OrderStatus.CANCELLED, GET_DELIVERY, GET_PREPAID,
        getEntry(), false, false, true
    );
    static String CANCELLED_PREPAID_CPA20_USER_CHANGED_MIND = constructModelForCancelled(OrderSubstatus.USER_CHANGED_MIND,
        GET_DELIVERY, GET_PREPAID, getEntry(), false, false, true
    );
    static String CANCELLED_BOOKNOW = constructModel(OrderStatus.CANCELLED, GET_PICKUP, GET_POSTPAID,
        getEntry(), false, true, false
    );
    static String DELIVERY = constructModel(OrderStatus.DELIVERY, GET_POST, GET_PREPAID,
        getEntry(), false, false, false
    );
    static String PICKUP = constructModel(OrderStatus.PICKUP, GET_PICKUP, GET_PREPAID,
        getEntry(), false, false, false
    );
    static String PICKUP_POSTPAID = constructModel(OrderStatus.PICKUP, GET_PICKUP, GET_POSTPAID,
        getEntry(), false, false, false
    );
    static String DELIVERED = constructModel(OrderStatus.DELIVERED, GET_DELIVERY, GET_PREPAID,
        getEntry(), false, false, false
    );
    static String DELIVERED_CPA20 = constructModel(OrderStatus.DELIVERED, GET_DELIVERY, GET_PREPAID,
        getEntry(), false, false, true
    );
    static String DELIVERED_NOAUTH_POSTPAID = constructModel(OrderStatus.DELIVERED, GET_DELIVERY, GET_POSTPAID,
        getEntry(), true, false, false
    );
    static String DELIVERED_NOAUTH_PREPAYED = constructModel(OrderStatus.DELIVERED, GET_DELIVERY, GET_PREPAID,
            getEntry(), true, false, false
    );
    static String DELIVERED_NOAUTH_CPA20 = constructModel(OrderStatus.DELIVERED, GET_DELIVERY, GET_POSTPAID,
        getEntry(), true, false, true
    );
    // 3 delivery * 2 payment* 2isNoAuth * 2 isBooked * aggregation
    public static final Map<String, Pair<String, EmailSnippetType>> xmlToSnippet = Collections.unmodifiableMap(new HashMap<String, Pair<String, EmailSnippetType>>() {{
        put("UNPAID", new Pair<>(UNPAID, EmailSnippetType.ORDER_UNPAID));
        put("UNPAID_CPA20", new Pair<>(UNPAID_CPA20, EmailSnippetType.ORDER_UNPAID));

        put("PROCESSING_DELIVERY", new Pair<>(PROCESSING_DELIVERY, EmailSnippetType.ORDER_PROCESSING));
        put("PROCESSING_DELIVERY_CPA20", new Pair<>(PROCESSING_DELIVERY_CPA20_FREE, EmailSnippetType.ORDER_PROCESSING));
        put("PROCESSING_DELIVERY_NOAUTH", new Pair<>(PROCESSING_DELIVERY_NOAUTH, EmailSnippetType.ORDER_PROCESSING));
        put("PROCESSING_MIXED", new Pair<>(PROCESSING_MIXED, EmailSnippetType.ORDER_PROCESSING));
        put("PROCESSING_PICKUP", new Pair<>(PROCESSING_PICKUP, EmailSnippetType.ORDER_PROCESSING));
        put("PROCESSING_PICKUP_POSTPAID", new Pair<>(PROCESSING_PICKUP_POSTPAID, EmailSnippetType.ORDER_PROCESSING));
        put("PROCESSING_BOOKNOW", new Pair<>(PROCESSING_BOOKNOW, EmailSnippetType.ORDER_PROCESSING));

        put("CHANGE", new Pair<>(PROCESSING_DELIVERY, EmailSnippetType.ORDER_DELIVERY_CHANGE));

        put("DELIVERY", new Pair<>(DELIVERY, EmailSnippetType.ORDER_DELIVERY));
        put("PICKUP", new Pair<>(PICKUP, EmailSnippetType.ORDER_PICKUP));
        put("PICKUP_POSTPAID", new Pair<>(PICKUP_POSTPAID, EmailSnippetType.ORDER_PICKUP));

        put("CANCELLED", new Pair<>(CANCELLED, EmailSnippetType.ORDER_CANCELLED));
        put("CANCELLED_PREPAID_SHOP", new Pair<>(CANCELLED_PREPAID_SHOP, EmailSnippetType.ORDER_CANCELLED));
        put("CANCELLED_PREPAID_YANDEX", new Pair<>(CANCELLED_PREPAID_YANDEX, EmailSnippetType.ORDER_CANCELLED));
        put("CANCELLED_PREPAID_CPA20", new Pair<>(CANCELLED_PREPAID_CPA20, EmailSnippetType.ORDER_CANCELLED));
        put("CANCELLED_PREPAID_CPA20_USER_CHANGED_MIND", new Pair<>(CANCELLED_PREPAID_CPA20_USER_CHANGED_MIND, EmailSnippetType.ORDER_CANCELLED));
        put("CANCELLED_BOOKNOW", new Pair<>(CANCELLED_BOOKNOW, EmailSnippetType.ORDER_CANCELLED));

        put("DELIVERED", new Pair<>(DELIVERED, EmailSnippetType.ORDER_DELIVERED));
        put("DELIVERED_CPA20", new Pair<>(DELIVERED_CPA20, EmailSnippetType.ORDER_DELIVERED));
        put("DELIVERED_NOAUTH_POSTPAID", new Pair<>(DELIVERED_NOAUTH_POSTPAID, EmailSnippetType.ORDER_DELIVERED));
        put("DELIVERED_NOAUTH_PREPAID", new Pair<>(DELIVERED_NOAUTH_PREPAYED, EmailSnippetType.ORDER_DELIVERED));
        put("DELIVERED_NOAUTH_CPA20", new Pair<>(DELIVERED_NOAUTH_CPA20, EmailSnippetType.ORDER_DELIVERED));
    }});

    @Autowired
    private EmailSnippetGenerator emailSnippetGenerator;

    private static List<String> getEntry() {
        return Arrays.asList(GET_ENTRY_1, GET_ENTRY_2);
    }

    private static String getDelivery(String type) {
        return getDelivery(type, 350);
    }

    private static String getDelivery(String type, int price) {
        return "            <delivery>\n" +
            "                <type>" + type + "</type>\n" +
            "                <serviceName>own service</serviceName>\n" +
            "                <price>" + price + "</price>\n" +
            "                <buyerPrice>" + price + "</buyerPrice>\n" +
            "                <deliveryDates>\n" +
            "                    <toDate>2013-07-03 00:00:00.0 MSK</toDate>\n" +
            "                    <fromDate>2013-07-03 00:00:00.0 MSK</fromDate>\n" +
            "                </deliveryDates>\n" +
            "                <regionId>213</regionId>\n" +
            "                <buyerAddress>\n" +
            "                    <country>Россия</country>\n" +
            "                    <postcode>123456</postcode>\n" +
            "                    <city>Москва</city>\n" +
            "                    <street>Льва Толстого</street>\n" +
            "                    <house>16</house>\n" +
            "                    <apartment>160</apartment>\n" +
            "                    <entrance>1</entrance>\n" +
            "                    <floor>6</floor>\n" +
            "                    <phone>+79163420445</phone>\n" +
            "                    <recipient>Кузнецов Алексей</recipient>\n" +
            "                </buyerAddress>\n" +
            "                <shopAddress>\n" +
            "                    <country>Россия</country>\n" +
            "                    <postcode>123456</postcode>\n" +
            "                    <city>Москва</city>\n" +
            "                    <street>Льва Толстого</street>\n" +
            "                    <house>16</house>\n" +
            "                    <apartment>160</apartment>\n" +
            "                    <entrance>1</entrance>\n" +
            "                    <floor>6</floor>\n" +
            "                    <phone>+79163420445</phone>\n" +
            "                    <recipient>Кузнецов Алексей</recipient>\n" +
            "                </shopAddress>\n" +
            "            </delivery>\n";
    }

    public static String constructAggModel(String... orders){
        StringBuilder result = new StringBuilder("<aggregation>");
        for (String order : orders) {
            result.append(order);
        }
        result.append("</aggregation>");
        return result.toString();
    }

    public static String constructModel(OrderStatus status, String delivery, String payment, List<String> entry,
                                        boolean noAuth, boolean isBooked, boolean cpa20) {
        return constructModel(status, delivery, payment, entry, noAuth, isBooked, cpa20, OrderSubstatus.SHOP_FAILED);
    }

    public static String constructModelForCancelled(OrderSubstatus substatus, String delivery, String payment, List<String> entry,
                                                    boolean noAuth, boolean isBooked, boolean cpa20) {
        return constructModel(OrderStatus.CANCELLED, delivery, payment, entry, noAuth, isBooked, cpa20, substatus);
    }

    public static String constructModel(OrderStatus status, String delivery, String payment, List<String> entry,
                                        boolean noAuth, boolean isBooked, boolean cpa20, OrderSubstatus substatus) {
        StringBuilder result = new StringBuilder("<local-order>\n<order>\n");

        result.append("<items>\n");
        entry.forEach(e -> result.append(e));
        result.append("</items>\n");

        result.append(delivery)
            .append(payment);

        result.append("<status>" + status.name() + "</status>\n");
        if (status == OrderStatus.CANCELLED) {
            result.append("<substatus>" + substatus + "</substatus>");
        }

        result.append("<noAuth>" + noAuth + "</noAuth>\n");
        result.append("<isBooked>" + isBooked + "</isBooked>\n");
        result.append("<cpa20>" + cpa20 + "</cpa20>");

        result.append(GET_BUYER)
            .append(
            "                <id>101</id>\n" +
            "                <shopId>774</shopId>\n" +
            "                <currency>RUR</currency>\n" +
            "                <buyerCurrency>RUR</buyerCurrency>\n" +
            "                <total>550</total>\n" +
            "                <buyerTotal>550</buyerTotal>\n" +
            "                <itemsTotal>200</itemsTotal>\n" +
            "                <buyerItemsTotal>200</buyerItemsTotal>\n" +
            "                <feeTotal>2</feeTotal>\n" +
            "                <creationDate>2013-07-10 14:42:12.0 MSK</creationDate>\n" +
            "                <fake>false</fake>\n" +
            "                <shopOrderId>1234</shopOrderId>\n" +
            "                <statusExpiryDate>2014-09-29 13:58:59</statusExpiryDate>\n" +
            "                <acceptMethod>WEB_INTERFACE</acceptMethod>\n");
        result.append("</order>\n");

        result.append(GET_SHOP)
            .append("<creationDateString>10 июля 2013</creationDateString>\n")
            .append("<updateTimeString>11 июля 2013</updateTimeString>")
            .append("    <sk>Ud$RX{n\\2552M&quot;x3v72=</sk>")
//        "    <statusBefore>PENDING</statusBefore>\n" +
//            "    <conversation>false</conversation>\n" +
//            "    <authorRole>USER</authorRole>\n" +
            .append("<statusBefore>UNPAID</statusBefore>\n")
            .append("<courierCallTime>Доставка состоится сегодня 11.07 в течение 3 часов. Курьер позвонит вам за час до доставки.</courierCallTime>")
            .append("</local-order>");
        return result.toString();
    }

    @Test
    public void testUnsbscribeLink() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("link", "fuck");
        String url = emailSnippetGenerator.generateHtml(EmailSnippetType.UNSUBSCRIBE_URL, model);
        assertTrue(url.trim().startsWith("https://market.yandex.ru/my/unsubscribe?"));
        assertTrue(url.trim().endsWith("fuck"));
    }

    @Test
    //@Ignore
    public void testXmlSamples() throws Exception {
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put("notification_type", "");
        modelMap.put("isHoliday", false);
        htmlGenerator(modelMap, PREFIX);
        modelMap.put("isHoliday", true);
        htmlGenerator(modelMap, PREFIX + "isHoliday_");
    }

    private void htmlGenerator(Map<String, Object> modelMap, String prefix) throws Exception {
        for (Map.Entry<String, Pair<String, EmailSnippetType>> entry : xmlToSnippet.entrySet()) {
            modelMap.put("order_data",
                freemarker.ext.dom.NodeModel.parse(
                    new InputSource(IOUtils.toInputStream(
                        entry.getValue().getFirst(), StandardCharsets.UTF_8))));
            String html = emailSnippetGenerator.generateHtml(entry.getValue().getSecond(), modelMap);
            //writeHtmlToFile(prefix, html, entry.getKey());
        }
    }

    private void writeHtmlToFile(String prefix, String html, String snippetName) throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter(
            new OutputStreamWriter(new FileOutputStream(prefix + snippetName + ".html"), StandardCharsets.UTF_8))) {
            out.println(htmlBefore + html + htmlAfter);
        }
    }

    private WantedModel generateWantedModel() {
        Model model = new Model();
        model.setId(123);
        model.setName("model");
        Category category = new Category(123L, "name");
        model.setCategory(category);
        WantedModel wantedModel = new WantedModel(model);
        wantedModel.setPictureUrl("url");
        return wantedModel;
    }

    private void putCommonParams(Map<String, Object> map) {
        map.put("model", generateWantedModel());
        map.put("modelName", "model");
        map.put("utmArgs", "utm_source=email&amp;utm_medium=trigger&amp;utm_campaign=postuplenie_v_nalichie");
        map.put("utm_1", "utm_source=email&amp;utm_medium=trigger&amp;utm_campaign=postuplenie_v_nalichie");
    }

    private Map<String, Object> prepareModelForModelEmailSnippet() {
        Map<String, Object> map = new HashMap<>();
        putCommonParams(map);
        return map;
    }

    private Map<String, Object> prepareModelForSimilarOrPopularModelsEmailSnippet(String similarOrPopular) {
        Map<String, Object> map = new HashMap<>();
        List<OfferModel> offerModels = new ArrayList<>();
        OfferModel offerModel = new OfferModel();
        offerModel.setModelId(123L);
        offerModel.setPictureUrl("url");
        offerModel.setModelName("name");
        offerModel.setCategory("category");
        offerModel.setCurrency("rur");
        offerModel.setFromPrice(300d);
        offerModels.add(offerModel);
        putCommonParams(map);
        map.put(similarOrPopular + "Models", offerModels);
        return map;
    }



    @Test
    public void testModelEmailSnippet() throws Exception {
        Map<String, Object> map = prepareModelForModelEmailSnippet();
        String html = emailSnippetGenerator.generateHtml(EmailSnippetType.MODEL, map);
        //writeHtmlToFile(PREFIX, html, "MODEL");
        assertTrue(!html.contains("utm_content"));
    }

    @Test
    public void testSimilarModelsEmailSnippet() throws FileNotFoundException {
        Map<String, Object> map = prepareModelForSimilarOrPopularModelsEmailSnippet("similar");
        String html = emailSnippetGenerator.generateHtml(EmailSnippetType.SIMILAR_MODELS, map);
        //writeHtmlToFile(PREFIX, html, "SIMILAR_MODELS");
        assertTrue(!html.contains("utm_content"));
        assertTrue(html.contains("utm_term=similar"));
    }
}
