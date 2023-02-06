const offerProps = {
    shop: 123,
    prices: { value: 1234, currency: 'RUR' },
    urls: { encrypted: '//ya.ru' },
};

export const hasDelivery = {
    ...offerProps,
    delivery: {
        shopPriorityRegion: {
            id: '213',
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
            id: '225',
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
            id: '213',
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
            value: 1000,
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
                    value: 1000,
                    isDeliveryIncluded: false,
                },
                dayFrom: 5,
                dayTo: 5,
                isDefault: true,
                serviceId: '99',
                paymentMethods: [
                    'CASH_ON_DELIVERY',
                ],
                partnerType: 'regular',
                region: {
                    entity: 'region',
                    id: '213',
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
};

export const deliveryToday = {
    ...offerProps,
    delivery: {
        shopPriorityRegion: {
            entity: 'region',
            id: '213',
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
            id: '225',
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
            id: '213',
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
            value: 1000,
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
                    value: 1000,
                    isDeliveryIncluded: false,
                },
                dayFrom: 0,
                dayTo: 0,
                isDefault: true,
                serviceId: '99',
                paymentMethods: [
                    'CASH_ON_DELIVERY',
                ],
                partnerType: 'regular',
                region: {
                    entity: 'region',
                    id: '213',
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
};

export const deliveryFree7days = {
    ...offerProps,
    delivery: {
        shopPriorityRegion: {
            entity: 'region',
            id: '213',
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
            id: '225',
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
            id: '213',
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
            value: 0,
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
                    value: 0,
                    isDeliveryIncluded: false,
                },
                dayFrom: 7,
                dayTo: 7,
                isDefault: true,
                serviceId: '99',
                paymentMethods: [
                    'CASH_ON_DELIVERY',
                ],
                partnerType: 'regular',
                region: {
                    entity: 'region',
                    id: '213',
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
};

export const regional13days = {
    ...offerProps,
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
            type: 6,
            subtitle: 'Москва и Московская область, Россия',
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
            type: 3,
        },
        isPriorityRegion: false,
        isCountrywide: true,
        isAvailable: true,
        hasPickup: false,
        hasLocalStore: false,
        hasPost: true,
        region: {
            entity: 'region',
            id: 101109,
            name: 'Большое Мурашкино',
            lingua: {
                name: {
                    genitive: 'Большого Мурашкино',
                    preposition: 'в',
                    prepositional: 'Большом Мурашкино',
                    accusative: 'Большое Мурашкино',
                },
            },
            type: 7,
            subtitle: 'Большемурашкинский район, Нижегородская область, Россия',
        },
        isFree: false,
        isDownloadable: false,
        inStock: false,
        postAvailable: true,
        options: [
            {
                price: {
                    currency: 'RUR',
                    value: 0,
                    isDeliveryIncluded: false,
                },
                priceForShop: {
                    currency: 'RUR',
                    value: '381',
                },
                discount: {
                    oldMin: {
                        currency: 'RUR',
                        value: 249,
                    },
                    discountType: 'threshold',
                },
                dayFrom: 13,
                dayTo: 13,
                orderBefore: '20',
                orderBeforeMin: '0',
                isDefault: true,
                serviceId: '1003937',
                shipmentDay: 2,
                paymentMethods: [
                    'YANDEX',
                    'CASH_ON_DELIVERY',
                ],
                partnerType: 'market_delivery',
                region: {
                    entity: 'region',
                    id: 101109,
                    name: 'Большое Мурашкино',
                    lingua: {
                        name: {
                            genitive: 'Большого Мурашкино',
                            preposition: 'в',
                            prepositional: 'Большом Мурашкино',
                            accusative: 'Большое Мурашкино',
                        },
                    },
                    type: 7,
                    subtitle: 'Большемурашкинский район, Нижегородская область, Россия',
                },
            },
        ],
        postStats: {
            minDays: 17,
            maxDays: 19,
            minPrice: {
                currency: 'RUR',
                value: 0,
                isDeliveryIncluded: false,
            },
            maxPrice: {
                currency: 'RUR',
                value: 0,
                isDeliveryIncluded: false,
            },
        },
        deliveryPartnerTypes: [
            'YANDEX_MARKET',
        ],
    },
};
