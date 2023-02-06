package ru.yandex.market.checkout.helpers;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.checkouter.ReturnableCategoryRule;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.BankDetails;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnItemType;
import ru.yandex.market.checkout.checkouter.storage.CheckouterSequence;
import ru.yandex.market.checkout.checkouter.storage.returns.ReturnDeliveryDao;
import ru.yandex.market.checkout.checkouter.storage.returns.ReturnEntityMapper;
import ru.yandex.market.checkout.checkouter.storage.returns.ReturnItemDao;
import ru.yandex.market.checkout.checkouter.storage.returns.ReturnsDao;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.storage.Storage;
import ru.yandex.market.checkout.storage.StorageSequence;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.util.report.ReportConfigurer;
import ru.yandex.market.checkouter.jooq.tables.records.ReturnRecord;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkouter.jooq.Tables.RETURN;

/**
 * @author sergeykoles
 * Created on: 22.02.18
 */
@WebTestHelper
public class ReturnHelper {

    public static final Long DEFAULT_DELIVERY_SERVICE_ID = 346736L;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private ReturnService returnService;
    @Autowired
    private ReturnsDao returnsDao;
    @Autowired
    private ReturnItemDao returnItemDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private WireMockServer shopInfoMock;
    @Autowired
    private ReportConfigurer reportConfigurer;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private ReturnDeliveryDao returnDeliveryDao;
    @Autowired
    private Clock clock;
    @Autowired
    private Storage storage;
    @Autowired
    private DSLContext dsl;

    /**
     * Создаёт заказ и к нему потом создаёт возврат на все позиции
     *
     * @param orderParameters      параметры заказа
     * @param returnPostConfigurer пост-процессор дефолтного возврата, можно подхачить, как надо.
     * @return кортеж: созданный заказ + возврат к этому заказу
     */
    public Pair<Order, Return> createOrderAndReturn(Parameters orderParameters,
                                                    BiFunction<? super Return, ? super Order, ? extends Return>
                                                            returnPostConfigurer) {
        Order order = orderCreateHelper.createOrder(orderParameters);
        return new Pair<>(order, createReturnForOrder(order, returnPostConfigurer));
    }

    /**
     * Создаёт возврат к указанному возврату. По умолчанию - возврат всех айтемов заказа.
     *
     * @param order                заказ, для которого надо создать возврат
     * @param returnPostConfigurer постпроцессор возврата, который будет создан. Может полностью заменить на
     *                             другую сущность Return.
     * @return созданный с учётом постпроцессора заказ
     */
    public Return createReturnForOrder(Order order, BiFunction<? super Return, ? super Order, ? extends Return>
            returnPostConfigurer) {
        Return ret = ReturnProvider.generateReturn(order);
        if (returnPostConfigurer != null) {
            ret = returnPostConfigurer.apply(ret, order);
        }
        Return toCreate = ret;
        return transactionTemplate.execute(txStatus -> {
            Return aReturn = createReturn(toCreate);
            long returnId = returnsDao.insertReturn(aReturn);
            toCreate.setId(returnId);
            returnItemDao.insertReturnItems(toCreate);
            if (toCreate.getDelivery() != null) {
                toCreate.getDelivery().setReturnId(returnId);
                long returnDeliveryId = returnDeliveryDao.insertReturnDelivery(toCreate.getDelivery(),
                        aReturn.getOrderId());
                returnsDao.setReturnDeliveryId(toCreate.getId(), returnDeliveryId);
            }
            return returnsDao.findReturnById(returnId).orElseThrow();
        });
    }

    public Return insertReturn(Return srcReturn) {
        return transactionTemplate.execute(txStatus -> {
            Return insertedReturn = copy(srcReturn);
            long returnId = getNextReturnId();
            insertedReturn.setId(returnId);
            insertReturnToBd(insertedReturn);
            returnItemDao.insertReturnItems(insertedReturn);
            if (insertedReturn.getDelivery() != null) {
                ReturnDelivery returnDelivery = insertedReturn.getDelivery();
                returnDelivery.setReturnId(insertedReturn.getId());
                Long deliveryId = returnDeliveryDao.insertReturnDelivery(returnDelivery, srcReturn.getOrderId());
                returnDelivery.setId(deliveryId);
                dsl.update(RETURN)
                        .set(RETURN.RETURN_DELIVERY_ID, deliveryId)
                        .execute();
            }
            return returnsDao.findReturnById(returnId)
                    .orElseThrow(() -> new IllegalStateException("Cannot find inserted return"));
        });
    }

    private Long getNextReturnId() {
        StorageSequence returnSequence = storage.getSequence(CheckouterSequence.RETURN_SEQ);
        return returnSequence.nextValue();
    }

    private void insertReturnToBd(Return returnToInsert) {
        ReturnRecord returnEntity = ReturnEntityMapper.convertRequestToEntity(returnToInsert, clock);
        returnEntity.setApplicationUrl(returnToInsert.getApplicationUrl());
        dsl.insertInto(RETURN)
                .set(returnEntity)
                .execute();
    }

    private Return createReturn(Return ret) {
        Return re = new Return();
        re.setOrderId(ret.getOrderId());
        re.setUserCompensationSum(ret.getUserCompensationSum());
        re.setStatus(ret.getStatus());
        re.setComment(ret.getComment());
        re.setDelivery(ret.getDelivery());
        // синтетика
        Instant now = Instant.now();
        re.setCreatedAt(now);
        re.setStatusUpdatedAt(now);
        re.setCompensationClientId(123L);
        re.setCompensationPersonId(321L);
        re.setUserId(ret.getUserId());
        return re;
    }

    public static BankDetails createDummyBankDetails() {
        return new BankDetails("00000000000000000009", "0123456789", "000000009", "bank", "bankCity", "firstName",
                "lastName", "middleName", "0123456789abcdef0123456789abcde_", null);
    }

    public Return createReturn(Long orderId, Return ret) {
        ret = returnService.initReturn(
                orderId, new ClientInfo(ClientRole.REFEREE, 123L), ret, Experiments.empty());
        return returnService.resumeReturn(ret.getOrderId(), new ClientInfo(ClientRole.REFEREE, 123L), ret.getId(),
                ret, true);
    }

    public Return addReturnDelivery(Order order, Return ret, @Nullable ReturnDelivery returnDelivery) {
        ReturnDelivery delivery = Optional.ofNullable(returnDelivery).orElse(getDefaultReturnDelivery());
        returnService.addReturnDelivery(order.getId(), ret.getId(), delivery, ClientInfo.SYSTEM, Experiments.empty());
        return returnService.findReturnById(ret.getId(), false, ClientInfo.SYSTEM);
    }

    public ReturnDelivery getDefaultReturnDelivery() {
        ReturnDelivery delivery = new ReturnDelivery();
        delivery.setDeliveryServiceId(100501L);
        delivery.setType(DeliveryType.PICKUP);
        delivery.setStatus(ReturnDeliveryStatus.CREATED);
        return delivery;
    }

    public Return initReturn(Long orderId, Return ret) {
        return returnService.initReturn(orderId, ClientInfo.SYSTEM, ret, Experiments.empty());
    }

    public Return resumeReturn(long orderId, long returnId, Return retChanges) {
        return resumeReturn(orderId, returnId, retChanges, true);
    }

    public Return resumeReturn(long orderId, long returnId, Return retChanges, boolean skipValidation) {
        if (retChanges == null) {
            retChanges = new Return();
        }
        return returnService.resumeReturn(orderId,
                new ClientInfo(ClientRole.REFEREE, 123L),
                returnId,
                retChanges,
                skipValidation);
    }

    public static Return addDeliveryItemToRequest(Return returnRequest) {
        ReturnItem deliveryItem = new ReturnItem();
        deliveryItem.setDeliveryService(true);
        deliveryItem.setItemId(null);
        deliveryItem.setCount(1);
        deliveryItem.setQuantity(BigDecimal.ONE);
        deliveryItem.setDeliveryServiceId(DEFAULT_DELIVERY_SERVICE_ID);
        returnRequest.getItems().add(deliveryItem);
        return returnRequest;
    }

    public void getReturnById(Long orderId, Long returnId, ResultActionsContainer resultMatchers)
            throws Exception {
        ResultActions resultActions = mockMvc.perform(
                get("/orders/{orderId}/returns/{returnId}", orderId, returnId)
                        .param("clientRole", "SYSTEM")
                        .param("uid", "0")
        );
        resultMatchers.propagateResultActions(resultActions);
    }

    public void mockShopInfo() {
        shopInfoMock.stubFor(WireMock.get(urlPathMatching("/supplierNames.*"))
                .willReturn(aResponse().withBody("[\n" +
                        "    {\n" +
                        "        \"id\": 10264970,\n" +
                        "        \"name\": \"Мир автотестов чекаута\"\n" +
                        "    }\n" +
                        "]")));
        shopInfoMock.stubFor(WireMock.get(urlPathMatching("/shopNames.*"))
                .willReturn(aResponse().withBody("[\n" +
                        "    {\n" +
                        "        \"id\": 10264970,\n" +
                        "        \"name\": \"Мир автотестов чекаута (ShopName)\"\n" +
                        "    }\n" +
                        "]")));
    }

    public void mockSupplierInfo() {
        shopInfoMock.stubFor(WireMock.get(urlPathMatching("/supplierInfo.*"))
                .willReturn(okJson("[\n" +
                        "    {\n" +
                        "        \"id\": 10264970,\n" +
                        "        \"name\": \"Мир автотестов чекаута\"\n" +
                        "    }\n" +
                        "]")));
    }

    public void processReturnPayments(Order order, Return ret) {
        returnService.processReturnPayments(order, ret);
        refundHelper.proceedAsyncRefunds(ret);
    }

    public void setNonReturnableItemsViaHttp(Collection<ReturnableCategoryRule> categories) throws Exception {
        mockMvc.perform(post("/returns/non-returnable-categories")
                        .content(new ObjectMapper().writeValueAsString(categories))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Deprecated
    public void mockActualDelivery() {
        Parameters parameters = new Parameters();
        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.defaultActualDelivery());
        reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, parameters.getReportParameters());
    }

    /**
     * Мок ответа репорта для опций доставки возврата. Генерирует по одной опции для DELIVERY/POST/PICKUP
     * Подразумевается что возврат будет создан для всех товаров входящих в заказ и с дефолтным id служб
     *
     * @param order - Заказ из которого возьмутся все товары и для них сгенерированы опции
     */
    public void mockActualDelivery(Order order) {
        List<Long> itemIds = order.getItems().stream()
                .map(OrderItem::getId)
                .collect(Collectors.toList());
        mockActualDelivery(itemIds, order, DEFAULT_DELIVERY_SERVICE_ID);
    }

    /**
     * Мок ответа репорта для опций доставки возврата. Генерирует по одной опции для DELIVERY/POST/PICKUP
     * Подразумевается что возврат будет создан для всех товаров входящих в заказ.
     *
     * @param order             - Заказ из которого возьмутся все товары и для них сгенерированы опции
     * @param deliveryServiceId - id службы доставки
     */
    public void mockActualDelivery(Order order, Long deliveryServiceId) {
        List<Long> itemIds = order.getItems().stream()
                .map(OrderItem::getId)
                .collect(Collectors.toList());
        mockActualDelivery(itemIds, order, deliveryServiceId);
    }

    /**
     * Мок ответа репорта для опций доставки возврата. Генерирует по одной опции для DELIVERY/POST/PICKUP
     * С id службы доставки по умолчанию
     *
     * @param order   - Заказ из которого возьмутся данные по товарам
     * @param aReturn - Возврат из которого берется информация по составу возвращаемых товаров в нем
     */
    public void mockActualDelivery(Return aReturn, Order order) {
        mockActualDelivery(aReturn, order, DEFAULT_DELIVERY_SERVICE_ID);
    }

    /**
     * Мок ответа репорта для опций доставки возврата. Генерирует по одной опции для DELIVERY/POST/PICKUP
     *
     * @param order             - Заказ из которого возьмутся данные по товарам
     * @param aReturn           - Возврат из которого берется информация по составу возвращаемых товаров в нем
     * @param deliveryServiceId - id службы доставки
     */
    public void mockActualDelivery(Return aReturn, Order order, Long deliveryServiceId) {
        List<Long> returnItemIds = aReturn.getItems().stream()
                .map(ReturnItem::getItemId)
                .collect(Collectors.toList());
        mockActualDelivery(returnItemIds, order, deliveryServiceId);
    }

    private void mockActualDelivery(List<Long> returnItemIds, Order order, Long deliveryServiceId) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(
                order.getItems().stream()
                        .filter(orderItem -> returnItemIds.contains(orderItem.getId()))
                        .collect(Collectors.toList())
        );
        DeliveryTimeInterval interval = new DeliveryTimeInterval(LocalTime.MIN, LocalTime.MAX);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(deliveryServiceId, 1, 1, interval)
                        .addPickup(deliveryServiceId)
                        .addPostTerm(deliveryServiceId)
                        .build());
        parameters.getReportParameters().setExtraActualDeliveryParams(Map.of("is-return", "1"));
        reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, parameters.getReportParameters());
    }

    /**
     * Скопирует возврат, но в комментарий возврата к каждой товарной позиции установит случайный комментарий
     * Это нужно для придания каждому вызову создания возврата уникальности - за счет разных комментариев, ключ
     * идемпотентности в запросах будет отличатся
     */
    public static Return copyWithRandomizeItemComments(@NotNull Return src) {
        Return newReturn = copy(src);
        newReturn.getItems().stream()
                .filter(item -> item.getType() == ReturnItemType.ORDER_ITEM)
                .forEach(item -> item.setReturnReason(UUID.randomUUID().toString()));
        return newReturn;
    }

    public static Return copy(@NotNull Return src) {
        Return newReturn = new Return();
        newReturn.setId(src.getId());
        newReturn.setOrderId(src.getOrderId());
        newReturn.setItems(copyItems(src.getItems()));
        newReturn.setUserCompensationSum(src.getUserCompensationSum());
        newReturn.setUserCreditCompensationSum(src.getUserCreditCompensationSum());
        newReturn.setBankDetails(src.getBankDetails());
        newReturn.setComment(src.getComment());
        newReturn.setStatus(src.getStatus());
        newReturn.setCreatedAt(src.getCreatedAt());
        newReturn.setUpdatedAt(src.getUpdatedAt());
        newReturn.setStatusUpdatedAt(src.getStatusUpdatedAt());
        newReturn.setDelivery(copyReturnDelivery(src.getDelivery()));
        newReturn.setPayOffline(src.getPayOffline());
        newReturn.setApplicationUrl(src.getApplicationUrl());
        newReturn.setCompensationClientId(src.getCompensationClientId());
        newReturn.setCompensationPersonId(src.getCompensationPersonId());
        newReturn.setCompensationContractId(src.getCompensationContractId());
        newReturn.setFullName(src.getFullName());
        newReturn.setProcessingDetails(src.getProcessingDetails());
        newReturn.setCertificateOfInterestPaidUrl(src.getCertificateOfInterestPaidUrl());
        newReturn.setUserEmail(src.getUserEmail());
        newReturn.setUserPhone(src.getUserPhone());
        newReturn.setPersonalPhoneId(src.getPersonalPhoneId());
        newReturn.setLargeSize(src.getLargeSize());
        newReturn.setFastReturn(src.getFastReturn());
        newReturn.setDeliveryCompensationType(src.getDeliveryCompensationType());
        newReturn.setUserId(src.getUserId());
        newReturn.setOrderCreatedAt(src.getOrderCreatedAt());
        return newReturn;
    }

    private static ReturnDelivery copyReturnDelivery(ReturnDelivery src) {
        if (src == null) {
            return null;
        }
        ReturnDelivery newDelivery = new ReturnDelivery();
        newDelivery.setId(src.getId());
        newDelivery.setReturnId(src.getReturnId());
        newDelivery.setType(src.getType());
        newDelivery.setStatus(src.getStatus());
        newDelivery.setStatusUpdatedAt(src.getStatusUpdatedAt());
        newDelivery.setDeliveryServiceId(src.getDeliveryServiceId());
        newDelivery.setOutletId(src.getOutletId());
        newDelivery.setOutletIds(src.getOutletIds() == null ? null : new ArrayList<>(src.getOutletIds()));
        newDelivery.setTrack(src.getTrack()); // --сделай копию если понадобится
        newDelivery.setReceiptPhotoUrl(src.getReceiptPhotoUrl());
        newDelivery.setOutlet(src.getOutlet());  // --сделай копию если понадобится
        newDelivery.setMarketPartner(src.getMarketPartner());
        newDelivery.setPrice(src.getPrice()); // --сделай копию если понадобится
        newDelivery.setPostTrackNeeded(src.getPostTrackNeeded());
        newDelivery.setRegionId(src.getRegionId());
        newDelivery.setOwTicketId(src.getOwTicketId());
        return newDelivery;
    }

    private static List<ReturnItem> copyItems(List<ReturnItem> items) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .map(ReturnHelper::copyItem)
                .collect(Collectors.toList());
    }

    private static ReturnItem copyItem(@NotNull ReturnItem src) {
        ReturnItem newReturnItem = new ReturnItem();
        newReturnItem.setId(src.getId());
        newReturnItem.setReturnId(src.getReturnId());
        newReturnItem.setItemId(src.getItemId());
        newReturnItem.setItemTitle(src.getItemTitle());
        newReturnItem.setDeliveryServiceId(src.getDeliveryServiceId());
        newReturnItem.setCount(src.getCount());
        newReturnItem.setQuantity(src.getQuantity());
        newReturnItem.setSupplierCompensation(src.getSupplierCompensation());
        newReturnItem.setReturnReason(src.getReturnReason());
        newReturnItem.setDefective(src.isDefective());
        newReturnItem.setReasonType(src.getReasonType());
        newReturnItem.setSubreasonType(src.getSubreasonType());
        newReturnItem.setPicturesUrls(copyPicturesUrls(src.getPicturesUrls()));
        newReturnItem.setReturnAddressDisplayed(src.isReturnAddressDisplayed());
        newReturnItem.setDecisionType(src.getDecisionType());
        newReturnItem.setDecisionComment(src.getDecisionComment());
        return newReturnItem;
    }

    private static List<URL> copyPicturesUrls(List<URL> picturesUrls) {
        if (picturesUrls == null) {
            return null;
        }
        return new ArrayList<>(picturesUrls);
    }
}
