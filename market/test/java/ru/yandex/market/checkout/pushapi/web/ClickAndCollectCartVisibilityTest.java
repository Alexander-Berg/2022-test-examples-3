package ru.yandex.market.checkout.pushapi.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartParameters;
import ru.yandex.market.checkout.pushapi.service.EnvironmentService;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.personal_market.PersonalMarketService;
import ru.yandex.market.personal_market.PersonalRetrieveResponse;
import ru.yandex.market.personal_market.client.model.CommonType;
import ru.yandex.market.personal_market.client.model.CommonTypeEnum;
import ru.yandex.market.personal_market.client.model.FullName;
import ru.yandex.market.personal_market.client.model.GpsCoord;
import ru.yandex.market.personal_market.client.model.MultiTypeRetrieveResponseItem;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_PHONE;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_UID;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES;
import static ru.yandex.market.checkout.pushapi.service.RequestDetailsService.PERSONAL_DATA_ENABLE_PERCENT;

public class ClickAndCollectCartVisibilityTest extends AbstractOrderVisibilityTestBase {

    private static final String UTF8 = StandardCharsets.UTF_8.name();

    @Autowired
    private PushApiCartHelper pushApiCartHelper;

    @Autowired
    private WireMockServer shopadminStubMock;

    @Autowired
    private PersonalMarketService mockPersonalMarketService;

    @Autowired
    private EnvironmentService environmentService;


    private static final DeliveryType deliveryType = DeliveryType.DELIVERY;
    private Map<OrderVisibility, Boolean> orderVisibilityMap;

    PushApiCartParameters parameters;

    @BeforeEach
    public void setUpLocal() {
        parameters = new PushApiCartParameters();
        prepareDelivery(parameters.getRequest().getDelivery(), deliveryType);
        prepareClickAndCollectCart(parameters.getRequest());
    }

    @Test
    public void cartWithNullVisibilityMap() throws Exception {
        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "0");
        // Arrange
        orderVisibilityMap = null;
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        when(mockPersonalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(List.of())));
        // Act
        mockSettingsForDifferentParameters(parameters);
        pushApiCartHelper.cart(parameters);

        // Assert
        checkCartVisibility(false);
    }

    @Test
    public void cartWithEmptyVisibilityMap() throws Exception {
        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "0");
        // Arrange
        orderVisibilityMap = emptyMap();
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        when(mockPersonalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(List.of())));
        // Act
        mockSettingsForDifferentParameters(parameters);
        pushApiCartHelper.cart(parameters);

        // Assert
        checkCartVisibility(false);
    }

    @Test
    public void cartWithBuyerInVisibilityMap() throws Exception {
        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "0");
        // Arrange
        orderVisibilityMap = singletonMap(BUYER, true);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        when(mockPersonalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(List.of())));
        // Act
        mockSettingsForDifferentParameters(parameters);
        pushApiCartHelper.cart(parameters);

        // Assert
        checkCartVisibility(false);
    }

    @Test
    public void cartWithIgnoreDataHiding() throws Exception {
        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "0");
        // Arrange
        orderVisibilityMap = ImmutableMap.of(BUYER, true, IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        // Act
        mockSettingsForDifferentParameters(parameters);
        pushApiCartHelper.cart(parameters);

        // Assert
        checkCartVisibility(true);
    }

    @Test
    public void checkVisibilityCartWithoutBuyerUid() throws Exception {
        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "100");

        // Arrange
        orderVisibilityMap = ImmutableMap.of(
                BUYER, true,
                IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true,
                BUYER_PHONE, false,
                BUYER_UID, false);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        when(mockPersonalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(
                        List.of()
                ))
        );

        parameters.getRequest().getBuyer().setPersonalFullNameId(null);
        parameters.getRequest().getBuyer().setPersonalPhoneId(null);
        parameters.getRequest().getBuyer().setPersonalEmailId(null);

        // Act
        mockSettingsForDifferentParameters(parameters);
        pushApiCartHelper.cart(parameters);

        // Assert
        checkCartVisibility(true);
    }

    @Test
    public void checkVisibilityBuyerWithPersonal() throws Exception {

        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "100");

        var fullName = new MultiTypeRetrieveResponseItem();
        var email = new MultiTypeRetrieveResponseItem();
        var phone = new MultiTypeRetrieveResponseItem();

        fullName.setId("a1c595eb35404207aecfa080f90a8986");
        email.setId("9e92bc743c624f958b8876c7841a653b");
        phone.setId("c0dec0dedec0dec0dec0dec0dedec0de");

        fullName.setType(CommonTypeEnum.FULL_NAME);
        email.setType(CommonTypeEnum.EMAIL);
        phone.setType(CommonTypeEnum.PHONE);

        {
            var type = new CommonType();
            var name = new FullName();
            name.setForename("Fedor");
            name.setSurname("Dostoevksy");
            name.setPatronymic("Mihailovich");

            type.setFullName(name);
            fullName.setValue(type);
        }

        {
            var type = new CommonType();
            type.setEmail("workerAndMargo@begemot.vo");
            email.setValue(type);
        }

        {
            var type = new CommonType();
            type.setPhone("6666666666");
            phone.setValue(type);
        }

        when(mockPersonalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(
                        List.of(
                                fullName,
                                email,
                                phone
                        )
                ))
        );

        // Arrange
        orderVisibilityMap = ImmutableMap.of(
                BUYER, true,
                IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true,
                BUYER_UID, false);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        // Act

        mockSettingsForDifferentParameters(parameters);
        pushApiCartHelper.cart(parameters);

        // Assert
        checkCartVisibility(false, true, deliveryType, orderVisibilityMap);
    }

    @Test
    public void checkVisibilityBuyerWithPersonalPartially() throws Exception {
        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "100");

        var fullName = new MultiTypeRetrieveResponseItem();
        var email = new MultiTypeRetrieveResponseItem();

        fullName.setId("a1c595eb35404207aecfa080f90a8986");
        email.setId("9e92bc743c624f958b8876c7841a653b");

        fullName.setType(CommonTypeEnum.FULL_NAME);
        email.setType(CommonTypeEnum.EMAIL);

        {
            var type = new CommonType();
            var name = new FullName();
            name.setForename("Fedor");
            name.setSurname("Dostoevksy");
            name.setPatronymic("Mihailovich");

            type.setFullName(name);
            fullName.setValue(type);
        }

        {
            var type = new CommonType();
            type.setEmail("workerAndMargo@begemot.vo");
            email.setValue(type);
        }

        when(mockPersonalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(
                        List.of(
                                fullName,
                                email
                        )
                ))
        );

        // Arrange
        orderVisibilityMap = ImmutableMap.of(
                BUYER, true,
                IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true,
                BUYER_UID, false);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        parameters.getRequest().getBuyer().setPersonalFullNameId(null);
        parameters.getRequest().getBuyer().setPersonalEmailId(null);

        // Act

        mockSettingsForDifferentParameters(parameters);
        pushApiCartHelper.cart(parameters);

        // Assert
        checkCartVisibility(false, true, deliveryType, orderVisibilityMap);
    }

    @Test
    public void checkVisibilityAddressWithPersonal() throws Exception {
        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "100");

        var fullName = new MultiTypeRetrieveResponseItem();
        var email = new MultiTypeRetrieveResponseItem();
        var phone = new MultiTypeRetrieveResponseItem();

        fullName.setId("a1c595eb35404207aecfa080f90a8986");
        email.setId("9e92bc743c624f958b8876c7841a653b");
        phone.setId("c0dec0dedec0dec0dec0dec0dedec0de");

        fullName.setType(CommonTypeEnum.FULL_NAME);
        email.setType(CommonTypeEnum.EMAIL);
        phone.setType(CommonTypeEnum.PHONE);

        {
            var type = new CommonType();
            var name = new FullName();
            name.setForename("Fedor");
            name.setSurname("Dostoevksy");
            name.setPatronymic("Mihailovich");

            type.setFullName(name);
            fullName.setValue(type);
        }

        {
            var type = new CommonType();
            type.setEmail("workerAndMargo@begemot.vo");
            email.setValue(type);
        }

        {
            var type = new CommonType();
            type.setPhone("6666666666");
            phone.setValue(type);
        }

        when(mockPersonalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(
                        List.of(
                                fullName,
                                email,
                                phone
                        )
                ))
        );

        // Arrange
        orderVisibilityMap = ImmutableMap.of(
                BUYER, true,
                IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true,
                BUYER_UID, false);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        // Act

        mockSettingsForDifferentParameters(parameters);
        pushApiCartHelper.cart(parameters);

        // Assert
        checkCartVisibility(true);
    }

    @Test
    public void cartBuyerWithPersonal() throws Exception {

        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "100");

        var fullName = new MultiTypeRetrieveResponseItem();
        var email = new MultiTypeRetrieveResponseItem();
        var phone = new MultiTypeRetrieveResponseItem();

        fullName.setId("a1c595eb35404207aecfa080f90a8986");
        email.setId("9e92bc743c624f958b8876c7841a653b");
        phone.setId("c0dec0dedec0dec0dec0dec0dedec0de");

        fullName.setType(CommonTypeEnum.FULL_NAME);
        email.setType(CommonTypeEnum.EMAIL);
        phone.setType(CommonTypeEnum.PHONE);

        {
            var type = new CommonType();
            var name = new FullName();
            name.setForename("Fedor");
            name.setSurname("Dostoevksy");
            name.setPatronymic("Mihailovich");

            type.setFullName(name);
            fullName.setValue(type);
        }

        {
            var type = new CommonType();
            type.setEmail("workerAndMargo@begemot.vo");
            email.setValue(type);
        }

        {
            var type = new CommonType();
            type.setPhone("6666666666");
            phone.setValue(type);
        }

        when(mockPersonalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(
                        List.of(
                                fullName,
                                email,
                                phone
                        )
                ))
        );

        // Arrange
        orderVisibilityMap = ImmutableMap.of(
                BUYER, true,
                IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true,
                BUYER_UID, false);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        // Act
        parameters.setDataType(DataType.JSON);
        mockSettingsForDifferentParameters(parameters);
        pushApiCartHelper.cart(parameters);

        // Assert
        Map<String, Object> root = getRequestBody();
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "firstName"))), "Fedor");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "lastName"))), "Dostoevksy");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "middleName"))), "Mihailovich");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "email"))), "workerAndMargo@begemot.vo");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "phone"))), "6666666666");
    }

    @Test
    public void cartBuyerWithPersonalPartially() throws Exception {
        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "100");

        var fullName = new MultiTypeRetrieveResponseItem();
        var email = new MultiTypeRetrieveResponseItem();
        var address = new MultiTypeRetrieveResponseItem();

        fullName.setId("cd368e42341ff6ca7bbd12a05998e705");
        email.setId("9e92bc743c624f958b8876c7841a653b");
        address.setId("34251639gwbcesaqq239098jhcdxe453");

        fullName.setType(CommonTypeEnum.FULL_NAME);
        email.setType(CommonTypeEnum.EMAIL);

        {
            var type = new CommonType();
            var name = new FullName();
            name.setForename("Fedor");
            name.setSurname("Dostoevksy");
            name.setPatronymic("Mihailovich");

            type.setFullName(name);
            fullName.setValue(type);
        }

        {
            var type = new CommonType();
            type.setEmail("workerAndMargo@begemot.vo");
            email.setValue(type);
        }

        {
            var type = new CommonType();
            type.setAddress(Map.of("city", "Moscow", "street", "Petrovka", "house", "38"));
            address.setValue(type);
        }

        when(mockPersonalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(
                        List.of(
                                fullName,
                                email,
                                address
                        )
                ))
        );

        // Arrange
        orderVisibilityMap = ImmutableMap.of(
                BUYER, true,
                IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true,
                BUYER_UID, false);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);


        parameters.getRequest().getBuyer().setPersonalFullNameId(null);
        parameters.getRequest().getBuyer().setPersonalPhoneId(null);

        // Act
        parameters.setDataType(DataType.JSON);
        mockSettingsForDifferentParameters(parameters);
        pushApiCartHelper.cart(parameters);

        // Assert

        Map<String, Object> root = getRequestBody();
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "firstName"))), "Leo");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "lastName"))), "Tolstoy");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "email"))), "workerAndMargo@begemot.vo");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "phone"))), "+71234567891");
    }

    @Test
    public void cartAddressWithPersonal() throws Exception {
        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "100");

        var fullName = new MultiTypeRetrieveResponseItem();
        var email = new MultiTypeRetrieveResponseItem();
        var phone = new MultiTypeRetrieveResponseItem();
        var address = new MultiTypeRetrieveResponseItem();
        var gps = new MultiTypeRetrieveResponseItem();

        fullName.setId("a1c595eb35404207aecfa080f90a8986");
        email.setId("9e92bc743c624f958b8876c7841a653b");
        phone.setId("c0dec0dedec0dec0dec0dec0dedec0de");
        address.setId("sfsafdaffasfafasfaf");
        gps.setId("sdfjsdfgffhfafhasjfaijf");

        fullName.setType(CommonTypeEnum.FULL_NAME);
        email.setType(CommonTypeEnum.EMAIL);
        phone.setType(CommonTypeEnum.PHONE);
        address.setType(CommonTypeEnum.ADDRESS);
        gps.setType(CommonTypeEnum.GPS_COORD);

        {
            var type = new CommonType();
            var name = new FullName();
            name.setForename("Fedor");
            name.setSurname("Dostoevksy");
            name.setPatronymic("Mihailovich");

            type.setFullName(name);
            fullName.setValue(type);
        }

        {
            var type = new CommonType();
            type.setEmail("workerAndMargo@begemot.vo");
            email.setValue(type);
        }

        {
            var type = new CommonType();
            type.setPhone("6666666666");
            phone.setValue(type);
        }

        {
            var type = new CommonType();
            type.setAddress(Map.of("city", "Moscow", "street", "Petrovka", "house", "38"));
            address.setValue(type);
        }

        {
            var type = new CommonType();
            var coord = new GpsCoord();
            coord.setLatitude(BigDecimal.valueOf(2424.0));
            coord.setLongitude(BigDecimal.valueOf(-2324.0));
            type.setGpsCoord(coord);
            gps.setValue(type);
        }

        when(mockPersonalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(
                        List.of(
                                fullName,
                                email,
                                phone,
                                address,
                                gps
                        )
                ))
        );

        // Arrange
        orderVisibilityMap = ImmutableMap.of(
                BUYER, true,
                IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true,
                BUYER_UID, false);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        // Act
        parameters.setDataType(DataType.JSON);

        var shopAddress = new AddressImpl();
        shopAddress.setBlock("1");
        shopAddress.setHouse("2");
        shopAddress.setStreet("3");
        shopAddress.setFloor("4");
        shopAddress.setEntrance("5");
        shopAddress.setCity("6");
        shopAddress.setCountry("8");
        shopAddress.setType(AddressType.SHOP);

        shopAddress.setPersonalFullNameId("a1c595eb35404207aecfa080f90a8986");
        shopAddress.setPersonalAddressId("sfsafdaffasfafasfaf");
        shopAddress.setPersonalGpsId("sdfjsdfgffhfafhasjfaijf");
        shopAddress.setPersonalPhoneId("c0dec0dedec0dec0dec0dec0dedec0de");

        mockSettingsForDifferentParameters(parameters);
        parameters.getRequest().getDelivery().setShopAddress(shopAddress);

        pushApiCartHelper.cart(parameters);

        // Assert
        Map<String, Object> root = getRequestBody();
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "firstName"))), "Fedor");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "lastName"))), "Dostoevksy");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "middleName"))), "Mihailovich");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "email"))), "workerAndMargo@begemot.vo");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "phone"))), "6666666666");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "delivery", "address", "city"))), "Moscow");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "delivery", "address", "house"))), "38");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "delivery", "address", "phone"))), "6666666666");
    }

    @Test
    public void cartAddressWithPersonalWithoutPhone() throws Exception {
        environmentService.deleteValue(PERSONAL_DATA_ENABLE_PERCENT);
        environmentService.addValue(PERSONAL_DATA_ENABLE_PERCENT, "100");

        var fullName = new MultiTypeRetrieveResponseItem();
        var email = new MultiTypeRetrieveResponseItem();
        var phone = new MultiTypeRetrieveResponseItem();
        var address = new MultiTypeRetrieveResponseItem();
        var gps = new MultiTypeRetrieveResponseItem();

        fullName.setId("a1c595eb35404207aecfa080f90a8986");
        email.setId("9e92bc743c624f958b8876c7841a653b");
        phone.setId("c0dec0dedec0dec0dec0dec0dedec0de");
        address.setId("sfsafdaffasfafasfaf");
        gps.setId("sdfjsdfgffhfafhasjfaijf");

        fullName.setType(CommonTypeEnum.FULL_NAME);
        email.setType(CommonTypeEnum.EMAIL);
        phone.setType(CommonTypeEnum.PHONE);
        address.setType(CommonTypeEnum.ADDRESS);
        gps.setType(CommonTypeEnum.GPS_COORD);

        {
            var type = new CommonType();
            var name = new FullName();
            name.setForename("Fedor");
            name.setSurname("Dostoevksy");
            name.setPatronymic("Mihailovich");

            type.setFullName(name);
            fullName.setValue(type);
        }

        {
            var type = new CommonType();
            type.setEmail("workerAndMargo@begemot.vo");
            email.setValue(type);
        }

        {
            var type = new CommonType();
            type.setPhone("6666666666");
            phone.setValue(type);
        }

        {
            var type = new CommonType();
            type.setAddress(Map.of("city", "Moscow", "street", "Petrovka", "house", "38"));
            address.setValue(type);
        }

        {
            var type = new CommonType();
            var coord = new GpsCoord();
            coord.setLatitude(BigDecimal.valueOf(2424.0));
            coord.setLongitude(BigDecimal.valueOf(-2324.0));
            type.setGpsCoord(coord);
            gps.setValue(type);
        }

        when(mockPersonalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(new PersonalRetrieveResponse(
                        List.of(
                                fullName,
                                email,
                                phone,
                                address,
                                gps
                        )
                ))
        );

        // Arrange
        orderVisibilityMap = ImmutableMap.of(
                BUYER, true,
                BUYER_PHONE, false,
                IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true,
                BUYER_UID, false);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        // Act
        parameters.setDataType(DataType.JSON);

        var shopAddress = new AddressImpl();
        shopAddress.setBlock("1");
        shopAddress.setHouse("2");
        shopAddress.setStreet("3");
        shopAddress.setFloor("4");
        shopAddress.setEntrance("5");
        shopAddress.setCity("6");
        shopAddress.setCountry("8");
        shopAddress.setType(AddressType.SHOP);

        shopAddress.setPersonalFullNameId("a1c595eb35404207aecfa080f90a8986");
        shopAddress.setPersonalAddressId("sfsafdaffasfafasfaf");
        shopAddress.setPersonalGpsId("sdfjsdfgffhfafhasjfaijf");
        shopAddress.setPersonalPhoneId("c0dec0dedec0dec0dec0dec0dedec0de");

        mockSettingsForDifferentParameters(parameters);
        parameters.getRequest().getDelivery().setShopAddress(shopAddress);

        pushApiCartHelper.cart(parameters);

        // Assert
        Map<String, Object> root = getRequestBody();
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "firstName"))), "Fedor");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "lastName"))), "Dostoevksy");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "middleName"))), "Mihailovich");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "email"))), "workerAndMargo@begemot.vo");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "buyer", "phone"))), null);
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "delivery", "address", "city"))), "Moscow");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "delivery", "address", "house"))), "38");
        Assertions.assertEquals(
                getByPath(root, new ArrayList<>(Arrays.asList("cart", "delivery", "address", "phone"))), null);
    }

    private Map<String, Object> getRequestBody() throws IOException {
        var serveEvents = shopadminStubMock.getAllServeEvents();
        assertThat(serveEvents, hasSize(1));
        ServeEvent event = Iterables.getOnlyElement(serveEvents);

        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.readValue(event.getRequest().getBodyAsString(), Map.class);
    }

    private Object getByPath(Object root, ArrayList<String> path) {
        if (path.isEmpty()) {
            return root;
        } else {
            String step = path.remove(0);
            return getByPath(((Map<String, Object>) root).get(step), path);
        }
    }

    private void checkCartVisibility(boolean showBuyer) throws IOException {
        checkCartVisibility(false, showBuyer, deliveryType, orderVisibilityMap);
    }
}
