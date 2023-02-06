const PAYMENT_CONFIG = {
    // Russia
    regionId: 225,
    countryCode: 'RU',
    currencyCode: 'RUB',
};

module.exports = {
    GOOGLE_PAY: {
        ...PAYMENT_CONFIG,
    },
    APPLE_PAY: {
        ...PAYMENT_CONFIG,
    },
};
