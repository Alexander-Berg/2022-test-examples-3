/* eslint-disable */
module.exports = [
    {
        entity: 'product',
        type: 'model',
        id: 11031637,
        titles: {
            raw: 'Apple iPhone 6 Plus 16Gb'
        },
        description: 'GSM, LTE-A, смартфон, iOS 8',
        rating: 4.5
    }, {
        entity: 'product',
        type: 'model',
        id: 10500049,
        titles: {
            raw: 'Apple iPhone 4S 8Gb'
        },
        description: 'GSM/CDMA, 3G, смартфон, iOS 5',
        rating: 4
    }, {
        entity: 'offer',
        id: 1220348942,
        shop: {
            id: 1622
        },
        delivery: {
            availableServices: [
                {
                    serviceId: 99,
                    daysFrom: 10,
                    daysTo: 20
                },
                {
                    serviceId: 1,
                    daysFrom: 3,
                    daysTo: 6
                },
                {
                    serviceId: 102,
                    serviceName: "serviceName from search results",
                    daysFrom: 3,
                    daysTo: 6
                }
            ],
            shopPriorityCountry: {
                id: 225
            }
        }
    }, {
        entity: 'offer',
        id: 1220348943,
        shop: {
            id: 666
        },
        delivery: {
            availableServices: [
                {
                    serviceId: 99,
                    daysFrom: 10,
                    daysTo: 20
                },
                {
                    serviceId: 1,
                    daysFrom: 3,
                    daysTo: 6
                },
                {
                    serviceId: 102,
                    serviceName: "serviceName from search results",
                    daysFrom: 3,
                    daysTo: 6
                }

            ],
            shopPriorityCountry: {
                id: 666
            }
        }
    }
];
