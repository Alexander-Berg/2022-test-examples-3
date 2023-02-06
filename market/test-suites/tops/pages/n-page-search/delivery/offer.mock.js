export default {
    delivery: {
        shopPriorityRegion: {
            entity: 'region',
            id: 2,
            name: 'Санкт-Петербург',
            lingua: {
                name: {
                    genitive: 'Санкт-Петербурга',
                    preposition: 'в',
                    prepositional: 'Санкт-Петербурге',
                    accusative: 'Санкт-Петербург',
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
        isPriorityRegion: false,
        isCountrywide: true,
        isAvailable: true,
        hasPickup: false,
        hasLocalStore: false,
        hasPost: false,
        isFake: false,
        region: {
            entity: 'region',
            id: 2,
            name: 'Санкт-Петербург',
            lingua: {
                name: {
                    genitive: 'Санкт-Петербурга',
                    preposition: 'в',
                    prepositional: 'Санкт-Петербурге',
                    accusative: 'Санкт-Петербург',
                },
            },
        },
        availableServices: [
            {
                serviceId: 99,
                serviceName: 'Собственная служба',
                id: 99,
            },
            {
                serviceId: 213,
                serviceName: 'RusPost Basildon EMS',
                isMarketBranded: true,
            },
        ],
        isFree: true,
        isDownloadable: false,
        inStock: true,
        postAvailable: true,
        options: [
            {
                price: {
                    currency: 'RUR',
                    value: '300',
                    isDeliveryIncluded: false,
                },
                dayFrom: 1,
                dayTo: 1,
                orderBefore: '18',
                isDefault: true,
                serviceId: '99',
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
        pickupOptions: [{
            serviceId: 213,
            serviceName: 'Собственная служба',
            tariffId: 0,
            price: {
                currency: 'RUR',
                value: '100',
            },
            dayFrom: 5,
            dayTo: 5,
            orderBefore: 24,
        }],
    },
    shop: {
        id: 1,
        name: 'shop',
        slug: 'shop',
    },
    urls: {
        encrypted: '/redir/encrypted',
        decrypted: '/redir/decrypted',
        geo: '/redir/geo',
        offercard: '/redir/offercard',
    },
};
