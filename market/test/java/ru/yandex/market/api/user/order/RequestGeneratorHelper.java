package ru.yandex.market.api.user.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mockito.ArgumentMatcher;

import ru.yandex.market.api.common.RoomAddress;
import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.v2.BundleSettings;
import ru.yandex.market.api.domain.v2.BundleSettings.QuantityLimit;
import ru.yandex.market.api.domain.v2.DeliveryV2;
import ru.yandex.market.api.domain.v2.OfferPriceV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.ShopInfoV2;
import ru.yandex.market.api.user.order.checkout.AddressDeliveryPoint;
import ru.yandex.market.api.user.order.checkout.CheckoutRequest;
import ru.yandex.market.api.user.order.checkout.DeliveryPointId;
import ru.yandex.market.api.user.order.preorder.OrderOptionsRequest;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckoutParameters;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletPhone;
import ru.yandex.market.checkout.checkouter.order.AdditionalCartInfo;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.common.report.model.FeedOfferId;

/**
 * TODO: Сейчас у нас в тестах чекаутера 3 способа подготовки тестовых данных
 * Функции генератора в этом классе, билдеры унаследованные от RandomBuilder и
 * ручное создание в самом тесте. Кажется, что билдеры выглядят более красиво, но
 * их необходимо доработать и, возможно, отказаться от общего предка.
 *
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@SuppressFBWarnings("SE_BAD_FIELD")
public final class RequestGeneratorHelper {
    public static final Long WEIGHT_IN_GRAMMS = 100L;

    private static final int YEAR_OFFSET = 1900;
    private static final int MONTH_OFFSET = 1;

    static int year(int year) {
        return year - YEAR_OFFSET;
    }

    static int month(int month) {
        return month - MONTH_OFFSET;
    }

    static int day(int day) {
        return day;
    }

    static final long TEST_USER_ID = 123456;
    static final int TEST_USER_REGION_ID = 123;
    static final int TEST_SECOND_USER_REGION_ID = 42;
    static final Currency TEST_USER_CURRENCY = Currency.RUR;

    // Участвуют в проверке валидации запроса
    static final int TEST_COUNTRY_REGION_ID = 3;
    static final int TEST_WRONG_SHOP_ID = 999;

    /*
     * Test shop 1
     */
    static final int TEST_SHOP_ID = 111;
    static final String TEST_SHOP_NAME = "Test shop name";
    static final long TEST_SHOP_FEED_ID = 111222;

    /*
     * Test shop 1 offer 1
     */
    static final String TEST_OFFER_SHOP_ID = "iddqd-from-shop";
    static final String TEST_OFFER_ID = "iddqd";
    static final BigDecimal TEST_OFFER_PRICE = new BigDecimal("123.99");
    private static final BigDecimal TEST_OFFER_2_PRICE = new BigDecimal("99.99");
    static final String TEST_OFFER_RIGHT_FEE_SHOW = "iddqd-right-fee-show";
    static final String TEST_OFFER_WRONG_FEE_SHOW = "iddqd-wrong-fee-show";
    static final String CHECKOUTER_FEE_SHOW = "checkouter-fee-show";
    static final String TEST_OFFER_DELIVERY_BRIEF = "Test Offer Brief";
    static final String TEST_ORDER_LABEL = "UUDDLRLRBA";

    static final Collection<String> TEST_RGBS = Collections.singletonList("white");

    private static final OfferV2 TEST_OFFER_V2 = new OfferV2() {{
        setId(new OfferId(TEST_OFFER_ID, TEST_OFFER_WRONG_FEE_SHOW));
        setFeeShow(TEST_OFFER_WRONG_FEE_SHOW);
        setShopOfferId(new ShopOfferId(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID));
        setPrice(new OfferPriceV2(TEST_OFFER_PRICE.toString(), "0", TEST_OFFER_PRICE.toString()));
        setShop(new ShopInfoV2() {{
            setId(TEST_SHOP_ID);
        }});
        setBundleSettings(new BundleSettings(new QuantityLimit(2, 5)));
        setCpa(true);

        DeliveryV2 deliveryV2 = DeliveryV2.withExplicitDefaults();
        deliveryV2.setBrief(TEST_OFFER_DELIVERY_BRIEF);
        setDelivery(deliveryV2);
    }};

    private static final OfferV2 TEST_OFFER_V2_2 = new OfferV2() {{
        setId(new OfferId(TEST_OFFER_2_ID, TEST_OFFER_2_WRONG_FEE_SHOW));
        setFeeShow(TEST_OFFER_2_WRONG_FEE_SHOW);
        setShopOfferId(new ShopOfferId(TEST_SHOP_FEED_ID, TEST_OFFER_2_SHOP_ID));
        setPrice(new OfferPriceV2(TEST_OFFER_2_PRICE.toString(), "0", TEST_OFFER_2_PRICE.toString()));
        setShop(new ShopInfoV2() {{
            setId(TEST_SHOP_ID);
        }});
        setCpa(true);
    }};

    /*
     * Test shop 1 offer 2
     */
    private static final String TEST_OFFER_2_SHOP_ID = "idkfa-from-shop";
    private static final String TEST_OFFER_2_ID = "idkfa";
    private static final String TEST_OFFER_2_WRONG_FEE_SHOW = "idkfa-wrong-fee-show";

    private static final Map<String, OfferV2> OFFERS = ImmutableMap.<String, OfferV2> builder()
            .put(TEST_OFFER_V2.getId().getWareMd5(), TEST_OFFER_V2)
            .put(TEST_OFFER_V2_2.getId().getWareMd5(), TEST_OFFER_V2_2)
            .build();

    private static final Map<ShopOfferId, OfferV2> OFFER_INFO_BY_SHOP_OFFER_KEY = ImmutableMap.<ShopOfferId, OfferV2> builder()
        .put(TEST_OFFER_V2.getShopOfferId(), TEST_OFFER_V2)
        .put(TEST_OFFER_V2_2.getShopOfferId(), TEST_OFFER_V2_2)
        .build();

    static final long TEST_ORDER_ID = 1234L;
    static final String TEST_MARKET_OFFER_ID = "!234Test";
    static final long TEST_SUPPLIER_ID = 345L;
    static final String TEST_SKU_ID = "678";

    /**
     * Рекомендуется использовать эту функцию для мока OfferSupplier
     * @param ids коллекция идентификаторов
     * @return отфильтрованная коллекция
     */
    static Map<String, OfferV2> extactOffersV2(Collection<String> ids) {
        return Maps.filterKeys(OFFERS, ids::contains);
    }

    /**
     * Рекомендуется использовать эту функцию для мока OfferSupplier
     * @param ids коллекция идентификтаоров
     * @return отфильтрованная коллекция
     */
    static Map<ShopOfferId, OfferV2> extractOfferInfoByShopOfferKey(Collection<ShopOfferId> ids) {
        return Maps.filterKeys(OFFER_INFO_BY_SHOP_OFFER_KEY, ids::contains);
    }

    static final MultiOrder CHECKOUTED_RESPONSE = new MultiOrder() {{
        setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        setPaymentType(PaymentType.POSTPAID);
        setBuyerRegionId((long) TEST_USER_REGION_ID);
        setCarts(new ArrayList<Order>() {{
            add(new Order() {{
                setShopId((long) TEST_SHOP_ID);
                addItem(new OrderItem() {{
                    setWareMd5(TEST_OFFER_ID);
                    setFeedOfferId(new FeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID));
                }});
            }});
        }});
    }};

    static class MultiCartMatcher extends ArgumentMatcher<MultiCart> {
        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof MultiCart)) {
                return false;
            }
            MultiCart multiCart = (MultiCart) argument;

            OrderItem item = multiCart.getCarts().get(0).getItem(new FeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID));
            return item.getShowInfo().equals(TEST_OFFER_RIGHT_FEE_SHOW);
        }
    }

    static class MultiOrderMatcher extends ArgumentMatcher<MultiOrder> {
        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof MultiOrder)) {
                return false;
            }
            MultiOrder multiCart = (MultiOrder) argument;

            OrderItem item = multiCart.getCarts().get(0).getItem(new FeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID));
            return item.getShowInfo().equals(TEST_OFFER_RIGHT_FEE_SHOW);
        }
    }

    public static class CartParametersMatcher extends ArgumentMatcher<CartParameters> {
        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof CartParameters)) {
                return false;
            }
            CartParameters parameters = (CartParameters) argument;

            return (parameters.getUid() == TEST_USER_ID) && (!parameters.isSandbox());
        }
    }

    public static class CartParametersMatcherWithYandexEmployeePerk extends ArgumentMatcher<CartParameters> {

        private boolean status;

        public CartParametersMatcherWithYandexEmployeePerk(boolean perkStatus) {
            this.status = perkStatus;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof CartParameters)) {
                return false;
            }
            CartParameters parameters = (CartParameters) argument;

            return (this.status == parameters.getYandexEmployee()) && !parameters.isSandbox();
        }
    }

    public static class CartParametersMatcherWithShowInstallments extends ArgumentMatcher<CartParameters> {

        private boolean showInstallments;

        public CartParametersMatcherWithShowInstallments(boolean showInstallments) {
            this.showInstallments = showInstallments;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof CartParameters)) {
                return false;
            }
            CartParameters parameters = (CartParameters) argument;

            return (this.showInstallments == parameters.getShowInstallments());
        }
    }

    public static class CheckoutParametersMatcherWithShowInstallments extends ArgumentMatcher<CheckoutParameters> {

        private boolean showInstallments;

        public CheckoutParametersMatcherWithShowInstallments(boolean showInstallments) {
            this.showInstallments = showInstallments;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof CheckoutParameters)) {
                return false;
            }
            CheckoutParameters parameters = (CheckoutParameters) argument;

            return (this.showInstallments == parameters.getShowInstallments());
        }
    }

    public static class CartParametersMatcherWithShowCreditBroker extends ArgumentMatcher<CartParameters> {

        private boolean showCreditBroker;

        public CartParametersMatcherWithShowCreditBroker(boolean showCreditBroker) {
            this.showCreditBroker = showCreditBroker;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof CartParameters)) {
                return false;
            }
            CartParameters parameters = (CartParameters) argument;

            return (this.showCreditBroker == parameters.getShowCreditBroker());
        }
    }

    public static class CheckoutParametersMatcherWithShowCreditBroker extends ArgumentMatcher<CheckoutParameters> {

        private boolean showCreditBroker;

        public CheckoutParametersMatcherWithShowCreditBroker(boolean showCreditBroker) {
            this.showCreditBroker = showCreditBroker;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof CheckoutParameters)) {
                return false;
            }
            CheckoutParameters parameters = (CheckoutParameters) argument;

            return (this.showCreditBroker == parameters.getShowCreditBroker());
        }
    }

    public static class CartParametersMatcherWithPerks extends ArgumentMatcher<CartParameters> {

        private final String perksString;

        public CartParametersMatcherWithPerks(final String perksString) {
            this.perksString = perksString;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof CartParameters)) {
                return false;
            }
            final CartParameters parameters = (CartParameters) argument;
            return perksString.equals(parameters.getPerks());
        }
    }

    public static class CartParametersMatcherWithOptionalRules extends ArgumentMatcher<CartParameters> {

        private final Boolean isOptionalRulesEnabled;

        public CartParametersMatcherWithOptionalRules(final Boolean isOptionalRulesEnabled) {
            this.isOptionalRulesEnabled = isOptionalRulesEnabled;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof CartParameters)) {
                return false;
            }
            final CartParameters parameters = (CartParameters) argument;
            return isOptionalRulesEnabled.equals(parameters.getOptionalRulesEnabled());
        }
    }

    public static class CartParametersMatcherWithSeparateOrdersCalculation extends ArgumentMatcher<CartParameters> {

        private final Boolean calculateOrdersSeparately;

        public CartParametersMatcherWithSeparateOrdersCalculation(final Boolean calculateOrdersSeparately) {
            this.calculateOrdersSeparately = calculateOrdersSeparately;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof CartParameters)) {
                return false;
            }
            final CartParameters parameters = (CartParameters) argument;
            return calculateOrdersSeparately.equals(parameters.getCalculateOrdersSeparately());
        }
    }

    public static class OrderSearchRequestMatcher extends ArgumentMatcher<OrderSearchRequest> {
        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof OrderSearchRequest)) {
                return false;
            }

            OrderSearchRequest request = (OrderSearchRequest) argument;

            return TEST_ORDER_ID == request.orderIds.get(0);
        }
    }

    public static class RequestClientInfoMatcher extends ArgumentMatcher<RequestClientInfo> {
        @Override
        public boolean matches(Object argument) {

            if (!(argument instanceof RequestClientInfo)) {
                return false;
            }

            RequestClientInfo clientInfo = (RequestClientInfo) argument;

            return TEST_USER_ID == clientInfo.getClientId() && ClientRole.USER == clientInfo.getClientRole();
        }
    }

    static OrderOptionsRequest generateOrderOptionRequestWithOneOrder(long userId,
                                                                      int userRegionId,
                                                                      Currency userCurrency,
                                                                      int shopId,
                                                                      OfferId offerId,
                                                                      String offerFeeShow,
                                                                      BigDecimal offerPrice,
                                                                      int offerCount) {
        OrderOptionsRequest request = new OrderOptionsRequest();
        request.setRegionId(userRegionId);
        request.setCurrency(userCurrency);
        request.setShopOrders(new ArrayList<OrderOptionsRequest.ShopOrder>() {{
            add(new OrderOptionsRequest.ShopOrder(
                shopId,
                new ArrayList<OrderOptionsRequest.OrderItem>() {{
                    add(new OrderOptionsRequest.OrderItem() {{
                        setOfferId(offerId);
                        setPrice(offerPrice);
                        setCount(offerCount);
                    }});
                }},
                null
            ));
        }});

        return request;
    }

    static OrderOptionsRequest generateOrderOptionRequestWithOneOrderAndAddress(long userId,
                                                                                int userRegionId,
                                                                                Currency userCurrency,
                                                                                int shopId,
                                                                                OfferId offerId,
                                                                                String offerFeeShow,
                                                                                BigDecimal offerPrice,
                                                                                int offerCount) {
        OrderOptionsRequest request = generateOrderOptionRequestWithOneOrder(
                userId,
                userRegionId,
                userCurrency,
                shopId,
                offerId,
                offerFeeShow,
                offerPrice,
                offerCount
        );
        request.getShopOrders().get(0).setDeliveryPoint(generateAddressDeliveryPoint(userRegionId));
        return request;
    }

    static CheckoutRequest generateCheckoutRequestWithOneOrder(int userRegionId,
                                                               Currency userCurrency,
                                                               int shopId,
                                                               OfferId offerId,
                                                               String offerFeeShow,
                                                               BigDecimal offerPrice,
                                                               int offerCount) {
        CheckoutRequest request = new CheckoutRequest();
        request.setRegionId(userRegionId);
        request.setCurrency(userCurrency);
        request.setShopOrders(new ArrayList<CheckoutRequest.ShopOrder>(){{
            add(new CheckoutRequest.ShopOrder(){{
                    setShopId(shopId);
                    setItems(new ArrayList<CheckoutRequest.OrderItem>(){{
                        add(new CheckoutRequest.OrderItem(){{
                            setOfferId(offerId);
                            setPrice(offerPrice);
                            setCount(offerCount);
                            setPayload(new Payload(1, TEST_OFFER_SHOP_ID, offerId.getWareMd5(), offerFeeShow));
                        }});
                    }});
                    setDeliveryPoint(new AddressDeliveryPoint(){{
                        setDeliveryOptionId(new DeliveryPointId());
                        setRegionId(54);
                        setRecipient("recipient");
                        setPhone("111-11-11");
                        setAddress(new RoomAddress() {{
                            setCountry("mather-russia");
                            setCity("ekb");
                            setHouse("11");
                        }});
                    }});
            }});
        }});
        request.setBuyer(new Buyer() {{
            setFirstName("FirstName");
            setLastName("LastName");
            setPhone("111-11-11");
            setEmail("buyer@email.com");
        }});

        return request;
    }

    static OrderOptionsRequest.ShopOrder generateShopOrder(int regionId) {
        OrderOptionsRequest.ShopOrder result = new OrderOptionsRequest.ShopOrder(
                TEST_SHOP_ID,
                new ArrayList<OrderOptionsRequest.OrderItem>() {{
                    add(new OrderOptionsRequest.OrderItem() {{
                        setOfferId(new OfferId(TEST_OFFER_ID, null));
                        setPrice(TEST_OFFER_PRICE);
                        setCount(1);
                    }});
                }},
                null
        );

        result.setDeliveryPoint(generateAddressDeliveryPoint(regionId));

        return result;
    }

    static OrderOptionsRequest.AddressDeliveryPoint generateAddressDeliveryPoint(int regionId) {
        return new OrderOptionsRequest.AddressDeliveryPoint() {{
            setId(new DeliveryPointId("234234", "dfsghfsdrhsf"));
            setRegionId(regionId);
            setCountry("Test-Country");
            setCity("Test-City");
            setDistrict("Test-District");
            setStreet("Test-Street");
            setHouse("Test-House");
            setPreciseRegionId(123L);
        }};
    }

    static MultiCart generateMultiCartWithOneOrder(int userRegionId,
                                                   int shopId,
                                                   long shopFeedId,
                                                   String showInfo,
                                                   String shopOfferId,
                                                   BigDecimal offerPrice,
                                                   int offerCount) {
        MultiCart response = new MultiCart();
        response.setPaymentOptions(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY));
        response.setCarts(Lists.newArrayList(new Order(){{
            setShopId((long) shopId);
            BigDecimal total = offerPrice.multiply(new BigDecimal(offerCount));
            setItemsTotal(total);
            setBuyerItemsTotal(total);
            setTotal(total);
            setBuyerTotal(total);
            setFeeTotal(BigDecimal.ZERO);
            setGlobal(false);

            setItems(Lists.newArrayList(new OrderItem(){{
                setFeedId(shopFeedId);
                setOfferId(shopOfferId);
                setShowInfo(showInfo);
                setCategoryId(12345);
                setOfferName("Test-Offer-Name");
                setCount(offerCount);
                setDelivery(true);
                setBuyerPrice(offerPrice);
                setPictureURL("http://test.com/offer/image.jpg");
            }}));

            setDeliveryOptions(Lists.newArrayList(
                    new Delivery(){{
                        setHash("Test-Delivery-Id");
                        setType(DeliveryType.DELIVERY);
                        setServiceName("Test-Delivery-Service-name");
                        BigDecimal deliveryPrice = new BigDecimal("69.99");
                        setPrice(deliveryPrice);
                        setBuyerPrice(deliveryPrice);
                        setDeliveryDates(new DeliveryDates(){{
                            setFromDate(new Date(year(2016), month(1), day(1)));
                            setToDate(new Date(year(2016), month(1), day(5)));
                        }});
                        RawDeliveryIntervalsCollection rawDeliveryIntervalsCollection = new RawDeliveryIntervalsCollection();
                        rawDeliveryIntervalsCollection.add(new RawDeliveryInterval(new Date()));
                        setRawDeliveryIntervals(rawDeliveryIntervalsCollection);
                        setPaymentOptions(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY));
                        setEstimated(true);
                    }},
                    new Delivery(){{
                        setHash("Test-Outlet-Delivery-Id");
                        setType(DeliveryType.PICKUP);
                        setServiceName("Test-Self-pick");
                        setPrice(BigDecimal.ZERO);
                        setBuyerPrice(BigDecimal.ZERO);
                        setDeliveryDates(new DeliveryDates(){{
                            setFromDate(new Date(year(2016), month(1), day(1)));
                            setToDate(new Date(year(2016), month(2), day(1)));
                        }});
                        setPaymentOptions(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY));
                        setOutlets(Lists.newArrayList(new ShopOutlet(){{
                            setId(112233L);
                            setName("Test-Outlet-Name");
                            setRegionId(userRegionId);
                            setCity("Test-Outlet-City");
                            setStreet("Test-Outlet-Street");
                            setHouse("Test-Outlet-House");
                            setGps("37.614006,55.756994");
                            setPhones(Lists.newArrayList(
                                    new ShopOutletPhone("7", "123", "4567890", null)
                            ));
                        }}));
                        setEstimated(null);
                    }}
            ));
            setPaymentOptions(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY));

            AdditionalCartInfo info = new AdditionalCartInfo();
            info.setWeight(WEIGHT_IN_GRAMMS);
            setAdditionalCartInfo(Collections.singletonList(info));
        }}));
        return response;
    }
}
