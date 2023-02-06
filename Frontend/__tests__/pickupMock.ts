const freePickup = {
    delivery: {
        isPriorityRegion: true,
        isCountrywide: true,
        isAvailable: true,
        hasPickup: true,
        hasLocalStore: false,
        hasPost: true,
        isForcedRegion: false,
        isFree: true,
        isDownloadable: false,
        inStock: true,
        postAvailable: true,
        options: [
            {
                price: {
                    currency: 'RUR',
                    value: '0',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                },
                priceForShop: {
                    currency: 'RUR',
                    value: '121',
                },
                discount: {
                    oldMin: {
                        currency: 'RUR',
                        value: '249',
                    },
                    discountType: 'threshold',
                },
                dayFrom: 2,
                dayTo: 2,
                isDefault: true,
                serviceId: '179',
                tariffId: 5777,
                shipmentDay: 1,
                paymentMethods: [
                    'YANDEX',
                    'CASH_ON_DELIVERY',
                ],
                partnerType: 'market_delivery',
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
        ],
        pickupOptions: [
            {
                serviceId: 198,
                serviceName: 'Пункт выдачи посылок Беру',
                tariffId: 5993,
                isMarketBranded: true,
                price: {
                    currency: 'RUR',
                    value: '0',
                },
                discount: {
                    oldMin: {
                        currency: 'RUR',
                        value: '99',
                    },
                    discountType: 'threshold',
                },
                shipmentDay: 1,
                dayFrom: 2,
                dayTo: 2,
                orderBefore: 24,
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
            {
                serviceId: 107,
                serviceName: 'PickPoint',
                isMarketBranded: true,
                price: {
                    currency: 'RUR',
                    value: '0',
                },
                discount: {
                    oldMin: {
                        currency: 'RUR',
                        value: '99',
                    },
                    discountType: 'threshold',
                },
                shipmentDay: 1,
                packagingTime: 'PT28H10M',
                dayFrom: 2,
                dayTo: 2,
                orderBefore: 24,
                groupCount: 647,
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
            {
                serviceId: 106,
                serviceName: 'Boxberry',
                isMarketBranded: true,
                price: {
                    currency: 'RUR',
                    value: '0',
                },
                discount: {
                    oldMin: {
                        currency: 'RUR',
                        value: '99',
                    },
                    discountType: 'threshold',
                },
                shipmentDay: 1,
                packagingTime: 'PT28H0M',
                dayFrom: 2,
                dayTo: 2,
                orderBefore: 24,
                groupCount: 351,
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
            {
                serviceId: 162,
                serviceName: 'Стриж Почтоматы',
                isMarketBranded: true,
                price: {
                    currency: 'RUR',
                    value: '0',
                },
                discount: {
                    oldMin: {
                        currency: 'RUR',
                        value: '99',
                    },
                    discountType: 'threshold',
                },
                shipmentDay: 1,
                packagingTime: 'PT36H10M',
                dayFrom: 2,
                dayTo: 2,
                orderBefore: 24,
                groupCount: 30,
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
            {
                serviceId: 51,
                serviceName: 'СДЭК',
                isMarketBranded: true,
                price: {
                    currency: 'RUR',
                    value: '0',
                },
                discount: {
                    oldMin: {
                        currency: 'RUR',
                        value: '99',
                    },
                    discountType: 'threshold',
                },
                shipmentDay: 1,
                packagingTime: 'PT29H10M',
                dayFrom: 3,
                dayTo: 3,
                orderBefore: 24,
                groupCount: 152,
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
            {
                serviceId: 1003937,
                serviceName: 'DPD',
                isMarketBranded: true,
                price: {
                    currency: 'RUR',
                    value: '0',
                },
                discount: {
                    oldMin: {
                        currency: 'RUR',
                        value: '99',
                    },
                    discountType: 'threshold',
                },
                shipmentDay: 2,
                packagingTime: 'PT36H10M',
                dayFrom: 4,
                dayTo: 4,
                orderBefore: 24,
                groupCount: 429,
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
        ],
        postStats: {
            minDays: 6,
            maxDays: 8,
            minPrice: {
                currency: 'RUR',
                value: '0',
                isDeliveryIncluded: false,
                isPickupIncluded: false,
            },
            maxPrice: {
                currency: 'RUR',
                value: '0',
                isDeliveryIncluded: false,
                isPickupIncluded: false,
            },
        },
        deliveryPartnerTypes: [
            'YANDEX_MARKET',
        ],
    },
};

const pickupWithPrice = {
    delivery: {
        isPriorityRegion: true,
        isCountrywide: true,
        isAvailable: true,
        hasPickup: true,
        hasLocalStore: true,
        hasPost: false,
        isForcedRegion: false,
        isFree: false,
        isDownloadable: false,
        inStock: false,
        postAvailable: false,
        options: [
            {
                price: {
                    currency: 'RUR',
                    value: '320',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                },
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
        ],
        pickupOptions: [
            {
                serviceId: 99,
                serviceName: 'Собственная служба',
                tariffId: 0,
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
            {
                serviceId: 99,
                serviceName: 'Собственная служба',
                tariffId: 0,
                price: {
                    currency: 'RUR',
                    value: '650',
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
        ],
        deliveryPartnerTypes: [

        ],
    },
};

const optionsCombo = {
    delivery: {
        isPriorityRegion: true,
        isCountrywide: true,
        isAvailable: true,
        hasPickup: true,
        hasLocalStore: false,
        hasPost: true,
        isForcedRegion: false,
        isFree: false,
        isDownloadable: false,
        inStock: true,
        postAvailable: true,
        options: [
            {
                price: {
                    currency: 'RUR',
                    value: '249',
                    isDeliveryIncluded: false,
                    isPickupIncluded: false,
                },
                priceForShop: {
                    currency: 'RUR',
                    value: '121',
                },
                dayFrom: 1,
                dayTo: 1,
                orderBefore: '18',
                orderBeforeMin: '0',
                isDefault: true,
                serviceId: '179',
                tariffId: 5777,
                shipmentDay: 0,
                paymentMethods: [
                    'YANDEX',
                    'CASH_ON_DELIVERY',
                ],
                partnerType: 'market_delivery',
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
        ],
        pickupOptions: [
            {
                serviceId: 198,
                serviceName: 'Пункт выдачи посылок Беру',
                tariffId: 5993,
                isMarketBranded: true,
                price: {
                    currency: 'RUR',
                    value: '99',
                },
                shipmentDay: 0,
                dayFrom: 1,
                dayTo: 1,
                orderBefore: 20,
                orderBeforeMin: 0,
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
            {
                serviceId: 106,
                serviceName: 'Boxberry',
                tariffId: 4829,
                isMarketBranded: true,
                price: {
                    currency: 'RUR',
                    value: '99',
                },
                shipmentDay: 0,
                packagingTime: 'PT28H0M',
                dayFrom: 1,
                dayTo: 1,
                orderBefore: 18,
                orderBeforeMin: 0,
                groupCount: 351,
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
            {
                serviceId: 51,
                serviceName: 'СДЭК',
                tariffId: 4897,
                isMarketBranded: true,
                price: {
                    currency: 'RUR',
                    value: '99',
                },
                shipmentDay: 1,
                packagingTime: 'PT29H10M',
                dayFrom: 3,
                dayTo: 3,
                orderBefore: 21,
                orderBeforeMin: 0,
                groupCount: 152,
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
                    type: 6,
                    subtitle: 'Москва и Московская область, Россия',
                },
            },
        ],
    },
};

export {
    freePickup,
    pickupWithPrice,
    optionsCombo,
};
