export default {
    cpc: 'testOfferCpc350',
    showUid: '15423769060781146930200001',
    entity: 'offer',
    shop: {
        id: 1,
        slug: 'shop',
        name: 'shop',
    },
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
            value: '1000',
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
                    value: '1000',
                    isDeliveryIncluded: false,
                },
                dayFrom: 2,
                dayTo: 2,
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
