const requestSample = {
    body: {
        email: 'uiui@gmail.com',
        subscriptions: [
            {
                email: 'uiui@gmail.com',
                type: 'PRICE_DROP',
                title: 'Портативная акустика JBL Charge 3',
                modelId: 13925684,
                price: '5500',
            },
            {
                email: 'uiui@gmail.com',
                type: 'ADVERTISING',
            },
        ],
    },
};

const expectedResult = [{
    currency: 'RUR',
    email: 'uiui@gmail.com',
    id: 3597804,
    modelId: 13925684,
    price: '5500',
    regionId: '213',
    subscriptionStatus: 'NEED_SEND_CONFIRMATION',
    subscriptionType: 'PRICE_DROP',
}, {
    email: 'uiui@gmail.com',
    id: 3597805,
    regionId: '213',
    subscriptionStatus: 'NEED_SEND_CONFIRMATION',
    subscriptionType: 'ADVERTISING',
    children: undefined,
}];

const resourceResponse = [
    {
        id: 3597804,
        email: 'uiui@gmail.com',
        subscriptionType: 'PRICE_DROP',
        subscriptionStatus: 'NEED_SEND_CONFIRMATION',
        parameters: {
            modelId: 13925684,
            price: '5500',
            currency: 'RUR',
            regionId: '213',
        },
    },
    {
        id: 3597805,
        email: 'uiui@gmail.com',
        subscriptionType: 'ADVERTISING',
        subscriptionStatus: 'NEED_SEND_CONFIRMATION',
        parameters: {
            regionId: '213',
        },
    },
];

module.exports = {
    requestSample,
    resourceResponse,
    expectedResult,
};
