import {map} from 'ambar';

import {
    mergeState,
    createProduct,
    createOffer,
    createFilter,
    createFilterValue,
    createEntityFilter,
    createEntityFilterValue,
} from '@yandex-market/kadavr/mocks/Report/helpers';

const slug = 'planshet-samsung-galaxy-tab-s2-9-7-sm-t819-lte-32gb';

const goldId = '14896254';
const goldFixture = {
    'id': goldId,
    'value': 'золотистый',
    'code': '#FFD700',
    'initialFound': 5,
    'found': 5,
};

const blackId = '14896255';
const blackFixture = {
    'id': blackId,
    'value': 'черный',
    'code': '#000000',
    'initialFound': 5,
    'found': 5,
};

const filterId = '14871214';
const filterFixture = {
    'id': filterId,
    'type': 'enum',
    'name': 'Цвет товара',
    'xslname': 'color_vendor',
    'subType': 'image_picker',
    'kind': 2,
    'position': 4,
    'noffers': 1,
    'valuesGroups': [{
        type: 'all',
        valuesIds: [goldId, blackId],
    }],
};

const category = {
    'entity': 'category',
    'id': 54545,
    'nid': 54545,
    'name': 'Планшеты',
    'slug': 'planshety',
    'fullName': 'Планшеты',
    'type': 'guru',
    'cpaType': 'cpa_with_cpc_pessimization',
    'isLeaf': true,
    'kinds': [],
};

const navnode = {
    'category': category,
    'entity': 'navnode',
    'id': 54545,
    'name': 'Планшеты',
    'slug': 'planshety',
    'fullName': 'Планшеты',
    'isLeaf': true,
    'rootNavnode': {
        'entity': 'navnode',
        'id': 54432,
    },
};

const productId = 13905590;
const productFixture = {
    'deletedId': null,
    'entity': 'product',
    'slug': slug,
    'categories': [category],
    'navnodes': [navnode],
    'filters': [filterId],
    'meta': {},
    'type': 'model',
    'id': productId,
    'offers': {
        'count': 11,
    },
};

const navState = {
    'category': {
        'entity': 'category',
        'id': 90401,
        'isLeaf': false,
        'modelsCount': 4866521,
        'name': 'Все товары',
        'nid': 54432,
        'offersCount': 110847388,
    },
    'childrenType': 'mixed',
    'entity': 'navnode',
    'fullName': 'Все товары',
    'hasPromo': false,
    'id': 54432,
    'isLeaf': false,
    'link': {
        'params': {
            'hid': [
                '90401',
            ],
            'nid': [
                '54432',
            ],
        },
        'target': 'catalog',
    },
    'name': 'Все товары',
    'slug': 'vse-tovary',
    'type': 'category',
    'navnodes': [navnode],
};

// Создаем продукт
const product = createProduct(productFixture, productId);

// Создаем общий фильтр с двумя значениями
const colorFilter = createFilter(filterFixture, filterId);
const goldValue = createFilterValue(goldFixture, filterId, goldId);
const blackValue = createFilterValue(blackFixture, filterId, blackId);


// Создаем оффера с фильтрами
const _generateOffer = (shopId, assign) => ({
    'entity': 'offer',
    'slug': slug,
    'categories': [category],
    'navnodes': [navnode],
    'shop': {
        'entity': 'shop',
        'id': shopId,
        'name': 'Mobile Mega',
        'slug': 'mobile-mega',
        'status': 'actual',
        'cutoff': '',
        'feed': {
            'id': 123123,
        },
    },
    'prices': {
        'currency': 'RUR',
        'value': '23590',
        'isDeliveryIncluded': false,
        'rawValue': '23590',
    },
    'isAdult': false,
    'isCutPrice': false,
    'isDailyDeal': false,
    'isFulfillment': false,
    'isRecommendedByVendor': false,
    'urls': {
        'encrypted': '/redir/encrypted',
        'decrypted': '/redir/decrypted',
        'geo': '/redir/geo',
        'offercard': '/redir/offercard',
    },
    'cpc': 'DqqPjIrWS5xIT',
    'vendor': {
        'id': 2222,
        'entity': 'vendor',
        'name': 'some_vendor_name',
    },
    'delivery': {
        'shopPriorityRegion': {
            'entity': 'region',
            'id': 62007514,
            'name': 'ea mol',
            'lingua': {
                'name': {
                    'accusative': 'ut aliqua',
                    'genitive': 'veniam Excepteur consequat',
                    'preposition': 'sit',
                    'prepositional': 'nulla amet',
                },
            },
        },
        'shopPriorityCountry': {
            'entity': 'region',
            'id': 59868827,
            'name': 'in officia exercitation',
            'lingua': {
                'name': {
                    'accusative': 'anim aute',
                    'genitive': 'reprehenderit',
                    'preposition': 'dolor ad Duis aliqua sunt',
                    'prepositional': 'voluptate cillum',
                },
            },
        },
        'region': {
            'lingua': {
                'name': {
                    'accusative': 'anim aute',
                    'genitive': 'reprehenderit',
                    'preposition': 'dolor ad Duis aliqua sunt',
                    'prepositional': 'voluptate cillum',
                },
            },
            'title': 'Регион, в который будет осуществляться доставка курьером',
        },
        'isPriorityRegion': true,
        'isCountrywide': false,
        'isAvailable': true,
        'isDownloadable': false,
        'isFree': true,
        'inStock': false,
        'hasLocalStore': true,
        'hasPickup': false,
        'price': {
            'currency': 'reprehenderit ',
            'value': 87304050,
        },
        'options': [
            {
                'isDefault': true,
                'dayFrom': 94596195,
                'dayTo': 93108146,
                'orderBefore': 22,
                'price': {
                    'currency': 'dolor Duis exercitation',
                    'value': 48487912,
                },
            },
            {
                'isDefault': true,
                'dayFrom': 91116704,
                'dayTo': 36388990,
                'orderBefore': 3,
                'price': {
                    'currency': 'officia Lorem',
                    'value': 49364185,
                },
            },
            {
                'isDefault': false,
                'dayFrom': 93311832,
                'dayTo': 76109638,
                'orderBefore': 24,
                'price': {
                    'currency': 'velit in',
                    'value': 11734747,
                },
            },
        ],
    },
    ...assign,
});

const defaultOfferIds = [100, 101];
const topOfferIds = [102, 103];

const createOfferStates = (offerIds, isDefault) => map(offerId => {
    const assignee = isDefault ? {
        'benefit': {
            'type': 'cheapest',
            'description': 'Хорошая цена от надёжного магазина',
            'isPrimary': true,
        },
    } : {
        payments: {
            deliveryCard: true,
            deliveryCash: true,
            prepaymentCard: true,
            prepaymentOther: false,
        },
    };
    const fixture = _generateOffer(offerId + 100, assignee);
    const offer = createOffer(fixture, offerId);
    const filter = createEntityFilter(filterFixture, 'offer', offerId, filterId);
    const filterValueFixture = offerId % 2 === 0 ? goldFixture : blackFixture;
    const filterValue = createEntityFilterValue(filterValueFixture, offerId, filterId, filterValueFixture.id);

    return mergeState([
        offer,
        filter,
        filterValue,
    ]);
}, [...offerIds]);

const recommendedOffers = createOfferStates(defaultOfferIds, true);
const topOffers = createOfferStates(topOfferIds, false);

const recommendedOffersState = mergeState([
    {
        data: {
            search: {
                totalOffersBeforeFilters: 11,
            },
        },
    },
    colorFilter,
    goldValue,
    blackValue,
    product,
    ...recommendedOffers,
]);

const topOffersState = mergeState([
    {
        data: {
            search: {
                totalOffersBeforeFilters: 11,
            },
        },
    },
    colorFilter,
    goldValue,
    blackValue,
    product,
    ...topOffers,
]);

export default {
    recommendedOffersState,
    topOffersState,
    filterId,
    goldId,
    slug,
    productId,
    navState,
};
