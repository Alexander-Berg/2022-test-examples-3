const offersCommons = {
    entity: 'offer',
    description: '',
    meta: {},
    isTurboUrl: false,
    isCutPrice: false,
    previouslyUsed: false,
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
        price: {
            currency: 'RUR',
            value: '300',
            isDeliveryIncluded: false,
        },
        isFree: false,
        isDownloadable: false,
        inStock: true,
        postAvailable: false,
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
        pickupOptions: [
            {
                serviceId: 99,
                serviceName: 'Собственная служба',
                price: {
                    currency: 'RUR',
                    value: '0',
                },
                groupCount: 1,
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
    prices: {
        currency: 'RUR',
        isDeliveryIncluded: false,
    },
    seller: {
        price: '7290',
        currency: 'RUR',
        sellerToUserExchangeRate: 1,
    },
    urls: {
        encrypted: '/redir/encrypted',
        decrypted: '/redir/decrypted',
        geo: '/redir/geo',
        offercard: '/redir/offercard',
    },
};

export default {
    offer1: {
        ...offersCommons,
        prices: {
            ...offersCommons.prices,
            rawValue: '7290',
            value: '7290',
        },
        showUid: '386426439299069477300001',
        shop: {
            entity: 'shop',
            id: 1672,
            name: 'Super Shop',
            slug: 'shop',
        },
    },
    offer2: {
        ...offersCommons,
        prices: {
            ...offersCommons.prices,
            rawValue: '8000',
            value: '8000',
        },
        showUid: '387538890997944792400001',
        shop: {
            entity: 'shop',
            id: 1673,
            name: 'The Best Shop',
            slug: 'shop',
        },
    },
    offer3: {
        ...offersCommons,
        prices: {
            ...offersCommons.prices,
            rawValue: '9000',
            value: '9000',
        },
        showUid: '387538890497944792400001',
        shop: {
            entity: 'shop',
            id: 1674,
            name: 'The Best of the Best Shop',
            slug: 'shop',
        },
    },
};
