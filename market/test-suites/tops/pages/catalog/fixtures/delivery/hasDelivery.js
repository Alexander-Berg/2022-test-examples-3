export default {
    cpc: 'testOfferCpcHasDelivery',
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
        isPriorityRegion: false,
        isCountrywide: true,
        isAvailable: true,
        hasPickup: false,
        hasLocalStore: false,
        hasPost: false,
        isFake: false,
        region: {
            entity: 'region',
            id: 10001,
            name: 'Евразия',
            lingua: {
                name: {
                    genitive: 'Евразии',
                    preposition: 'в',
                    prepositional: 'Евразии',
                    accusative: 'Евразию',
                },
            },
        },
        availableServices: [
            {
                serviceId: 1,
                serviceName: 'Почта России',
            },
            {
                serviceId: 51,
                serviceName: 'СДЭК',
            },
            {
                serviceId: 2,
                serviceName: 'EMS Почта России',
            },
        ],
        isFree: false,
        isDownloadable: false,
        inStock: false,
        postAvailable: true,
        options: [],
    },
    urls: {
        encrypted: '/redir/',
    },
};
