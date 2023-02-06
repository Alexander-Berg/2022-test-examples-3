package ru.yandex.market.logistic.gateway.service.converter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.market.logistic.api.model.common.CompositeId;
import ru.yandex.market.logistic.api.model.common.PartialId;
import ru.yandex.market.logistic.api.model.common.PartialIdType;
import ru.yandex.market.logistic.api.model.fulfillment.CargoTypes;
import ru.yandex.market.logistic.api.model.fulfillment.InboundType;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.Expiration;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemExpiration;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OutboundStatus;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Car;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CisHandleMode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Consignment;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Contractor;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Courier;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Inbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundUnitDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Intake;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemReference;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Location;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Person;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Phone;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Register;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnRegister;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Service;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Status;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StatusCode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Tax;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TaxType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.VatValue;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.CreateIntakeResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.CreateRegisterResponse;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.CreateIntakeConverter;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.CreateRegisterConverter;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.CreateReturnRegisterConverter;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.FulfillmentConverter;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.InboundConverter;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.OutboundConverter;
import ru.yandex.market.logistic.gateway.utils.delivery.DtoFactory;
import ru.yandex.market.logistic.gateway.utils.fulfillment.ApiDtoFactory;

import static ru.yandex.market.logistic.gateway.utils.fulfillment.DtoFactory.createCreateIntakeResponse;
import static ru.yandex.market.logistic.gateway.utils.fulfillment.DtoFactory.createCreateRegisterResponse;
import static ru.yandex.market.logistic.gateway.utils.fulfillment.DtoFactory.createIntake;
import static ru.yandex.market.logistic.gateway.utils.fulfillment.DtoFactory.createRegister;
import static ru.yandex.market.logistic.gateway.utils.fulfillment.DtoFactory.createReturnRegister;

public class FulfillmentConverterTest extends BaseTest {

    @Test
    public void convertUnitIdToApi() {
        UnitId unitId = new UnitId("111", 222L, "333");

        ru.yandex.market.logistic.api.model.fulfillment.UnitId expected =
            new ru.yandex.market.logistic.api.model.fulfillment.UnitId.UnitIdBuilder(222L, "333")
                .setId("111")
                .build();
        ru.yandex.market.logistic.api.model.fulfillment.UnitId actual =
            FulfillmentConverter.convertUnitIdToApi(unitId).orElse(null);

        assertions.assertThat(actual)
            .as("Asserting the actual unitId is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertItemStocksFromApi() {
        ItemStocks itemStocks = getItemStocksApi();

        ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemStocks expected = DtoFactory.createItemStocks();
        ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemStocks actual =
            FulfillmentConverter.convertItemStocksFromApi(itemStocks).orElse(null);

        assertions.assertThat(actual)
            .as("Asserting the actual itemStocks is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertUnitIdFromApi() {
        ru.yandex.market.logistic.api.model.fulfillment.UnitId unitId =
            new ru.yandex.market.logistic.api.model.fulfillment.UnitId.UnitIdBuilder(222L, "333")
                .setId("111")
                .build();

        UnitId expected = new UnitId("111", 222L, "333");
        UnitId actual = FulfillmentConverter.convertUnitIdFromApi(unitId).orElse(null);

        assertions.assertThat(actual)
            .as("Asserting the actual unitId is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertResourceIdFromApi() {
        ResourceId resourceId = new ResourceId.ResourceIdBuilder().setYandexId("111").setPartnerId("333").build();

        ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId expected =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                    .setYandexId("111").setPartnerId("333").build();
        ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId actual =
            FulfillmentConverter.convertResourceIdFromApi(resourceId).orElse(null);

        assertions.assertThat(actual)
            .as("Asserting the actual resourceId is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertResourceIdToApi() {
        ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId resourceId =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                .setYandexId("111").setPartnerId("333").build();

        ResourceId expected = new ResourceId("111", "333");
        ResourceId actual = FulfillmentConverter.convertResourceIdToApi(resourceId).orElse(null);

        assertions.assertThat(actual)
            .as("Asserting the actual resourceId is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertStockFromApi() {
        Stock stock = getStockApi();

        ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock expected = DtoFactory.createStock();
        ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock actual =
            FulfillmentConverter.convertStockFromApi(stock).orElse(null);

        assertions.assertThat(actual)
            .as("Asserting the actual stock is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertStockTypeFromApi() {
        assertions.assertThat(FulfillmentConverter.convertStockTypeFromApi(StockType.QUARANTINE).orElse(null))
            .as("Asserting the actual stockType is equal to expected")
            .isEqualTo(ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.QUARANTINE);
    }

    @Test
    public void convertDateTimeFromApi() {
        DateTime dateTime = new DateTime("2016-03-21T12:34:56+03:00");

        ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime expected =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime("2016-03-21T12:34:56+03:00");
        ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime actual =
            FulfillmentConverter.convertDateTimeFromApi(dateTime).orElse(null);

        assertions.assertThat(actual)
            .as("Asserting the actual dateTime is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertInboundDetailsFromApi() {
        String yandexId = "111";
        String partnerId = "222";

        int actual = 2;
        int declared = 2;
        int defect = 0;
        int surplus = 1;

        String unitId = "444";
        Long unitVendorId = 100L;
        String unitArticle = "TestArticle";

        List<CompositeId> instances = ImmutableList.of(
            new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis123"))),
            new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis222")))
        );

        List<CompositeId> unfitInstances = ImmutableList.of(
            new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis-unfit-123"))),
            new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis-unfit-222")))
        );

        ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId resourceId =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                    .setYandexId(yandexId).setPartnerId(partnerId).build();

        UnitId unit = new UnitId(unitId, unitVendorId, unitArticle);
        InboundUnitDetails inboundUnitDetails = new InboundUnitDetails(unit, actual, declared, defect, surplus);

        inboundUnitDetails.setInstances(ImmutableList.of(
            new ru.yandex.market.logistic.gateway.common.model.common.CompositeId(List.of(
                new ru.yandex.market.logistic.gateway.common.model.common.PartialId(
                    ru.yandex.market.logistic.gateway.common.model.common.PartialIdType.CIS, "cis123"))),
            new ru.yandex.market.logistic.gateway.common.model.common.CompositeId(
                List.of(new ru.yandex.market.logistic.gateway.common.model.common.PartialId(
                    ru.yandex.market.logistic.gateway.common.model.common.PartialIdType.CIS, "cis222")))
        ));

        inboundUnitDetails.setUnfitInstances(ImmutableList.of(
            new ru.yandex.market.logistic.gateway.common.model.common.CompositeId(List.of(
                new ru.yandex.market.logistic.gateway.common.model.common.PartialId(
                    ru.yandex.market.logistic.gateway.common.model.common.PartialIdType.CIS, "cis-unfit-123"))),
            new ru.yandex.market.logistic.gateway.common.model.common.CompositeId(
                List.of(new ru.yandex.market.logistic.gateway.common.model.common.PartialId(
                    ru.yandex.market.logistic.gateway.common.model.common.PartialIdType.CIS, "cis-unfit-222")))
        ));

        InboundDetails expectedInboundDetails =
            new InboundDetails(resourceId, Collections.singletonList(inboundUnitDetails));

        ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundUnitDetails details =
            new ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundUnitDetails(
                new ru.yandex.market.logistic.api.model.fulfillment.UnitId.UnitIdBuilder(
                    unitVendorId, unitArticle)
                    .setId(unitId)
                    .build(),
                declared,
                actual,
                defect,
                surplus);
        details.setInstances(instances);
        details.setUnfitInstances(unfitInstances);

        InboundDetails inboundDetails = InboundConverter.convertInboundDetailsFromApi(
            new ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundDetails(
                new ResourceId(yandexId, partnerId),
                Collections.singletonList(
                    details))
        ).orElse(null);

        assertions.assertThat(inboundDetails)
            .as("Asserting the actual inbound details is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expectedInboundDetails);
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @Test
    public void convertInboundToApi() {
        String yandexId = "23456";
        String partnerId = "Zakazik";

        String unitId = "1";
        Long unitVendorId = 2L;
        String unitArticle = "art";

        List<String> vendorCodes = Arrays.asList("123", "456", "ABC");

        String barcodeCode = "code";
        String barcodeType = "type";
        String itemName = "Name";
        String itemDescription = "description";
        int itemCount = 10;
        int itemUndefinedCount = 7;
        BigDecimal itemPrice = BigDecimal.valueOf(130.5);
        BigDecimal itemUntaxedPrice = BigDecimal.valueOf(120);
        String itemArticle = "Article";

        Boolean itemHasLifeTime = true;
        Integer itemLifeTime = 123;
        Integer itemBoxCount = 1;
        Integer itemBoxCapacity = 1;
        String itemComment = "comment";
        String itemNameEnglish = "EnglishName";
        String itemUrl = "http://url.url";
        String itemCategoryName = "Category";
        String interval = "2019-01-02T11:00:00+00:00/2019-02-12T12:00:05+00:00";

        String inboundServiceCode = "SORT";
        String inboundServiceName = "Lunapark with BlackJack and whores";
        String inboundServiceDescription = "Short description for current service";
        Long categoryId = 10L;
        boolean inboundServiceIsOptional = false;

        String taxType = "VAT";
        int taxValue = 2;

        int cargoType = 700;

        int korobyteWidth = 100;
        int korobyteHeight = 101;
        int korobyteLength = 102;
        BigDecimal korobyteWeightGross = BigDecimal.valueOf(100.0);
        BigDecimal korobyteWeightNet = BigDecimal.valueOf(100.1);
        BigDecimal korobyteWeightTare = BigDecimal.valueOf(100.2);

        String inboundComment = "inboundComment";


        String warehouseYandexId = "65655";
        String warehousePartnerId = "Y656D";
        String warehouseInstruction = "WarehouseInstruction";
        String warehouseInc = "fillgoodinc";

        String phoneNumber = "+79613888370";
        String personName = "SayMyName";

        String courierName = "Skyler";
        String carNumber = "RU36577";

        ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId resourceId =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                .setYandexId(yandexId)
                .setPartnerId(partnerId)
                .build();

        String contractorId = "contractorId";
        String contractorName = "contractorName";

        ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime actualUpdatedDateTime =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime("2016-03-21T12:34:56+03:00");

        DateTime expectedUpdatedDateTime = new DateTime("2016-03-21T12:34:56+03:00");

        List<Map<String, String>> instances = List.of(Map.of("cis", "123abc"));

        Boolean surplusAllowed = true;

        Integer checkImei = 1;
        String imeiMask = ".+";
        Integer checkSn = 1;
        String snMask = ".+";

        Consignment consignment = new Consignment(
            null,
            new Item(
                new UnitId(unitId, unitVendorId, unitArticle),
                itemArticle,
                vendorCodes,
                Collections.singletonList(new Barcode(barcodeCode, barcodeType, BarcodeSource.UNKNOWN)),
                itemName,
                itemDescription,
                itemCount,
                itemUndefinedCount,
                itemPrice,
                new Tax(TaxType.valueOf(taxType), VatValue.create(taxValue)),
                itemUntaxedPrice,
                CargoType.create(cargoType),
                Collections.singletonList(CargoType.PERISHABLE_CARGO),
                new Korobyte(korobyteWidth,
                    korobyteHeight,
                    korobyteLength,
                    korobyteWeightGross,
                    korobyteWeightNet,
                    korobyteWeightTare),
                Collections.singletonList(new Service(
                    ServiceType.valueOf(inboundServiceCode),
                    inboundServiceName,
                    inboundServiceDescription,
                    inboundServiceIsOptional
                )),
                itemHasLifeTime,
                itemLifeTime,
                itemBoxCount,
                itemBoxCapacity,
                itemComment,
                itemNameEnglish,
                itemUrl,
                itemCategoryName,
                new Contractor(contractorId, contractorName),
                createRemainingLifetimesFromApi(),
                actualUpdatedDateTime,
                categoryId,
                null,
                null,
                null,
                instances,
                surplusAllowed,
                checkImei,
                imeiMask,
                checkSn,
                snMask,
                CisHandleMode.ACCEPT_ONLY_DECLARED
            ),
            null);

        final ru.yandex.market.logistic.api.model.fulfillment.Inbound convertedInbound =
            InboundConverter.convertInboundToApi(
                new Inbound(resourceId,
                    ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType.DEFAULT,
                    Collections.singletonList(consignment),
                    new Warehouse(
                        resourceId,
                        new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                            .setYandexId(warehouseYandexId)
                            .setPartnerId(warehousePartnerId)
                            .build(),
                        new Location.LocationBuilder("Россия", "Московская область", "Москва").build(),
                        warehouseInstruction,
                        null,
                        new Person(personName, null, null, null),
                        Collections.singletonList(new Phone(phoneNumber, null)),
                        warehouseInc
                    ),
                    new Courier(
                        Collections.singletonList(new Person(courierName, null, null, null)),
                        new Car(carNumber, null), null, null),
                    DateTimeInterval.fromFormattedValue(interval),
                    inboundComment)).orElse(null);

        final ru.yandex.market.logistic.api.model.fulfillment.Consignment expectedConsignment =
            new ru.yandex.market.logistic.api.model.fulfillment.Consignment.ConsignmentBuilder(
                new ru.yandex.market.logistic.api.model.fulfillment.Item.ItemBuilder(itemName, itemCount, itemPrice)
                    .setUnitId(new ru.yandex.market.logistic.api.model.fulfillment.UnitId.UnitIdBuilder(
                            unitVendorId, unitArticle)
                        .setId(unitId)
                        .build())
                    .setArticle(itemArticle)
                    .setVendorCodes(vendorCodes)
                    .setBarcodes(Collections.singletonList(
                        new ru.yandex.market.logistic.api.model.fulfillment.Barcode.BarcodeBuilder(barcodeCode)
                            .setType(barcodeType)
                            .build()))
                    .setDescription(itemDescription)
                    .setTax(new ru.yandex.market.logistic.api.model.fulfillment.Tax(
                        ru.yandex.market.logistic.api.model.fulfillment.TaxType.valueOf(taxType),
                        ru.yandex.market.logistic.api.model.fulfillment.VatValue.create(taxValue)))
                    .setUntaxedPrice(itemUntaxedPrice)
                    .setCargoType(ru.yandex.market.logistic.api.model.fulfillment.CargoType.create(cargoType))
                    .setCargoTypes(new CargoTypes(Collections.singletonList(
                        ru.yandex.market.logistic.api.model.fulfillment.CargoType.create(cargoType)))
                    )
                    .setKorobyte(new ru.yandex.market.logistic.api.model.fulfillment.Korobyte.KorobyteBuiler(
                        korobyteWidth,
                        korobyteHeight,
                        korobyteLength,
                        korobyteWeightGross)
                        .setWeightNet(korobyteWeightNet)
                        .setWeightTare(korobyteWeightTare)
                        .build())
                    .setInboundServices(
                        Collections.singletonList(
                                new ru.yandex.market.logistic.api.model.fulfillment.Service.ServiceBuilder(
                            ru.yandex.market.logistic.api.model.fulfillment.ServiceType.valueOf(inboundServiceCode),
                            inboundServiceIsOptional)
                            .setName(inboundServiceName)
                            .setDescription(inboundServiceDescription)
                            .build()
                        ))
                    .setHasLifeTime(itemHasLifeTime)
                    .setLifeTime(itemLifeTime)
                    .setBoxCount(itemBoxCount)
                    .setBoxCapacity(itemBoxCapacity)
                    .setComment(itemComment)
                    .setContractor(new ru.yandex.market.logistic.api.model.fulfillment.Contractor(
                            contractorId, contractorName))
                    .setRemainingLifetimes(createRemainingLifetimesToApi())
                    .setUpdatedDateTime(expectedUpdatedDateTime)
                    .setCategoryId(categoryId)
                    .setUndefinedCount(itemUndefinedCount)
                    .setInstances(List.of(Map.of("CIS", "123abc")))
                    .setBase64EncodedInstances(List.of(Map.of("CIS", "MTIzYWJj")))
                    .setSurplusAllowed(surplusAllowed)
                    .setSnMask(snMask)
                    .setCheckSn(checkSn)
                    .setImeiMask(imeiMask)
                    .setCheckImei(checkImei)
                    .setCisHandleMode(ru.yandex.market.logistic.api.model.fulfillment.CisHandleMode.ACCEPT_ONLY_DECLARED)
                    .build()
            ).build();

        final ru.yandex.market.logistic.api.model.fulfillment.Car expectedCar =
            new ru.yandex.market.logistic.api.model.fulfillment.Car.CarBuilder(carNumber).build();

        ru.yandex.market.logistic.api.model.fulfillment.Inbound expectedInbound =
            new ru.yandex.market.logistic.api.model.fulfillment.Inbound.InboundBuilder(
                new ResourceId(yandexId, partnerId),
                InboundType.DEFAULT,
                Collections.singletonList(expectedConsignment),
                ru.yandex.market.logistic.api.utils.DateTimeInterval.fromFormattedValue(interval))
                .setWarehouse(new ru.yandex.market.logistic.api.model.fulfillment.Warehouse.WarehouseBuilder(
                        new ResourceId(warehouseYandexId, warehousePartnerId),
                        new ru.yandex.market.logistic.api.model.fulfillment.Location.LocationBuilder(
                            "Россия", "Москва", "Московская область").build(),
                        Collections.emptyList(),
                        warehouseInc
                    )
                    .setResourceId(new ResourceId(yandexId, partnerId))
                        .setInstruction(warehouseInstruction)
                        .setContact(
                                new ru.yandex.market.logistic.api.model.fulfillment.Person.PersonBuilder(personName)
                                        .build())
                        .setPhones(Collections.singletonList(
                                new ru.yandex.market.logistic.api.model.fulfillment.Phone.PhoneBuilder(phoneNumber)
                                        .build()))
                        .build()
                )
                .setCourier(new ru.yandex.market.logistic.api.model.fulfillment.Courier.CourierBuilder(
                    Collections.singletonList(
                            new ru.yandex.market.logistic.api.model.fulfillment.Person.PersonBuilder(courierName)
                                    .build()))
                    .setCar(expectedCar)
                    .build())
                .setComment(inboundComment)
                .build();

        assertions.assertThat(convertedInbound)
            .as("Asserting the actual inbound details is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expectedInbound);
    }

    @Test
    public void convertReferenceItemFromApi() {
        String id = "id0";
        long vendorId = 0;
        String article = "article";
        int width = 100;
        int height = 100;
        int length = 100;
        BigDecimal weightGross = BigDecimal.valueOf(100);
        BigDecimal weightNet = BigDecimal.valueOf(99);
        BigDecimal weightTare = BigDecimal.valueOf(1);
        int lifeTime = 30;
        String code = "code";
        String type = "type";
        BarcodeSource gatewaySource = BarcodeSource.PARTNER;
        ru.yandex.market.logistic.api.model.fulfillment.BarcodeSource apiSource =
            ru.yandex.market.logistic.api.model.fulfillment.BarcodeSource.PARTNER;
        String itemId = "id1";
        long itemVendorId = 1;
        String itemArticle = "article1";
        String itemName = "itemName";
        int itemCount = 10;
        BigDecimal itemPrice = BigDecimal.valueOf(1000);
        CargoType itemCargoType = CargoType.DANGEROUS_CARGO;
        ru.yandex.market.logistic.api.model.fulfillment.CargoType apiCargoType =
            ru.yandex.market.logistic.api.model.fulfillment.CargoType.DANGEROUS_CARGO;
        List<CargoType> itemCargoTypes = Collections.singletonList(itemCargoType);
        CargoTypes apiCargoTypes = new CargoTypes(Collections.singletonList(apiCargoType));

        ItemReference expectedReferenceItems =
            new ItemReference(
                new UnitId(id, vendorId, article),
                new Korobyte(width, height, length, weightGross, weightNet, weightTare),
                lifeTime,
                Collections.singleton(new Barcode(code, type, gatewaySource)),
                new Item.ItemBuilder(itemName, itemCount, itemPrice, itemCargoType, List.of())
                    .setUnitId(new UnitId(itemId, itemVendorId, itemArticle))
                    .setBarcodes(Collections.emptyList())
                    .setCargoTypes(itemCargoTypes)
                    .setInboundServices(Collections.emptyList())
                    .setCisHandleMode(CisHandleMode.NOT_DEFINED)
                    .build()
            );

        ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemReference apiReferenceItem =
            new ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemReference.ItemReferenceBuilder(
                new ru.yandex.market.logistic.api.model.fulfillment.UnitId(id, vendorId, article))
                .setKorobyte(new ru.yandex.market.logistic.api.model.fulfillment.Korobyte.KorobyteBuiler(
                    width,
                    height,
                    length,
                    weightGross)
                    .setWeightNet(weightNet)
                    .setWeightTare(weightTare)
                    .build()
                )
                .setLifeTime(lifeTime)
                .setBarcodes(Collections.singleton(
                        new ru.yandex.market.logistic.api.model.fulfillment.Barcode.BarcodeBuilder(code)
                    .setSource(apiSource)
                    .setType(type)
                    .build())
                )
                .setItem(new ru.yandex.market.logistic.api.model.fulfillment.Item.ItemBuilder(itemName,
                    itemCount,
                    itemPrice)
                    .setUnitId(new ru.yandex.market.logistic.api.model.fulfillment.UnitId(itemId, itemVendorId,
                        itemArticle))
                    .setCargoType(apiCargoType)
                    .setCargoTypes(apiCargoTypes)
                    .build()
                )
                .build();

        List<ItemReference> actualReferenceItems = FulfillmentConverter
            .convertReferenceItemsFromApi(Collections.singletonList(apiReferenceItem));

        assertions
            .assertThat(actualReferenceItems.get(0))
            .as("Asserting the actual item reference list is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expectedReferenceItems);
    }

    @Test
    public void convertInboundHistoryFromApi() {

        String yandexId = "777";
        String partnerId = "888";
        String dateTime = "2018-12-21T11:59:59+03:00";

        ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId expectedResourceId =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                .setYandexId(yandexId)
                .setPartnerId(partnerId)
                .build();

        InboundStatusHistory expectedInboundStatusHistory =
            new InboundStatusHistory(Collections.singletonList(
                new Status(StatusCode.ACCEPTED,
                    new ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime(dateTime))),
                expectedResourceId);

        ResourceId resourceId = new ResourceId(yandexId, partnerId);
        InboundStatusHistory inboundStatusHistory = InboundConverter.convertInboundHistoryFromApi(
            new ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusHistory(
                Collections.singletonList(
                    new ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatus(
                        resourceId,
                        ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusType.ACCEPTED,
                        new DateTime(dateTime))), resourceId)).orElse(null);


        assertions.assertThat(inboundStatusHistory)
            .as("Asserting the actual inbound history is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expectedInboundStatusHistory);
    }

    @Test
    public void convertItemExpirationsListToApi() {
        final ru.yandex.market.logistic.gateway.common.model.fulfillment.Expiration expectedExpiration =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.Expiration(
                new ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime("2016-03-21T12:34:56+03:00"),
                Collections.singletonList(DtoFactory.createStock())
            );

        final ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemExpiration expectedItemExpiration =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemExpiration(
                new UnitId("111", 222L, "333"),
                Collections.singletonList(expectedExpiration)
            );

        final Expiration apiExpiration = new Expiration(new DateTime("2016-03-21T12:34:56+03:00"),
            Collections.singletonList(getStockApi()));

        final ItemExpiration apiItemExpiration = new ItemExpiration(
            new ru.yandex.market.logistic.api.model.fulfillment.UnitId.UnitIdBuilder(222L, "333")
                .setId("111")
                .build(),
            Collections.singletonList(apiExpiration));


        final List<ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemExpiration> result =
            FulfillmentConverter.convertItemExpirationsListFromApi(Collections.singletonList(apiItemExpiration));

        assertions.assertThat(result)
            .isNotNull();
        assertions.assertThat(result)
            .hasSize(1);
        assertions.assertThat(result.get(0))
            .as("Asserting the actual item expirations is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expectedItemExpiration);
    }

    @Test
    public void convertOutboundStatusFromApi() {
        OutboundStatus outboundStatusIn = getOutboundStatusApi();
        ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundStatus expectedOutboundStatus =
            getOutboundStatusNotApi();

        Optional<ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundStatus> actualOutboundStatus =
            OutboundConverter.convertOutboundStatusFromApi(outboundStatusIn);

        assertions.assertThat(actualOutboundStatus)
            .as("Asserting that actual outbound status optional is present")
            .isPresent();
        assertions.assertThat(actualOutboundStatus.get())
            .as("Asserting that outbound status wrapper is converted correctly")
            .isEqualToComparingFieldByFieldRecursively(expectedOutboundStatus);
    }

    @Test
    public void convertRegisterToApi() {
        Register register = createRegister();
        Optional<ru.yandex.market.logistic.api.model.fulfillment.Register> actualRegister =
            CreateRegisterConverter.convertRegisterToApi(register);

        assertions.assertThat(actualRegister)
            .as("Asserting that actual register optional is present")
            .isPresent();
        assertions.assertThat(actualRegister.get())
            .as("Asserting that actual register optional is converted correctly")
            .isEqualToComparingFieldByFieldRecursively(ApiDtoFactory.createRegister());
    }

    @Test
    public void convertCreateRegisterResponseFromApi() {
        ru.yandex.market.logistic.api.model.fulfillment.response.CreateRegisterResponse createRegisterResponse =
            ApiDtoFactory.createCreateRegisterResponse();

        Optional<CreateRegisterResponse> actualCreateRegisterResponse =
            CreateRegisterConverter.convertCreateRegisterResponseFromApi(createRegisterResponse);

        assertions.assertThat(actualCreateRegisterResponse)
            .as("Asserting that actual create register response optional is present")
            .isPresent();
        assertions.assertThat(actualCreateRegisterResponse.get())
            .as("Asserting that actual create register response optional is converted correctly")
            .isEqualToComparingFieldByFieldRecursively(createCreateRegisterResponse());
    }

    @Test
    public void convertIntakeToApi() {
        Intake intake = createIntake();
        Optional<ru.yandex.market.logistic.api.model.fulfillment.Intake> actualIntake =
            CreateIntakeConverter.convertIntakeToApi(intake);

        assertions.assertThat(actualIntake)
            .as("Asserting that actual intake optional is present")
            .isPresent();
        assertions.assertThat(actualIntake.get())
            .as("Asserting that actual intake optional is converted correctly")
            .isEqualToComparingFieldByFieldRecursively(ApiDtoFactory.createIntake());
    }

    @Test
    public void convertCreateIntakeResponseFromApi() {
        ru.yandex.market.logistic.api.model.fulfillment.response.CreateIntakeResponse response =
            ApiDtoFactory.createCreateIntakeResponse();

        Optional<CreateIntakeResponse> actualResponse =
            CreateIntakeConverter.convertCreateIntakeResponseFromApi(response);

        assertions.assertThat(actualResponse)
            .as("Asserting that actual create intake response optional is present")
            .isPresent();

        assertions.assertThat(actualResponse.get())
            .as("Asserting that actual create intake response optional is converted correctly")
            .isEqualToComparingFieldByFieldRecursively(createCreateIntakeResponse());
    }

    @Test
    public void convertReturnRegisterToApi() {
        ReturnRegister returnRegister = createReturnRegister();
        Optional<ru.yandex.market.logistic.api.model.fulfillment.ReturnRegister> actualRegister =
            CreateReturnRegisterConverter.convertReturnRegisterToApi(returnRegister);

        assertions.assertThat(actualRegister)
            .as("Asserting that actual register optional is present")
            .isPresent();
        assertions.assertThat(actualRegister.get())
            .as("Asserting that actual register optional is converted correctly")
            .isEqualToComparingFieldByFieldRecursively(ApiDtoFactory.createReturnRegister());
    }

    private ItemStocks getItemStocksApi() {
        return new ItemStocks.ItemStocksBuilder(
            Collections.singletonList(getStockApi()))
            .setUnitId(new ru.yandex.market.logistic.api.model.fulfillment.UnitId.UnitIdBuilder(222L, "333")
                .setId("111")
                .build())
            .setWarehouseId(new ResourceId.ResourceIdBuilder().setYandexId("9955214").build())
            .build();
    }

    private Stock getStockApi() {
        return new Stock(StockType.QUARANTINE, 3, new DateTime("2016-03-21T12:34:56+03:00"));
    }

    private OutboundStatus getOutboundStatusApi() {
        ResourceId outboundId = new ResourceId.ResourceIdBuilder()
            .setYandexId("123")
            .setPartnerId("ABC123")
            .build();

        ru.yandex.market.logistic.api.model.fulfillment.response.entities.Status status =
            new ru.yandex.market.logistic.api.model.fulfillment.response.entities.Status(
                ru.yandex.market.logistic.api.model.fulfillment.response.entities.StatusCode.ASSEMBLING,
                new DateTime("2019-01-10T18:42:59+03:00"));

        return new OutboundStatus(outboundId, status);
    }

    private ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundStatus getOutboundStatusNotApi() {
        ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId outboundId =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                .setYandexId("123")
                .setPartnerId("ABC123")
                .build();

        ru.yandex.market.logistic.gateway.common.model.fulfillment.Status status =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.Status(
                ru.yandex.market.logistic.gateway.common.model.fulfillment.StatusCode.ASSEMBLING,
                new ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime("2019-01-10T18:42:59+03:00")
            );

        return new ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundStatus(outboundId, status);
    }

    private ru.yandex.market.logistic.gateway.common.model.fulfillment.RemainingLifetimes
    createRemainingLifetimesFromApi() {
        return new ru.yandex.market.logistic.gateway.common.model.fulfillment.RemainingLifetimes(
            createShelfLivesFromApi(),
            createShelfLivesFromApi()
        );
    }

    private ru.yandex.market.logistic.gateway.common.model.fulfillment.ShelfLives createShelfLivesFromApi() {
        return new ru.yandex.market.logistic.gateway.common.model.fulfillment.ShelfLives(
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ShelfLife(10),
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ShelfLife(30)
        );
    }

    private RemainingLifetimes createRemainingLifetimesToApi() {
        return new RemainingLifetimes(createShelfLivesToApi(), createShelfLivesToApi());
    }

    private ShelfLives createShelfLivesToApi() {
        return new ShelfLives(new ShelfLife(10), new ShelfLife(30));
    }
}
