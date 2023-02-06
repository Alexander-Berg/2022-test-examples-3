import productWithCPADO from '@self/platform/spec/hermione/test-suites/tops/pages/search/fixtures/productWithCPADO.mock';
import {
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';

const defaultOption = {
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


const createProductWithCPADO = (offerMock, deliveryOptions = {}) => {
    const options = [{...defaultOption, ...deliveryOptions}];
    offerMock.delivery.options = options;

    return mergeState([
        productWithCPADO.generateStateAndDataFromMock(offerMock).state,
        {
            data: {
                search: {
                    total: 1,
                    totalOffers: 1,
                },
            },
        },
    ]);
};

export default createProductWithCPADO;
