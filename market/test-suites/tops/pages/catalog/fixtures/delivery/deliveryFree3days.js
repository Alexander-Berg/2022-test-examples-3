export default {
    cpc: 'testOfferCpcFree7days',
    showUid: '15423769060781146930200001',
    entity: 'offer',
    delivery: {
        shopPriorityRegion: {
            entity: 'region',
            id: 213,
            name: 'Москва',
            lingua: {
                name: {
                    genitive: 'Москвы',
                    preposition: 'в',
                    prepositional: 'Москве',
                    accusative: 'Москву',
                },
            },
        },
        shopPriorityCountry: {
            entity: 'region',
            id: 225,
            name: 'Россия',
            lingua: {
                name: {
                    genitive: 'России',
                    preposition: 'в',
                    prepositional: 'России',
                    accusative: 'Россию',
                },
            },
        },
        isPriorityRegion: true,
        isCountrywide: true,
        isAvailable: true,
        hasPickup: true,
        hasLocalStore: false,
        hasPost: false,
        isFake: false,
        region: {
            entity: 'region',
            id: 213,
            name: 'Москва',
            lingua: {
                name: {
                    genitive: 'Москвы',
                    preposition: 'в',
                    prepositional: 'Москве',
                    accusative: 'Москву',
                },
            },
        },
        availableServices: [
            {
                serviceId: 99,
                serviceName: 'Собственная служба',
            },
        ],
        price: {
            currency: 'RUR',
            value: '0',
            isDeliveryIncluded: false,
        },
        isFree: false,
        isDownloadable: false,
        inStock: true,
        postAvailable: true,
        options: [
            {
                price: {
                    currency: 'RUR',
                    value: '0',
                    isDeliveryIncluded: false,
                },
                dayFrom: 3,
                dayTo: 3,
                isDefault: true,
                serviceId: '99',
                paymentMethods: [
                    'CASH_ON_DELIVERY',
                ],
                partnerType: 'regular',
                region: {
                    entity: 'region',
                    id: 213,
                    name: 'Москва',
                    lingua: {
                        name: {
                            genitive: 'Москвы',
                            preposition: 'в',
                            prepositional: 'Москве',
                            accusative: 'Москву',
                        },
                    },
                },
            },
        ],
    },
    urls: {
        encrypted: '/redir/',
    },
};
