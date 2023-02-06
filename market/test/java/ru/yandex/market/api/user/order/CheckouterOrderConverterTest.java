package ru.yandex.market.api.user.order;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.api.common.RoomAddress;
import ru.yandex.market.api.common.client.rules.BlueMobileApplicationRule;
import ru.yandex.market.api.common.client.rules.BlueMobilePromoCodeHack;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.OfferIdEncodingService;
import ru.yandex.market.api.domain.v2.AddressV2;
import ru.yandex.market.api.domain.v2.PerkType;
import ru.yandex.market.api.domain.v2.BreakIntervalV2;
import ru.yandex.market.api.domain.v2.BundleSettings;
import ru.yandex.market.api.domain.v2.GeoCoordinatesV2;
import ru.yandex.market.api.domain.v2.GeoPointV2;
import ru.yandex.market.api.domain.v2.Image;
import ru.yandex.market.api.domain.v2.OfferPriceV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.OpenHoursV2;
import ru.yandex.market.api.domain.v2.OutletV2;
import ru.yandex.market.api.domain.v2.PerkStatus;
import ru.yandex.market.api.domain.v2.RegionV2;
import ru.yandex.market.api.domain.v2.ThumbnailSize;
import ru.yandex.market.api.domain.v2.specifications.InternalSpecification;
import ru.yandex.market.api.geo.domain.RegionType;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.internal.common.features.Feature;
import ru.yandex.market.api.internal.report.ReportClient;
import ru.yandex.market.api.matchers.ApiOrderPromoMatcher;
import ru.yandex.market.api.matchers.BreakIntervalMatcher;
import ru.yandex.market.api.matchers.BreakIntervalV2Matcher;
import ru.yandex.market.api.matchers.DeliveryIntervalMatcher;
import ru.yandex.market.api.matchers.ImageMatcher;
import ru.yandex.market.api.matchers.ItemPromoMatcher;
import ru.yandex.market.api.matchers.OpenHoursMatcher;
import ru.yandex.market.api.matchers.PaymentMatcher;
import ru.yandex.market.api.matchers.PaymentPartitionMatcher;
import ru.yandex.market.api.matchers.ReceiptMatcher;
import ru.yandex.market.api.matchers.RegionV2Matcher;
import ru.yandex.market.api.matchers.TrackingMatcher;
import ru.yandex.market.api.matchers.WeekScheduleMatcher;
import ru.yandex.market.api.offer.OfferService;
import ru.yandex.market.api.offer.Phone;
import ru.yandex.market.api.outlet.Geo;
import ru.yandex.market.api.personal.Email;
import ru.yandex.market.api.personal.FullName;
import ru.yandex.market.api.personal.PersonalClient;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.UserDevice;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.user.order.builders.BuyerBuilder;
import ru.yandex.market.api.user.order.builders.CheckoutRequestBuilder;
import ru.yandex.market.api.user.order.builders.CheckoutRequestOrderItemBuilder;
import ru.yandex.market.api.user.order.builders.CheckoutRequestShopOrderBuilder;
import ru.yandex.market.api.user.order.builders.DeliveryBuilder;
import ru.yandex.market.api.user.order.builders.MultiCartBuilder;
import ru.yandex.market.api.user.order.builders.MultiCartOrderBuilder;
import ru.yandex.market.api.user.order.builders.MultiCartOrderDeliveryBuilder;
import ru.yandex.market.api.user.order.builders.MultiCartOrderItemBuilder;
import ru.yandex.market.api.user.order.builders.MultiOrderBuilder;
import ru.yandex.market.api.user.order.builders.OrderBuilder;
import ru.yandex.market.api.user.order.builders.OrderItemBuilder;
import ru.yandex.market.api.user.order.builders.OrderOptionsRequestBuilder;
import ru.yandex.market.api.user.order.builders.OrderOptionsRequestOrderItemBuilder;
import ru.yandex.market.api.user.order.builders.OrderOptionsRequestShopOrderBuilder;
import ru.yandex.market.api.user.order.builders.OrderPromoBuilder;
import ru.yandex.market.api.user.order.builders.ParcelBuilder;
import ru.yandex.market.api.user.order.builders.ShopOutletBuilder;
import ru.yandex.market.api.user.order.card.SelectedCartInfoConverter;
import ru.yandex.market.api.user.order.cashback.CashbackPaymentStatus;
import ru.yandex.market.api.user.order.cashback.WelcomeCashback;
import ru.yandex.market.api.user.order.checkout.AddressDeliveryPoint;
import ru.yandex.market.api.user.order.checkout.CancelPolicy;
import ru.yandex.market.api.user.order.checkout.CheckoutRequest;
import ru.yandex.market.api.user.order.checkout.CheckoutResponse;
import ru.yandex.market.api.user.order.checkout.Checkpoint;
import ru.yandex.market.api.user.order.checkout.DeliveryPointId;
import ru.yandex.market.api.user.order.checkout.DeliveryPointIdEncodingService;
import ru.yandex.market.api.user.order.checkout.Location;
import ru.yandex.market.api.user.order.checkout.OutletDeliveryPoint;
import ru.yandex.market.api.user.order.checkout.PersistentOrder;
import ru.yandex.market.api.user.order.checkout.WeekSchedule;
import ru.yandex.market.api.user.order.credit.CreditInformation;
import ru.yandex.market.api.user.order.helper.CapiCreditInformationGeneratorHelper;
import ru.yandex.market.api.user.order.installments.InstallmentsInformation;
import ru.yandex.market.api.user.order.installments.InstallmentsOption;
import ru.yandex.market.api.user.order.installments.MonthlyPayment;
import ru.yandex.market.api.user.order.multicart.ApiCostLimitInformation;
import ru.yandex.market.api.user.order.preorder.OrderOptionsRequest;
import ru.yandex.market.api.user.order.preorder.OrderOptionsResponse;
import ru.yandex.market.api.user.order.preorder.WorkScheduleFormat;
import ru.yandex.market.api.user.order.promo.ApiOrderPromo;
import ru.yandex.market.api.user.order.promo.ItemPromo;
import ru.yandex.market.api.user.order.station.LegalInfo;
import ru.yandex.market.api.user.order.station.PlusSubscriptionInformation;
import ru.yandex.market.api.user.order.station.WebLegalInfoDataItem;
import ru.yandex.market.api.util.concurrent.Pipelines;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.ConsolidatedCarts;
import ru.yandex.market.checkout.checkouter.cart.CostLimitInformation;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cart.MultiCartTotals;
import ru.yandex.market.checkout.checkouter.cart.station.Price;
import ru.yandex.market.checkout.checkouter.cart.station.StationLegalInfo;
import ru.yandex.market.checkout.checkouter.cart.station.StationMobileLegalInfo;
import ru.yandex.market.checkout.checkouter.cart.station.StationSubscriptionInfo;
import ru.yandex.market.checkout.checkouter.cart.station.StationWebLegalInfo;
import ru.yandex.market.checkout.checkouter.cart.station.StationWebLegalInfoData;
import ru.yandex.market.checkout.checkouter.cart.station.StationWebLegalInfoDataItem;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackThreshold;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressSource;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryCustomizer;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.ExtraCharge;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.delivery.outlet.NearestOutlet;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletDeliveryTimeInterval;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.AdditionalCartInfo;
import ru.yandex.market.checkout.checkouter.order.CartPresetInfo;
import ru.yandex.market.checkout.checkouter.order.CashbackEmitInfo;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderCancelPolicy;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PresetInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemsRemovalChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentAgent;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOption;
import ru.yandex.market.checkout.checkouter.pay.PaymentOptionHiddenReason;
import ru.yandex.market.checkout.checkouter.pay.PaymentPartition;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubmethod;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.common.report.model.CancelType;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.OfferPicture;
import ru.yandex.market.common.report.model.json.common.Region;
import ru.yandex.market.common.report.model.json.common.delivery.DeliveryLingua;
import ru.yandex.market.common.report.model.specs.InternalSpec;
import ru.yandex.market.common.report.model.specs.Specs;
import ru.yandex.market.common.report.model.specs.UsedParam;
import ru.yandex.market.loyalty.api.model.CashbackPermision;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.matchers.ApiOrderPromoMatcher.orderPromo;
import static ru.yandex.market.api.matchers.ItemPromoMatcher.itemPromo;
import static ru.yandex.market.api.matchers.ReceiptMatcher.creationDate;
import static ru.yandex.market.api.matchers.ReceiptMatcher.receipt;
import static ru.yandex.market.api.matchers.ReceiptMatcher.status;
import static ru.yandex.market.api.matchers.ReceiptMatcher.type;
import static ru.yandex.market.api.matchers.TrackingMatcher.checkpoint;
import static ru.yandex.market.api.matchers.TrackingMatcher.checkpointId;
import static ru.yandex.market.api.matchers.TrackingMatcher.checkpoints;
import static ru.yandex.market.api.matchers.TrackingMatcher.code;
import static ru.yandex.market.api.matchers.TrackingMatcher.country;
import static ru.yandex.market.api.matchers.TrackingMatcher.deliveryStatus;
import static ru.yandex.market.api.matchers.TrackingMatcher.location;
import static ru.yandex.market.api.matchers.TrackingMatcher.message;
import static ru.yandex.market.api.matchers.TrackingMatcher.time;
import static ru.yandex.market.api.matchers.TrackingMatcher.tracking;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.CHECKOUTER_FEE_SHOW;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_OFFER_DELIVERY_BRIEF;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_OFFER_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_OFFER_PRICE;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_OFFER_RIGHT_FEE_SHOW;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_OFFER_SHOP_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_OFFER_WRONG_FEE_SHOW;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_ORDER_LABEL;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_SECOND_USER_REGION_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_SHOP_FEED_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_SHOP_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_USER_CURRENCY;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_USER_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_USER_REGION_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.day;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.generateMultiCartWithOneOrder;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.generateOrderOptionRequestWithOneOrder;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.generateOrderOptionRequestWithOneOrderAndAddress;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.generateShopOrder;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.month;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.year;

/**
 * TODO: Эти тесты плохие, т.к. основаны на коде, а не на логике чекаута. Желательно их улучшить по возможности.
 *
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithContext
@WithMocks
public class CheckouterOrderConverterTest extends BaseTest {

    CheckouterOrderConverter converter;

    @Mock
    OfferService offerService;

    @Mock
    OfferIdEncodingService offerIdEncodingService;

    @Mock
    ReportClient reportClient;

    @Mock
    BlueMobileApplicationRule blueMobileApplicationRule;

    @Mock
    CoinConverter coinConverter;

    PromoCodeErrorConverter promoCodeErrorConverter;

    @Inject
    UrlParamsFactoryImpl urlParamsFactoryImpl;

    @Mock
    DeliveryPointIdEncodingService deliveryPointIdEncodingService;

    @Inject
    InstallmentsInformationConverter installmentsInformationConverter;

    @Inject
    CreditInformationConverter creditInformationConverter;

    @Inject
    SelectedCartInfoConverter selectedCartInfoConverter;

    @Mock
    PersonalClient personalClient;

    @Mock
    private ClientHelper clientHelper;

    GenericParams genericParams = new GenericParamsBuilder()
            .setThumbnailSize(Collections.singleton(ThumbnailSize.W50xH50))
            .setPp(IntLists.EMPTY_LIST)
            .setOpp(IntLists.EMPTY_LIST)
            .build();

    private Collection<PerkStatus> userPerks = Collections.emptyList();

    @Before
    @Override
    public void setUp() throws Exception {
        when(offerService.getOffersV2(anyCollectionOf(String.class), anyCollectionOf(Field.class), anyBoolean(),
                anyBoolean(), any(), anyInt()))
                .then(invocation -> {
                    Collection<String> ids = (Collection<String>) invocation.getArguments()[0];
                    return Pipelines.startWithValue(RequestGeneratorHelper.extactOffersV2(ids));
                });

        when(offerService.getOffersByShopOfferId(anyCollectionOf(ShopOfferId.class), anyInt(),
                any(GenericParams.class))).then(invocation -> {
            Collection<ShopOfferId> ids = (Collection<ShopOfferId>) invocation.getArguments()[0];
            return RequestGeneratorHelper.extractOfferInfoByShopOfferKey(ids);
        });

        when(offerIdEncodingService.decode(anyString())).thenAnswer(invocation -> {
            String[] val = ((String) invocation.getArguments()[0]).split(":");
            return new OfferId(val[0], val.length > 1 ? val[1] : null);
        });

        when(offerIdEncodingService.encode(any(OfferId.class))).thenAnswer(invocation -> {
            OfferId offerId = (OfferId) invocation.getArguments()[0];
            return offerId.getWareMd5() + (isNullOrEmpty(offerId.getFeeShow()) ? "" : (":" + offerId.getFeeShow()));
        });

        when(blueMobileApplicationRule.test()).thenReturn(false);

        when(reportClient.getOutletsByIds(anyString())).thenReturn(Pipelines.startWithValue(Collections.emptyList()));
        when(reportClient.getOutletsByIds("19890488"))
            .thenAnswer(invocation -> {
                    OutletV2 outlet = new OutletV2();
                    outlet.setId("19890488");
                    outlet.setName("Почта России: Посылка Нестандартная");
                    ArrayList<OpenHoursV2> schedule = new ArrayList<>();
                    schedule.add(new OpenHoursV2("1", "1", "10:00", "18:00"));
                    schedule.add(new OpenHoursV2("2", "2", "10:00", "18:00"));
                    schedule.add(new OpenHoursV2("3", "3", "10:00", "18:00"));
                    schedule.add(new OpenHoursV2("4", "4", "10:00", "18:00"));
                    schedule.add(new OpenHoursV2("5", "5", "10:00", "18:00"));
                    schedule.add(new OpenHoursV2("6", "6", "10:00", "18:00"));
                    schedule.add(
                        new OpenHoursV2(
                            "7",
                            "7",
                            "10:00",
                            "18:00",
                            Arrays.asList(
                                new BreakIntervalV2("11:00", "12:00"),
                                new BreakIntervalV2("16:00", "17:00")
                            )
                        )
                    );
                    outlet.setSchedule(schedule);
                    AddressV2 address = new AddressV2();
                    address.setLocality("Москва");
                    address.setPremiseNumber("12 к.6");
                    address.setRegionId(213);
                    address.setThoroughfare("шоссе Челобитьевское");
                    GeoPointV2 geoPoint = new GeoPointV2();
                    geoPoint.setCoordinates(new GeoCoordinatesV2(37.547953, 55.912768));
                    address.setGeoPoint(geoPoint);
                    outlet.setAddress(address);
                    return Pipelines.startWithValue(Collections.singletonList(outlet));
                }
            );

        when(coinConverter.convertCoinInfo(any())).thenReturn(null);

        promoCodeErrorConverter = new PromoCodeErrorConverter(new BlueMobilePromoCodeHack(clientHelper));


        initPersonal();

        converter = new CheckouterOrderConverter(
                offerService,
                offerIdEncodingService,
                reportClient,
                coinConverter,
                promoCodeErrorConverter,
                urlParamsFactoryImpl,
                deliveryPointIdEncodingService,
                installmentsInformationConverter,
                creditInformationConverter,
                selectedCartInfoConverter,
                personalClient);
    }

    private void initPersonal() {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode rootNode1 = mapper.createObjectNode();
        ObjectNode val1 = mapper.createObjectNode();
        rootNode1.put("id", "1");
        val1.put("phone", "03");
        rootNode1.set("value", val1);
        when(personalClient.multiTypesStore(Collections.singletonList(new ru.yandex.market.api.personal.Phone("03"))))
                .thenReturn(Pipelines.startWithValue(Collections.singletonList(rootNode1)));

        ObjectNode phoneNode = mapper.createObjectNode();
        ObjectNode phoneVal = mapper.createObjectNode();
        phoneNode.put("id", "2");
        phoneVal.put("phone", "+7999888777");
        phoneNode.set("value", phoneVal);

        ObjectNode emailNode = mapper.createObjectNode();
        ObjectNode emailVal = mapper.createObjectNode();
        emailNode.put("id", "3");
        emailVal.put("email", "test@test.com");
        emailNode.set("value", emailVal);

        ObjectNode nameNode = mapper.createObjectNode();
        ObjectNode nameObj = mapper.createObjectNode();
        ObjectNode nameVal = mapper.createObjectNode();
        nameVal.put("forename", "ABC");
        nameVal.put("surname", "DEF");
        nameVal.put("patronymic", "GHI");

        nameObj.set("full_name", nameVal);

        nameNode.put("id", "4");
        nameNode.set("value", nameObj);

        when(personalClient.multiTypesStore(Arrays.asList(
                new ru.yandex.market.api.personal.Phone("+7999888777"),
                new Email("test@test.com"),
                new FullName("ABC", "DEF", "GHI"))))
                .thenReturn(Pipelines.startWithValue(Arrays.asList(phoneNode, emailNode, nameNode)));
    }

    @Test
    public void shouldConvertOrderOptionRequest() {
        OrderOptionsRequest optionsRequest = generateOrderOptionRequestWithOneOrderAndAddress(
            TEST_USER_ID,
            TEST_USER_REGION_ID,
            TEST_USER_CURRENCY,
            TEST_SHOP_ID,
            new OfferId(TEST_OFFER_ID, null),
            TEST_OFFER_RIGHT_FEE_SHOW,
            TEST_OFFER_PRICE,
            1);
        optionsRequest.setPaymentOptions(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY));
        ContextHolder.get().setUser(new User(null, null, new Uuid("test-uuid"), null));

        MultiCart multiCart = converter.convertToMultiCart(optionsRequest, genericParams);

        // Проверяем, что запрашиваем информацию об офферах только один раз
        verify(offerService, times(1)).getOffersV2(anyCollectionOf(String.class),
                anyCollectionOf(Field.class), anyBoolean(), anyBoolean(), any(), anyInt());

        ru.yandex.market.checkout.checkouter.order.Buyer buyer = multiCart.getBuyer();
        assertEquals("test-uuid", buyer.getUuid());

        assertEquals(TEST_USER_CURRENCY.originalCurrency(), multiCart.getBuyerCurrency());
        assertEquals(Long.valueOf(TEST_USER_REGION_ID), multiCart.getBuyerRegionId());
        assertEquals(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY), multiCart.getPaymentOptions());
        assertEquals(1, multiCart.getCarts().size());

        Order order = multiCart.getCarts().get(0);
        assertEquals(Long.valueOf(TEST_SHOP_ID), order.getShopId());
        assertEquals(1, order.getItems().size());

        OrderItem orderItem = order.getItems().stream().findAny().get();
        assertEquals(TEST_OFFER_ID, orderItem.getWareMd5());
        assertEquals(Integer.valueOf(1), orderItem.getCount());
        assertEquals(TEST_OFFER_PRICE, orderItem.getBuyerPrice());
        assertEquals(TEST_OFFER_WRONG_FEE_SHOW, orderItem.getShowInfo());
        assertEquals(TEST_OFFER_SHOP_ID, orderItem.getOfferId());
        assertEquals(Long.valueOf(TEST_SHOP_FEED_ID), orderItem.getFeedId());
        assertEquals(TEST_OFFER_PRICE, orderItem.getPrice());

        Delivery delivery = order.getDelivery();
        assertEquals("234234", delivery.getHash());
        assertEquals("dfsghfsdrhsf", delivery.getDeliveryOptionId());

        Address address = delivery.getBuyerAddress();
        assertEquals("Test-Country", address.getCountry());
        assertEquals("Test-City", address.getCity());
        assertEquals("Test-District", address.getDistrict());
        assertEquals("Test-Street", address.getStreet());
        assertEquals("Test-House", address.getHouse());
        assertEquals(123L, address.getPreciseRegionId().longValue());

        BnplInfo bnplInfo = multiCart.getBnplInfo();
        assertFalse(bnplInfo.isAvailable());
        assertFalse(bnplInfo.isSelected());
    }

    @Test
    public void shouldConvertRegionIdInEachBucket() {
        OrderOptionsRequest optionsRequest = new OrderOptionsRequest();
        optionsRequest.setRegionId(TEST_USER_REGION_ID);
        optionsRequest.setCurrency(TEST_USER_CURRENCY);
        optionsRequest.setShopOrders(new ArrayList<OrderOptionsRequest.ShopOrder>() {{
            add(generateShopOrder(TEST_USER_REGION_ID));
            add(generateShopOrder(TEST_SECOND_USER_REGION_ID));
        }});

        MultiCart multiCart = converter.convertToMultiCart(optionsRequest, genericParams);

        Order firstOrder = multiCart.getCarts().get(0);
        Order secondOrder = multiCart.getCarts().get(1);

        assertEquals(Long.valueOf(TEST_USER_REGION_ID), firstOrder.getDelivery().getRegionId());
        assertEquals(Long.valueOf(TEST_SECOND_USER_REGION_ID), secondOrder.getDelivery().getRegionId());
    }

    @Test
    public void shoulConvertShopOrderRegionToEmptyDelivery() {
        OrderOptionsRequest optionsRequest = new OrderOptionsRequestBuilder()
            .withShopOrder(
                new OrderOptionsRequestShopOrderBuilder()
                    .random()
                    .withRegionId(TEST_USER_REGION_ID)
                    .build()
            )
            .withRegionId(TEST_SECOND_USER_REGION_ID)
            .build();

        MultiCart multiCart = converter.convertToMultiCart(optionsRequest, genericParams);

        Order firstOrder = multiCart.getCarts().get(0);
        assertEquals(TEST_USER_REGION_ID, firstOrder.getDelivery().getRegionId().intValue());

    }

    @Test
    public void shouldConvertBuyerInOrderOptionRequest() {
        ContextHolder.get().getFeatures().put(Feature.PERSONAL_ENABLED, "1");
        OrderOptionsRequest optionsRequest = generateOrderOptionRequestWithOneOrderAndAddress(
            TEST_USER_ID,
            TEST_USER_REGION_ID,
            TEST_USER_CURRENCY,
            TEST_SHOP_ID,
            new OfferId(TEST_OFFER_ID, null),
            TEST_OFFER_RIGHT_FEE_SHOW,
            TEST_OFFER_PRICE,
            1);
        optionsRequest.setPaymentOptions(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY));

        Buyer buyer = new Buyer();
        buyer.setPhone("03");
        optionsRequest.setBuyer(buyer);

        MultiCart multiCart = converter.convertToMultiCart(optionsRequest, genericParams);

        assertEquals("1", multiCart.getBuyer().getPersonalPhoneId());
        assertEquals("03", multiCart.getBuyer().getPhone());
    }

    @Test
    public void shouldConvertOrderOptionRequestOrderLabel() {
        OrderOptionsRequest optionsRequest = new OrderOptionsRequestBuilder()
            .random()
            .withShopOrder(
                new OrderOptionsRequestShopOrderBuilder()
                    .random()
                    .withLabel(TEST_ORDER_LABEL)
                    .build()
            )
            .build();

        MultiCart multiCart = converter.convertToMultiCart(optionsRequest, genericParams);

        assertEquals(TEST_ORDER_LABEL, multiCart.getCarts().get(0).getLabel());
    }

    @Test
    public void shouldUseFeeShowFromEncodedOfferId() {
        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .withShopOrder(
                new OrderOptionsRequestShopOrderBuilder()
                    .random()
                    .addItem(
                        new OrderOptionsRequestOrderItemBuilder()
                            .withOfferId(new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW))
                            .build()
                    ).build()
            ).build();

        MultiCart result = converter.convertToMultiCart(request, genericParams);

        OrderItem item = result.getCarts().get(0).getItems().iterator().next();
        assertEquals(TEST_OFFER_ID, item.getWareMd5());
        assertEquals(TEST_OFFER_RIGHT_FEE_SHOW, item.getShowInfo());
    }

    @Test
    public void shouldConvertMultiCartResponse() {

        OutletV2 outletV2 = new OutletV2();
        outletV2.setId(String.valueOf(112233L));
        outletV2.setPhones(Lists.newArrayList(
            new Phone("+7(123)4567890", "71234567890", null)
        ));
        outletV2.setName("Test-Outlet-Name");
        AddressV2 addressV2 = new AddressV2();
        addressV2.setRegionId(213);
        addressV2.setGeoPoint(new GeoPointV2(new Geo(213, "37.614006", "55.756994", 0d)));
        addressV2.setLocality("Test-Outlet-City");
        addressV2.setThoroughfare("Test-Outlet-Street");
        addressV2.setPremiseNumber("Test-Outlet-House");
        outletV2.setAddress(addressV2);
        outletV2.setSchedule(
            Collections.singletonList(
                new OpenHoursV2("1", "7", "9:00", "21:00")
            )
        );

        when(reportClient.getOutletsByIds(String.valueOf(112233L)))
            .thenReturn(
                Pipelines.startWithValue(
                    Collections.singletonList(outletV2)
                )
            );

        OrderOptionsResponse response = converter.convertToOptionsResponse(
            generateMultiCartWithOneOrder(
                TEST_USER_REGION_ID,
                TEST_SHOP_ID,
                TEST_SHOP_FEED_ID,
                CHECKOUTER_FEE_SHOW,
                TEST_OFFER_SHOP_ID,
                TEST_OFFER_PRICE,
                1
            ),
            generateOrderOptionRequestWithOneOrder(
                TEST_USER_ID,
                TEST_USER_REGION_ID,
                TEST_USER_CURRENCY,
                TEST_SHOP_ID,
                new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW),
                TEST_OFFER_WRONG_FEE_SHOW,
                TEST_OFFER_PRICE,
                1
            ),
            genericParams,
            true,
            false,
            false,
            userPerks
        );

        assertEquals(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY), response.getPaymentMethods());
        assertEquals(1, response.getShops().size());

        OrderOptionsResponse.ShopOptions options = response.getShops().get(0);
        assertEquals(1, options.getItems().size());
        assertEquals(Boolean.FALSE, options.isGlobal());

        ShopOrderItem shopOrderItem = options.getItems().get(0);

        Image image = shopOrderItem.getImage();
        assertEquals("http://test.com/offer/image.jpg", image.getUrl());
        assertEquals(TEST_OFFER_DELIVERY_BRIEF, shopOrderItem.getDeliveryView().getBrief());

        Payload payload = shopOrderItem.getPayload();
        assertEquals(TEST_SHOP_FEED_ID, payload.getFeedId());
        assertEquals(TEST_OFFER_SHOP_ID, payload.getShopOfferId());
        assertEquals(TEST_OFFER_ID, payload.getMarketOfferId());
        assertEquals(CHECKOUTER_FEE_SHOW, payload.getFee());

        assertEquals(2, options.getDeliveryOptions().size());

        ServiceDeliveryOption serviceDeliveryOption = (ServiceDeliveryOption) options.getDeliveryOptions().get(0);
        assertEquals(new DeliveryPointId("Test-Delivery-Id"), serviceDeliveryOption.getId());
        assertEquals(new BigDecimal("69.99"), serviceDeliveryOption.getPrice());
        assertEquals(LocalDate.of(2016, 1, 1), serviceDeliveryOption.getBeginDate());
        assertEquals(LocalDate.of(2016, 1, 5), serviceDeliveryOption.getEndDate());
        assertEquals(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY), serviceDeliveryOption.getPaymentMethods());
        assertEquals("Test-Delivery-Service-name", serviceDeliveryOption.getTitle());
        assertTrue(serviceDeliveryOption.getEstimated());

        OutletDeliveryOption outletDeliveryOption = (OutletDeliveryOption) options.getDeliveryOptions().get(1);
        assertEquals(new DeliveryPointId("Test-Outlet-Delivery-Id"), outletDeliveryOption.getId());
        assertEquals(BigDecimal.ZERO, outletDeliveryOption.getPrice());
        assertEquals(LocalDate.of(2016, 1, 1), outletDeliveryOption.getBeginDate());
        assertEquals(LocalDate.of(2016, 2, 1), outletDeliveryOption.getEndDate());
        assertEquals(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY), outletDeliveryOption.getPaymentMethods());
        assertEquals(1, outletDeliveryOption.getOutlets().size());
        assertNull(outletDeliveryOption.getEstimated());

        Outlet outlet = outletDeliveryOption.getOutlets().get(0);
        assertEquals(112233L, outlet.getId());
        assertEquals("Test-Outlet-Name", outlet.getName());
        assertEquals(null, outlet.getNotes());

        RoomAddress address = outlet.getAddress();
        assertEquals("Test-Outlet-City", address.getCity());
        assertEquals("Test-Outlet-Street", address.getStreet());
        assertEquals("Test-Outlet-House", address.getHouse());

        assertEquals(1, outlet.getWorkSchedules().size());
        WeekSchedule weekSchedule = outlet.getWorkSchedules().get(0);
        assertEquals(DayOfWeek.MONDAY, weekSchedule.getBeginDayOfWeek());
        assertEquals(DayOfWeek.SUNDAY, weekSchedule.getEndDayOfWeek());
        assertEquals(540, weekSchedule.getBeginMinuteOfDay());
        assertEquals(1260, weekSchedule.getEndMinuteOfDay());

        assertEquals(1, outlet.getPhones().size());
        assertEquals("71234567890", outlet.getPhones().get(0));

        assertEquals(BigDecimal.valueOf(0.1), options.getSummary().getWeight());
        assertEquals(BigDecimal.valueOf(0.1), response.getSummary().getWeight());
    }

    @Test
    public void shouldConvertMultiCartResponseIfIndexEmpty() {
        OrderOptionsResponse response = converter.convertToOptionsResponse(
            generateMultiCartWithOneOrder(
                TEST_USER_REGION_ID,
                TEST_SHOP_ID,
                TEST_SHOP_FEED_ID,
                CHECKOUTER_FEE_SHOW,
                TEST_OFFER_SHOP_ID,
                TEST_OFFER_PRICE,
                1
            ),
            generateOrderOptionRequestWithOneOrder(
                TEST_USER_ID,
                TEST_USER_REGION_ID,
                TEST_USER_CURRENCY,
                TEST_SHOP_ID,
                new OfferId(TEST_OFFER_ID, null),
                null,
                TEST_OFFER_PRICE,
                1
            ),
            genericParams,
            true,
            false,
            false,
            userPerks
        );
        assertNotNull(response);
    }

    @Test
    public void shouldUseFeeShowFromOfferIdFirst() {
        OrderOptionsResponse response = converter.convertToOptionsResponse(
            generateMultiCartWithOneOrder(
                TEST_USER_REGION_ID,
                TEST_SHOP_ID,
                TEST_SHOP_FEED_ID,
                CHECKOUTER_FEE_SHOW,
                TEST_OFFER_SHOP_ID,
                TEST_OFFER_PRICE,
                1
            ),
            generateOrderOptionRequestWithOneOrder(
                TEST_USER_ID,
                TEST_USER_REGION_ID,
                TEST_USER_CURRENCY,
                TEST_SHOP_ID,
                new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW),
                TEST_OFFER_WRONG_FEE_SHOW,
                TEST_OFFER_PRICE,
                1
            ),
            genericParams,
            true,
            false,
            false,
            userPerks
        );

        ShopOrderItem item = response.getShops().get(0).getItems().get(0);
        assertEquals(CHECKOUTER_FEE_SHOW, item.getPayload().getFee());
        assertEquals(TEST_OFFER_ID + ":" + TEST_OFFER_RIGHT_FEE_SHOW, item.getMarketOfferId());
    }

    @Test
    public void shouldReturnOriginalOfferId() {
        OrderOptionsResponse response = converter.convertToOptionsResponse(
            generateMultiCartWithOneOrder(
                TEST_USER_REGION_ID,
                TEST_SHOP_ID,
                TEST_SHOP_FEED_ID,
                CHECKOUTER_FEE_SHOW,
                TEST_OFFER_SHOP_ID,
                TEST_OFFER_PRICE,
                1
            ),
            generateOrderOptionRequestWithOneOrder(
                TEST_USER_ID,
                TEST_USER_REGION_ID,
                TEST_USER_CURRENCY,
                TEST_SHOP_ID,
                new OfferId(TEST_OFFER_ID, null),
                null,
                TEST_OFFER_PRICE,
                1
            ),
            genericParams,
            true,
            false,
            false,
            userPerks
        );

        ShopOrderItem item = response.getShops().get(0).getItems().get(0);
        assertEquals(TEST_OFFER_ID, item.getMarketOfferId());
        assertEquals(CHECKOUTER_FEE_SHOW, item.getPayload().getFee());
    }

    @Test
    public void shouldConvertMultiCartResponseOrderLabel() {
        MultiCart multiCart = new MultiCartBuilder()
            .random()
            .withOrder(
                new MultiCartOrderBuilder()
                    .random()
                    .withItem(
                        new OrderItemBuilder()
                            .random()
                            .build()
                    )
                    .withLabel(TEST_ORDER_LABEL)
                    .build()
            )
            .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .withShopOrder(
                new OrderOptionsRequestShopOrderBuilder()
                    .random()
                    .build()
            )
            .build();

        OrderOptionsResponse response = converter.convertToOptionsResponse(multiCart, request, genericParams,
                true, false, false, userPerks);

        assertEquals(TEST_ORDER_LABEL, response.getShops().get(0).getLabel());
    }

    @Test
    public void shouldConvertMultiCartResponseOrderParcelInfo() {
        String parcelInfo = "w:5;p:5;pc:RUR;tp:5;tpc:RUR;d:10x20x30;ct:1/2/3;wh:145;ffwh:145;";
        MultiCart multiCart = new MultiCartBuilder()
            .random()
            .withOrder(
                new MultiCartOrderBuilder()
                        .withParcelInfo(parcelInfo)
                    .random()
                    .build()
            )
            .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .withShopOrder(
                new OrderOptionsRequestShopOrderBuilder()
                    .random()
                    .build()
            )
            .build();

        OrderOptionsResponse response = converter.convertToOptionsResponse(multiCart, request, genericParams,
                true, false, false, userPerks);

        assertEquals(parcelInfo, response.getShops().get(0).getParcelInfo());
    }

    @Test
    public void shouldConvertCheckoutRequest() {
        CheckoutRequest request = new CheckoutRequest();
        request.setRegionId(TEST_USER_REGION_ID);
        request.setCurrency(TEST_USER_CURRENCY);
        request.setBuyer(new Buyer() {{
            setFirstName("ABC");
            setLastName("DEF");
            setMiddleName("GHI");
            setPhone("+7999888777");
            setEmail("test@test.com");
        }});
        request.setShopOrders(Lists.newArrayList(new CheckoutRequest.ShopOrder() {{
            setShopId(TEST_SHOP_ID);
            setItems(Lists.newArrayList(new CheckoutRequest.OrderItem() {{
                setOfferId(new OfferId(TEST_OFFER_ID, null));
                setPrice(TEST_OFFER_PRICE);
                setCount(1);
                setPayload(new Payload(
                    TEST_SHOP_FEED_ID,
                    TEST_OFFER_SHOP_ID,
                    TEST_OFFER_ID,
                    TEST_OFFER_RIGHT_FEE_SHOW
                ));
            }}));
            setDeliveryPoint(new AddressDeliveryPoint() {{
                setRegionId(TEST_USER_REGION_ID);
                setRecipient("Test-recipient");
                setPhone("+7999123456");
                setDeliveryOptionId(new DeliveryPointId("Test-Delivery-Option-Id"));
                setServiceName("Test-Delivery-Service");
                setAddress(new RoomAddress() {{
                    setCountry("Test-Country");
                    setCity("Test-City");
                    setStreet("Test-Street");
                    setBlock("Test-Block");
                    setHouse("Test-House");
                    setFloor("Test-Floor");
                }});
            }});
            setLabel(TEST_ORDER_LABEL);
        }}));

        ContextHolder.get().setUser(new User(null, null, new Uuid("test-uuid"), null));
        ContextHolder.get().getFeatures().put(Feature.PERSONAL_ENABLED, "1");

        MultiOrder order = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        assertEquals(Long.valueOf(TEST_USER_REGION_ID), order.getBuyerRegionId());
        assertEquals(TEST_USER_CURRENCY.originalCurrency(), order.getBuyerCurrency());

        ru.yandex.market.checkout.checkouter.order.Buyer buyer = order.getBuyer();
        assertEquals("ABC", buyer.getFirstName());
        assertEquals("DEF", buyer.getLastName());
        assertEquals("GHI", buyer.getMiddleName());
        assertEquals("4", buyer.getPersonalFullNameId());
        assertEquals("test@test.com", buyer.getEmail());
        assertEquals("3", buyer.getPersonalEmailId());
        assertEquals("2", buyer.getPersonalPhoneId());
        assertEquals("+7999888777", buyer.getPhone());
        assertEquals("test-uuid", buyer.getUuid());

        assertEquals(1, order.getCarts().size());

        Order shopOrder = order.getCarts().get(0);
        assertEquals(Long.valueOf(TEST_SHOP_ID), shopOrder.getShopId());
        assertEquals(1, shopOrder.getItems().size());
        assertEquals(TEST_ORDER_LABEL, shopOrder.getLabel());

        OrderItem item = shopOrder.getItems().iterator().next();
        assertEquals(Integer.valueOf(1), item.getCount());
        assertEquals(TEST_OFFER_PRICE, item.getBuyerPrice());
        assertEquals(Long.valueOf(TEST_SHOP_FEED_ID), item.getFeedId());
        assertEquals(TEST_OFFER_SHOP_ID, item.getOfferId());
        assertEquals(TEST_OFFER_RIGHT_FEE_SHOW, item.getShowInfo());

        Delivery delivery = shopOrder.getDelivery();
        assertEquals(Long.valueOf(TEST_USER_REGION_ID), delivery.getRegionId());
        assertEquals(DeliveryType.DELIVERY, delivery.getType());
        assertEquals("Test-Delivery-Service", delivery.getServiceName());
        assertEquals("Test-Delivery-Option-Id", delivery.getHash());

        Address address = delivery.getBuyerAddress();
        assertEquals("Test-Country", address.getCountry());
        assertEquals("Test-City", address.getCity());
        assertEquals("Test-Street", address.getStreet());
        assertEquals("Test-Block", address.getBlock());
        assertEquals("Test-House", address.getHouse());
        assertNull(address.getGps());
    }

    @Test
    public void shouldConvertCheckoutRequestWithLocation() {
        String longitude = "37.622504";
        String latitude = "55.753215";
        CheckoutRequest request = new CheckoutRequest();
        request.setShopOrders(Lists.newArrayList(new CheckoutRequest.ShopOrder() {{
            setDeliveryPoint(new AddressDeliveryPoint() {{
                setAddress(new RoomAddress() {{
                    setLocation(new Location(latitude, longitude));
                }});
            }});
            setLabel(TEST_ORDER_LABEL);
        }}));

        ContextHolder.get().setUser(new User(null, null, new Uuid("test-uuid"), null));

        MultiOrder order = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        Order shopOrder = order.getCarts().get(0);
        Delivery delivery = shopOrder.getDelivery();
        Address address = delivery.getBuyerAddress();
        assertEquals(address.getGps(), longitude + "," + latitude);
    }

    @Test
    public void shouldConvertCheckoutRequestWithAddressSource() {
        AddressSource addressSource = AddressSource.PERS_ADDRESS;
        CheckoutRequest request = new CheckoutRequest();
        request.setShopOrders(Lists.newArrayList(new CheckoutRequest.ShopOrder() {{
            setDeliveryPoint(new AddressDeliveryPoint() {{
                setAddress(new RoomAddress() {{
                    setAddressSource(addressSource);
                }});
            }});
            setLabel(TEST_ORDER_LABEL);
        }}));

        ContextHolder.get().setUser(new User(null, null, new Uuid("test-uuid"), null));

        MultiOrder order = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        Order shopOrder = order.getCarts().get(0);
        Delivery delivery = shopOrder.getDelivery();
        Address address = delivery.getBuyerAddress();
        assertEquals(addressSource, address.getAddressSource());
    }

    @Test
    public void shouldConvertCheckoutRequestWithPreciseRegionId() {
        Long expectedPreciseRegionId = 321L;
        CheckoutRequest request = new CheckoutRequest();
        request.setShopOrders(Lists.newArrayList(new CheckoutRequest.ShopOrder() {{
            setDeliveryPoint(new AddressDeliveryPoint() {{
                setAddress(new RoomAddress() {{
                    setPreciseRegionId(expectedPreciseRegionId);
                }});
            }});
            setLabel(TEST_ORDER_LABEL);
        }}));

        ContextHolder.get().setUser(new User(null, null, new Uuid("test-uuid"), null));

        MultiOrder order = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        Order shopOrder = order.getCarts().get(0);
        Delivery delivery = shopOrder.getDelivery();
        Address address = delivery.getBuyerAddress();
        assertEquals(expectedPreciseRegionId, address.getPreciseRegionId());
    }

    @Test
    public void shouldConvertCheckoutRequestWithPrescriptionGuids() {
        final Set<String> PRESCRIPTION_GUIDS = Collections.singleton("0258f5f8-020c-4e7e-bef3-85bc551e0664");
        CheckoutRequest request = new CheckoutRequestBuilder()
                .random()
                .withOrder(new CheckoutRequestShopOrderBuilder()
                        .random()
                        .withItem(new CheckoutRequestOrderItemBuilder()
                                .random()
                                .withPrescriptionGuids(PRESCRIPTION_GUIDS)
                                .build()
                        )
                        .build()
                )
                .build();
        MultiOrder multiOrder = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        OrderItem item = multiOrder.getOrders().get(0).getItems().iterator().next();
        assertEquals(PRESCRIPTION_GUIDS, item.getPrescriptionGuids());
    }

    @Test
    public void shouldConvertCheckoutRequestAndPreferFeeShowFromCheckouter() {
        CheckoutRequest request = new CheckoutRequestBuilder()
            .random()
            .withOrder(new CheckoutRequestShopOrderBuilder()
                .random()
                .withItem(new CheckoutRequestOrderItemBuilder()
                    .random()
                    .withId(new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW))
                    .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, CHECKOUTER_FEE_SHOW)
                    .build()
                )
                .build()
            )
            .build();

        MultiOrder multiOrder = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        OrderItem item = multiOrder.getOrders().get(0).getItems().iterator().next();
        assertEquals(CHECKOUTER_FEE_SHOW, item.getShowInfo());
    }

    @Test
    public void shouldConvertCheckoutRequestAndReturnOldOfferIdIfOldWasPassed() {
        MultiOrder multiOrder = new MultiOrderBuilder()
                .random()
                .withOrders(new MultiCartOrderBuilder()
                        .random()
                        .withItem(new MultiCartOrderItemBuilder()
                                .random()
                                .withShowInfo(TEST_OFFER_RIGHT_FEE_SHOW)
                                .withFeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID)
                                .withWareMd5(TEST_OFFER_ID)
                                .build()
                        )
                        .build()
                )
            .build();

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
            .random()
            .withOrder(new CheckoutRequestShopOrderBuilder()
                .random()
                .withItem(new CheckoutRequestOrderItemBuilder()
                    .random()
                    .withId(new OfferId(TEST_OFFER_ID, null))
                    .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                    .build()
                )
                .build()
            )
            .build();

        CheckoutResponse response = converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams,
                userPerks);

        ShopOrderItem item = response.getShopShopOrders().get(0).getItems().get(0);
        assertEquals(TEST_OFFER_ID, item.getMarketOfferId());
        assertEquals(TEST_OFFER_RIGHT_FEE_SHOW, item.getPayload().getFee());
    }

    @Test
    public void shouldConvertCheckoutRequestDontCallByDefault() {
        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
            .random()
            .build();

        MultiOrder multiOrder = converter.convertToMultiOrder(checkoutRequest, Collections.emptyList(), genericParams);

        assertTrue(multiOrder.getBuyer().isDontCall());
    }

    @Test
    public void shouldConvertCheckoutRequestCall() {
        Buyer buyer = new BuyerBuilder().random().withWaitingCall(true).build();

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
            .random()
            .withBuyer(buyer)
            .build();

        MultiOrder multiOrder = converter.convertToMultiOrder(checkoutRequest, Collections.emptyList(), genericParams);

        assertFalse(multiOrder.getBuyer().isDontCall());
    }

    @Test
    public void shouldConvertCheckoutRequestAndReturnNewOfferIdIfNewWasPassed() {
        MultiOrder multiOrder = new MultiOrderBuilder()
                .random()
                .withOrders(new MultiCartOrderBuilder()
                        .random()
                        .withItem(new MultiCartOrderItemBuilder()
                                .random()
                                .withShowInfo(TEST_OFFER_RIGHT_FEE_SHOW)
                                .withFeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID)
                                .withWareMd5(TEST_OFFER_ID)
                                .build()
                        )
                        .build()
                )
            .build();

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
            .random()
            .withOrder(new CheckoutRequestShopOrderBuilder()
                .random()
                .withItem(new CheckoutRequestOrderItemBuilder()
                    .random()
                    .withId(new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW))
                    .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                    .build()
                )
                .build()
            )
            .build();

        CheckoutResponse response = converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams,
                userPerks);

        ShopOrderItem item = response.getShopShopOrders().get(0).getItems().get(0);
        assertEquals(TEST_OFFER_ID + ":" + TEST_OFFER_RIGHT_FEE_SHOW, item.getMarketOfferId());
        assertEquals(TEST_OFFER_RIGHT_FEE_SHOW, item.getPayload().getFee());
    }

    @Test
    public void shouldConvertCheckoutRequestOrderLabel() {
        CheckoutRequest request = new CheckoutRequestBuilder()
            .random()
            .withOrder(
                new CheckoutRequestShopOrderBuilder()
                    .random()
                    .withItem(
                        new CheckoutRequestOrderItemBuilder()
                            .random()
                            .withId(new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW))
                            .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID,    TEST_OFFER_WRONG_FEE_SHOW)
                            .build()
                    )
                    .withLabel(TEST_ORDER_LABEL)
                    .build()
            )
            .build();

        MultiOrder multiOrder = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        Order order = multiOrder.getOrders().get(0);
        assertEquals(TEST_ORDER_LABEL, order.getLabel());
    }

    @Test
    public void shouldConvertCheckoutRequestDeviceId() {
        CheckoutRequest request = new CheckoutRequestBuilder()
                .random()
                .withOrder(new CheckoutRequestShopOrderBuilder()
                        .random()
                        .build()
                )
                .withUserDevice(new UserDevice() {{
                    setEmulator(false);
                    setDeviceId(ImmutableMap.of(
                            "android_device_id", "0",
                            "google_service_id", "1",
                            "android_hardware_serial", "2"
                    ));
                }})
                .build();

        MultiOrder multiOrder = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        String deviceIdsProp = multiOrder.getOrders().get(0).getProperty(OrderPropertyType.DEVICE_ID);
        assertNotNull(deviceIdsProp);
    }

    @Test
    public void shouldConvertCartRequestDeviceId() {
        OrderOptionsRequest optionsRequest = generateOrderOptionRequestWithOneOrderAndAddress(
                TEST_USER_ID,
                TEST_USER_REGION_ID,
                TEST_USER_CURRENCY,
                TEST_SHOP_ID,
                new OfferId(TEST_OFFER_ID, null),
                TEST_OFFER_RIGHT_FEE_SHOW,
                TEST_OFFER_PRICE,
                1);
        optionsRequest.setUserDevice(new UserDevice() {{
            setEmulator(false);
            setDeviceId(ImmutableMap.of(
                    "android_device_id", "0",
                    "google_service_id", "1",
                    "android_hardware_serial", "2"
            ));
        }});
        optionsRequest.setPaymentOptions(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY));
        ContextHolder.get().setUser(new User(null, null, new Uuid("test-uuid"), null));

        MultiCart multiCart = converter.convertToMultiCart(optionsRequest, genericParams);

        String deviceIdsProp = multiCart.getCarts().get(0).getProperty(OrderPropertyType.DEVICE_ID);
        assertNotNull(deviceIdsProp);
    }

    @Test
    public void shouldConvertCheckoutRequestYandexPlusUser() {
        CheckoutRequest request = new CheckoutRequestBuilder()
                .random()
                .withOrder(new CheckoutRequestShopOrderBuilder()
                        .random()
                        .build()
                )
                .build();

        MultiOrder multiOrder = converter.convertToMultiOrder(
                request,
                Collections.singletonList(PerkType.YANDEX_PLUS.getId()),
                genericParams
        );

        String property = multiOrder.getOrders().get(0).getProperty(CheckouterOrderConverter.PROPERTY_YANDEX_PLUS_USER);
        assertEquals(property, "true");
    }

    @Test
    public void shouldConvertCheckoutResponseWithValidationErrors() {
        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setValidationErrors(Lists.newArrayList(
            new ValidationResult("Test-Validation-Error", ValidationResult.Severity.ERROR))
        );

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder().random().build();

        CheckoutResponse checkoutResponse =
            converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams, userPerks);

        assertEquals(1, checkoutResponse.getErrors().size());
        OrderError error = checkoutResponse.getErrors().get(0);
        assertTrue(error instanceof UnknownOrderError);
    }

    @Test
    public void shouldConvertOrderOptionsResponseWithUnprocessableCoin() {
        MultiCart multiCart = new MultiCartBuilder()
            .random()
            .withOrder(
                new OrderBuilder()
                .random()
                    .withItems(new OrderItemBuilder()
                        .random()
                        .build())
                .build()
            )
            .withErrors(new ValidationResult("NOT_PROCESSABLE_COIN", ValidationResult.Severity.ERROR))
            .build();

        OrderOptionsRequest orderOptionsRequest = new OrderOptionsRequestBuilder().random().build();

        OrderOptionsResponse orderOptionsResponse =
            converter.convertToOptionsResponse(multiCart, orderOptionsRequest, genericParams,
                    false, false, false, userPerks);

        assertEquals(1, orderOptionsResponse.getErrors().size());
        OrderError error = orderOptionsResponse.getErrors().iterator().next();
        assertTrue(error instanceof NotProcessableCoinError);
    }

    @Test
    public void shouldConvertCostLimitInfo() {
        MultiCart multiCart = new MultiCartBuilder()
                .random()
                .build();
        multiCart.setCostLimitInformation(
                new CostLimitInformation() {{
                    setMinCost(BigDecimal.ONE);
                    setRemainingBeforeCheckout(BigDecimal.TEN);
                    setErrors(Collections.singletonList(Code.TOO_CHEAP_MULTI_CART));
                }}
        );

        OrderOptionsRequest orderOptionsRequest = new OrderOptionsRequestBuilder().random().build();

        OrderOptionsResponse orderOptionsResponse =
                converter.convertToOptionsResponse(multiCart, orderOptionsRequest, genericParams,
                        false, false, false, Collections.emptyList());

        ApiCostLimitInformation info = orderOptionsResponse.getCostLimitInformation();
        assertNotNull(info);
        assertEquals(BigDecimal.ONE, info.getMinCost());
        assertEquals(BigDecimal.TEN, info.getRemainingBeforeCheckout());
        assertEquals(1, info.getErrors().size());
        assertEquals(ApiCostLimitInformation.Code.TOO_CHEAP_MULTI_CART, info.getErrors().get(0));
    }

    @Test
    public void shouldConvertCheckoutResponseWithOrderLabel() {
        MultiOrder multiOrder = new MultiOrderBuilder()
                .random()
                .withOrders(
                        new OrderBuilder()
                                .random()
                                .withLabel(TEST_ORDER_LABEL)
                                .build()
                )
            .build();

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
            .random()
            .withOrder(
                new CheckoutRequestShopOrderBuilder()
                    .random()
                    .withItem(
                        new CheckoutRequestOrderItemBuilder()
                            .random()
                            .withId(new OfferId(TEST_OFFER_ID, null))
                            .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                            .build()
                    )
                    .build()
            )
            .build();

        CheckoutResponse checkoutResponse =
            converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams, Collections.emptyList());

        assertEquals(1, checkoutResponse.getShopShopOrders().size());

        ShopOrder shopOrder = checkoutResponse.getShopShopOrders().get(0);
        assertEquals(TEST_ORDER_LABEL, shopOrder.getLabel());
    }

    @Test
    public void shouldConvertCheckoutResponseWithOrderFailures() {
        MultiOrder multiOrder = new MultiOrderBuilder()
            .random()
            .withFailure(new MultiCartOrderBuilder()
                    .random()
                    .withItem(new MultiCartOrderItemBuilder()
                        .random()
                        .withWareMd5(TEST_OFFER_ID)
                        .withShowInfo(TEST_OFFER_RIGHT_FEE_SHOW)
                        .withFeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID)
                        .build()
                    )
                    .build(),
                OrderFailure.Code.UNKNOWN_ERROR,
                "Error-Description"
            )
            .build();

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
            .random()
            .withOrder(new CheckoutRequestShopOrderBuilder()
                .random()
                .withItem(new CheckoutRequestOrderItemBuilder()
                    .random()
                    .withId(new OfferId(TEST_OFFER_ID, null))
                    .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                    .build()
                )
                .build()
            )
            .build();

        CheckoutResponse checkoutResponse =
            converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams, Collections.emptyList());

        assertEquals(1, checkoutResponse.getShopShopOrders().size());

        ShopOrder shopOrder = checkoutResponse.getShopShopOrders().get(0);
        assertEquals(1, shopOrder.getErrors().size());

        ShopOrderError shopOrderError = shopOrder.getErrors().get(0);
        assertTrue(shopOrderError instanceof UnknownOrderError);

        ShopOrderItem item = shopOrder.getItems().get(0);
        assertEquals(TEST_OFFER_ID, item.getMarketOfferId());
    }

    @Test
    public void shouldConvertCheckoutResponseWithOutOfDateOrderFailure() {
        MultiOrder multiOrder = new MultiOrderBuilder()
            .random()
            .withFailure(new MultiCartOrderBuilder()
                    .random()
                    .withLabel(TEST_ORDER_LABEL)
                    .withItem(new MultiCartOrderItemBuilder()
                        .random()
                        .withWareMd5(TEST_OFFER_ID)
                        .withShowInfo(TEST_OFFER_RIGHT_FEE_SHOW)
                        .withFeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID)
                        .build()
                    )
                    .withChanges(CartChange.DELIVERY)
                    .build(),
                OrderFailure.Code.OUT_OF_DATE,
                null
            )
            .build();

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
            .random()
            .withOrder(new CheckoutRequestShopOrderBuilder()
                .random()
                .withLabel(TEST_ORDER_LABEL)
                .withItem(new CheckoutRequestOrderItemBuilder()
                    .random()
                    .withId(new OfferId(TEST_OFFER_ID, null))
                    .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                    .build()
                )
                .build()
            )
            .build();

        CheckoutResponse checkoutResponse =
            converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams, Collections.emptyList());

        assertEquals(1, checkoutResponse.getShopShopOrders().size());

        ShopOrder shopOrder = checkoutResponse.getShopShopOrders().get(0);
        assertTrue(shopOrder.getModifications().contains(CartChange.DELIVERY.name()));

        assertEquals(1, shopOrder.getErrors().size());
        ShopOrderError shopOrderError = shopOrder.getErrors().get(0);
        assertTrue(shopOrderError instanceof InconsistentOrderError);

        InconsistentOrderError inconsistentOrderError = (InconsistentOrderError) shopOrderError;
        assertEquals(1, inconsistentOrderError.getModifications().size());
        assertTrue(inconsistentOrderError.getModifications().contains(CartChange.DELIVERY.name()));

        ShopOrderItem item = shopOrder.getItems().get(0);
        assertEquals(TEST_OFFER_ID, item.getMarketOfferId());
    }

    @Test
    public void shouldConvertCheckoutResponseWithOrderFailuresAndNewOfferId() {
        MultiOrder multiOrder = new MultiOrderBuilder()
            .random()
            .withFailure(new MultiCartOrderBuilder()
                    .random()
                    .withItem(new MultiCartOrderItemBuilder()
                        .random()
                        .withWareMd5(TEST_OFFER_ID)
                        .withShowInfo(TEST_OFFER_RIGHT_FEE_SHOW)
                        .withFeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID)
                        .build()
                    )
                    .build(),
                OrderFailure.Code.UNKNOWN_ERROR,
                "Error-Description"
            )
            .build();

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
            .random()
            .withOrder(new CheckoutRequestShopOrderBuilder()
                .random()
                .withItem(new CheckoutRequestOrderItemBuilder()
                    .random()
                    .withId(new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW))
                    .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                    .build()
                )
                .build()
            )
            .build();

        CheckoutResponse checkoutResponse =
            converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams, userPerks);

        assertEquals(1, checkoutResponse.getShopShopOrders().size());

        ShopOrder shopOrder = checkoutResponse.getShopShopOrders().get(0);
        assertEquals(1, shopOrder.getErrors().size());

        ShopOrderError shopOrderError = shopOrder.getErrors().get(0);
        assertTrue(shopOrderError instanceof UnknownOrderError);

        ShopOrderItem item = shopOrder.getItems().get(0);
        assertEquals(TEST_OFFER_ID + ":" + TEST_OFFER_RIGHT_FEE_SHOW, item.getMarketOfferId());
    }

    @Test
    public void shouldConvertSuccessfulOrder() {
        Order order = new Order();
        order.setId(123L);
        order.setGlobal(true);
        order.setShopId((long) TEST_SHOP_ID);
        order.setItems(Lists.newArrayList(
            new OrderItem(new FeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID), TEST_OFFER_PRICE, 1) {{
                setWareMd5(TEST_OFFER_ID);
            }}
        ));
        order.setBuyerCurrency(Currency.RUR);
        order.setLabel(TEST_ORDER_LABEL);
        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setCarts(Lists.newArrayList(order));

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
            .random()
            .withOrder(new CheckoutRequestShopOrderBuilder()
                .random()
                .withItem(new CheckoutRequestOrderItemBuilder()
                    .random()
                    .withId(new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW))
                    .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                    .build()
                )
                .build()
            )
            .build();

        CheckoutResponse response =
            converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams, userPerks);

        assertEquals(0, response.getErrors().size());
        assertEquals(1, response.getShopShopOrders().size());

        ShopOrder shopOrder = response.getShopShopOrders().get(0);
        assertEquals(TEST_SHOP_ID, shopOrder.getShopId());
        assertEquals(Boolean.TRUE, shopOrder.isGlobal());
        assertEquals(1, shopOrder.getItems().size());
        assertEquals(TEST_ORDER_LABEL, shopOrder.getLabel());

        ShopOrderItem item = shopOrder.getItems().get(0);
        assertNull(item.getErrors());
        assertEquals(1, item.getCount());
        assertEquals(TEST_OFFER_PRICE, item.getPrice());
        assertEquals(TEST_OFFER_DELIVERY_BRIEF, item.getDeliveryView().getBrief());
    }

    @Test
    public void shouldConvertHistoryEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(123L);
        event.setType(HistoryEventType.NEW_ORDER);
        event.setAuthor(new ClientInfo(ClientRole.USER, 1L, (long) TEST_SHOP_ID));
        event.setFromDate(new Date(year(2016), month(1), day(1)));
        event.setToDate(new Date(year(2016), month(1), day(5)));
        event.setOrderAfter(new Order() {{
            setStatus(OrderStatus.PLACING);
        }});

        HistoryEvent result = converter.convertHistoryEvent(event);
        assertEquals(OrderStatus.PLACING, result.getStatus());
        assertEquals(null, result.getSubstatus());
        assertEquals(new Date(year(2016), month(1), day(1)).getTime(), (long) result.getTime());
    }

    @Test
    public void shouldAddHiddenPaymentOptions() {
        RawDeliveryIntervalsCollection rawDeliveryIntervalsCollection = new RawDeliveryIntervalsCollection();
        rawDeliveryIntervalsCollection.add(new RawDeliveryInterval(new Date()));
        MultiCart cart = new MultiCartBuilder()
            .random()
            .withOrder(new MultiCartOrderBuilder()
                .random()
                .withItem(new MultiCartOrderItemBuilder()
                    .random()
                    .build()
                )
                .withDeliveryOptions(new MultiCartOrderDeliveryBuilder(DeliveryType.DELIVERY)
                    .random()
                    .withPaymentOptions(PaymentMethod.CASH_ON_DELIVERY)
                    .withHiddenPaymentOptions(new PaymentOption(PaymentMethod.YANDEX, PaymentOptionHiddenReason.MUID))
                    .withDeliveryIntervals(rawDeliveryIntervalsCollection)
                    .build()
                )
                .build()
            )
            .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .build();

        OrderOptionsResponse response = converter.convertToOptionsResponse(cart, request, genericParams,
                true, false, false, userPerks);

        Set<DeliveryOption.HiddenPaymentOption> hiddenPaymentMethods =        response.getShops().get(0).getDeliveryOptions().get(0).getHiddenPaymentMethods();
        assertEquals(Sets.newHashSet(new DeliveryOption.HiddenPaymentOption(PaymentMethod.YANDEX, PaymentOptionHiddenReason.MUID)), hiddenPaymentMethods);
    }

    @Test
    public void testTransformWithBundleSettings() {
        RawDeliveryIntervalsCollection rawDeliveryIntervalsCollection = new RawDeliveryIntervalsCollection();
        rawDeliveryIntervalsCollection.add(new RawDeliveryInterval(new Date()));
        MultiCart cart = new MultiCartBuilder()
            .random()
            .withOrder(new MultiCartOrderBuilder()
                .random()
                .withItem(new MultiCartOrderItemBuilder()
                    .random()
                    .withWareMd5(TEST_OFFER_ID)
                    .build()
                )
                .withDeliveryOptions(new MultiCartOrderDeliveryBuilder(DeliveryType.DELIVERY)
                    .random()
                    .withPaymentOptions(PaymentMethod.CASH_ON_DELIVERY)
                    .withHiddenPaymentOptions(new PaymentOption(PaymentMethod.YANDEX,PaymentOptionHiddenReason.MUID))
                    .withDeliveryIntervals(rawDeliveryIntervalsCollection)
                    .build()
                )
                .build()
            )
            .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .withShopOrder(new OrderOptionsRequestShopOrderBuilder()
                .addItem(new OrderOptionsRequestOrderItemBuilder()
                    .random()
                    .withOfferId(new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW))
                    .build()
                )
                .build()
            )
            .build();

        OrderOptionsResponse response = converter.convertToOptionsResponse(cart, request, genericParams,
                true, false, false, userPerks);
        ShopOrderItem shopOptions = response.getShops().get(0).getItem(0);
        BundleSettings bundleSettings = shopOptions.getBundleSettings();

        assertNotNull(bundleSettings);
        assertEquals(2, bundleSettings.getQuantityLimit().getMinimum());
        assertEquals(5, bundleSettings.getQuantityLimit().getStep());
    }

    @Test
    public void shouldConvertReceipt() {
        Receipt receipt = new Receipt();
        receipt.setType(ReceiptType.INCOME);
        receipt.setStatus(ReceiptStatus.PRINTED);
        receipt.setId(1L);
        receipt.setCreatedAt(Instant.ofEpochMilli(0L));

        assertThat(
            converter.convertReceipt(receipt),
            receipt(
                ReceiptMatcher.id(1L),
                type(ru.yandex.market.api.user.order.checkout.Receipt.Type.INCOME),
                status(ru.yandex.market.api.user.order.checkout.Receipt.Status.PRINTED),
                creationDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()))
            )
        );
    }

    @Test
    public void shouldConvertCheckpoint() {
        TrackCheckpoint checkpoint = new TrackCheckpoint(
            "con", null, "loc", "msg", CheckpointStatus.INFO_RECEIVED, null,
            Date.from(Instant.ofEpochMilli(7L)), 3
        );
        checkpoint.setId(1L);

        assertThat(
            converter.convertToCheckpoint(checkpoint),
            checkpoint(
                checkpointId(1L),
                country("con"),
                location("loc"),
                message("msg"),
                TrackingMatcher.status(Checkpoint.Status.INFO_RECEIVED),
                deliveryStatus(3),
                time(7L)
            )
        );
    }

    @Test
    public void shouldConvertTrackWithCheckpoints() {
        TrackCheckpoint tc = new TrackCheckpoint();
        tc.setId(1L);
        TrackCheckpoint tc2 = new TrackCheckpoint();
        tc2.setId(2L);

        Track track = new Track();
        track.setId(1L);
        track.setTrackCode("tCode");
        track.setCheckpoints(Arrays.asList(tc, tc2));

        assertThat(
            converter.convertToTracking(track),
            tracking(
                TrackingMatcher.id(1L),
                code("tCode"),
                checkpoints(
                    containsInAnyOrder(
                        checkpointId(1L),
                        checkpointId(2L)
                    )
                )
            )
        );
    }

    @Test
    public void shouldConvertTrackWithoutCheckpoints() {
        Track track = new Track();
        track.setId(1L);
        track.setTrackCode("tCode");

        assertThat(
            converter.convertToTracking(track),
            tracking(
                TrackingMatcher.id(1L),
                code("tCode"),
                checkpoints(
                    emptyIterable()
                )
            )
        );
    }

    @Test
    public void shouldConvertOrderWithTrack() {
        Track t = new Track();
        t.setId(12L);

        Parcel shipment = new Parcel();
        shipment.setTracks(Collections.singletonList(t));

        Order order = new OrderBuilder().random().build();
        order.setDelivery(new DeliveryBuilder()
            .random()
            .outlet(new ShopOutletBuilder().phones(Collections.emptyList()).random())
            .shipments(Collections.singletonList(shipment))
            .build());

        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);

        assertThat(
            result.getTracking(),
            tracking(
                TrackingMatcher.id(12L)
            )
        );
    }

    @Test
    public void shouldConvertOrderWithPropertiesContainsNullValues() {
        String propertyName = "NullableValue";
        Order order = new OrderBuilder().random().build();
        order.setProperty(propertyName, null);

        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);

        assertNull(
                result.getProperties().get(propertyName)
        );
    }

    @Test
    public void shouldConvertImageWithThumbSize() {
        Order order = new OrderBuilder()
            .random()
            .withItems(
                new OrderItemBuilder()
                    .random()
                    .withPicture("//avatar/")
                    .build()
            )
            .build();

        GenericParams genericParams = new GenericParamsBuilder()
            .setThumbnailSize(Collections.singleton(ThumbnailSize.W200xH200))
            .build();

        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);

        assertThat(
            result.getItems().get(0).getImage(),
            (Matcher<Image>) (ImageMatcher.image("http://avatar/200x200", 200, 200))
        );
    }

    @Test
    public void shouldConvertPostcode() {
        String postCode = "443087";
        AddressImpl addressImpl = new AddressImpl();
        addressImpl.setPostcode(postCode);

        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.POST);
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date()));
        delivery.setBuyerAddress(addressImpl);

        Order order = getOrder(delivery);

        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);

        assertTrue(result.getDeliveryPoint().getClass() == AddressDeliveryPoint.class);
        assertEquals("Post code not converted", postCode, ((AddressDeliveryPoint)result.getDeliveryPoint()).getAddress().getPostCode());
    }

    @Test
    public void shouldConvertOutletStoragePeriod() {
        Integer outletStoragePeriod = 5;

        Order order = prepareOrder();
        order.getDelivery().setOutletStoragePeriod(outletStoragePeriod);

        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);

        assertTrue(result.getDeliveryPoint().getClass() == OutletDeliveryPoint.class);
        assertEquals("Outlet storage period not converted", outletStoragePeriod, ((OutletDeliveryPoint)result.getDeliveryPoint()).getOutletStoragePeriod());
    }

    @Test
    public void shouldConvertOutletStorageLimitDate() {
        LocalDate outletStorageLimitDate = LocalDate.of(2020, 3, 23);

        Order order = prepareOrder();
        order.getDelivery().setOutletStorageLimitDate(outletStorageLimitDate);

        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);

        assertTrue(result.getDeliveryPoint().getClass() == OutletDeliveryPoint.class);
        assertEquals("Outlet storage limit date not converted", outletStorageLimitDate, ((OutletDeliveryPoint)result.getDeliveryPoint()).getOutletStorageLimitDate());
    }

    @Test
    public void shouldConvertPostOutlet() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.POST);
        delivery.setPostOutletId(19890488L);
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date()));
        Order order = getOrder(delivery);

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, GenericParams.DEFAULT);
        Outlet outlet = ((PostDeliveryOption) persistentOrder.getDeliveryOption()).getOutlet();
        assertEquals(19890488, outlet.getId());
        assertEquals("Почта России: Посылка Нестандартная", outlet.getName());
        assertEquals("55.912768,37.547953", outlet.getAddress().getGeoLocation());
        assertEquals(7, outlet.getWorkSchedules().size());

        WeekSchedule weekSchedule0 = outlet.getWorkSchedules().get(0);
        assertEquals(DayOfWeek.MONDAY, weekSchedule0.getBeginDayOfWeek());
        assertEquals(DayOfWeek.MONDAY, weekSchedule0.getBeginDayOfWeek());
        assertEquals(600, weekSchedule0.getBeginMinuteOfDay());
        assertEquals(1080, weekSchedule0.getEndMinuteOfDay());

        WeekSchedule weekSchedule5 = outlet.getWorkSchedules().get(5);
        assertEquals(DayOfWeek.SATURDAY, weekSchedule5.getBeginDayOfWeek());
        assertEquals(DayOfWeek.SATURDAY, weekSchedule5.getBeginDayOfWeek());

        WeekSchedule weekSchedule6 = outlet.getWorkSchedules().get(6);
        assertEquals(2, weekSchedule6.getBreaks().size());
        assertEquals("11:00", weekSchedule6.getBreaks().get(0).getBegin());
        assertEquals("12:00", weekSchedule6.getBreaks().get(0).getEnd());
        assertEquals("16:00", weekSchedule6.getBreaks().get(1).getBegin());
        assertEquals("17:00", weekSchedule6.getBreaks().get(1).getEnd());

    }

    @Test
    public void shouldConvertDeliveryIntervals() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setBuyerPrice(BigDecimal.valueOf(499));

        RawDeliveryIntervalsCollection rawDeliveryIntervals = new RawDeliveryIntervalsCollection();
        rawDeliveryIntervals.add(new RawDeliveryInterval(new Date(1526936400000L), LocalTime.parse("10:00"), LocalTime.parse("14:00")));
        rawDeliveryIntervals.add(new RawDeliveryInterval(new Date(1526936400000L), LocalTime.parse("10:00"), LocalTime.parse("18:00"), true));
        delivery.setRawDeliveryIntervals(rawDeliveryIntervals);
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date()));

        Order order = getOrder(delivery);

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, GenericParams.DEFAULT);
        List<DeliveryInterval> intervals = ((ServiceDeliveryOption) persistentOrder.getDeliveryOption()).getIntervals();

        assertThat(intervals, contains(
            DeliveryIntervalMatcher.deliveryInterval(LocalTime.parse("10:00"), LocalTime.parse("14:00"), false, BigDecimal.valueOf(499)),
            DeliveryIntervalMatcher.deliveryInterval(LocalTime.parse("10:00"), LocalTime.parse("18:00"), true, BigDecimal.valueOf(499))));
    }

    @Test
    public void shouldConvertDeliveryFeatures() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date()));
        delivery.setFeatures(Sets.newHashSet(DeliveryFeature.ON_DEMAND, DeliveryFeature.ON_DEMAND_YALAVKA));

        Order order = getOrder(delivery);

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1,                    GenericParams.DEFAULT);
        Set<DeliveryFeature> convertedFeatures = persistentOrder.getDeliveryOption().getFeatures();

        assertThat(convertedFeatures, containsInAnyOrder(DeliveryFeature.ON_DEMAND, DeliveryFeature.ON_DEMAND_YALAVKA));
    }

    @Test
    public void shouldConvertOrderDeliveryInterval() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setBuyerPrice(BigDecimal.valueOf(499));

        delivery.setRawDeliveryIntervals(new RawDeliveryIntervalsCollection());
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date(), LocalTime.parse("10:00"), LocalTime.parse("14:00")));

        Order order = getOrder(delivery);

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, GenericParams.DEFAULT);
        List<DeliveryInterval> intervals = ((ServiceDeliveryOption) persistentOrder.getDeliveryOption()).getIntervals();

        assertThat(intervals, contains(
            DeliveryIntervalMatcher.deliveryInterval(LocalTime.parse("10:00"), LocalTime.parse("14:00"), true, BigDecimal.valueOf(499))));

    }

    @Test
    public void shouldConvertOutlet() {
        Order order = prepareOrder();

        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);
        Assert.assertTrue(result.getDeliveryOption() instanceof OutletDeliveryOption);

        OutletDeliveryOption option = (OutletDeliveryOption) result.getDeliveryOption();
        Assert.assertThat(option.getOutlets(), hasSize(1));

        List<WeekSchedule> schedule = option.getOutlets().get(0).getWorkSchedules();
        Assert.assertThat(
            schedule,
            containsInAnyOrder(
                WeekScheduleMatcher.schedule(
                    WeekScheduleMatcher.beginDow(DayOfWeek.MONDAY),
                    WeekScheduleMatcher.endDow(DayOfWeek.THURSDAY),
                    WeekScheduleMatcher.beginMod(600),
                    WeekScheduleMatcher.endMod(1200),
                    WeekScheduleMatcher.breaks(
                        containsInAnyOrder(
                            BreakIntervalMatcher.intervals(
                                BreakIntervalMatcher.begin("12:00"),
                                BreakIntervalMatcher.end("13:00")
                            ),
                            BreakIntervalMatcher.intervals(
                                BreakIntervalMatcher.begin("18:00"),
                                BreakIntervalMatcher.end("19:00")
                            )
                        )
                    )
                ),
                WeekScheduleMatcher.schedule(
                    WeekScheduleMatcher.beginDow(DayOfWeek.FRIDAY),
                    WeekScheduleMatcher.endDow(DayOfWeek.SATURDAY),
                    WeekScheduleMatcher.beginMod(900),
                    WeekScheduleMatcher.endMod(1200)
                )
            )
        );

    }

    @Test
    public void shouldConvertOutletWorkScheduleFormatV2() {
        Order order = prepareOrder();

        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V2, genericParams);
        Assert.assertTrue(result.getDeliveryOption() instanceof OutletDeliveryOption);

        OutletDeliveryOption option = (OutletDeliveryOption) result.getDeliveryOption();
        Assert.assertThat(option.getOutlets(), hasSize(1));

        List<OpenHoursV2> schedule = option.getOutlets().get(0).getWorkSchedulesV2();

        Assert.assertThat(
            schedule,
            containsInAnyOrder(
                OpenHoursMatcher.openHours(
                    OpenHoursMatcher.daysFrom("1"),
                    OpenHoursMatcher.daysTill("4"),
                    OpenHoursMatcher.from("10:00"),
                    OpenHoursMatcher.till("20:00"),
                    OpenHoursMatcher.breaks(
                        cast(
                            containsInAnyOrder(
                                BreakIntervalV2Matcher.interval(
                                    BreakIntervalV2Matcher.from("12:00"),
                                    BreakIntervalV2Matcher.till("13:00")
                                ),
                                BreakIntervalV2Matcher.interval(
                                    BreakIntervalV2Matcher.from("18:00"),
                                    BreakIntervalV2Matcher.till("19:00")
                                )
                            )
                        )
                    )
                ),
                OpenHoursMatcher.openHours(
                    OpenHoursMatcher.daysFrom("5"),
                    OpenHoursMatcher.daysTill("6"),
                    OpenHoursMatcher.from("15:00"),
                    OpenHoursMatcher.till("20:00")
                )
            )
        );

    }

    @Test
    public void shouldConvertPayment() {
        long orderId = 1L;
        Payment payment = new Payment();
        payment.setCurrency(Currency.RUR);
        payment.setPartitions(Arrays.asList(
            new PaymentPartition(PaymentAgent.SBER_SPASIBO, BigDecimal.valueOf(3)),
            new PaymentPartition(PaymentAgent.YANDEX_CASHBACK, BigDecimal.valueOf(2)),
            new PaymentPartition(PaymentAgent.DEFAULT, BigDecimal.valueOf(7))
        ));
        payment.setTotalAmount(BigDecimal.valueOf(12));

        Order order = new OrderBuilder()
            .random()
            .withId(orderId)
            .withTotal(BigDecimal.valueOf(12))
            .withBuyerTotal(BigDecimal.valueOf(12))
            .withItemsTotal(BigDecimal.valueOf(11))
            .withBuyerItemsTotal(BigDecimal.valueOf(11))
            .withPayment(payment)
            .build();

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(order, WorkScheduleFormat.V2, GenericParams.DEFAULT);

        Assert.assertEquals(BigDecimal.valueOf(7), persistentOrder.getTotal());
        Assert.assertEquals(BigDecimal.valueOf(7), persistentOrder.getBuyerTotal());

        Assert.assertEquals(BigDecimal.valueOf(6), persistentOrder.getSubtotal());
        Assert.assertEquals(BigDecimal.valueOf(6), persistentOrder.getBuyerSubtotal());

        Assert.assertThat(persistentOrder.getPayment(), PaymentMatcher.payment(
            PaymentMatcher.totalAmount(BigDecimal.valueOf(12)),
            PaymentMatcher.currency(ru.yandex.market.api.common.currency.Currency.RUR),
            PaymentMatcher.partitions(cast(containsInAnyOrder(
                PaymentPartitionMatcher.paymentPartition(
                    PaymentPartitionMatcher.paymentAgent(PaymentAgent.SBER_SPASIBO),
                    PaymentPartitionMatcher.amount(BigDecimal.valueOf(3))
                ),
                PaymentPartitionMatcher.paymentPartition(
                    PaymentPartitionMatcher.paymentAgent(PaymentAgent.DEFAULT),
                    PaymentPartitionMatcher.amount(BigDecimal.valueOf(7))
                ),
                PaymentPartitionMatcher.paymentPartition(
                    PaymentPartitionMatcher.paymentAgent(PaymentAgent.YANDEX_CASHBACK),
                    PaymentPartitionMatcher.amount(BigDecimal.valueOf(2))
                )
            )))
        ));
    }

    @Test
    public void shouldInsertSizeToImageUrl() {
        Image image = converter.toOrderItemImage(
            new OrderItemBuilder()
                .withPicture("https://avatars.mds.yandex.net/get-mpic/331398/img_id4268809638018824601.jpeg/")
                .build(),
            ThumbnailSize.W50xH50
        );

        Assert.assertEquals("https://avatars.mds.yandex.net/get-mpic/331398/img_id4268809638018824601.jpeg/50x50", image.getUrl());
    }

    @Test
    public void shouldFindFirstImageUrlIfEndsWithJpg() {
        Image image = converter.toOrderItemImage(
            new OrderItemBuilder()
                .withPicture("https://avatars.mds.yandex.net/get-mpic/331398/img_id4268809638018824601.jpg")
                .build(),
            ThumbnailSize.W50xH50
        );

        Assert.assertEquals("https://avatars.mds.yandex.net/get-mpic/331398/img_id4268809638018824601.jpg",                        image.getUrl());
    }

    @Test
    public void shouldFindImageUrlWithCorrectSize() {
        Image image = converter.toOrderItemImage(
            new OrderItemBuilder()
                .withOfferPictures(Arrays.asList(
                    new OfferPicture("https://avatars.mds.yandex.net/get-mpic/331398/img_id4268809638018824601.jpeg/50x50") {{
                        setContainerWidth(50);
                        setContainerHeight(50);
                    }},
                    new OfferPicture("https://avatars.mds.yandex.net/get-mpic/331398/img_id4268809638018824601.jpeg/100x100") {{
                        setContainerWidth(100);
                        setContainerHeight(100);
                    }},
                    new OfferPicture("https://avatars.mds.yandex.net/get-mpic/331398/img_id4268809638018824601.jpeg/300x300") {{
                        setContainerWidth(300);
                        setContainerHeight(300);
                    }}
                ))
                .build(),
            ThumbnailSize.W100xH100
        );

        Assert.assertEquals("https://avatars.mds.yandex.net/get-mpic/331398/img_id4268809638018824601.jpeg/100x100",                            image.getUrl());
    }

    @Test
    public void shouldFindFirstImageUrlIfThereIsNoCorrectSize() {
        Image image = converter.toOrderItemImage(
            new OrderItemBuilder()
                .withOfferPictures(Arrays.asList(
                    new OfferPicture("https://avatars.mds.yandex.net/get-mpic/331398/img_id4268809638018824601.jpeg/50x50") {{
                        setContainerWidth(50);
                        setContainerHeight(50);
                    }},
                    new OfferPicture("https://avatars.mds.yandex.net/get-mpic/331398/img_id4268809638018824601.jpeg/100x100") {{
                        setContainerWidth(100);
                        setContainerHeight(100);
                    }}
                ))
                .build(),
            ThumbnailSize.W300xH300
        );

        Assert.assertEquals("https://avatars.mds.yandex.net/get-mpic/331398/img_id4268809638018824601.jpeg/50x50",                        image.getUrl());
    }

    @Test
    public void shouldConvertBuyerPriceNominalInOrderOptions() {
        OrderItem item = new OrderItemBuilder()
            .random()
            .withBuyerPriceNominal(new BigDecimal("987.65"))
            .build();

        Order order = new OrderBuilder()
            .random()
            .withItems(item)
            .build();

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(
            order,
            WorkScheduleFormat.V2,
            genericParams
        );

        Assert.assertEquals(
            new BigDecimal("987.65"),
            persistentOrder.getItems().get(0).getBuyerPriceNominal()
        );
    }

    @Test
    public void shouldConvertBuyerPriceNominalInCheckout() {
        CheckoutRequest.OrderItem item = new CheckoutRequestOrderItemBuilder()
            .withId(new OfferId("abc", null))
            .withBuyerPriceNominal(new BigDecimal("123.45"))
            .random()
            .build();

        CheckoutRequest.ShopOrder shopOrder = new CheckoutRequestShopOrderBuilder()
            .withItem(item)
            .build();

        CheckoutRequest request = new CheckoutRequestBuilder()
            .withOrder(shopOrder)
            .random()
            .build();

        MultiOrder multiOrder = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        OrderItem result = multiOrder.getCarts().get(0)
            .getItems().iterator().next();
        Assert.assertEquals(
            new BigDecimal("123.45"),
            result.getPrices().getBuyerPriceNominal()
        );
    }

    @Test
    public void shouldConvertItemPromos() {
        MultiCart cart = new MultiCartBuilder()
            .random()
            .withOrder(new MultiCartOrderBuilder()
                .random()
                .withItem(new MultiCartOrderItemBuilder()
                    .random()
                    .withPromos(new HashSet<>(Arrays.asList(
                        new ru.yandex.market.checkout.checkouter.order.promo.ItemPromo(
                            new PromoDefinition(PromoType.MARKET_COIN, "AY2_tf7I2GRxTt5JzvUDQw",                    null, 42576L),
                            BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO),
                        new ru.yandex.market.checkout.checkouter.order.promo.ItemPromo(
                            PromoDefinition.yandexPlusPromo(),
                            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO
                        )
                    )))
                    .build()
                )
                .build()
            )
            .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .build();

        OrderOptionsResponse response = converter.convertToOptionsResponse(cart, request, genericParams,
                true, false, false, userPerks);

        List<ItemPromo> promos = response.getShops().get(0).getItems().get(0).getPromos();

        assertNotNull(promos);
        assertThat(
            promos,
            containsInAnyOrder(
                itemPromo(
                    ItemPromoMatcher.type(PromoType.MARKET_COIN),
                    ItemPromoMatcher.buyerDiscount(BigDecimal.TEN),
                    ItemPromoMatcher.subsidy(BigDecimal.ZERO),
                    ItemPromoMatcher.buyerSubsidy(BigDecimal.ZERO),
                    ItemPromoMatcher.marketPromoId("AY2_tf7I2GRxTt5JzvUDQw"),
                    ItemPromoMatcher.coinId(42576)
                ),
                itemPromo(
                    ItemPromoMatcher.type(PromoType.YANDEX_PLUS),
                    ItemPromoMatcher.buyerDiscount(BigDecimal.ONE),
                    ItemPromoMatcher.subsidy(BigDecimal.ZERO),
                    ItemPromoMatcher.buyerSubsidy(BigDecimal.ZERO)
                )
            )
        );
    }

       @Test
    public void shouldConvertOrderPromos() {
        MultiCart cart = new MultiCartBuilder()
            .random()
            .withOrder(new MultiCartOrderBuilder()
                .random()
                .withItem(new MultiCartOrderItemBuilder()
                    .random()
                    .build()
                )
                .withPromos(
                    new OrderPromoBuilder()
                        .withType(PromoType.MARKET_COIN)
                        .withMarketPromoId("12345")
                        .withCoinId(101135L)
                        .withBuyerItemsDiscount(BigDecimal.ZERO)
                        .withDeliveryDiscount(BigDecimal.TEN)
                        .build(),
                    new OrderPromoBuilder()
                        .withType(PromoType.MARKET_COUPON)
                        .withBuyerItemsDiscount(BigDecimal.TEN)
                        .withDeliveryDiscount(BigDecimal.ZERO)
                        .withPromoCode("coupon")
                        .build()
                )
                .build()
            )
            .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .build();

        OrderOptionsResponse response = converter.convertToOptionsResponse(cart, request, genericParams,
                true, false, false, userPerks);
        List<ApiOrderPromo> promos = response.getShops().get(0).getPromos();

        assertThat(promos,
            containsInAnyOrder(
                orderPromo(
                    ApiOrderPromoMatcher.type(PromoType.MARKET_COIN),
                    ApiOrderPromoMatcher.marketPromoId("12345"),
                    ApiOrderPromoMatcher.coinId(101135L),
                    ApiOrderPromoMatcher.buyerItemsDiscount(BigDecimal.ZERO),
                    ApiOrderPromoMatcher.deliveryDiscount(BigDecimal.TEN),
                    ApiOrderPromoMatcher.subsidy(null),
                    ApiOrderPromoMatcher.buyerSubsidy(null)
                ),
                orderPromo(
                    ApiOrderPromoMatcher.type(PromoType.MARKET_COUPON),
                    ApiOrderPromoMatcher.promoCode("coupon"),
                    ApiOrderPromoMatcher.buyerItemsDiscount(BigDecimal.TEN),
                    ApiOrderPromoMatcher.deliveryDiscount(BigDecimal.ZERO)
                )
            )
        );
    }

    @Test
    public void shouldProcessUnknownErrors() {
        ValidationResult errorPost = new ValidationResult(
            "NO_POST_OFFICE_FOR_POST_CODE",
            ValidationResult.Severity.ERROR
        );

        ValidationResult unknownError = new ValidationResult(
            "UNKNOWN_ERR",
            ValidationResult.Severity.ERROR
        );

        ValidationResult warningPost = new ValidationResult(
            "NO_POST_OFFICE_FOR_POST_CODE",
            ValidationResult.Severity.WARNING
        );

        ValidationResult unknownWarning = new ValidationResult(
            "UNKNOWN_ERR",
            ValidationResult.Severity.WARNING
        );

        ValidationResult orderCoinError = new ValidationResult(
                "NOT_PROCESSABLE_COIN",
                ValidationResult.Severity.ERROR
        );

        ValidationResult orderUnknownError = new ValidationResult(
                "UNKNOWN_ERR",
                ValidationResult.Severity.ERROR
        );

        ValidationResult orderUnknownWarning = new ValidationResult(
                "UNKNOWN_WARN",
                ValidationResult.Severity.WARNING
        );

        MultiCart cart = new MultiCartBuilder()
                .random()
                .withOrder(
                        new OrderBuilder()
                                .random()
                                .withItems(
                                        new OrderItemBuilder()
                                                .random()
                                                .build()
                                )
                                .withErrors(orderCoinError, orderUnknownError)
                                .withWarnings(orderUnknownWarning)
                                .build()
                )
                .withErrors(errorPost, unknownError)
                .withWarnings(warningPost, unknownWarning)
                .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .build();

        OrderOptionsResponse response = converter.convertToOptionsResponse(
            cart,
            request,
            genericParams,
            false,
            false,
            false,
            userPerks
        );

        Assert.assertTrue(
            response.getErrors().stream().anyMatch(NoPostOfficeForPostCodeError.class::isInstance)
        );
        Assert.assertTrue(
            response.getErrors().stream().anyMatch(
                x -> {
                    if (x instanceof UnknownOrderError) {
                        return "UNKNOWN_ERR".equals(((UnknownOrderError) x).getType());
                    } else {
                        return false;
                    }
                }
            )
        );

        Assert.assertTrue(
            response.getWarnings().stream().anyMatch(NoPostOfficeForPostCodeError.class::isInstance)
        );
        Assert.assertFalse(
            response.getWarnings().stream().anyMatch(
                x -> {
                    if (x instanceof UnknownOrderError) {
                        return "UNKNOWN_ERR".equals(((UnknownOrderError) x).getType());
                    } else {
                        return false;
                    }
                }
            )
        );

        OrderOptionsResponse.ShopOptions shopOptions = response.getShops().get(0);

        Assert.assertTrue(
            shopOptions.getErrors().stream()
                .anyMatch(NotProcessableCoinError.class::isInstance)
        );

        Assert.assertTrue(
                shopOptions.getErrors().stream().anyMatch(
                        x -> {
                            if (x instanceof UnknownOrderError) {
                                return "UNKNOWN_ERR".equals(((UnknownOrderError) x).getType());
                            } else {
                                return false;
                            }
                        }
                )
        );

        assertThat(
                shopOptions.getWarnings(),
                hasItem(both(isA(UnknownOrderError.class))
                        .and(hasProperty("type", equalTo("UNKNOWN_WARN"))))
        );
    }

    @Test
    public void shouldConvertShopOrderItemWithSupplierDescription() {
        OrderItem item = new OrderItemBuilder()
            .random()
            .withSupplierDescription("desc_supp_test")
            .build();

        ShopOrderItem result = converter.convertToShopOrderItem(
            item,
            null,
            0,
            genericParams,
            false
        );

        Assert.assertEquals(
            result.getSupplierDescription(),
            "desc_supp_test"
        );
    }

    @Test
    public void shouldConvertShopOrderItemWithManufactCountries() {
        Region region1 = new Region();
        region1.setId(123);
        region1.setName("Нижний Новгород");

        DeliveryLingua l1 = new DeliveryLingua();
        DeliveryLingua.Name name1 = new DeliveryLingua.Name();
        name1.setGenitive("Нижнему Новгороду");
        l1.setName(name1);
        region1.setLingua(l1);

        Region region2 = new Region();
        region2.setId(321);
        region2.setName("Казань");

        DeliveryLingua l2 = new DeliveryLingua();
        DeliveryLingua.Name name2 = new DeliveryLingua.Name();
        name2.setGenitive("Казани");
        l2.setName(name2);
        region2.setLingua(l2);

        OrderItem item = new OrderItemBuilder()
            .random()
            .withManufactCountries(
                region1,
                null,
                region2
            )
            .build();

        ShopOrderItem result = converter.convertToShopOrderItem(
            item,
            null,
            0,
            genericParams,
            false
        );

        Assert.assertThat(
            result.getManufactCountries(),
            containsInAnyOrder(
                RegionV2Matcher.regionV2(
                    RegionV2Matcher.id(123),
                    RegionV2Matcher.name("Нижний Новгород"),
                    RegionV2Matcher.nameRuGenitive("Нижнему Новгороду"),
                    RegionV2Matcher.type(RegionType.COUNTRY)
                ),
                RegionV2Matcher.regionV2(
                    RegionV2Matcher.id(321),
                    RegionV2Matcher.name("Казань"),
                    RegionV2Matcher.nameRuGenitive("Казани"),
                    RegionV2Matcher.type(RegionType.COUNTRY)
                )
            )
        );
    }

    @Test
    public void shouldConvertRegionToNullIfNull() {
        RegionV2 result = converter.convertCheckoutRegionToRegionV2(null);
        Assert.assertThat(result, nullValue(RegionV2.class));
    }

    @Test
    public void shouldConvertRegionWithOnlyId() {
        Region region = new Region();
        region.setId(213);

        RegionV2 result = converter.convertCheckoutRegionToRegionV2(region);

        Assert.assertThat(
            result,
            RegionV2Matcher.regionV2(
                RegionV2Matcher.id(213),
                RegionV2Matcher.type(RegionType.COUNTRY)
            )
        );
    }

    @Test
    public void shouldConvertRegionWithOnlyName() {
        Region region = new Region();
        region.setName("Казань");

        RegionV2 result = converter.convertCheckoutRegionToRegionV2(region);

        Assert.assertThat(
            result,
            RegionV2Matcher.regionV2(
                RegionV2Matcher.name("Казань"),
                RegionV2Matcher.type(RegionType.COUNTRY)
            )
        );
    }

    @Test
    public void shouldConvertRegionWithOnlyNameGenitive() {
        Region region = new Region();
        DeliveryLingua l = new DeliveryLingua();
        DeliveryLingua.Name name = new DeliveryLingua.Name();

        name.setGenitive("Казани");
        l.setName(name);
        region.setLingua(l);

        RegionV2 result = converter.convertCheckoutRegionToRegionV2(region);

        Assert.assertThat(
            result,
            RegionV2Matcher.regionV2(
                RegionV2Matcher.nameRuGenitive("Казани"),
                RegionV2Matcher.type(RegionType.COUNTRY)
            )
        );
    }

    @Test
    public void shouldConvertShopOrderItemWithMsku() {
        OrderItem item = new OrderItemBuilder()
            .random()
            .withMsku(123L)
            .build();


        ShopOrderItem result = converter.convertToShopOrderItem(item, null, 0, genericParams, false);

        Assert.assertThat(
            result.getSkuLink(),
            allOf(
                    startsWith("http://m.pokupki.market.yandex.ru/product/"),
                    containsString("/123")
            )
        );
    }


    @Test
    public void shouldConvertShopOrderItemWithoutMsku() {
        OrderItem item = new OrderItemBuilder()
            .random()
            .build();

        ShopOrderItem result = converter.convertToShopOrderItem(item, null, 0, genericParams, false);

        Assert.assertThat(
            result.getSkuLink(),
            isEmptyOrNullString()
        );
    }

    @Test
    public void shouldConvertShopOrderItemWithSku() {
        OrderItem item = new OrderItemBuilder()
            .random()
            .withSku("456")
            .build();

        ShopOrderItem result = converter.convertToShopOrderItem(item, null, 0, genericParams, false);
        Assert.assertThat(
            result.getSkuLink(),
            allOf(
                    startsWith("http://m.pokupki.market.yandex.ru/product/"),
                    containsString("/456")
            )
        );
    }

    @Test
    public void shouldConvertShopOrderItemWithMskuAndSku() {
        OrderItem item = new OrderItemBuilder()
            .random()
            .withMsku(666L)
            .withSku("13")
            .build();

        ShopOrderItem result = converter.convertToShopOrderItem(item, null, 0, genericParams, false);
        Assert.assertThat(
            result.getSkuLink(),
            allOf(
                    startsWith("http://m.pokupki.market.yandex.ru/product/"),
                    containsString("/666")
            )
        );
    }

    @Test
    public void shouldConvertShopOrderItemWithBundleIdAndLabel() {
        OrderItem item = new OrderItemBuilder()
            .random()
            .withBundleId("some-bundle-id")
            .withLabel("some-label")
            .build();

        ShopOrderItem result = converter.convertToShopOrderItem(item, null, 0, genericParams, false);
        assertEquals("some-bundle-id", result.getBundleId());
        assertEquals("some-label", result.getLabel());
    }

    @Test
    public void shouldConvertShopOrderItemWithWarehouseIdAndFulfilmentWarehouseId() {
        OrderItem item = new OrderItemBuilder()
                .random()
                .withWarehouseId(172)
                .withFulfilmentWarehouseId(164L)
                .build();

        ShopOrderItem result = converter.convertToShopOrderItem(item, null, 0, genericParams, false);
        assertEquals(new Integer(172), result.getWarehouseId());
        assertEquals(new Long(164), result.getFulfilmentWarehouseId());
    }

    @Test
    public void shouldUsePriceLeftForFreeDeliveryInThreshold() {
        BigDecimal priceLeft = new BigDecimal(100L);

        MultiCart multiCart = new MultiCartBuilder()
            .random()
            .withPriceLeftForFreeDelivery(priceLeft)
            .withOrder(
                new OrderBuilder()
                    .random()
                    .withItems(
                        new OrderItemBuilder()
                            .random()
                            .withFeedId(56L)
                            .withOfferId("off_id")
                            .build()
                    )
                    .build()
            )
            .withTotals(new MultiCartTotals())
            .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .withShopOrder(
                new OrderOptionsRequestShopOrderBuilder()
                    .random()
                    .addItem(
                        new OrderOptionsRequestOrderItemBuilder()
                            .random()
                            .withOfferId(new OfferId("123", "abc"))
                            .build()
                    )
                    .build()
            )
            .build();

        OfferV2 offer = new OfferV2();
        offer.setId(new OfferId("123", "abc"));
        offer.setWareMd5("123");
        offer.setFeeShow("abc");
        offer.setPrice(new OfferPriceV2("300", null, null));
        offer.setShopOfferId(new ShopOfferId(56L, "off_id"));

        Mockito
            .when(
                offerService.getOffersV2(
                    Mockito.anyCollectionOf(String.class),
                    Mockito.anyCollectionOf(Field.class),
                    Mockito.anyBoolean(),
                    Mockito.anyBoolean(),
                    Mockito.any(GenericParams.class),
                    Mockito.anyInt()
                )
            )
            .thenReturn(
                Pipelines.startWithValue(
                    ImmutableMap
                        .<String, OfferV2>builder()
                        .put("123", offer)
                        .build()
                )
            );

        OrderOptionsResponse response = converter.convertToOptionsResponse(
            multiCart,
            request,
            genericParams,
            false,
            false,
            false,
            userPerks
        );

        Assert.assertEquals(priceLeft, response.getSummary().getDelivery().getLeftToFree());
    }

    @Test
    public void shouldReturnMedicineSpecsInResponse() {
        Specs medicalSpecs = new Specs(
                new HashSet<>(Collections.singletonList(
                        new InternalSpec("vidal", Collections.singletonList(new UsedParam("J05AX13")))
                ))
        );
        Specs.fromSpecValues(new HashSet<>(Collections.singletonList("medicine")));

        MultiCart multiCart = new MultiCartBuilder()
                .random()
                .withBuyerRegionId(322)
                .withOrder(new OrderBuilder()
                        .random()
                        .withId(228L)
                        .withItems(
                                new OrderItemBuilder()
                                        .random()
                                        .withMsku(101L)
                                        .withOfferId("101L")
                                        .withMedicalSpecsInternal(medicalSpecs)
                                        .build(),
                                new OrderItemBuilder()
                                        .random()
                                        .withMsku(102L)
                                        .withOfferId("102L")
                                        .build())
                        .build())
                .build();


        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
                .random()
                .withShopOrder(
                        new OrderOptionsRequestShopOrderBuilder()
                                .random()
                                .addItem(
                                        new OrderOptionsRequestOrderItemBuilder()
                                                .random()
                                                .withOfferId(new OfferId("123", "abc"))
                                                .build()
                                )
                                .build()
                )
                .build();

        OrderOptionsResponse response = converter.convertToOptionsResponse(
                multiCart,
                request,
                genericParams,
                true,
                false,
                true,
                userPerks,
                true);

        List<ShopOrderItem> orderItems = response.getShops().get(0).getItems();

        List<InternalSpecification> internalSpec = orderItems.get(0).getSpecs().getInternal();

        assertEquals("vidal", internalSpec.get(0).getValue());
        assertEquals("spec", internalSpec.get(0).getType());
        assertEquals(1, internalSpec.get(0).getUsedParams().size());
        assertEquals("J05AX13", internalSpec.get(0).getUsedParams().get(0).getName());
        assertNull(orderItems.get(1).getSpecs());
    }

    @Test
    public void shouldReturnSummaryWeight() {
        Long firstWeight = 224L;
        Long secondWeight = 349L;
        MultiCart cart = new MultiCart();
        cart.setCarts(Lists.newArrayList(
                new Order(){{
                    setShopId(123L);

                    AdditionalCartInfo info = new AdditionalCartInfo();
                    info.setWeight(firstWeight);
                    setAdditionalCartInfo(Collections.singletonList(info));
                }},
                new Order(){{
                    setShopId(123L);

                    AdditionalCartInfo info = new AdditionalCartInfo();
                    info.setWeight(secondWeight);
                    setAdditionalCartInfo(Collections.singletonList(info));
                }}
        ));

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
                .random()
                .build();

        OrderOptionsResponse response = converter.convertToOptionsResponse(cart, request, genericParams,
                true,false, false, userPerks);

        assertEquals(BigDecimal.valueOf(0.6), response.getSummary().getWeight());
        assertThat(
                response.getShops().stream().map(so -> so.getSummary().getWeight()).collect(Collectors.toList()),
                containsInAnyOrder(BigDecimal.valueOf(0.3), BigDecimal.valueOf(0.4))
        );
    }

    @Test
    public void shouldPassBundleIdToResultOrderItems() {
        final String[] initialBundleIds = new String[] { null, "", "1" };
        final CheckoutRequest.OrderItem[] orderItems = new CheckoutRequest.OrderItem[initialBundleIds.length];
        for (int i = 0; i < initialBundleIds.length; i++) {
            orderItems[i] = new CheckoutRequestOrderItemBuilder()
                    .random()
                    .withPayload(i, String.valueOf(i), "", "")
                    .withBundleId(initialBundleIds[i])
                    .build();
        }

        final CheckoutRequest request = new CheckoutRequestBuilder()
                .random()
                .withOrder(
                        new CheckoutRequestShopOrderBuilder()
                        .random()
                        .withItems(orderItems)
                        .build()
                )
                .build();

        final MultiCart multiCart = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        final Order order = multiCart.getCarts().get(0);
        final List<String> resultBundleIds = order.getItems()
                .stream()
                .map(OfferItem::getBundleId)
                .collect(Collectors.toList());

        assertThat(resultBundleIds, containsInAnyOrder(initialBundleIds));
    }

    @Test
    public void shouldMergeOrderItemsWithSameOfferItemKeys() {
        when(offerService.getOffersV2(anyCollectionOf(String.class), anyCollectionOf(Field.class), anyBoolean(),
                anyBoolean(), any(), anyInt()))
                .thenReturn(Pipelines.startWithValue(Collections.singletonMap("ware1", createCpaOffer())));

        final Payload payload = new Payload(1, "shop1", null, null);
        final OfferId offerId = new OfferId("ware1", null);
        final String bundleId = "bundleId";
        final CheckoutRequest request = new CheckoutRequestBuilder()
                .random()
                .withOrder(
                        new CheckoutRequestShopOrderBuilder()
                                .random()
                                .withItems(
                                        new CheckoutRequestOrderItemBuilder()
                                                .random()
                                                .withPayload(payload)
                                                .withId(offerId)
                                                .withBundleId(bundleId)
                                                .build(),
                                        new CheckoutRequestOrderItemBuilder()
                                                .random()
                                                .withPayload(payload)
                                                .withId(offerId)
                                                .withBundleId(bundleId)
                                                .build()
                                )
                                .build()
                )
                .build();

        final MultiCart multiCart = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);
        final Order order = multiCart.getCarts().get(0);

        assertThat(order.getItems(), hasSize(1));
    }

    @Test
    public void shouldNotMergeOrderItemsWithDifferentOfferItemKeys() {
        when(offerService.getOffersV2(anyCollectionOf(String.class), anyCollectionOf(Field.class), anyBoolean(),
                anyBoolean(), any(), anyInt()))
                .thenReturn(Pipelines.startWithValue(
                        Stream.of("ware1", "ware2", "ware3")
                                .collect(Collectors.toMap(Function.identity(), it -> createCpaOffer()))
                ));

        final CheckoutRequest request = new CheckoutRequestBuilder()
                .random()
                .withOrder(
                        new CheckoutRequestShopOrderBuilder()
                                .random()
                                .withItems(
                                        new CheckoutRequestOrderItemBuilder()
                                                .random()
                                                .withPayload(1, "shop1", null, null)
                                                .withId(new OfferId("ware1", null))
                                                .withBundleId("bundle1")
                                                .build(),
                                        new CheckoutRequestOrderItemBuilder()
                                                .random()
                                                .withPayload(1, "shop2", null, null)
                                                .withId(new OfferId("ware2", null))
                                                .withBundleId("bundle1")
                                                .build(),
                                        new CheckoutRequestOrderItemBuilder()
                                                .random()
                                                .withPayload(1, "shop1", null, null)
                                                .withId(new OfferId("ware3", null))
                                                .withBundleId("bundle2")
                                                .build()
                                )
                                .build()
                )
                .build();

        final MultiCart multiCart = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);
        final Order order = multiCart.getCarts().get(0);

        assertThat(order.getItems(), hasSize(3));
    }

    @Test
    public void shouldConvertVerificationErrorsForNormalOrders() {

        final Order order = new OrderBuilder()
                .random()
                .withErrors(new ValidationResult("Test-Validation-Error", ValidationResult.Severity.ERROR))
                .build();
        final MultiOrder multiOrder = new MultiOrderBuilder()
                .random()
                .withOrders(order)
                .build();

        final CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
                .random()
                .withOrder(new CheckoutRequestShopOrderBuilder()
                        .random()
                        .withItem(new CheckoutRequestOrderItemBuilder()
                                .random()
                                .withId(new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW))
                                .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                                .build()
                        )
                        .build()
                )
                .build();

        final CheckoutResponse checkoutResponse =
                converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams, userPerks);

        assertThat(checkoutResponse.getShopShopOrders().size(), equalTo(1));

        final ShopOrder shopOrder = checkoutResponse.getShopShopOrders().get(0);
        assertThat(shopOrder.getErrors(), contains(instanceOf(UnknownOrderError.class)));
    }

    @Test
    public void shouldConvertVerificationErrorsForFailedOrders() {

        final MultiOrder multiOrder = new MultiOrderBuilder()
                .random()
                .withFailure(new MultiCartOrderBuilder()
                                .random()
                                .withItem(new MultiCartOrderItemBuilder()
                                        .random()
                                        .withWareMd5(TEST_OFFER_ID)
                                        .withShowInfo(TEST_OFFER_RIGHT_FEE_SHOW)
                                        .withFeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID)
                                        .build()
                                )
                                .withValidationErrors(new ValidationResult(
                                        "ACTUAL_DELIVERY_OFFER_PROBLEMS", ValidationResult.Severity.ERROR
                                ))
                                .build(),
                        OrderFailure.Code.UNKNOWN_ERROR,
                        "Error-Description"
                )
                .build();

        final CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
                .random()
                .withOrder(new CheckoutRequestShopOrderBuilder()
                        .random()
                        .withItem(new CheckoutRequestOrderItemBuilder()
                                .random()
                                .withId(new OfferId(TEST_OFFER_ID, null))
                                .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                                .build()
                        )
                        .build()
                )
                .build();

        final CheckoutResponse checkoutResponse =
                converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams, userPerks);

        assertThat(checkoutResponse.getShopShopOrders().size(), equalTo(1));

        final ShopOrder shopOrder = checkoutResponse.getShopShopOrders().get(0);
        //noinspection unchecked
        assertThat(
                shopOrder.getErrors(),
                containsInAnyOrder(instanceOf(UnknownOrderError.class), instanceOf(ActualDeliveryOfferProblemsError.class))
        );
    }

    @Test
    public void shouldConvertNearestOutlet() {
        MultiCart cart = new MultiCartBuilder()
                .random()
                .withOrder(new MultiCartOrderBuilder()
                        .random()
                        .withItem(new MultiCartOrderItemBuilder()
                                .random()
                                .build()
                        )
                        .withNearestOutlet(new NearestOutlet() {{
                            setId(123L);
                            setGps("12.3456,78.9012");
                        }})
                        .build()
                )
                .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
                .random()
                .build();

        OrderOptionsResponse response = converter.convertToOptionsResponse(cart, request, genericParams,
                true, false, false, userPerks);

        ApiNearestOutlet outlet = response.getShops().get(0).getNearestOutlet();
        assertNotNull(outlet);
        assertEquals(123L, outlet.getId().longValue());
        assertEquals("12.3456,78.9012", outlet.getGps());
    }

    @Test
    public void shouldConvertShopOfferId() {
        OrderItem item = new OrderItemBuilder()
                .random()
                .withOfferId(TEST_OFFER_ID)
                .withFeedId(TEST_SHOP_FEED_ID)
                .build();

        ShopOrderItem result = converter.convertToShopOrderItem(
                item,
                null,
                0,
                genericParams,
                false
        );

        Assert.assertEquals(
                result.getShopOfferId().getFeedId(),
                TEST_SHOP_FEED_ID
        );
        Assert.assertEquals(
                result.getShopOfferId().getOfferId(),
                TEST_OFFER_ID
        );
    }

    @Test
    public void shouldConvertCashbackEmitInfo() {
        BigDecimal total = BigDecimal.valueOf(123);

        Order order = new OrderBuilder().random().build();
        order.setCashbackEmitInfo(new CashbackEmitInfo(total, PaymentStatus.CLEARED));

        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);

        Assert.assertEquals(result.getCashbackEmitInfo().getTotalAmount(), total);
        Assert.assertEquals(result.getCashbackEmitInfo().getStatus(), CashbackPaymentStatus.CLEARED);
    }

    @Test
    public void shouldModelId() {
        Long modelId = 123L;
        Order order = new OrderBuilder()
                .random()
                .withItems(
                        new OrderItemBuilder()
                                .random()
                                .withFeedId(56L)
                                .withOfferId("off_id")
                                .withModelId(modelId)
                                .build()
                )
                .build();
        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);
        Long resultItemModelId = result.getItems()
                .stream()
                .findFirst()
                .map(ShopOrderItem::getModelId)
                .orElse(null);
        Assert.assertEquals(resultItemModelId, modelId);
    }

    @Test
    public void shouldConvertOrderWithItemsChangedPayload() {
        Order order = new OrderBuilder()
                .random()
                .withItems(new OrderItemBuilder().random().build())
                .withDelivery(new DeliveryBuilder().random()
                        .courier()
                        .shipments(Collections.singletonList(new ParcelBuilder().random().build()))
                        .build())
                .build();

        order.setChangeRequests(Collections.singletonList(
                new ChangeRequest(
                        1L,
                        order.getId(),
                        new ItemsRemovalChangeRequestPayload(order.getItems(), order.getDelivery().getParcels(), HistoryEventReason.DELIVERY_SERVICE_DELAYED),
                        ChangeRequestStatus.PROCESSING,
                        Instant.now(),
                        "message",
                        ClientRole.SYSTEM
                )
        ));

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(
                order, WorkScheduleFormat.V1, genericParams
        );
        Assert.assertTrue(persistentOrder.getChangeRequests().get(0).getPayload() instanceof ru.yandex.market.api.user.order.change.ItemsRemovalChangeRequestPayload);
    }

    @Test
    public void shouldConvertPresetsInResponse() {
        CartPresetInfo cart1 = new CartPresetInfo();
        cart1.setLabel("cart_with_available_delivery");
        cart1.setDeliveryAvailable(true);
        cart1.setTryingAvailable(true);

        CartPresetInfo cart2 = new CartPresetInfo();
        cart2.setLabel("cart_with_unavailable_delivery");
        cart2.setDeliveryAvailable(false);

        CartPresetInfo cart3 = new CartPresetInfo();
        cart3.setLabel("cart_with_unknown_delivery");

        CartPresetInfo cart4 = new CartPresetInfo();
        cart4.setDeliveryAvailable(true);

        PresetInfo preset1 = new PresetInfo();
        preset1.setPresetId("preset_full");
        preset1.setType(DeliveryType.DELIVERY);
        preset1.setCarts(Arrays.asList(cart1, cart2, cart3, cart4));

        PresetInfo preset2 = new PresetInfo();
        preset2.setPresetId("preset_without_carts");
        preset2.setType(DeliveryType.DELIVERY);

        PresetInfo preset3 = new PresetInfo();
        preset3.setPresetId("preset_with_empty_carts");
        preset3.setType(DeliveryType.DELIVERY);
        preset3.setCarts(Collections.emptyList());

        PresetInfo preset4 = new PresetInfo();
        preset4.setPresetId("preset_without_delivery_type");
        preset4.setCarts(Collections.singletonList(cart1));

        PresetInfo preset5 = new PresetInfo();
        preset5.setType(DeliveryType.DELIVERY);
        preset5.setCarts(Collections.singletonList(cart1));

        MultiCart multiCart = new MultiCartBuilder()
                .random()
                .withPresets(preset1, preset2, preset3, preset4, preset5)
                .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
                .random()
                .withShopOrder(
                        new OrderOptionsRequestShopOrderBuilder()
                                .random()
                                .build()
                )
                .build();

        OrderOptionsResponse response = converter.convertToOptionsResponse(
                multiCart,
                request,
                genericParams,
                false,
                false,
                false,
                userPerks
        );

        Assert.assertNotNull(response.getPresets());
        Assert.assertEquals(1, response.getPresets().size());

        OrderOptionsResponse.ApiPresetInfo apiPreset = response.getPresets().get(0);
        Assert.assertEquals("preset_full", apiPreset.getPresetId());
        Assert.assertEquals("DELIVERY", apiPreset.getType());

        Assert.assertNotNull(apiPreset.getShops());
        Assert.assertEquals(3, apiPreset.getShops().size());

        OrderOptionsResponse.ApiPresetInfo.ShopPresetInfo shopPresetInfo1 = apiPreset.getShops().get(0);
        Assert.assertEquals("cart_with_available_delivery", shopPresetInfo1.getLabel());
        Assert.assertTrue(shopPresetInfo1.isDeliveryAvailable());
        Assert.assertTrue(shopPresetInfo1.isTryingAvailable());

        OrderOptionsResponse.ApiPresetInfo.ShopPresetInfo shopPresetInfo2 = apiPreset.getShops().get(1);
        Assert.assertEquals("cart_with_unavailable_delivery", shopPresetInfo2.getLabel());
        Assert.assertFalse(shopPresetInfo2.isDeliveryAvailable());
        Assert.assertFalse(shopPresetInfo2.isTryingAvailable());

        OrderOptionsResponse.ApiPresetInfo.ShopPresetInfo shopPresetInfo3 = apiPreset.getShops().get(2);
        Assert.assertEquals("cart_with_unknown_delivery", shopPresetInfo3.getLabel());
        Assert.assertTrue(shopPresetInfo3.isDeliveryAvailable());
        Assert.assertFalse(shopPresetInfo3.isTryingAvailable());
    }

    @Test
    public void shouldConvertPresetsInRequest() {
        Location location = new Location();
        location.setLatitude("55.733969");
        location.setLongitude("37.587093");

        RoomAddress address = new RoomAddress();
        address.setRegionId(213L);
        address.setPostCode("119021");
        address.setCountry("Россия");
        address.setCity("Москва");
        address.setStreet("Льва Толстого");
        address.setHouse("16");
        address.setLocation(location);

        OrderOptionsRequest.DeliveryPreset deliveryPreset = new OrderOptionsRequest.DeliveryPreset();
        deliveryPreset.setPresetId("preset_delivery");
        deliveryPreset.setRegionId(213);
        deliveryPreset.setBuyerAddress(address);

        OrderOptionsRequest.PostPreset postPreset = new OrderOptionsRequest.PostPreset();
        postPreset.setPresetId("preset_post");
        postPreset.setRegionId(213);
        postPreset.setBuyerAddress(address);

        OrderOptionsRequest.PickupPreset pickupPreset = new OrderOptionsRequest.PickupPreset();
        pickupPreset.setPresetId("preset_pickup");
        pickupPreset.setRegionId(213);
        pickupPreset.setOutletId(1234567890);

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
                .random()
                .withShopOrder(
                        new OrderOptionsRequestShopOrderBuilder()
                                .random()
                                .build()
                )
                .withPresets(deliveryPreset, postPreset, pickupPreset)
                .build();

        MultiCart multiCart = converter.convertToMultiCart(request, genericParams);

        Assert.assertNotNull(multiCart.getPresets());
        Assert.assertEquals(3, multiCart.getPresets().size());

        PresetInfo deliveryPresetInfo = multiCart.getPresets().get(0);
        Assert.assertEquals("preset_delivery", deliveryPresetInfo.getPresetId());
        Assert.assertEquals(DeliveryType.DELIVERY, deliveryPresetInfo.getType());
        Assert.assertEquals(213L, (long) deliveryPresetInfo.getRegionId());
        Assert.assertEquals("119021", deliveryPresetInfo.getBuyerAddress().getPostcode());
        Assert.assertEquals("Россия", deliveryPresetInfo.getBuyerAddress().getCountry());
        Assert.assertEquals("Москва", deliveryPresetInfo.getBuyerAddress().getCity());
        Assert.assertEquals("Льва Толстого", deliveryPresetInfo.getBuyerAddress().getStreet());
        Assert.assertEquals("16", deliveryPresetInfo.getBuyerAddress().getHouse());
        Assert.assertEquals("37.587093,55.733969", deliveryPresetInfo.getBuyerAddress().getGps());

        PresetInfo postPresetInfo = multiCart.getPresets().get(1);
        Assert.assertEquals("preset_post", postPresetInfo.getPresetId());
        Assert.assertEquals(DeliveryType.POST, postPresetInfo.getType());
        Assert.assertEquals(213L, (long) postPresetInfo.getRegionId());
        Assert.assertEquals("119021", postPresetInfo.getBuyerAddress().getPostcode());
        Assert.assertEquals("Россия", postPresetInfo.getBuyerAddress().getCountry());
        Assert.assertEquals("Москва", postPresetInfo.getBuyerAddress().getCity());
        Assert.assertEquals("Льва Толстого", postPresetInfo.getBuyerAddress().getStreet());
        Assert.assertEquals("16", postPresetInfo.getBuyerAddress().getHouse());
        Assert.assertEquals("37.587093,55.733969", postPresetInfo.getBuyerAddress().getGps());

        PresetInfo pickupPresetInfo = multiCart.getPresets().get(2);
        Assert.assertEquals("preset_pickup", pickupPresetInfo.getPresetId());
        Assert.assertEquals(DeliveryType.PICKUP, pickupPresetInfo.getType());
        Assert.assertEquals(213L, (long) pickupPresetInfo.getRegionId());
        Assert.assertEquals(1234567890L, (long) pickupPresetInfo.getOutletId());
    }

    @Test
    public void testWelcomeCashbackHasRequiredPerk() {
        CashbackThreshold cashbackThreshold = new CashbackThreshold(
                "promoKey",
                Collections.singleton(ru.yandex.market.loyalty.api.model.perk.PerkType.WELCOME_CASHBACK),
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.TEN,
                1
        );
        MultiCart multiCart = new MultiCartBuilder()
                .random()
                .withCashback(
                        new Cashback(
                                new CashbackOptions(
                                        "promoKey",
                                        1,
                                        BigDecimal.TEN,
                                        Collections.emptyMap(),
                                        Collections.emptyList(),
                                        CashbackPermision.ALLOWED,
                                        null,
                                        Collections.emptyList(),
                                        Collections.singletonList(cashbackThreshold),
                                        null
                                ),
                                null
                        )
                )
                .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
                .random()
                .build();

        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(PerkType.WELCOME_CASHBACK.getId());
        WelcomeCashback welcomeCashback = converter.convertToOptionsResponse(
                multiCart,
                request,
                genericParams,
                true,
                false,
                false,
                Collections.singletonList(perkStatus)
        ).getCashback().getWelcomeCashback();

        Assert.assertEquals(cashbackThreshold.getPromoKey(), welcomeCashback.getPromoKey());
        Assert.assertEquals(cashbackThreshold.getMinMultiCartTotal(), welcomeCashback.getMinMultiCartTotal());
        Assert.assertEquals(cashbackThreshold.getRemainingMultiCartTotal(), welcomeCashback.getRemainingMultiCartTotal());
        Assert.assertEquals(cashbackThreshold.getAmount(), welcomeCashback.getAmount());
    }

    @Test
    public void testWelcomeCashbackHasNoRequiredPerk() {
        CashbackThreshold cashbackThreshold = new CashbackThreshold(
                "promoKey",
                Collections.singleton(ru.yandex.market.loyalty.api.model.perk.PerkType.WELCOME_CASHBACK),
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.TEN,
                1
        );
        MultiCart multiCart = new MultiCartBuilder()
                .random()
                .withCashback(
                        new Cashback(
                                new CashbackOptions(
                                        "promoKey",
                                        1,
                                        BigDecimal.TEN,
                                        Collections.emptyMap(),
                                        Collections.emptyList(),
                                        CashbackPermision.ALLOWED,
                                        null,
                                        Collections.emptyList(),
                                        Collections.singletonList(cashbackThreshold),
                                        null
                                ),
                                null
                        )
                )
                .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
                .random()
                .build();

        WelcomeCashback welcomeCashback = converter.convertToOptionsResponse(
                multiCart,
                request,
                genericParams,
                true,
                false,
                false,
                userPerks
        ).getCashback().getWelcomeCashback();

        Assert.assertNull(welcomeCashback);
    }

    @Test
    public void testWelcomeCashbackHasNoMatchedThreshold() {
        CashbackThreshold cashbackThreshold = new CashbackThreshold(
                "promoKey",
                Collections.singleton(ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_PLUS),
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.TEN,
                1
        );
        MultiCart multiCart = new MultiCartBuilder()
                .random()
                .withCashback(
                        new Cashback(
                                new CashbackOptions(
                                        "promoKey",
                                        1,
                                        BigDecimal.TEN,
                                        Collections.emptyMap(),
                                        Collections.emptyList(),
                                        CashbackPermision.ALLOWED,
                                        null,
                                        Collections.emptyList(),
                                        Collections.singletonList(cashbackThreshold),
                                        null
                                ),
                                null
                        )
                )
                .build();

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
                .random()
                .build();

        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(PerkType.WELCOME_CASHBACK.getId());
        WelcomeCashback welcomeCashback = converter.convertToOptionsResponse(
                multiCart,
                request,
                genericParams,
                true,
                false,
                false,
                Collections.singletonList(perkStatus)
        ).getCashback().getWelcomeCashback();

        Assert.assertNull(welcomeCashback);
    }

    @Test
    public void shouldConvertToMultiOrderWithBnplInfo() {
        GenericParams customGenericParams = new GenericParamsBuilder(genericParams)
                .setBnplInfoSelected(true)
                .build();
        CheckoutRequest.OrderItem item = new CheckoutRequestOrderItemBuilder()
                .withId(new OfferId("abc", null))
                .withBuyerPriceNominal(new BigDecimal("123.45"))
                .random()
                .build();

        CheckoutRequest.ShopOrder shopOrder = new CheckoutRequestShopOrderBuilder()
                .withItem(item)
                .build();

        CheckoutRequest request = new CheckoutRequestBuilder()
                .withOrder(shopOrder)
                .random()
                .build();

        MultiOrder multiOrder = converter.convertToMultiOrder(request, Collections.emptyList(), customGenericParams);

        OrderItem result = multiOrder.getCarts().get(0)
                .getItems().iterator().next();
        Assert.assertEquals(
                new BigDecimal("123.45"),
                result.getPrices().getBuyerPriceNominal()
        );

        BnplInfo bnplInfo = multiOrder.getBnplInfo();
        assertFalse(bnplInfo.isAvailable());
        assertTrue(bnplInfo.isSelected());
    }

    @Test
    public void shouldConvertToMultiCartWithBnplInfo() {
        OrderOptionsRequest optionsRequest = new OrderOptionsRequestBuilder()
                .withShopOrder(
                        new OrderOptionsRequestShopOrderBuilder()
                                .random()
                                .withRegionId(TEST_USER_REGION_ID)
                                .build()
                )
                .withRegionId(TEST_SECOND_USER_REGION_ID)
                .build();

        GenericParams customGenericParams = new GenericParamsBuilder(genericParams)
                .setBnplInfoSelected(true)
                .build();
        MultiCart multiCart = converter.convertToMultiCart(optionsRequest, customGenericParams);

        BnplInfo bnplInfo = multiCart.getBnplInfo();
        assertFalse(bnplInfo.isAvailable());
        assertTrue(bnplInfo.isSelected());
    }

    @Test
    public void shouldConvertBnplOrderProperty() {
        String bnplOfferName = "BNPL";
        OrderItem bnplOrderItem = new OrderItemBuilder()
                .random()
                .withOfferId(bnplOfferName)
                .withBnpl(true)
                .build();
        OrderItem nonBnplOrderItem = new OrderItemBuilder()
                .random()
                .withBnpl(false)
                .build();
        Order order = new OrderBuilder().random().build();
        order.setBnpl(true);
        order.setItems(Arrays.asList(bnplOrderItem, nonBnplOrderItem));
        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);
        Assert.assertTrue(result.isBnpl());

        order.setBnpl(false);
        result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);
        Assert.assertFalse(result.isBnpl());

        result.getItems().forEach(item -> {
            if (bnplOfferName.equals(item.getShopOfferId().getOfferId())) {
                Assert.assertTrue(item.isBnpl());
            } else {
                Assert.assertFalse(item.isBnpl());
            }
        });
    }

    @Test
    public void shouldConvertPaymentSubmethod() {
        Order order = new OrderBuilder().random().build();
        order.setPaymentSubmethod(PaymentSubmethod.STATION_SUBSCRIPTION);

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1,
                genericParams);

        assertEquals(PaymentSubmethod.STATION_SUBSCRIPTION, persistentOrder.getPaymentSubmethod());

    }

    @Test
    public void shouldConvertPlusSubscriptionOrderProperty() {
        MultiOrder multiOrder = new MultiOrderBuilder()
                .random()
                .withOrders(new MultiCartOrderBuilder()
                        .random()
                        .withItem(new MultiCartOrderItemBuilder()
                                .random()
                                .withShowInfo(TEST_OFFER_RIGHT_FEE_SHOW)
                                .withFeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID)
                                .withWareMd5(TEST_OFFER_ID)
                                .build()
                        )
                        .build()
                )
                .build();
        Map<String, StationWebLegalInfoDataItem> items = new HashMap<>();
        items.put("group", new StationWebLegalInfoDataItem(
                "link",
                "web_text",
                "web_link"
        ));
        StationSubscriptionInfo stationSubscriptionInfo = new StationSubscriptionInfo(
                new Price(BigDecimal.valueOf(399.0), "RUR"),
                Price.ZERO_RUB_PRICE,
                24,
                "Month",
                Arrays.asList(
                        new StationLegalInfo(
                                new StationWebLegalInfo(
                                        Arrays.asList("group"),
                                        new StationWebLegalInfoData(
                                                "web_text",
                                                items
                                        )
                                ),
                                new StationMobileLegalInfo("mobile_text", "mobile_url")
                        )

                )
        );
        multiOrder.setPlusSubscription(stationSubscriptionInfo);

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
                .random()
                .withOrder(new CheckoutRequestShopOrderBuilder()
                        .random()
                        .withItem(new CheckoutRequestOrderItemBuilder()
                                .random()
                                .withId(new OfferId(TEST_OFFER_ID, null))
                                .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                                .build()
                        )
                        .build()
                )
                .build();

        CheckoutResponse response = converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams,
                userPerks);

        PlusSubscriptionInformation plusSubscription = response.getPlusSubscription();
        assertNotNull(plusSubscription);
        assertEquals(plusSubscription.getStationPrice().getValue().doubleValue(), 399.0, 0.001);
        assertEquals(plusSubscription.getStationPrice().getCurrency(), "RUR");
        assertEquals(plusSubscription.getSubscriptionPrice().getValue().doubleValue(), 0.0, 0.001);
        assertEquals(plusSubscription.getSubscriptionPrice().getCurrency(), "RUR");
        assertEquals(plusSubscription.getPayDurationCount(), 24);
        assertEquals(plusSubscription.getPayDurationType(), "Month");
        assertEquals(plusSubscription.getLegalInfos().size(), 1);

        LegalInfo legalInfo = plusSubscription.getLegalInfos().get(0);
        assertEquals(legalInfo.getWeb().getOfferNamesGroup().get(0), "group");
        assertEquals(legalInfo.getWeb().getLegalInfo().getText(), "web_text");

        WebLegalInfoDataItem webLegalInfoDataItem = legalInfo.getWeb().getLegalInfo().getItems().get("group");
        assertEquals(webLegalInfoDataItem.getLink(), "web_link");
        assertEquals(webLegalInfoDataItem.getType(), "link");
        assertEquals(webLegalInfoDataItem.getText(), "web_text");

        assertEquals(legalInfo.getMobile().getText(), "mobile_text");
        assertEquals(legalInfo.getMobile().getUrl(), "mobile_url");

    }

    @Test
    public void shouldConvertPlusSubscriptionOrderPropertyWithoutLegals() {
        MultiOrder multiOrder = new MultiOrderBuilder()
                .random()
                .withOrders(new MultiCartOrderBuilder()
                        .random()
                        .withItem(new MultiCartOrderItemBuilder()
                                .random()
                                .withShowInfo(TEST_OFFER_RIGHT_FEE_SHOW)
                                .withFeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID)
                                .withWareMd5(TEST_OFFER_ID)
                                .build()
                        )
                        .build()
                )
                .build();

        StationSubscriptionInfo stationSubscriptionInfo = new StationSubscriptionInfo(
                new Price(BigDecimal.valueOf(399.0), "RUR"),
                Price.ZERO_RUB_PRICE,
                24,
                "Month",
                null
        );
        multiOrder.setPlusSubscription(stationSubscriptionInfo);

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
                .random()
                .withOrder(new CheckoutRequestShopOrderBuilder()
                        .random()
                        .withItem(new CheckoutRequestOrderItemBuilder()
                                .random()
                                .withId(new OfferId(TEST_OFFER_ID, null))
                                .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                                .build()
                        )
                        .build()
                )
                .build();

        CheckoutResponse response = converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams,
                userPerks);

        PlusSubscriptionInformation plusSubscription = response.getPlusSubscription();
        assertNotNull(plusSubscription);
        assertEquals(plusSubscription.getStationPrice().getValue().doubleValue(), 399.0, 0.001);
        assertEquals(plusSubscription.getStationPrice().getCurrency(), "RUR");
        assertEquals(plusSubscription.getSubscriptionPrice().getValue().doubleValue(), 0.0, 0.001);
        assertEquals(plusSubscription.getSubscriptionPrice().getCurrency(), "RUR");
        assertEquals(plusSubscription.getPayDurationCount(), 24);
        assertEquals(plusSubscription.getPayDurationType(), "Month");
        assertTrue(plusSubscription.getLegalInfos().isEmpty());
    }

    @Test
    public void shouldConvertPlusSubscriptionOrderPropertyWithoutWebLegal() {
        MultiOrder multiOrder = new MultiOrderBuilder()
                .random()
                .withOrders(new MultiCartOrderBuilder()
                        .random()
                        .withItem(new MultiCartOrderItemBuilder()
                                .random()
                                .withShowInfo(TEST_OFFER_RIGHT_FEE_SHOW)
                                .withFeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID)
                                .withWareMd5(TEST_OFFER_ID)
                                .build()
                        )
                        .build()
                )
                .build();

        StationSubscriptionInfo stationSubscriptionInfo = new StationSubscriptionInfo(
                new Price(BigDecimal.valueOf(399.0), "RUR"),
                Price.ZERO_RUB_PRICE,
                24,
                "Month",
                Arrays.asList(
                        new StationLegalInfo(
                                null,
                                null
                        )

                )
        );
        multiOrder.setPlusSubscription(stationSubscriptionInfo);

        CheckoutRequest checkoutRequest = new CheckoutRequestBuilder()
                .random()
                .withOrder(new CheckoutRequestShopOrderBuilder()
                        .random()
                        .withItem(new CheckoutRequestOrderItemBuilder()
                                .random()
                                .withId(new OfferId(TEST_OFFER_ID, null))
                                .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
                                .build()
                        )
                        .build()
                )
                .build();

        CheckoutResponse response = converter.convertToCheckoutResponse(multiOrder, checkoutRequest, genericParams,
                userPerks);

        PlusSubscriptionInformation plusSubscription = response.getPlusSubscription();
        assertNotNull(plusSubscription);
        assertEquals(plusSubscription.getStationPrice().getValue().doubleValue(), 399.0, 0.001);
        assertEquals(plusSubscription.getStationPrice().getCurrency(), "RUR");
        assertEquals(plusSubscription.getSubscriptionPrice().getValue().doubleValue(), 0.0, 0.001);
        assertEquals(plusSubscription.getSubscriptionPrice().getCurrency(), "RUR");
        assertEquals(plusSubscription.getPayDurationCount(), 24);
        assertEquals(plusSubscription.getPayDurationType(), "Month");
        assertEquals(plusSubscription.getLegalInfos().size(), 1);

        LegalInfo legalInfo = plusSubscription.getLegalInfos().get(0);

        assertNull(legalInfo.getWeb());
        assertNull(legalInfo.getMobile());

    }

    @Test
    public void shouldConvertLeaveAtTheDoor() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date()));
        delivery.setLeaveAtTheDoor(true);

        Order order = getOrder(delivery);

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);
        Boolean convertedLeaveAtTheDoor = persistentOrder.getDeliveryOption().getLeaveAtTheDoor();

        assertTrue(convertedLeaveAtTheDoor);
    }

    @Test
    public void shouldConvertIsTryingAvailable() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date()));
        delivery.setTryingAvailable(true);

        Order order = getOrder(delivery);

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);

        assertTrue(persistentOrder.getDeliveryOption().getTryingAvailable());
    }

    @Test
    public void shouldSetIsTryingAvailableAsDefault() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date()));
        //delivery.setTryingAvailable(true);

        Order order = getOrder(delivery);

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);

        assertFalse(persistentOrder.getDeliveryOption().getTryingAvailable());
    }

    @Test
    public void shouldConvertDeliveryCustomizers() {
        Delivery delivery = new Delivery();
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date()));
        delivery.setType(DeliveryType.DELIVERY);

        List<DeliveryCustomizer> customizers = new ArrayList<>();
        customizers.add(new DeliveryCustomizer("key", "name", "type"));
        customizers.add(new DeliveryCustomizer("key1", "name1", "type1"));
        delivery.setCustomizers(customizers);

        Order order = getOrder(delivery);

        PersistentOrder persistentOrder = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);
        assertThat(persistentOrder.getDeliveryOption().getCustomizers(), hasSize(2));
        assertThat(persistentOrder.getDeliveryOption().getCustomizers(), containsInAnyOrder(
                new DeliveryCustomizer("key", "name", "type"),
                new DeliveryCustomizer("key1", "name1", "type1")
        ));

    }

    @Test
    public void shouldConvertWasSplitByCombinator() {
        CheckoutRequest.ShopOrder shopOrder = new CheckoutRequest.ShopOrder();
        shopOrder.setWasSplitByCombinator(true);
        CheckoutRequest request = new CheckoutRequestBuilder().withOrder(shopOrder).build();

        MultiOrder multiOrder = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        String property = multiOrder.getOrders().get(0).getProperty(CheckouterOrderConverter.PROPERTY_WAS_SPLIT_BY_COMBINATOR);
        assertEquals(property, "true");
    }

    @Test
    public void shouldConvertIsPartialDeliveryAvailable() {
        CheckoutRequest.ShopOrder shopOrder = new CheckoutRequest.ShopOrder();
        shopOrder.setPartialDeliveryAvailable(true);
        CheckoutRequest request = new CheckoutRequestBuilder().withOrder(shopOrder).build();

        MultiOrder multiOrder = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        String property = multiOrder.getOrders().get(0).getProperty(CheckouterOrderConverter.PROPERTY_IS_PARTIAL_CHECKOUT_AVAILABLE);
        assertEquals(property, "true");
    }

    @Test
    public void shouldConvertInstallmentsInfo() {
        CheckoutRequest.ShopOrder shopOrder = new CheckoutRequestShopOrderBuilder()
                .build();
        CheckoutRequest request = new CheckoutRequestBuilder()
                .withOrder(shopOrder)
                .random()
                .build();
        InstallmentsInformation requestInstallment = getInstallmentsInformation();
        request.setInstallmentsInformation(requestInstallment);

        MultiOrder multiOrder = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

       assertNotNull(multiOrder.getInstallmentsInfo());
       assertEquals(requestInstallment.getSelected().getTerm(), multiOrder.getInstallmentsInfo().getSelected().getTerm());
    }

    private InstallmentsInformation getInstallmentsInformation() {
        InstallmentsInformation installmentsInformation = new InstallmentsInformation();
        InstallmentsOption option = new InstallmentsOption();
        MonthlyPayment monthlyPayment = new MonthlyPayment();
        monthlyPayment.setCurrency(ru.yandex.market.api.common.currency.Currency.RUR);
        monthlyPayment.setValue("363.5");
        option.setMonthlyPayment(monthlyPayment);
        option.setTerm("6");
        installmentsInformation.setSelected(option);
        return installmentsInformation;
    }

    @Test
    public void shouldConvertCreditInformation() {
        CheckoutRequest.ShopOrder shopOrder = new CheckoutRequestShopOrderBuilder().build();
        CheckoutRequest request = new CheckoutRequestBuilder()
                .withOrder(shopOrder)
                .random()
                .build();

        CreditInformation creditInformation = CapiCreditInformationGeneratorHelper.create();
        request.setCreditInformation(creditInformation);
        MultiOrder multiOrder = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        assertNotNull(multiOrder.getCreditInformation());
        assertEquals(creditInformation.getSelected().getTerm(), multiOrder.getCreditInformation().getSelected().getTerm());
    }

    @Test
    public void shouldConvertOutletDeliveryTimeIntervals() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date()));

        OutletDeliveryTimeInterval firstInterval = new OutletDeliveryTimeInterval(1234L, LocalTime.parse("10:00"), LocalTime.parse("10:00"));
        OutletDeliveryTimeInterval secondInterval = new OutletDeliveryTimeInterval(5678L, LocalTime.parse("18:00"), LocalTime.parse("18:00"));
        List<OutletDeliveryTimeInterval> outletDeliveryTimeIntervals = Arrays.asList(firstInterval, secondInterval);
        delivery.setOutletTimeIntervals(outletDeliveryTimeIntervals);

        Order order = getOrder(delivery);

        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);
        OutletDeliveryOption option = (OutletDeliveryOption) result.getDeliveryOption();

        assertTrue(result.getDeliveryOption() instanceof OutletDeliveryOption);
        assertEquals(option.getOutletTimeIntervals().size(), 2);
        assertTrue(option.getOutletTimeIntervals().contains(firstInterval));
        assertTrue(option.getOutletTimeIntervals().contains(secondInterval));
    }

    @Test
    public void shouldConvertPaymentSystemForMultiCartForPrepaidPaymentMethods() {
        OrderOptionsRequest optionsRequest = new OrderOptionsRequest();
        optionsRequest.setShopOrders(new ArrayList<OrderOptionsRequest.ShopOrder>(2) {
            {
                OrderOptionsRequest.ShopOrder prepaidOrder = new OrderOptionsRequest.ShopOrder();
                prepaidOrder.setPaymentMethod(PaymentMethod.YANDEX);
                OrderOptionsRequest.ShopOrder postpaidOrder = new OrderOptionsRequest.ShopOrder();
                postpaidOrder.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
                add(prepaidOrder);
                add(postpaidOrder);
            }
        });
        optionsRequest.setPaymentSystem("paymentSystem");

        MultiCart multiCart = converter.convertToMultiCart(optionsRequest, genericParams);

        assertEquals("paymentSystem", multiCart.getCarts().get(0).getPaymentSystem());
        assertNull("paymentSystem", multiCart.getCarts().get(1).getPaymentSystem());
    }

    @Test
    public void shouldConvertPaymentSystemForMultiOrderForPrepaidPaymentMethods() {
        CheckoutRequest.ShopOrder prepaidShopOrder = new CheckoutRequest.ShopOrder();
        prepaidShopOrder.setPaymentMethod(PaymentMethod.YANDEX);
        CheckoutRequest.ShopOrder postpaidShopOrder = new CheckoutRequest.ShopOrder();
        postpaidShopOrder.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        CheckoutRequest request = new CheckoutRequestBuilder()
                .withOrders(prepaidShopOrder, postpaidShopOrder)
                .withPaymentSystem("paymentSystem")
                .build();

        MultiOrder multiOrder = converter.convertToMultiOrder(request, Collections.emptyList(), genericParams);

        String prepaidPaymentSystem = multiOrder.getOrders().get(0).getPaymentSystem();
        assertEquals("paymentSystem", prepaidPaymentSystem);

        String postpaidPaymentSystem = multiOrder.getOrders().get(1).getPaymentSystem();
        assertNull(postpaidPaymentSystem);
    }

    @Test
    public void shouldConvertGrouping() {
        ConsolidatedCarts consolidatedCarts = new ConsolidatedCarts();
        String label = "label1";
        LocalDate date = LocalDate.of(2022, 03, 22);
        consolidatedCarts.setCartLables(Arrays.asList(label));
        consolidatedCarts.setAvailableDates(Arrays.asList(date));
        MultiCart multiCart = new MultiCartBuilder()
                .random()
                .withOrder(
                        new OrderBuilder()
                                .random()
                                .withItems(new OrderItemBuilder()
                                        .random()
                                        .build())
                                .build()
                )
                .withGrouping(consolidatedCarts)
                .build();

        OrderOptionsRequest orderOptionsRequest = new OrderOptionsRequestBuilder().random().build();

        OrderOptionsResponse orderOptionsResponse =
                converter.convertToOptionsResponse(multiCart, orderOptionsRequest, genericParams,
                        false, false, false, userPerks);

        assertEquals(1, orderOptionsResponse.getGrouping().size());
        ConsolidatedCarts consolidatedCartsActual =
                orderOptionsResponse.getGrouping().iterator().next();
        assertEquals(1, consolidatedCartsActual.getAvailableDates().size());
        assertEquals(1, consolidatedCartsActual.getCartLables().size());
        assertEquals(date, consolidatedCartsActual.getAvailableDates().iterator().next());
        assertEquals(label, consolidatedCartsActual.getCartLables().iterator().next());
    }

    @Test
    public void shouldConvertExtraCharge() {
        ExtraCharge extraCharge = new ExtraCharge(
                BigDecimal.valueOf(123),
                BigDecimal.valueOf(-3),
                Collections.singletonList("REASON_CODE"));
        Delivery delivery = new MultiCartOrderDeliveryBuilder(DeliveryType.DELIVERY).random().build();
        delivery.setExtraCharge(extraCharge);
        Order order = new OrderBuilder().random().build();
        order.setDeliveryOptions(Collections.singletonList(delivery));
        MultiCart multiCart = new MultiCartBuilder().random().withOrder(order).build();
        OrderOptionsRequest orderOptionsRequest = new OrderOptionsRequestBuilder().random().build();

        OrderOptionsResponse orderOptionsResponse =
                converter.convertToOptionsResponse(multiCart, orderOptionsRequest, genericParams,
                        false, false, false, userPerks);

        assertEquals(1, orderOptionsResponse.getShops().size());
        OrderOptionsResponse.ShopOptions shop = orderOptionsResponse.getShops().get(0);
        assertEquals(1, shop.getDeliveryOptions().size());
        DeliveryOption deliveryOption = shop.getDeliveryOptions().get(0);
        assertEquals(extraCharge, deliveryOption.getExtraCharge());
    }

    @Test
    public void convertCancelPolicyTest() {
        OrderCancelPolicy orderCancelPolicy = new OrderCancelPolicy();
        orderCancelPolicy.setDaysForCancel(4);
        orderCancelPolicy.setReason("123");
        orderCancelPolicy.setType(CancelType.TIME_LIMIT);
        orderCancelPolicy.setTimeUntilExpiration(LocalDate.now());
        orderCancelPolicy.setNotAvailable(Boolean.TRUE);

        Order order = new OrderBuilder().random().build();
        order.setOrderCancelPolicy(orderCancelPolicy);
        PersistentOrder result = converter.convertToPersistentOrder(order, WorkScheduleFormat.V1, genericParams);
        CancelPolicy cancelPolicy = result.getCancelPolicy();
        Assert.assertNotNull(cancelPolicy);

        assertEquals(orderCancelPolicy.getDaysForCancel(), cancelPolicy.getDaysForCancel());
        assertEquals(orderCancelPolicy.getType(), cancelPolicy.getCancelType());
        assertEquals(orderCancelPolicy.getReason(), cancelPolicy.getReason());
        assertEquals(orderCancelPolicy.getNotAvailable(), cancelPolicy.getNotAvailable());
        assertEquals(orderCancelPolicy.getTimeUntilExpiration(), cancelPolicy.getTimeUntilExpiration());
    }

    private OfferV2 createCpaOffer() {
        final OfferV2 result = new OfferV2();
        result.setCpa(true);
        result.setId(new OfferId(null, null));
        return result;
    }

    @NotNull
    private Order prepareOrder() {
        long outletId = 1234;
        Order order = new OrderBuilder().random().build();
        order.setDelivery(new DeliveryBuilder()
            .random()
            .outletId(outletId)
            .outlet(new ShopOutletBuilder()
                .phones(Collections.emptyList())
                .outletId(outletId))
            .build());

        OutletV2 outlet = new OutletV2();
        outlet.setId(String.valueOf(outletId));
        outlet.setPhones(Collections.emptyList());
        AddressV2 address = new AddressV2();
        address.setRegionId(213);
        address.setGeoPoint(new GeoPointV2(new Geo(213, "0", "0", 0d)));
        outlet.setAddress(address);
        outlet.setSchedule(
            Arrays.asList(
                new OpenHoursV2("1", "4", "10:00", "20:00",
                    Arrays.asList(
                        new BreakIntervalV2("12:00", "13:00"),
                        new BreakIntervalV2("18:00", "19:00")
                    )
                ),
                new OpenHoursV2("5", "6", "15:00", "20:00")
            )
        );

        when(reportClient.getOutletsByIds(String.valueOf(outletId)))
            .thenReturn(
                Pipelines.startWithValue(
                    Collections.singletonList(outlet)
                )
            );
        return order;
    }

    private Order getOrder(Delivery delivery) {
        return new OrderBuilder()
            .random()
            .withDelivery(delivery)
            .withItems(
                new OrderItemBuilder()
                    .random()
                    .withPicture("//avatar/")
                    .build()
            )
            .build();
    }

    private static <T> Matcher<T> cast(Matcher<?> matcher) {
        return (Matcher<T>) matcher;
    }
}
