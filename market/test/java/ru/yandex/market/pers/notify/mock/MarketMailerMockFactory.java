package ru.yandex.market.pers.notify.mock;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.CartRequest;
import ru.yandex.market.checkout.carter.web.CartListViewModel;
import ru.yandex.market.checkout.carter.web.CartViewModel;
import ru.yandex.market.checkout.carter.web.ItemOfferViewModel;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderChangesViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderItemChangesViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderItemViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderPricesChangesViewModel;
import ru.yandex.market.checkout.referee.CheckoutReferee;
import ru.yandex.market.cluster.ModelTransitionsParser;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.OfferPicture;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;
import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.grade.client.dto.mailer.MailerAbstractGrade;
import ru.yandex.market.pers.grade.client.dto.mailer.MailerModelGrade;
import ru.yandex.market.pers.grade.client.dto.mailer.MailerShopGrade;
import ru.yandex.market.pers.notify.ems.MailAttachment;
import ru.yandex.market.pers.notify.entity.OfferModel;
import ru.yandex.market.pers.notify.export.MdsExportService;
import ru.yandex.market.pers.notify.export.TestCrmUserSubscriptionItemWriter;
import ru.yandex.market.pers.notify.export.crm.CrmUserSubscriptionItemWriter;
import ru.yandex.market.pers.notify.external.checkouter.CheckouterService;
import ru.yandex.market.pers.notify.external.history.HistoryElement;
import ru.yandex.market.pers.notify.external.history.HistoryService;
import ru.yandex.market.pers.notify.external.report.ReportService;
import ru.yandex.market.pers.notify.external.sender.SenderClient;
import ru.yandex.market.pers.notify.external.solomon.SolomonService;
import ru.yandex.market.pers.notify.service.AboService;
import ru.yandex.market.pers.notify.service.GeoExportService;
import ru.yandex.market.pers.notify.service.ThumbnailService;
import ru.yandex.market.report.model.Category;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.report.model.Offer;
import ru.yandex.market.report.model.OfferPrices;
import ru.yandex.market.report.model.Prices;
import ru.yandex.market.report.model.ProductType;
import ru.yandex.market.report.model.Region;
import ru.yandex.market.report.model.Shop;
import ru.yandex.market.report.model.Vendor;
import ru.yandex.qe.yt.cypress.http.HttpCypress;
import ru.yandex.yql.YqlDataSource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.anySetOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 15.03.16
 */
@SuppressFBWarnings({"MS_MUTABLE_COLLECTION", "MS_MUTABLE_ARRAY"})
public class MarketMailerMockFactory extends MockFactory {
    public static final Map<Object, Consumer<Object>> MOCKS = new HashMap<>();
    public static final Long[] MODEL_IDS = {
        108204L, 106840L, 118676L, 119369L, 135385L, 227752L, 227796L
    };
    public static final Set<Long> DISCOUNT_MODEL_IDS = new HashSet<>(Arrays.asList(
        118984L, 119611L, 119336L
    ));
    public static final int MAX_MODEL_PRICE = 1000;
    private static final ThreadLocalRandom RND = ThreadLocalRandom.current();
    public static final String FIRST_NAME = "Иван";
    public static final String LAST_NAME = "Паровозов";
    public static final String FULL_NAME = FIRST_NAME + " " + LAST_NAME;
    public static final Instant DELIVERY_FROM_INSTANT = Instant.from(Year.of(2020).atDay(1)
            .atStartOfDay(ZoneId.of("UTC")));
    static final byte[] WARRANTY_PDF_BYTES = {2, 54, 12, 35};
    public static final int ITEMS_COUNT_DELTA = 1;

    public static Stream<HistoryElement> generateHistoryElements(int count) {
        return Stream.generate(MarketMailerMockFactory::generateHistoryElement).limit(count);
    }

    public static HistoryElement generateHistoryElement() {
        HistoryElement result = new HistoryElement();
        result.setUuid(RND.nextBoolean() ? null : UUID.randomUUID().toString());
        result.setUserId(RND.nextBoolean() ? null : RND.nextLong());
        result.setDate(RND.nextLong());
        result.setResourceId(RND.nextLong());
        result.setHid(RND.nextBoolean() ? null : RND.nextLong());
        result.setNid(RND.nextBoolean() ? null : RND.nextLong());
        result.setName(UUID.randomUUID().toString());
        result.setType(HistoryElement.Type.values()[RND.nextInt(HistoryElement.Type.values().length)].name());
        return result;
    }

    public static Receipt generateReceipt() {
        Receipt result = new Receipt();
        result.setId(723654L);
        result.setType(ReceiptType.INCOME);
        return result;
    }

    public static Order generateOrder(PaymentType paymentType) {
        return generateOrder(OrderStatus.PROCESSING, false, paymentType, FIRST_NAME, LAST_NAME);
    }

    public static Order generateOrder() {
        return generateOrder(PaymentType.PREPAID);
    }

    public static Order generateOrder(OrderStatus orderStatus, boolean noAuth, PaymentType paymentType,
                                      String firstName, String lastName) {
        Order result = new Order();
        result.setRgb(Color.BLUE);
        result.setId((long) RND.nextInt(10_000));
        result.setFake(false);
        result.setStatusUpdateDate(new Date());
        result.setSubstatusUpdateDate(new Date());
        result.setStatus(orderStatus);
        result.setNoAuth(noAuth);
        result.setUid(RND.nextLong());
        result.setShopOrderId("123456");
        result.setShopId(RND.nextLong());
        result.setShopName("Рога и Копыта");
        result.setPaymentType(paymentType);
        Buyer buyer = new Buyer(result.getUid());
        buyer.setEmail(UUID.randomUUID().toString());
        buyer.setFirstName(firstName);
        buyer.setLastName(lastName);
        result.setBuyer(buyer);

        List<OrderItem> items = new ArrayList<>();
        int count = 2;
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < count; i++) {
            final int itemCount = RND.nextInt(10);
            final BigDecimal price = new BigDecimal(RND.nextInt(10_000));
            items.add(generateOrderItem(price, itemCount, Long.valueOf(i)));
            total = total.add(price.multiply(BigDecimal.valueOf(itemCount)));
        }
        result.setItems(items);
        result.setBuyerItemsTotal(total);

        final BigDecimal deliveryPrice = BigDecimal.valueOf(100L);
        result.setDelivery(generateOrderDelivery(deliveryPrice));
        total = total.add(deliveryPrice);
        result.setTotal(total);
        result.setBuyerTotal(total);
        return result;
    }

    public static ChangeRequest generateChangeRequest(ChangeRequestType requestType,
                                                      ChangeRequestStatus requestStatus,
                                                      Instant requestInstant) {
        return new ChangeRequest(RND.nextLong(), RND.nextLong(), new MockChangeReqestPayload(requestType),
                requestStatus, requestInstant, null, ClientRole.SYSTEM);
    }

    private static class MockChangeReqestPayload extends AbstractChangeRequestPayload {
        protected MockChangeReqestPayload(@NotNull ChangeRequestType type) {
            super(type);
        }
    }

    private static OrderPromo getPromo(PromoType promoType) {
        OrderPromo promo = new OrderPromo();
        promo.setType(promoType);
        promo.setBuyerItemsDiscount(BigDecimal.valueOf(RND.nextInt(100)));

        return promo;
    }

    public static OrderItem generateOrderItem(BigDecimal price, int count, Long ffShopId) {
        OrderItem item = new OrderItem(
            new FeedOfferId(UUID.randomUUID().toString(), RND.nextLong()),
            price, count
        );
        item.setModelId(Math.abs(RND.nextLong()));
        item.setOfferName(UUID.randomUUID().toString());
        item.setSupplierId(ffShopId);
        return item;
    }

    private static Delivery generateOrderDelivery(BigDecimal deliveryPrice) {
        Delivery delivery = new Delivery();
        delivery.setBuyerPrice(deliveryPrice);
        Date date = Date.from(LocalDate.now().plusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        delivery.setDeliveryDates(new DeliveryDates(date, date));
        final AddressImpl address = new AddressImpl();
        address.setCountry("Россия");
        address.setCity("Москва");
        address.setStreet("Льва Толстого");
        address.setHouse("16");
        delivery.setBuyerAddress(address);

        return delivery;
    }

    public static CartViewModel generateCart() {
        CartListViewModel list = new CartListViewModel();
        list.setType(CartList.Type.BASKET);
        list.setId(1L);
        list.setItems(generateCartItems(MODEL_IDS.length));
        return new CartViewModel(Collections.singletonList(list));
    }

    public static List<ItemOfferViewModel> generateCartItems(int count) {
        if (count > MODEL_IDS.length) {
            throw new RuntimeException("Too big list");
        }

        List<ItemOfferViewModel> result = new ArrayList<>();

        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < MODEL_IDS.length; i++) {
            indexes.add(i);
        }
        Collections.shuffle(indexes);
        for (int i = 0; i < count; i++) {
            long modelId = MODEL_IDS[indexes.get(i)];

            result.add(generateCartItem(String.valueOf(modelId)));
        }

        return result;
    }

    public static ItemOfferViewModel generateCartItem(String modelId) {
        ItemOfferViewModel result = new ItemOfferViewModel();
        result.setObjType(CartItem.Type.OFFER);
        result.setObjId(modelId);
        result.setName(UUID.randomUUID().toString());
        result.setModelId(Long.parseLong(modelId));
        return result;
    }

    public static List<Offer> generateOffers(int count) {
        return Stream.generate(MarketMailerMockFactory::generateOffer)
            .limit(count)
            .collect(Collectors.toList());
    }

    public static Offer generateOffer() {
        return generateOffer(null);
    }

    public static Offer generateOffer(Integer homeRegion) {
        Offer offer = new Offer();
        offer.setWareId(UUID.randomUUID().toString());
        offer.setPictureUrl(UUID.randomUUID().toString());
        offer.setName(UUID.randomUUID().toString());
        offer.setShop(
            new Shop(RND.nextLong(10_000), UUID.randomUUID().toString(), RND.nextInt(10),
                new Region(homeRegion == null ? RND.nextLong(100_000) : homeRegion, UUID.randomUUID().toString()))
        );
        offer.setModelId(RND.nextInt(10_000));
        offer.setCategory(new Category(RND.nextInt(10_000), UUID.randomUUID().toString()));
        offer.setPrices(new OfferPrices(
                Currency.values()[RND.nextInt(Currency.values().length)],
                BigDecimal.valueOf(RND.nextDouble(100_000.0)),
                BigDecimal.valueOf(RND.nextDouble(100_000.0)))
        );
        offer.setDeliveryAvailable(true);
        offer.setOnStock(true);
        offer.setFeedId(UUID.randomUUID().toString());
        offer.setShopOfferId(UUID.randomUUID().toString());
        offer.setPictureUrl(UUID.randomUUID().toString());
        offer.setCpa("real");
        return offer;
    }

    public static OfferModel generateOfferModel() {
        OfferModel offerModel = new OfferModel();
        offerModel.setCurrency(Currency.values()[RND.nextInt(Currency.values().length)].name());
        offerModel.setFromPrice(Math.abs(RND.nextDouble(1_000_000.0)));
        offerModel.setCategoryId(Math.abs(RND.nextLong(1_000_000)));
        offerModel.setCategory(UUID.randomUUID().toString());
        offerModel.setModelId(Math.abs(RND.nextLong(1_000_000)));
        offerModel.setModelName(UUID.randomUUID().toString());
        offerModel.setPictureUrl(UUID.randomUUID().toString());
        return offerModel;
    }

    public static OrderChangesViewModel generateOrderChanges() {
        OrderChangesViewModel model = new OrderChangesViewModel();

        List<OrderItemChangesViewModel> items = new ArrayList<>();
        int count = 2;
        BigDecimal totalBefore = BigDecimal.ZERO;
        BigDecimal totalAfter = BigDecimal.ZERO;
        for (int i = 0; i < count; i++) {
            OrderItemChangesViewModel orderItem = new OrderItemChangesViewModel();
            final int itemsCountBefore = RND.nextInt(2, 10);
            final int itemsCountAfter = itemsCountBefore - ITEMS_COUNT_DELTA;
            orderItem.setBeforeCount(itemsCountBefore);
            orderItem.setAfterCount(itemsCountAfter);

            final BigDecimal price = new BigDecimal(RND.nextInt(10_000));
            orderItem.setItem(generateOrderItemViewModel());
            orderItem.getItem().setBuyerPrice(price);
            items.add(orderItem);

            totalBefore = totalBefore.add(price.multiply(BigDecimal.valueOf(itemsCountBefore)));
            totalAfter = totalAfter.add(price.multiply(BigDecimal.valueOf(itemsCountAfter)));
        }
        model.setItemsChanged(items);

        OrderPricesChangesViewModel prices = new OrderPricesChangesViewModel();
        prices.setBeforeBuyerItemsTotal(totalBefore);
        prices.setAfterBuyerItemsTotal(totalAfter);
        prices.setDeltaBuyerItemsTotal(totalBefore.subtract(totalAfter));
        model.setChangedTotal(prices);

        return model;
    }

    private static OrderItemViewModel generateOrderItemViewModel() {
        OrderItemViewModel item = new OrderItemViewModel();

        item.setPictures(Collections.singletonList(new OfferPicture(UUID.randomUUID().toString())));
        item.setOfferName(UUID.randomUUID().toString());

        return item;
    }

    // Service mocks

    public ModelTransitionsParser getModelTransitionsParserMock() throws Exception {
        ModelTransitionsParser result = spy(new ModelTransitionsParser());
        initModelTransitionsParserMock(result);
        MOCKS.put(result, (o) -> this.initModelTransitionsParserMock((ModelTransitionsParser) o));
        return result;
    }

    public synchronized void initModelTransitionsParserMock(ModelTransitionsParser result) {
        doAnswer(invocation -> {
            Path path = Files.createTempFile("mmmt-file-", "-model-transitions.json");
            String classpathResource = (String) invocation.getArguments()[0];
            try (FileOutputStream out = new FileOutputStream(path.toString());
                 InputStream in = MarketMailerMockFactory.class.getClassLoader().getResourceAsStream(classpathResource)) {
                IOUtils.copy(in, out);
            } catch (Exception e) {
                throw new RuntimeException("Fuck at path: " + path + " with resource: " + classpathResource, e);
            }
            new ModelTransitionsParser().parse(path.toString(), (Consumer) invocation.getArguments()[1]);
            return null;
        }).when(result).parse(anyString(), any(Consumer.class));
    }

    public HistoryService getHistoryServiceMock() {
        HistoryService result = mock(HistoryService.class);
        initHistoryServiceMock(result);
        MOCKS.put(result, (o) -> this.initHistoryServiceMock((HistoryService) o));
        return result;
    }

    public synchronized void initHistoryServiceMock(HistoryService result) {
        when(result.getHistory(any(Date.class), any(Date.class), anySetOf(HistoryElement.Type.class)))
            .thenAnswer(invocation ->
                generateHistoryElements(RND.nextInt(1000)));
    }

    public Clock getClockMock() {
        return Clock.fixed(DELIVERY_FROM_INSTANT, ZoneId.systemDefault());
    }

    public Carter getCarterMock() {
        Carter result = mock(Carter.class);
        initCarterMock(result);
        MOCKS.put(result, (o) -> this.initCarterMock((Carter) o));
        return result;
    }

    public synchronized void initCarterMock(Carter result) {
        when(result.getCart(any(CartRequest.class))).thenReturn(generateCart());
    }

    public CheckouterClient getCheckouterClientMock() throws IOException {
        return mock(CheckouterClient.class);
    }

    public CheckouterService getCheckouterServiceMock() throws IOException {
        CheckouterService result = mock(CheckouterService.class);
        initCheckouterServiceMock(result);
        MOCKS.put(result, (o) -> {
            try {
                this.initCheckouterServiceMock((CheckouterService) o);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return result;
    }

    public CheckouterReturnClient getCheckouterReturnClientMock() {
        return mock(CheckouterReturnClient.class);
    }

    public CheckouterOrderHistoryEventsClient getCheckouterOrderHistoryEventsClientMock() {
        return mock(CheckouterOrderHistoryEventsClient.class);
    }

    public synchronized void initCheckouterServiceMock(CheckouterService result) throws IOException {
        when(result.getOrder(anyLong(), anyObject(), anyLong(), anyBoolean())).thenReturn(generateOrder());
        when(result.getOrderReceiptPdf(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(result.getOrderReceipts(anyLong(), any(ClientRole.class), anyLong(), anyLong())).thenReturn(new Receipts(
                Collections.singletonList(generateReceipt())
        ));
        when(result.getWarrantyPdf(anyLong(), any(), any()))
                .thenAnswer(invocation -> new ByteArrayInputStream(WARRANTY_PDF_BYTES));
    }

    public ReportService getReportServiceMock() {
        ReportService result = mock(ReportService.class);
        initReportServiceMock(result);
        MOCKS.put(result, (o) -> this.initReportServiceMock((ReportService) o));
        return result;
    }

    public synchronized void initReportServiceMock(ReportService result) {
        when(result.isModelExist(anyInt())).thenReturn(true);
        when(result.searchOfferModel(anyLong())).thenReturn(generateOfferModel());
    }

    public ru.yandex.market.report.ReportService getReportServiceHuskMock() {
        ru.yandex.market.report.ReportService result = mock(ru.yandex.market.report.ReportService.class);
        initReportServiceHuskMock(result);
        MOCKS.put(result, (o) -> this.initReportServiceHuskMock((ru.yandex.market.report.ReportService) o));
        return result;
    }

    public synchronized void initReportServiceHuskMock(ru.yandex.market.report.ReportService result) {
        when(result.getModelById(anyLong())).thenReturn(Optional.of(generateModel()));
        when(result.getModelById(anyLong(), anyLong(), any())).thenReturn(Optional.of(generateModel()));
        when(result.getModelByIdWithPreorders(anyLong(), anyLong(), any())).thenReturn(Optional.of(generateModel()));
        when(result.getModelsByIds(any())).thenReturn(generateModels(RND.nextInt(100) + 1).stream()
            .collect(Collectors.toMap(Model::getId, m -> m)));
        when(result.getModelsByIds(any(), anyLong(), any())).thenReturn(generateModels(RND.nextInt(100) + 1).stream()
            .collect(Collectors.toMap(Model::getId, m -> m)));
        when(result.getProductAnalogs(anyLong(), anyLong(), anyLong(), anyLong(), any()))
            .thenReturn(generateModels(RND.nextInt(100) + 1));
        when(result.getModelsForCategoryIdOrderedByPrice(anyLong())).thenReturn(generateModels(RND.nextInt(100) + 1));
        when(result.getOffer(anyString(), anyLong())).thenAnswer((invocation) -> {
            Offer foundOffer = generateOffer();
            foundOffer.setOnStock(true);
            foundOffer.setDeliveryAvailable(true);
            foundOffer.setCpa("real");
            return Optional.of(foundOffer);
        });
        when(result.getOffersByModel(anyInt(), anyString(), anyLong(), anyInt(), anyInt()))
            .thenReturn(generateOffers(RND.nextInt(10) + 3));
        when(result.getOffersByModelWithPreorders(anyInt(), anyString(), anyLong(), anyInt(), anyInt()))
            .thenReturn(generateOffers(RND.nextInt(10) + 3));
        when(result.getOffersByRequest(any())).thenReturn(generateOffers(10));
        when(result.getModelsByRequest(any())).thenReturn(generateModels(10));
    }

    public static List<Model> generateModels(int cnt) {
        return Stream.generate(MarketMailerMockFactory::generateModel).limit(cnt).collect(Collectors.toList());
    }

    public static Model generateModel() {
        Model result = new Model();
        result.setId(RND.nextLong());
        result.setNew(true);
        result.setMaxDiscountPercent(new BigDecimal(25));
        result.setOffersCount(10);
        result.setOffersWithDiscountCount(5);
        result.setPictureUrl(UUID.randomUUID().toString());
        result.setCategory(new Category(RND.nextLong(), UUID.randomUUID().toString()));
        result.setName(UUID.randomUUID().toString());
        result.setPrices(new Prices(RND.nextDouble(MAX_MODEL_PRICE), RND.nextDouble(MAX_MODEL_PRICE),
            RND.nextDouble(MAX_MODEL_PRICE), new BigDecimal(25), Currency.BYN));
        result.setType(ProductType.values()[RND.nextInt(ProductType.values().length)]);
        result.setRating(new BigDecimal(RND.nextInt(3)));
        result.setVendor(new Vendor(RND.nextLong(), UUID.randomUUID().toString()));
        return result;
    }

    public MbiApiClient getMbiApiClientMock() {
        MbiApiClient result = mock(MbiApiClient.class);
        initMbiApiClientMock(result);
        MOCKS.put(result, (o) -> this.initMbiApiClientMock((MbiApiClient) o));
        return result;
    }

    private synchronized void initMbiApiClientMock(MbiApiClient result) {
        when(result.getPartnerInfo(anyLong())).thenReturn(
                new PartnerInfoDTO(0, 0L, CampaignType.SHOP, "shopname", "shop.market.yandex.ru",
                        "+74951111111", null, null, false, null),
                null);
    }

    public SenderClient getSenderClientMock() throws IOException {
        SenderClient result = mock(SenderClient.class);
        initSenderClientMock(result);
        MOCKS.put(result, (o) -> initSenderClientMock((SenderClient) o));
        return result;
    }

    public SolomonService getSolomonServiceMock() {
        SolomonService result = mock(SolomonService.class);
        initSolomonServiceMock(result);
        MOCKS.put(result, (o) -> initSolomonServiceMock((SolomonService) o));
        return result;
    }

    private void initSolomonServiceMock(SolomonService result) {
    }

    public static synchronized void initSenderClientMock(SenderClient result) {
        initSenderClientMock(result, SenderClient.SendTransactionalResponse.success(null));
    }

    @SuppressFBWarnings("DE_MIGHT_IGNORE")
    public static void initSenderClientMock(SenderClient result, SenderClient.SendTransactionalResponse response) {
        try {
            when(result.sendTransactionalMail(anyString(), anyObject(),
                anyBoolean(), anyMapOf(String.class, Object.class), anyListOf(MailAttachment.class)))
                .thenReturn(response);
        } catch (Exception ignored) {
        }
    }

    public CrmUserSubscriptionItemWriter getUserSubscriptionItemTskvWriter() {
        return new TestCrmUserSubscriptionItemWriter();
    }


    public ThumbnailService getThumbnailService() {
        ThumbnailService service = mock(ThumbnailService.class);
        initThumbnailService(service);
        return service;
    }

    public synchronized void initThumbnailService(ThumbnailService service) {
        try {
            when(service.checkUrlExist(anyString(), anyString())).thenReturn(true);
        } catch (Exception ignored) {
        }
    }

    public MdsExportService getMdsExportService() {
        MdsExportService result = mock(MdsExportService.class);
        initMdsExportService(result);
        MOCKS.put(result, (o) -> this.initMdsExportService((MdsExportService) o));
        return result;
    }

    public synchronized void initMdsExportService(MdsExportService service) {
    }

    public HttpCypress getYtClient() {
        HttpCypress result = mock(HttpCypress.class);
        initYtClient(result);
        MOCKS.put(result, (o) -> this.initYtClient((HttpCypress) o));
        return result;
    }

    public synchronized void initYtClient(HttpCypress service) {
    }

    public JdbcTemplate getYtJdbcTemplate() {
        JdbcTemplate result = mock(JdbcTemplate.class);
        initYtJdbcTemplate(result);
        MOCKS.put(result, (o) -> this.initYtJdbcTemplate((JdbcTemplate) o));
        return result;
    }

    public synchronized void initYtJdbcTemplate(JdbcTemplate jdbcTemplate) {
    }

    public YqlDataSource getYtDataSource() {
        YqlDataSource result = mock(YqlDataSource.class);
        initYtDataSource(result);
        MOCKS.put(result, (o) -> this.initYtDataSource((YqlDataSource) o));
        return result;
    }

    public synchronized void initYtDataSource(YqlDataSource service) {
    }

    public GradeClient getGradeClient() {
        GradeClient result = mock(GradeClient.class);
        initGradeClient(result);
        MOCKS.put(result, (o) -> this.initGradeClient((GradeClient) o));
        return result;
    }

    private void initGradeClient(GradeClient mock) {
        when(mock.getShopGradeForMailer(anyLong(), anyBoolean())).then(x -> fillRandomData(MailerShopGrade.simplePublic(
            RND.nextInt(10_000),
            RND.nextInt(10_000),
            RND.nextInt(10_000)
        )));

        when(mock.getModelGradeForMailer(anyInt())).then( x-> fillRandomData(MailerModelGrade.simplePublic(
            RND.nextInt(10_000),
            RND.nextInt(10_000),
            RND.nextInt(10_000)
        )));
    }

    private <T extends MailerAbstractGrade> T fillRandomData(T grade) {
        grade.setText(UUID.randomUUID().toString());
        grade.setPro(UUID.randomUUID().toString());
        grade.setContra(UUID.randomUUID().toString());
        grade.setCrTimeMs(new Date().getTime());
        grade.setGradeValue(1);
        return grade;
    }

    public static MailerShopGrade randomizeShopGrade(MailerShopGrade result) {
        result.setShopName("shop name " + UUID.randomUUID().toString());
        result.setText("text " + UUID.randomUUID().toString());
        result.setPro("pro " + UUID.randomUUID().toString());
        result.setContra("contra " + UUID.randomUUID().toString());
        result.setCrTimeMs(new Date().getTime());
        result.setGradeValue(1);
        result.setOrderId(String.valueOf(RND.nextLong()));
        return result;
    }

    public static MailerShopGrade fillShopGradeTexts(MailerShopGrade result, String shopName, String text, String pro, String contra, String orderId) {
        result.setShopName(shopName);
        result.setText(text);
        result.setPro(pro);
        result.setContra(contra);
        result.setCrTimeMs(new Date().getTime());
        result.setGradeValue(1);
        result.setOrderId(orderId);
        return result;
    }

    public PersAuthorClient getAuthorClient() {
        PersAuthorClient result = mock(PersAuthorClient.class);
        initAuthorClient(result);
        MOCKS.put(result, (o) -> this.initAuthorClient((PersAuthorClient) o));
        return result;
    }

    private void initAuthorClient(PersAuthorClient result) {
    }

    public HttpClient getTarantinoHttpClient() {
        HttpClient result = mock(HttpClient.class);
        initTarantinoHttpClient(result);
        MOCKS.put(result, (o) -> this.initTarantinoHttpClient((HttpClient) o));
        return result;
    }

    private void initTarantinoHttpClient(HttpClient result) {
        try {
            HttpResponseMockFactory.mockResponse(result, getTestData("live/tarantino_blogger_response.json"), 200);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream getTestData(String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }

    public static final String SECRET_KEY = "SECRET_KEY,/?<>\\|'\";:[{]}=+&^%$#@±§`";

    public synchronized void initAboService(AboService aboService) {
        when(aboService.generateSkFeedback(anyLong())).thenReturn(SECRET_KEY);
    }

    public AboService getAboService() {
        AboService aboService = mock(AboService.class);
        initAboService(aboService);
        MOCKS.put(aboService, (o) -> initAboService((AboService) o));
        return aboService;
    }

    public GeoExportService getGeoExportService() {
        GeoExportService geoExportService = mock(GeoExportService.class);
        initGeoExportService(geoExportService);
        MOCKS.put(geoExportService, (o) -> initGeoExportService((GeoExportService) o));
        return geoExportService;
    }

    public synchronized void initGeoExportService(GeoExportService geoExportService) {
        when(geoExportService.getPrepositionalRegionName(anyLong())).thenReturn(
            new GeoExportService.Region("в", "Москве")
        );
    }

    public CheckoutReferee getCheckoutReferee() {
        CheckoutReferee checkoutReferee = mock(CheckoutReferee.class);
        initCheckoutReferee(checkoutReferee);
        MOCKS.put(checkoutReferee, (o) -> initCheckoutReferee((CheckoutReferee) o));
        return checkoutReferee;
    }

    public synchronized void initCheckoutReferee(CheckoutReferee checkoutReferee) {
        try {
            when(checkoutReferee.downloadAttachment(anyLong(), anyLong(), anyLong(), anyLong(), any(), anyLong()))
                .thenAnswer(invocation -> new ByteArrayInputStream(new byte[]{'a', 'b', 'c'}));
        } catch (IOException unexpected) {
        }
    }
}
