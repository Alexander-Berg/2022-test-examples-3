package ru.yandex.market.ocrm.module.yadelivery;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.model.enums.BalanceOperationType;
import ru.yandex.market.logistics.lom.model.enums.BalancePaymentStatus;
import ru.yandex.market.logistics.lom.model.enums.BillingProductType;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.enums.CancellationSegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.CargoType;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.ContactType;
import ru.yandex.market.logistics.lom.model.enums.CourierType;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.lom.model.enums.FileType;
import ru.yandex.market.logistics.lom.model.enums.ItemChangeReason;
import ru.yandex.market.logistics.lom.model.enums.ItemUnitOperationType;
import ru.yandex.market.logistics.lom.model.enums.LocationType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PageSize;
import ru.yandex.market.logistics.lom.model.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.enums.PointType;
import ru.yandex.market.logistics.lom.model.enums.RegistryStatus;
import ru.yandex.market.logistics.lom.model.enums.RoutePointServiceType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.ServiceCodeName;
import ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.model.enums.TaxSystem;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.logistics.lom.model.enums.tags.OrderTag;
import ru.yandex.market.logistics.lom.model.enums.tags.WaybillSegmentTag;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * При изменении Enum-ов в LOM нужно, чтобы в production ЕО также был обновлен клиент,
 * иначе на стороне ЕО могут сломаться карточки логистических заказов и чтение эвентов LOM из LB.
 */
public class LomEnumsTest {

    private static final String MESSAGE = "В LOM изменился enum: %s, уведомите, пожалуйста, дежурного ЕО: " +
            "https://abc.yandex-team.ru/services/lilucrm-operator-window/duty/";

    /*
     Если в lom появятся новые enum-ы в пакете ru.yandex.market.logistics.lom.model.enums,
     можно запустить закомментрированный тест и заменить все существующие тесты на вывод из консоли

    @Test
    public void generateTests() {
        String template = """
                @Test
                public void test%1$s() {
                    assertEquals(
                            EnumSet.of(
                                    %2$s
                            ),
                            EnumSet.allOf(%1$s.class),
                            MESSAGE.formatted(%1$s.class)
                    );
                }
                            """;
        Reflections reflections = new Reflections("ru.yandex.market.logistics.lom.model.enums");
        String tests = reflections.getSubTypesOf(Enum.class)
                .stream()
                .sorted(Comparator.comparing(Class::getSimpleName))
                .map(enumClass -> {
                    Object values = EnumSet.allOf(enumClass).stream()
                            .sorted(Comparator.comparing(Object::toString))
                            .map(x -> "%s.%s".formatted(enumClass.getSimpleName(), x.toString()))
                            .collect(Collectors.joining(",\n"));
                    return template.formatted(enumClass.getSimpleName(), values);
                })
                .collect(Collectors.joining("\n"));
        System.out.println(tests);
    }
    */

    @Test
    public void testBalanceOperationType() {
        assertEquals(
                EnumSet.of(
                        BalanceOperationType.PAYMENT,
                        BalanceOperationType.RETURN
                ),
                EnumSet.allOf(BalanceOperationType.class),
                MESSAGE.formatted(BalanceOperationType.class)
        );
    }

    @Test
    public void testBalancePaymentStatus() {
        assertEquals(
                EnumSet.of(
                        BalancePaymentStatus.BASKET_CREATED,
                        BalancePaymentStatus.BASKET_CREATION_FAILURE,
                        BalancePaymentStatus.BASKET_PAYMENT_COMPLETE,
                        BalancePaymentStatus.BASKET_PAYMENT_ERROR,
                        BalancePaymentStatus.BASKET_PAYMENT_FAILURE,
                        BalancePaymentStatus.BASKET_PAYMENT_STARTED,
                        BalancePaymentStatus.ORDER_CREATED,
                        BalancePaymentStatus.ORDER_CREATION_FAILURE
                ),
                EnumSet.allOf(BalancePaymentStatus.class),
                MESSAGE.formatted(BalancePaymentStatus.class)
        );
    }

    @Test
    public void testBillingProductType() {
        assertEquals(
                EnumSet.of(
                        BillingProductType.SERVICE,
                        BillingProductType.WITHDRAW
                ),
                EnumSet.allOf(BillingProductType.class),
                MESSAGE.formatted(BillingProductType.class)
        );
    }

    @Test
    public void testCancellationOrderStatus() {
        assertEquals(
                EnumSet.of(
                        CancellationOrderStatus.CREATED,
                        CancellationOrderStatus.FAIL,
                        CancellationOrderStatus.MANUALLY_CONFIRMED,
                        CancellationOrderStatus.PROCESSING,
                        CancellationOrderStatus.REJECTED,
                        CancellationOrderStatus.REQUIRED_SEGMENT_FAIL,
                        CancellationOrderStatus.REQUIRED_SEGMENT_SUCCESS,
                        CancellationOrderStatus.SUCCESS,
                        CancellationOrderStatus.SYNC_FAIL,
                        CancellationOrderStatus.TECH_FAIL,
                        CancellationOrderStatus.UNKNOWN
                ),
                EnumSet.allOf(CancellationOrderStatus.class),
                MESSAGE.formatted(CancellationOrderStatus.class)
        );
    }

    @Test
    public void testCancellationSegmentStatus() {
        assertEquals(
                EnumSet.of(
                        CancellationSegmentStatus.FAIL,
                        CancellationSegmentStatus.MANUALLY_CONFIRMED,
                        CancellationSegmentStatus.NON_CANCELLABLE_SEGMENT,
                        CancellationSegmentStatus.PROCESSING,
                        CancellationSegmentStatus.SEGMENT_NOT_STARTED,
                        CancellationSegmentStatus.SUCCESS,
                        CancellationSegmentStatus.SUCCESS_BY_API,
                        CancellationSegmentStatus.SUCCESS_BY_CHECKPOINT,
                        CancellationSegmentStatus.SUCCESS_BY_RETURN,
                        CancellationSegmentStatus.SUCCESS_BY_TIMEOUT,
                        CancellationSegmentStatus.TECH_FAIL,
                        CancellationSegmentStatus.UNKNOWN,
                        CancellationSegmentStatus.WAITING_CHECKPOINTS,
                        CancellationSegmentStatus.WAITING_FOR_PROCESSING_AVAILABILITY
                ),
                EnumSet.allOf(CancellationSegmentStatus.class),
                MESSAGE.formatted(CancellationSegmentStatus.class)
        );
    }

    @Test
    public void testCargoType() {
        assertEquals(
                EnumSet.of(
                        CargoType.ABSORB_SMELL,
                        CargoType.ADULT,
                        CargoType.AEROSOLS_AND_GASES,
                        CargoType.AGROCHEMICALS,
                        CargoType.ANIMALS,
                        CargoType.FOR_PETS,
                        CargoType.ANIMAL_FEED,
                        CargoType.ART,
                        CargoType.ASSORTMENT,
                        CargoType.BULKY_CARGO,
                        CargoType.BULKY_CARGO_20_KG,
                        CargoType.BULKY_CARGO_MORE_THAN_PALLET,
                        CargoType.MULTI_PLACE_CARGO,
                        CargoType.CANNED_FOOD,
                        CargoType.CHEMICALS,
                        CargoType.CHILLED_FOOD,
                        CargoType.CIS_DISTINCT,
                        CargoType.CIS_OPTIONAL,
                        CargoType.CIS_REQUIRED,
                        CargoType.CONSTRUCTION_MATERIALS,
                        CargoType.CONSUMPTION_GOODS,
                        CargoType.COOL_FOOD,
                        CargoType.COSMETICS_AND_PERFUMERY,
                        CargoType.CREASE,
                        CargoType.DANGEROUS_AVIA_CARGO,
                        CargoType.DANGEROUS_CARGO,
                        CargoType.DIETARY_SUPPLEMENT,
                        CargoType.DIRTY,
                        CargoType.DOCUMENTS_AND_SECURITIES,
                        CargoType.DRY_FOOD,
                        CargoType.FASHION,
                        CargoType.FOOD,
                        CargoType.FRAGILE_CARGO,
                        CargoType.FRESH,
                        CargoType.FROZEN_FOOD,
                        CargoType.FURNITURE,
                        CargoType.GLASS,
                        CargoType.HOUSEHOLD_CHEMICALS,
                        CargoType.HYGIENIC_KIT,
                        CargoType.JEWELRY,
                        CargoType.WATCHES_AND_JEWELRY,
                        CargoType.LIQUID_CARGO,
                        CargoType.LI_ION_BATTERIES,
                        CargoType.LOOSE_CARGO,
                        CargoType.MEDICAL_SUPPLIES,
                        CargoType.NO_TRYING,
                        CargoType.ADULT_SHOES,
                        CargoType.CHILDREN_SHOES,
                        CargoType.ACCESSORY_SHOES,
                        CargoType.OUTERWEAR,
                        CargoType.OTHER_CLOTHES,
                        CargoType.CHILDREN_CLOTHES,
                        CargoType.CLOTHES_WITH_HANGERS,
                        CargoType.ACCESSORY_CLOTHES,
                        CargoType.ELECTRONICS,
                        CargoType.APPLIANCES,
                        CargoType.PHARMACY,
                        CargoType.VETERINARY_PHARMACY,
                        CargoType.BEAUTY,
                        CargoType.AUTO_AND_MOTO,
                        CargoType.TOYS,
                        CargoType.SPORTS,
                        CargoType.PENS,
                        CargoType.LOW_PRICE,
                        CargoType.MID_PRICE,
                        CargoType.HIGH_PRICE,
                        CargoType.PRESCRIPTION_MEDICINE,
                        CargoType.ODOROUS_CARGO,
                        CargoType.PERISHABLE_CARGO,
                        CargoType.PESTICIDES,
                        CargoType.POWDERS,
                        CargoType.PREPARED_FOOD,
                        CargoType.R18,
                        CargoType.RAW_CARGO,
                        CargoType.SEEDS,
                        CargoType.UNWRAPPED,
                        CargoType.UNBOXING_AVAILABLE,
                        CargoType.SEMIPRODUCT,
                        CargoType.SMALL_GOODS,
                        CargoType.STAINS,
                        CargoType.STORAGE_TEMPERATURE_BELOW_18,
                        CargoType.NOT_FOR_EXPRESS,
                        CargoType.NOT_FOR_FF_AND_SC,
                        CargoType.DIGITAL_PRODUCT,
                        CargoType.TECH_AND_ELECTRONICS,
                        CargoType.TECH_AND_ELECTRONICS_WITH_LI_ION_BATTERIES,
                        CargoType.MGT_WAREHOUSE,
                        CargoType.SGT_WAREHOUSE,
                        CargoType.KGT_WAREHOUSE,
                        CargoType.ULTRA_FRESH,
                        CargoType.UNKNOWN,
                        CargoType.VALUABLE,
                        CargoType.WEAPONS_AND_EXPLOSIVES,
                        CargoType.WET_CARGO,
                        CargoType.MERCURY
                ),
                EnumSet.allOf(CargoType.class),
                MESSAGE.formatted(CargoType.class)
        );
    }

    @Test
    public void testChangeOrderRequestReason() {
        assertEquals(
                EnumSet.of(
                        ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_DELIVERY,
                        ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_RECIPIENT,
                        ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_ROUTE_RECALCULATION,
                        ChangeOrderRequestReason.PRE_DELIVERY_ROUTE_RECALCULATION,
                        ChangeOrderRequestReason.DELIVERY_TIME_CLARIFIED_BY_DELIVERY,
                        ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_SHOP,
                        ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_USER,
                        ChangeOrderRequestReason.DELIVERY_SERVICE_PROBLEM,
                        ChangeOrderRequestReason.SHIPPING_DELAYED,
                        ChangeOrderRequestReason.CALL_COURIER_BY_USER,
                        ChangeOrderRequestReason.PROCESSING_DELAYED_BY_PARTNER,
                        ChangeOrderRequestReason.SHIPPING_DELAYED_BY_SENDER,
                        ChangeOrderRequestReason.LAST_MILE_CHANGED_BY_USER,
                        ChangeOrderRequestReason.DIMENSIONS_EXCEEDED_LOCKER,
                        ChangeOrderRequestReason.UNKNOWN
                ),
                EnumSet.allOf(ChangeOrderRequestReason.class),
                MESSAGE.formatted(ChangeOrderRequestReason.class)
        );
    }

    @Test
    public void testChangeOrderRequestStatus() {
        assertEquals(
                EnumSet.of(
                        ChangeOrderRequestStatus.CREATED,
                        ChangeOrderRequestStatus.FAIL,
                        ChangeOrderRequestStatus.INFO_RECEIVED,
                        ChangeOrderRequestStatus.ORDER_CANCELLED,
                        ChangeOrderRequestStatus.PARTNER_FAIL,
                        ChangeOrderRequestStatus.PROCESSING,
                        ChangeOrderRequestStatus.REJECTED,
                        ChangeOrderRequestStatus.REQUIRED_SEGMENT_FAIL,
                        ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS,
                        ChangeOrderRequestStatus.SUCCESS,
                        ChangeOrderRequestStatus.TECH_FAIL,
                        ChangeOrderRequestStatus.UNKNOWN
                ),
                EnumSet.allOf(ChangeOrderRequestStatus.class),
                MESSAGE.formatted(ChangeOrderRequestStatus.class)
        );
    }

    @Test
    public void testChangeOrderRequestType() {
        assertEquals(
                EnumSet.of(
                        ChangeOrderRequestType.CHANGE_TO_ON_DEMAND,
                        ChangeOrderRequestType.DELIVERY_DATE,
                        ChangeOrderRequestType.DELIVERY_OPTION,
                        ChangeOrderRequestType.ITEM_NOT_FOUND,
                        ChangeOrderRequestType.ORDER_CHANGED_BY_PARTNER,
                        ChangeOrderRequestType.ORDER_ITEM_IS_NOT_SUPPLIED,
                        ChangeOrderRequestType.RECALCULATE_ROUTE_DATES,
                        ChangeOrderRequestType.RECIPIENT,
                        ChangeOrderRequestType.UNKNOWN,
                        ChangeOrderRequestType.UPDATE_COURIER,
                        ChangeOrderRequestType.UPDATE_ITEMS_INSTANCES,
                        ChangeOrderRequestType.UPDATE_TRANSFER_CODES,
                        ChangeOrderRequestType.LAST_MILE,
                        ChangeOrderRequestType.CHANGE_LAST_MILE_TO_COURIER,
                        ChangeOrderRequestType.CHANGE_LAST_MILE_TO_PICKUP,
                        ChangeOrderRequestType.UPDATE_PLACES,
                        ChangeOrderRequestType.CHANGE_LAST_MILE_FROM_PICKUP_TO_PICKUP
                ),
                EnumSet.allOf(ChangeOrderRequestType.class),
                MESSAGE.formatted(ChangeOrderRequestType.class)
        );
    }

    @Test
    public void testContactType() {
        assertEquals(
                EnumSet.of(
                        ContactType.CONTACT,
                        ContactType.CREDENTIALS,
                        ContactType.RECIPIENT,
                        ContactType.PHYSICAL_PERSON_SENDER
                ),
                EnumSet.allOf(ContactType.class),
                MESSAGE.formatted(ContactType.class)
        );
    }

    @Test
    public void testCourierType() {
        assertEquals(
                EnumSet.of(
                        CourierType.CAR,
                        CourierType.COURIER
                ),
                EnumSet.allOf(CourierType.class),
                MESSAGE.formatted(CourierType.class)
        );
    }

    @Test
    public void testDeliveryType() {
        assertEquals(
                EnumSet.of(
                        DeliveryType.COURIER,
                        DeliveryType.MOVEMENT,
                        DeliveryType.PICKUP,
                        DeliveryType.POST
                ),
                EnumSet.allOf(DeliveryType.class),
                MESSAGE.formatted(DeliveryType.class)
        );
    }

    @Test
    public void testEntityType() {
        assertEquals(
                EnumSet.of(
                        EntityType.CHANGE_ORDER_REQUEST,
                        EntityType.CHANGE_ORDER_SEGMENT_REQUEST,
                        EntityType.DELIVERY_TRACK,
                        EntityType.MDS_FILE,
                        EntityType.ORDER,
                        EntityType.ORDER_CANCELLATION_REQUEST,
                        EntityType.ORDER_CHANGED_BY_PARTNER_REQUEST,
                        EntityType.PARTNER,
                        EntityType.REGISTRY,
                        EntityType.RETURN_REGISTRY,
                        EntityType.SEGMENT_CANCELLATION_REQUEST,
                        EntityType.SHIPMENT_APPLICATION,
                        EntityType.WAYBILL_SEGMENT
                ),
                EnumSet.allOf(EntityType.class),
                MESSAGE.formatted(EntityType.class)
        );
    }

    @Test
    public void testFileType() {
        assertEquals(
                EnumSet.of(
                        FileType.ADMIN_BATCH_OPERATION,
                        FileType.ORDER_LABEL,
                        FileType.SHIPMENT_ACCEPTANCE_CERTIFICATE
                ),
                EnumSet.allOf(FileType.class),
                MESSAGE.formatted(FileType.class)
        );
    }

    @Test
    public void testItemChangeReason() {
        assertEquals(
                EnumSet.of(
                        ItemChangeReason.ITEM_IS_NOT_SUPPLIED,
                        ItemChangeReason.ITEM_NOT_FOUND,
                        ItemChangeReason.ORDER_ITEM_CHANGED
                ),
                EnumSet.allOf(ItemChangeReason.class),
                MESSAGE.formatted(ItemChangeReason.class)
        );
    }

    @Test
    public void testItemUnitOperationType() {
        assertEquals(
                EnumSet.of(
                        ItemUnitOperationType.CROSSDOCK,
                        ItemUnitOperationType.FULFILLMENT
                ),
                EnumSet.allOf(ItemUnitOperationType.class),
                MESSAGE.formatted(ItemUnitOperationType.class)
        );
    }

    @Test
    public void testLocationType() {
        assertEquals(
                EnumSet.of(
                        LocationType.PICKUP,
                        LocationType.RECIPIENT,
                        LocationType.WAREHOUSE
                ),
                EnumSet.allOf(LocationType.class),
                MESSAGE.formatted(LocationType.class)
        );
    }

    @Test
    public void testOptionalOrderPart() {
        assertEquals(
                EnumSet.of(
                        OptionalOrderPart.CANCELLATION_REQUESTS,
                        OptionalOrderPart.CHANGE_REQUESTS,
                        OptionalOrderPart.GLOBAL_STATUSES_HISTORY,
                        OptionalOrderPart.UPDATE_RECIPIENT_ENABLED,
                        OptionalOrderPart.RETURNS_IDS
                ),
                EnumSet.allOf(OptionalOrderPart.class),
                MESSAGE.formatted(OptionalOrderPart.class)
        );
    }

    @Test
    public void testOrderStatus() {
        assertEquals(
                EnumSet.of(
                        OrderStatus.CANCELLED,
                        OrderStatus.DELIVERED,
                        OrderStatus.DRAFT,
                        OrderStatus.ENQUEUED,
                        OrderStatus.FINISHED,
                        OrderStatus.LOST,
                        OrderStatus.PROCESSING,
                        OrderStatus.PROCESSING_ERROR,
                        OrderStatus.RETURNED,
                        OrderStatus.RETURNING,
                        OrderStatus.UNKNOWN,
                        OrderStatus.VALIDATING,
                        OrderStatus.VALIDATION_ERROR
                ),
                EnumSet.allOf(OrderStatus.class),
                MESSAGE.formatted(OrderStatus.class)
        );
    }

    @Test
    public void testOrderTag() {
        assertEquals(
                EnumSet.of(
                        OrderTag.COMMITTED_VIA_DAAS_BACK_OFFICE,
                        OrderTag.COMMITTED_VIA_DAAS_OPEN_API,
                        OrderTag.COMMITTING_VIA_DAAS_BACK_OFFICE_FAIL,
                        OrderTag.COMMITTING_VIA_DAAS_OPEN_API_FAIL,
                        OrderTag.CREATED_VIA_DAAS_BACK_OFFICE,
                        OrderTag.CREATED_VIA_DAAS_OPEN_API,
                        OrderTag.B2B_CUSTOMER,
                        OrderTag.DELAYED_RDD_NOTIFICATION,
                        OrderTag.C2C
                ),
                EnumSet.allOf(OrderTag.class),
                MESSAGE.formatted(OrderTag.class)
        );
    }

    @Test
    public void testPageSize() {
        assertEquals(
                EnumSet.of(
                        PageSize.A4,
                        PageSize.A6
                ),
                EnumSet.allOf(PageSize.class),
                MESSAGE.formatted(PageSize.class)
        );
    }

    @Test
    public void testPartnerSubtype() {
        assertEquals(
                EnumSet.of(
                        PartnerSubtype.DARKSTORE,
                        PartnerSubtype.GO_PARTNER_LOCKER,
                        PartnerSubtype.GO_PLATFORM,
                        PartnerSubtype.MARKET_COURIER,
                        PartnerSubtype.MARKET_COURIER_SORTING_CENTER,
                        PartnerSubtype.MARKET_LOCKER,
                        PartnerSubtype.MARKET_OWN_PICKUP_POINT,
                        PartnerSubtype.PARTNER_CONTRACT_DELIVERY,
                        PartnerSubtype.PARTNER_PICKUP_POINT_IP,
                        PartnerSubtype.PARTNER_SORTING_CENTER,
                        PartnerSubtype.TAXI_EXPRESS,
                        PartnerSubtype.TAXI_LAVKA,
                        PartnerSubtype.COURIER_PLATFORM_FOR_SHOP,
                        PartnerSubtype.UNKNOWN
                ),
                EnumSet.allOf(PartnerSubtype.class),
                MESSAGE.formatted(PartnerSubtype.class)
        );
    }

    @Test
    public void testPartnerType() {
        assertEquals(
                EnumSet.of(
                        PartnerType.DELIVERY,
                        PartnerType.DROPSHIP,
                        PartnerType.DROPSHIP_BY_SELLER,
                        PartnerType.FULFILLMENT,
                        PartnerType.OWN_DELIVERY,
                        PartnerType.SORTING_CENTER,
                        PartnerType.SUPPLIER,
                        PartnerType.UNKNOWN,
                        PartnerType.YANDEX_GO_SHOP
                ),
                EnumSet.allOf(PartnerType.class),
                MESSAGE.formatted(PartnerType.class)
        );
    }

    @Test
    public void testPaymentMethod() {
        assertEquals(
                EnumSet.of(
                        PaymentMethod.CARD,
                        PaymentMethod.CASH,
                        PaymentMethod.PREPAID
                ),
                EnumSet.allOf(PaymentMethod.class),
                MESSAGE.formatted(PaymentMethod.class)
        );
    }

    @Test
    public void testPlatformClient() {
        assertEquals(
                EnumSet.of(
                        PlatformClient.BERU,
                        PlatformClient.DBS,
                        PlatformClient.YANDEX_DELIVERY,
                        PlatformClient.YANDEX_GO,
                        PlatformClient.FAAS
                ),
                EnumSet.allOf(PlatformClient.class),
                MESSAGE.formatted(PlatformClient.class)
        );
    }

    @Test
    public void testPointType() {
        assertEquals(
                EnumSet.of(
                        PointType.BACKWARD_MOVEMENT,
                        PointType.GO_PLATFORM,
                        PointType.HANDING,
                        PointType.LINEHAUL,
                        PointType.MOVEMENT,
                        PointType.PICKUP,
                        PointType.RETURN_MOVEMENT,
                        PointType.WAREHOUSE
                ),
                EnumSet.allOf(PointType.class),
                MESSAGE.formatted(PointType.class)
        );
    }

    @Test
    public void testRegistryStatus() {
        assertEquals(
                EnumSet.of(
                        RegistryStatus.CREATED,
                        RegistryStatus.ERROR,
                        RegistryStatus.PROCESSING
                ),
                EnumSet.allOf(RegistryStatus.class),
                MESSAGE.formatted(RegistryStatus.class)
        );
    }

    @Test
    public void testRoutePointServiceType() {
        assertEquals(
                EnumSet.of(
                        RoutePointServiceType.INBOUND,
                        RoutePointServiceType.INTERNAL,
                        RoutePointServiceType.OUTBOUND
                ),
                EnumSet.allOf(RoutePointServiceType.class),
                MESSAGE.formatted(RoutePointServiceType.class)
        );
    }

    @Test
    public void testSegmentStatus() {
        assertEquals(
                EnumSet.of(
                        SegmentStatus.CANCELLED,
                        SegmentStatus.ERROR,
                        SegmentStatus.ERROR_DELIVERY_CAN_NOT_BE_COMPLETED,
                        SegmentStatus.ERROR_LOST,
                        SegmentStatus.ERROR_NOT_FOUND,
                        SegmentStatus.CLAIM_STARTED,
                        SegmentStatus.CLAIM_PAID,
                        SegmentStatus.IN,
                        SegmentStatus.INFO_RECEIVED,
                        SegmentStatus.OUT,
                        SegmentStatus.PENDING,
                        SegmentStatus.RETURNED,
                        SegmentStatus.RETURN_ARRIVED,
                        SegmentStatus.RETURN_ORDER_PARTIALLY_RECEIPT_AT_SECONDARY_RECEPTION,
                        SegmentStatus.RETURN_PREPARED_FOR_UTILIZE,
                        SegmentStatus.RETURN_PREPARING,
                        SegmentStatus.RETURN_PREPARING_SENDER,
                        SegmentStatus.RETURN_RFF_ARRIVED_FULFILLMENT,
                        SegmentStatus.RETURN_RFF_PREPARING_FULFILLMENT,
                        SegmentStatus.RETURN_RFF_TRANSMITTED_FULFILLMENT,
                        SegmentStatus.RETURN_SHIPPED_FOR_UTILIZER,
                        SegmentStatus.RETURN_TRANSFERRED,
                        SegmentStatus.STARTED,
                        SegmentStatus.TRACK_RECEIVED,
                        SegmentStatus.TRANSIT_AUTOMATICALLY_REMOVED_ITEMS,
                        SegmentStatus.TRANSIT_AWAITING_CLARIFICATION,
                        SegmentStatus.TRANSIT_CHANGE_OUTBOUND_VERIFICATION_CODE_REQUESTED,
                        SegmentStatus.TRANSIT_COURIER_ARRIVED_TO_SENDER,
                        SegmentStatus.TRANSIT_COURIER_FOUND,
                        SegmentStatus.TRANSIT_COURIER_IN_TRANSIT_TO_SENDER,
                        SegmentStatus.TRANSIT_COURIER_NOT_FOUND,
                        SegmentStatus.TRANSIT_ON_DEMAND_DELIVERY_REQUESTED,
                        SegmentStatus.TRANSIT_COURIER_RECEIVED,
                        SegmentStatus.TRANSIT_COURIER_SEARCH,
                        SegmentStatus.TRANSIT_CUSTOMS_ARRIVED,
                        SegmentStatus.TRANSIT_CUSTOMS_CLEARED,
                        SegmentStatus.TRANSIT_DELIVERY_ARRIVED,
                        SegmentStatus.TRANSIT_TIME_CLARIFIED,
                        SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED,
                        SegmentStatus.TRANSIT_DELIVERY_AT_START_SORT,
                        SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION,
                        SegmentStatus.TRANSIT_OUT_OF_STOCK,
                        SegmentStatus.TRANSIT_PICKUP,
                        SegmentStatus.TRANSIT_PLACES_CHANGED,
                        SegmentStatus.TRANSIT_PREPARED,
                        SegmentStatus.TRANSIT_STORAGE_PERIOD_EXPIRED,
                        SegmentStatus.TRANSIT_STORAGE_PERIOD_EXTENDED,
                        SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT,
                        SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                        SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY,
                        SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT,
                        SegmentStatus.TRANSIT_UPDATED_BY_SHOP,
                        SegmentStatus.UNKNOWN
                ),
                EnumSet.allOf(SegmentStatus.class),
                MESSAGE.formatted(SegmentStatus.class)
        );
    }

    @Test
    public void testSegmentType() {
        assertEquals(
                EnumSet.of(
                        SegmentType.COURIER,
                        SegmentType.FULFILLMENT,
                        SegmentType.GO_PLATFORM,
                        SegmentType.MOVEMENT,
                        SegmentType.NO_OPERATION,
                        SegmentType.PICKUP,
                        SegmentType.POST,
                        SegmentType.SORTING_CENTER,
                        SegmentType.SUPPLIER
                ),
                EnumSet.allOf(SegmentType.class),
                MESSAGE.formatted(SegmentType.class)
        );
    }

    @Test
    public void testServiceCodeName() {
        assertEquals(
                EnumSet.of(
                        ServiceCodeName.CALL_COURIER,
                        ServiceCodeName.CARD_ALLOWED,
                        ServiceCodeName.CASH_ALLOWED,
                        ServiceCodeName.CASH_SERVICE,
                        ServiceCodeName.CHECK,
                        ServiceCodeName.COMPLECT,
                        ServiceCodeName.CONSOLIDATION,
                        ServiceCodeName.CUTOFF,
                        ServiceCodeName.DEFERRED_COURIER_YANDEX_GO,
                        ServiceCodeName.DELIVERY,
                        ServiceCodeName.DISABLE_PARTIAL_RETURN,
                        ServiceCodeName.DROPSHIP_EXPRESS,
                        ServiceCodeName.HANDING,
                        ServiceCodeName.INBOUND,
                        ServiceCodeName.INSURANCE,
                        ServiceCodeName.LAST_MILE,
                        ServiceCodeName.MOVEMENT,
                        ServiceCodeName.ON_DEMAND_YANDEX_GO,
                        ServiceCodeName.OTHER,
                        ServiceCodeName.PACK,
                        ServiceCodeName.PARTIAL_RETURN,
                        ServiceCodeName.PREPAY_ALLOWED,
                        ServiceCodeName.PROCESSING,
                        ServiceCodeName.REPACK,
                        ServiceCodeName.RETURN,
                        ServiceCodeName.RETURN_ALLOWED,
                        ServiceCodeName.RETURN_SORT,
                        ServiceCodeName.SHIPMENT,
                        ServiceCodeName.SHOP_LAST_MILE,
                        ServiceCodeName.SORT,
                        ServiceCodeName.STORAGE,
                        ServiceCodeName.TRANSPORT_MANAGER_MOVEMENT,
                        ServiceCodeName.TRYING,
                        ServiceCodeName.UNDEFINED,
                        ServiceCodeName.UNKNOWN,
                        ServiceCodeName.WAIT_20,
                        ServiceCodeName.XDOC_MOVEMENT,
                        ServiceCodeName.UNBOXING
                ),
                EnumSet.allOf(ServiceCodeName.class),
                MESSAGE.formatted(ServiceCodeName.class)
        );
    }

    @Test
    public void testShipmentApplicationStatus() {
        assertEquals(
                EnumSet.of(
                        ShipmentApplicationStatus.CANCELLED,
                        ShipmentApplicationStatus.CREATED,
                        ShipmentApplicationStatus.DELIVERY_SERVICE_PROCESSING,
                        ShipmentApplicationStatus.ERROR,
                        ShipmentApplicationStatus.NEW,
                        ShipmentApplicationStatus.REGISTRY_PROCESSING_ERROR,
                        ShipmentApplicationStatus.REGISTRY_SENT
                ),
                EnumSet.allOf(ShipmentApplicationStatus.class),
                MESSAGE.formatted(ShipmentApplicationStatus.class)
        );
    }

    @Test
    public void testShipmentOption() {
        assertEquals(
                EnumSet.of(
                        ShipmentOption.CASH_SERVICE,
                        ShipmentOption.CHECK,
                        ShipmentOption.COMPLECT,
                        ShipmentOption.DELIVERY,
                        ShipmentOption.INSURANCE,
                        ShipmentOption.OTHER,
                        ShipmentOption.PACK,
                        ShipmentOption.PARTIAL_RETURN,
                        ShipmentOption.REPACK,
                        ShipmentOption.RETURN,
                        ShipmentOption.RETURN_SORT,
                        ShipmentOption.SORT,
                        ShipmentOption.STORAGE,
                        ShipmentOption.TRYING,
                        ShipmentOption.WAIT_20,
                        ShipmentOption.UNBOXING
                ),
                EnumSet.allOf(ShipmentOption.class),
                MESSAGE.formatted(ShipmentOption.class)
        );
    }

    @Test
    public void testShipmentType() {
        assertEquals(
                EnumSet.of(
                        ShipmentType.IMPORT,
                        ShipmentType.WITHDRAW
                ),
                EnumSet.allOf(ShipmentType.class),
                MESSAGE.formatted(ShipmentType.class)
        );
    }

    @Test
    public void testStorageUnitType() {
        assertEquals(
                EnumSet.of(
                        StorageUnitType.PLACE,
                        StorageUnitType.ROOT
                ),
                EnumSet.allOf(StorageUnitType.class),
                MESSAGE.formatted(StorageUnitType.class)
        );
    }

    @Test
    public void testTaxSystem() {
        assertEquals(
                EnumSet.of(
                        TaxSystem.ENVD,
                        TaxSystem.ESN,
                        TaxSystem.OSN,
                        TaxSystem.PATENT,
                        TaxSystem.USN_INCOME,
                        TaxSystem.USN_INCOME_OUTCOME,
                        TaxSystem.NPD
                ),
                EnumSet.allOf(TaxSystem.class),
                MESSAGE.formatted(TaxSystem.class)
        );
    }

    @Test
    public void testVatType() {
        assertEquals(
                EnumSet.of(
                        VatType.NO_VAT,
                        VatType.VAT_0,
                        VatType.VAT_10,
                        VatType.VAT_20
                ),
                EnumSet.allOf(VatType.class),
                MESSAGE.formatted(VatType.class)
        );
    }

    @Test
    public void testWaybillSegmentTag() {
        assertEquals(
                EnumSet.of(
                        WaybillSegmentTag.CALL_COURIER,
                        WaybillSegmentTag.DBS,
                        WaybillSegmentTag.DEFERRED_COURIER,
                        WaybillSegmentTag.DIRECT,
                        WaybillSegmentTag.ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED,
                        WaybillSegmentTag.EXPRESS,
                        WaybillSegmentTag.ON_DEMAND,
                        WaybillSegmentTag.RECREATED,
                        WaybillSegmentTag.RETURN,
                        WaybillSegmentTag.B2B_CUSTOMER,
                        WaybillSegmentTag.COURIER_PRO,
                        WaybillSegmentTag.COURIER_CAR,
                        WaybillSegmentTag.COURIER_PEDESTRIAN,
                        WaybillSegmentTag.WIDE_INTERVAL,
                        WaybillSegmentTag.YANDEX_GO,
                        WaybillSegmentTag.SHOP_ORDER_CREATION_PARTNER_ERROR,
                        WaybillSegmentTag.SHOP_ORDER_CREATION_MARKET_ERROR,
                        WaybillSegmentTag.EXPRESS_BATCH,
                        WaybillSegmentTag.EXPRESS_WAREHOUSE_CLOSING,
                        WaybillSegmentTag.C2C,
                        WaybillSegmentTag.UNKNOWN
                ),
                EnumSet.allOf(WaybillSegmentTag.class),
                MESSAGE.formatted(WaybillSegmentTag.class)
        );
    }
}
