export const defaultDeliveryOption = {
    price: {
        currency: 'RUR',
        value: '49',
        isDeliveryIncluded: false,
        isPickupIncluded: false,
    },
    dayFrom: 0,
    dayTo: 0,
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
};


export const createOfferDeliveryOption = (option = {}) => ({
    ...defaultDeliveryOption,
    ...option,
});
