package ru.yandex.market.api.user.order;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.yandex.market.api.common.RoomAddress;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.OfferIdEncodingService;
import ru.yandex.market.api.error.ApiError;
import ru.yandex.market.api.error.InvalidParameterValueException;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.error.validation.IncorrectParameterValueError;
import ru.yandex.market.api.error.validation.MissingRequiredParameterError;
import ru.yandex.market.api.error.validation.ParameterErrorType;
import ru.yandex.market.api.error.validation.StringParamLengthValidationError;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.geo.domain.RegionType;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.offer.OfferService;
import ru.yandex.market.api.test.ExceptionMatcher;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.user.order.checkout.AddressDeliveryPoint;
import ru.yandex.market.api.user.order.checkout.CheckoutRequest;
import ru.yandex.market.api.user.order.checkout.DeliveryPointId;
import ru.yandex.market.api.user.order.preorder.OrderOptionsRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithMocks
@SuppressFBWarnings("SE_BAD_FIELD")
public class OrderRequestValidatorTest extends UnitTestBase {

    @InjectMocks
    private OrderRequestValidator validator;

    @Mock
    private OfferService offerService;

    @Mock
    private GeoRegionService geoRegionService;

    @Mock
    private OfferIdEncodingService offerIdEncodingService;

    @Before
    public void setUp() throws Exception {
        when(offerIdEncodingService.decode(any())).thenAnswer(invocation -> {
            String s = (String) invocation.getArguments()[0];
            if (s == null) {
                return new OfferId(null, null);
            }
            String[] split = s.split(":");
            return new OfferId(split[0], split.length > 1 ? split[1] : null);
        });

        when(geoRegionService.getInfo(eq(TEST_USER_REGION_ID))).thenReturn(new GeoRegion(0, null, RegionType.CITY, null, null));
        when(geoRegionService.getInfo(eq(TEST_COUNTRY_REGION_ID))).thenReturn(new GeoRegion(0, null, RegionType.COUNTRY, null, null));
    }

    @Test
    public void shouldValidateCorrectOrderOptionRequest() throws Exception {
        ValidationErrors errors = new ValidationErrors();
        validator.validate(generateOrderOptionRequestWithOneOrder(
            TEST_USER_ID,
            TEST_USER_REGION_ID,
            TEST_USER_CURRENCY,
            TEST_SHOP_ID,
            new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW),
            TEST_OFFER_RIGHT_FEE_SHOW,
            TEST_OFFER_PRICE,
            1
        ), errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void shouldValidateCorrectOrderOptionRequestWithOldOfferId() throws Exception {
        ValidationErrors errors = new ValidationErrors();
        validator.validate(generateOrderOptionRequestWithOneOrder(
            TEST_USER_ID,
            TEST_USER_REGION_ID,
            TEST_USER_CURRENCY,
            TEST_SHOP_ID,
            new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW),
            null,
            TEST_OFFER_PRICE,
            1
        ), errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void shouldNotValidateOrderOptionRequestWithIncorrectRegion() throws Exception {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {
            @Override
            protected boolean match(InvalidParameterValueException e) {
                String message = "Region id " + TEST_COUNTRY_REGION_ID + " of type COUNTRY is not allowed";
                return message.equals(e.getMessage()) &&
                    e.getErrors().contains(new ValidationError(
                        ParameterErrorType.INVALID_PROPERTY,
                        OrderRequestValidator.CheckoutRequestXpathError.REGION_ID,
                        message
                    ));
            }
        });

        ValidationErrors errors = new ValidationErrors();
        validator.validate(generateOrderOptionRequestWithOneOrderAndAddress(
            TEST_USER_ID,
            TEST_COUNTRY_REGION_ID,
            TEST_USER_CURRENCY,
            TEST_SHOP_ID,
            new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW),
            TEST_OFFER_RIGHT_FEE_SHOW,
            TEST_OFFER_PRICE,
            1
        ), errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void shouldValidateCorrectCheckoutReuqest() throws Exception {
        ValidationErrors errors = new ValidationErrors();
        validator.validate(generateCheckoutRequestWithOneOrder(
            TEST_USER_REGION_ID,
            TEST_USER_CURRENCY,
            TEST_SHOP_ID,
            new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW),
            TEST_OFFER_RIGHT_FEE_SHOW,
            TEST_OFFER_PRICE,
            1
        ), errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testEmptyOfferValidationCodes() {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {

                List<ValidationError> expected = Arrays.asList(
                    new ValidationError(
                        ParameterErrorType.MISSING_PROPERTY,
                        OrderRequestValidator.CheckoutRequestXpathError.REGION_ID,
                        "//order/@regionId"
                    ),
                    new ValidationError(
                        ParameterErrorType.MISSING_PROPERTY,
                        OrderRequestValidator.CheckoutRequestXpathError.CURRENCY,
                        "//order/@currency"
                    ),
                    new ValidationError(
                        ParameterErrorType.MISSING_PROPERTY,
                        OrderRequestValidator.CheckoutRequestXpathError.BUYER,
                        "//order/buyer"
                    ),
                    new ValidationError(
                        ParameterErrorType.MISSING_PROPERTY,
                        OrderRequestValidator.CheckoutRequestXpathError.SHOPS,
                        "//order/shops"
                    )
                );

                return e.getErrors().equals(expected);
            }
        });

        ValidationErrors errors = new ValidationErrors();
        validator.validate(new CheckoutRequest(), errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testValidateBuyer() {

        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {

                if (e.getErrors().contains(new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.BUYER_LASTNAME, "//order/buyer/@lastName"))) {
                    return false;
                }

                List<? extends ApiError> expected = Arrays.asList(
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.BUYER_FIRSTNAME, "//order/buyer/@firstName"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.BUYER_PHONE, "//order/buyer/@phone"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.BUYER_EMAIL, "//order/buyer/@email")
                );

                return e.getErrors().containsAll(expected);
            }
        });

        ValidationErrors errors = new ValidationErrors();

        CheckoutRequest request = new CheckoutRequest() {{
            setBuyer(new Buyer() {{
                setLastName("Baggins");
            }});
        }};

        validator.validate(request, errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testValidateInternalBuyers() {

        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {

                if (e.getErrors().contains(new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.ORDER_BUYER_FIRSTNAME, "//order/shops/shopOrder[0]/buyer/@firstName")) ||
                    e.getErrors().contains(new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.ORDER_BUYER_LASTNAME, "//order/shops/shopOrder[1]/buyer/@lastName")) ||
                    e.getErrors().contains(new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.BUYER, "//order/buyer"))) {
                    return false;
                }

                List<? extends ApiError> expected = Arrays.asList(
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.ORDER_BUYER_LASTNAME, "//order/shops/shopOrder[0]/buyer/@lastName"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.ORDER_BUYER_PHONE, "//order/shops/shopOrder[0]/buyer/@phone"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.ORDER_BUYER_EMAIL, "//order/shops/shopOrder[0]/buyer/@email"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.ORDER_BUYER_FIRSTNAME, "//order/shops/shopOrder[1]/buyer/@firstName"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.ORDER_BUYER_PHONE, "//order/shops/shopOrder[1]/buyer/@phone"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.ORDER_BUYER_EMAIL, "//order/shops/shopOrder[1]/buyer/@email")
                );

                return e.getErrors().containsAll(expected);
            }
        });

        ValidationErrors errors = new ValidationErrors();

        CheckoutRequest request = new CheckoutRequest() {{
            setShopOrders(new ArrayList<ShopOrder>() {{
                add(new ShopOrder() {{
                    setBuyer(new Buyer() {{
                        setFirstName("Bilbo");
                    }});
                }});
                add(new ShopOrder() {{
                    setBuyer(new Buyer() {{
                        setLastName("Beggins");
                    }});
                }});
            }});
        }};

        validator.validate(request, errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testNotAllInternalsBuyersPresent() {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {

                List<? extends ApiError> expected = Collections.singletonList(
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.BUYER, "//order/buyer")
                );

                return e.getErrors().containsAll(expected);
            }
        });

        ValidationErrors errors = new ValidationErrors();

        CheckoutRequest request = new CheckoutRequest() {{
            setShopOrders(new ArrayList<ShopOrder>() {{
                add(new ShopOrder() {{
                }});
                add(new ShopOrder() {{
                    setBuyer(new Buyer() {{
                        setLastName("Beggins");
                    }});
                }});
            }});
        }};

        validator.validate(request, errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testValidateShopOrder() {

        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {

                List<? extends ApiError> expected = Arrays.asList(
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.SHOP_ID, "//order/shops/shopOrder[0]/@shopId"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_POINT, "//order/shops/shopOrder[0]/deliveryPoint/"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.OFFER_ID, "//order/shops/shopOrder[0]/item[0]/@offerId"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.ITEM_PRICE, "//order/shops/shopOrder[0]/item[0]/@price"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.ITEM_COUNT, "//order/shops/shopOrder[0]/item[0]/@count"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.ITEM_PAYLOAD, "//order/shops/shopOrder[0]/item[0]/@payload")
                );

                return e.getErrors().containsAll(expected);
            }
        });

        ValidationErrors errors = new ValidationErrors();

        CheckoutRequest request = new CheckoutRequest() {{
            setShopOrders(new ArrayList<ShopOrder>() {{
                add(new ShopOrder() {{
                    setItems(new ArrayList<OrderItem>() {{
                        add(new OrderItem());
                    }});
                }});
            }});
        }};

        validator.validate(request, errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testValidateDeliveryPoint() {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {
                List<? extends ApiError> expected = Arrays.asList(
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_OPTION_ID, "//order/shops/shopOrder[0]/deliveryPoint/@deliveryOptionId"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_REGION_ID, "//order/shops/shopOrder[0]/deliveryPoint/@regionId"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_RECIPIENT, "//order/shops/shopOrder[0]/deliveryPoint/@recipient"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_PHONE, "//order/shops/shopOrder[0]/deliveryPoint/@phone"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_CITY, "//order/shops/shopOrder[0]/deliveryPoint/address/@city"),
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_HOUSE, "//order/shops/shopOrder[0]/deliveryPoint/address/@house")
                );

                return e.getErrors().containsAll(expected);
            }
        });

        ValidationErrors errors = new ValidationErrors();

        CheckoutRequest request = new CheckoutRequest() {{
            setShopOrders(new ArrayList<ShopOrder>() {{
                add(new ShopOrder() {{
                    setItems(new ArrayList<OrderItem>() {{
                        add(new OrderItem() {{
                            setDeliveryPoint(new AddressDeliveryPoint() {{
                                setAddress(new RoomAddress() {{
                                    setCountry("russia");
                                }});
                            }});
                        }});
                    }});
                }});
            }});
        }};

        validator.validate(request, errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testValidatePayload() {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {
                return e.getErrors().contains(
                    new IncorrectParameterValueError(OrderRequestValidator.CheckoutRequestXpathError.ITEM_PAYLOAD, "//order/shops/shopOrder[0]/item[0]/@payload"));
            }
        });

        ValidationErrors errors = new ValidationErrors();

        CheckoutRequest request = new CheckoutRequest() {{
            setShopOrders(new ArrayList<ShopOrder>() {{
                add(new ShopOrder() {{
                    setItems(new ArrayList<OrderItem>() {{
                        add(new OrderItem() {{
                            setPayload(new Payload(0, "qwer", "qwer", "qwer"));
                        }});
                    }});
                }});
            }});
        }};

        validator.validate(request, errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testOptionRequestWithEmptyOfferId() {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {
                return e.getErrors().contains(
                    new ValidationError(
                        ParameterErrorType.MISSING_PROPERTY,
                        OrderRequestValidator.CheckoutRequestXpathError.OFFER_ID,
                        "//order/shops/shopOrder[0]/item[0]/@offerId"
                    )
                );
            }
        });

        ValidationErrors errors = new ValidationErrors();
        validator.validate(generateOrderOptionRequestWithOneOrder(
            TEST_USER_ID,
            TEST_USER_REGION_ID,
            TEST_USER_CURRENCY,
            TEST_SHOP_ID,
            new OfferId(),
            null,
            TEST_OFFER_PRICE,
            1
        ), errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testOnlyEmptyShopIdError() throws Exception {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {
                if (e.getErrors().stream().filter(error -> OrderRequestValidator.CheckoutRequestXpathError.SHOP_ID == error.getCode())
                    .collect(toList()).size() != 1) {
                    return false;
                }
                return e.getErrors().contains(
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.SHOP_ID, "//order/shops/shopOrder[0]/@shopId"));
            }
        });

        ValidationErrors errors = new ValidationErrors();
        validator.validate(generateCheckoutRequestWithOneOrder(
            TEST_USER_REGION_ID,
            TEST_USER_CURRENCY,
            0,
            new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW),
            TEST_OFFER_RIGHT_FEE_SHOW,
            TEST_OFFER_PRICE,
            1
        ), errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testOptionOrderWithEmptyShopId() {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {
                if (e.getErrors().stream().filter(error -> OrderRequestValidator.CheckoutRequestXpathError.SHOP_ID == error.getCode())
                    .collect(toList()).size() != 1) {
                    return false;
                }
                return e.getErrors().contains(
                    new MissingRequiredParameterError(OrderRequestValidator.CheckoutRequestXpathError.SHOP_ID, "//order/shops/shopOrder[0]/@shopId"));
            }
        });

        ValidationErrors errors = new ValidationErrors();
        validator.validate(generateOrderOptionRequestWithOneOrder(
            TEST_USER_ID,
            TEST_USER_REGION_ID,
            TEST_USER_CURRENCY,
            0,
            new OfferId(TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW),
            TEST_OFFER_RIGHT_FEE_SHOW,
            TEST_OFFER_PRICE,
            1
        ), errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testOrderWithPlainWareMd5Id() throws Exception {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {
                return e.getErrors().contains(
                    new IncorrectParameterValueError(OrderRequestValidator.CheckoutRequestXpathError.OFFER_ID, "//order/shops/shopOrder[0]/item[0]/@offerId"));
            }
        });

        ValidationErrors errors = new ValidationErrors();
        validator.validate(generateCheckoutRequestWithOneOrder(
            TEST_USER_REGION_ID,
            TEST_USER_CURRENCY,
            1,
            new OfferId(TEST_OFFER_ID, null),
            TEST_OFFER_RIGHT_FEE_SHOW,
            TEST_OFFER_PRICE,
            1
        ), errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testOrderOptionsWithPlainWareMd5Id() throws Exception {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {
                return e.getErrors().contains(
                    new IncorrectParameterValueError(OrderRequestValidator.CheckoutRequestXpathError.OFFER_ID, "//order/shops/shopOrder[0]/item[0]/@offerId"));
            }
        });

        ValidationErrors errors = new ValidationErrors();
        validator.validate(generateOrderOptionRequestWithOneOrder(
            TEST_USER_ID,
            TEST_USER_REGION_ID,
            TEST_USER_CURRENCY,
            1,
            new OfferId(TEST_OFFER_ID, null),
            TEST_OFFER_RIGHT_FEE_SHOW,
            TEST_OFFER_PRICE,
            1
        ), errors);
        errors.throwIfHasErrors();
    }

    @Test
    public void testUncorrectAddressDeliveryPoint() {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {
                List<? extends ApiError> expected = Arrays.asList(
                    new StringParamLengthValidationError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_POSTCODE,
                        "//order/shops/shopOrder[0]/deliveryPoint/address/@postcode",
                        OrderRequestValidator.AddressConstants.DELIVERY_ADDRESS_MAX_POSTCODE_FIELD_LENGTH),
                    new StringParamLengthValidationError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_COUNTRY,
                        "//order/shops/shopOrder[0]/deliveryPoint/address/@country",
                        OrderRequestValidator.AddressConstants.DELIVERY_ADDRESS_MAX_COUNTRY_FIELD_LENGTH),
                    new StringParamLengthValidationError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_CITY,
                        "//order/shops/shopOrder[0]/deliveryPoint/address/@city",
                        OrderRequestValidator.AddressConstants.DELIVERY_ADDRESS_MAX_CITY_FIELD_LENGTH),
                    new StringParamLengthValidationError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_STREET,
                        "//order/shops/shopOrder[0]/deliveryPoint/address/@street",
                        OrderRequestValidator.AddressConstants.DELIVERY_ADDRESS_MAX_STREET_FIELD_LENGTH),
                    new StringParamLengthValidationError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_HOUSE,
                        "//order/shops/shopOrder[0]/deliveryPoint/address/@house",
                        OrderRequestValidator.AddressConstants.DELIVERY_ADDRESS_MAX_HOUSE_FIELD_LENGTH),
                    new StringParamLengthValidationError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_BLOCK,
                        "//order/shops/shopOrder[0]/deliveryPoint/address/@block",
                        OrderRequestValidator.AddressConstants.DELIVERY_ADDRESS_MAX_BLOCK_FIELD_LENGTH),
                    new StringParamLengthValidationError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_FLOOR,
                        "//order/shops/shopOrder[0]/deliveryPoint/address/@floor",
                        OrderRequestValidator.AddressConstants.DELIVERY_ADDRESS_MAX_FLOOR_FIELD_LENGTH),
                    new StringParamLengthValidationError(OrderRequestValidator.CheckoutRequestXpathError.DELIVERY_SUBWAY,
                        "//order/shops/shopOrder[0]/deliveryPoint/address/@subway",
                        OrderRequestValidator.AddressConstants.DELIVERY_ADDRESS_MAX_SUBWAY_FIELD_LENGTH)
                );

                return e.getErrors().containsAll(expected);
            }
        });

        ValidationErrors errors = new ValidationErrors();
        OrderOptionsRequest request = generateOrderOptionRequestWithOneOrder(
            TEST_USER_ID,
            TEST_USER_REGION_ID,
            TEST_USER_CURRENCY,
            1,
            new OfferId(TEST_OFFER_ID, null),
            TEST_OFFER_RIGHT_FEE_SHOW,
            TEST_OFFER_PRICE,
            1
        );
        request.getShopOrders().get(0).setDeliveryPoint(new OrderOptionsRequest.AddressDeliveryPoint() {{
            setId(new DeliveryPointId());
            setRegionId(TEST_USER_REGION_ID);
            setPostCode("123456789011234567890112345678901");
            setCountry("Россия - священная наша держава, Россия — любимая наша страна");
            setCity("Москва резиноваяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяя");
            setStreet(StringUtils.repeat("улица Льва Толстогоогоогоогоогооггоогоогоогоогооггоогоогоогоогооггоогоогоогоогооггоогоогоогоогооггоогго", 15));
            setHouse(StringUtils.repeat("четырнадцать", 15));
            setBlock(StringUtils.repeat("12345678901", 10));
            setFloor(StringUtils.repeat("12345678901", 15));
            setSubway(StringUtils.repeat("Парк культуры (станция метро, Сокольническая линия)", 3));
        }});
        validator.validate(request, errors);
        errors.throwIfHasErrors();
    }
}
