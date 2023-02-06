package ru.yandex.market.checkout.util.matching;

/**
 * @author : poluektov
 * date: 16.08.17.
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
public final class CheckoutErrorMatchers {

    public static final ErrorResponseMatcher MISSING_DELIVERY_FROM_DATE =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Missing delivery from-date");
    public static ErrorResponseMatcher missingCarts =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Missing orders");
    public static ErrorResponseMatcher missingAddress =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Missing address");
    public static ErrorResponseMatcher deliveryOutletIdEmpty =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "delivery outlet id and code are empty");
    public static ErrorResponseMatcher buyerAddressIsNotAppropriate =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Buyer delivery address is not appropriate for delivery " +
                    "type PICKUP");
    public static ErrorResponseMatcher outletIdIsNotAppropriate =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Delivery outlet is not appropriate for delivery type " +
                    "DELIVERY");
    public static ErrorResponseMatcher outletIdIsNotAppropriateForPost =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Delivery outlet is not appropriate for delivery type " +
                    "POST");
    public static ErrorResponseMatcher wrongEmail1 =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Email address has invalid format: abc");
    public static ErrorResponseMatcher wrongEmail2 =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Email address has invalid format: a@b");
    public static ErrorResponseMatcher emptyEmail =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Missing buyer email");
    @Deprecated
    public static ErrorResponseMatcher wrongPhone =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Value of buyer phone is not valid");
    public static ErrorResponseMatcher emptyPersonalPhoneId =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Missing Personal phone ID");
    public static ErrorResponseMatcher wrongPersonalPhoneId =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Length of Personal phone ID (33) must be between 0 and " +
                    "33 exclusively");
    public static ErrorResponseMatcher emptyPersonalEmailId =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Missing Personal email ID");
    public static ErrorResponseMatcher wrongPersonalEmailId =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Length of Personal email ID (33) " +
                    "must be between 0 and 33 exclusively");
    public static ErrorResponseMatcher emptyPersonalFullNameId =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Missing Personal full name ID");
    public static ErrorResponseMatcher wrongPersonalFullNameId =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Length of Personal full name ID (33) " +
                    "must be between 0 and 33 exclusively");
    public static ErrorResponseMatcher deliveryDatesMustMatch =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Delivery dates must match when time interval is " +
                    "specified");
    public static ErrorResponseMatcher missingPaymentMethod =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Missing payment method");
    public static ErrorResponseMatcher tooManyItems =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Value of order items (101) must be less than or equal " +
                    "to 100");
    public static final ErrorResponseMatcher MISSING_ERROR_NAME =
            new ErrorResponseMatcher(400, "INVALID_REQUEST", "Missing buyer first name");
    public static final ErrorResponseMatcher NO_AUTH =
            new ErrorResponseMatcher(403, "no_auth_forbidden", "Unauthorised checkout is currently forbidden");

    private CheckoutErrorMatchers() {
    }

    public static ErrorResponseMatcher wrongRegionId(Long regionId) {
        return new ErrorResponseMatcher(400, "INVALID_REQUEST", "Unknown delivery region id: " + regionId);
    }

    public static <T> ErrorResponseMatcher greaterThan(Comparable<T> value1, T minValue, String valueName) {
        return new ErrorResponseMatcher(400, "INVALID_REQUEST", "Value of " + valueName + " (" + value1 + ") must be " +
                "greater than " + minValue);
    }
}
