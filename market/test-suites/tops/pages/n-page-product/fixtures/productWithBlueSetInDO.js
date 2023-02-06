import {mergeState, createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

const shopId = '431782';
const productId = '123';
const offerId = '456';
const offer2Id = '123456';
const slug = 'onetwothree';

const createCategories = () => [
    {
        entity: 'category',
        id: 91491,
        name: 'Мобильные телефоны',
        fullName: 'Мобильные телефоны',
        type: 'guru',
        isLeaf: true,
        slug: 'mobilnye-telefony',
    },
];

const createNavnodes = () => [
    {
        entity: 'navnode',
        id: 54726,
        name: 'Мобильные телефоны',
        slug: 'mobilnye-telefony',
        fullName: 'Мобильные телефоны',
        isLeaf: true,
        rootNavnode: {},
    },
];


const offerBase = {
    categories: createCategories(),
    navnodes: createNavnodes(),
    prices: {
        currency: 'RUR',
        value: '37490',
        isDeliveryIncluded: false,
        rawValue: '37490',
        discount: {
            oldMin: '45000',
            percent: 20,
        },
    },
    feed: {
        id: '12345566',
    },
    shop: {
        id: shopId,
        entity: 'shop',
        name: 'Тестовый магазин проекта Фулфиллмент',
        status: 'actual',
        slug: 'slag-suag-swag',
        logo: 'shop-logo',
        feed: {
            id: '123.123',
            offerId: '123',
        },
    },
    benefit: {
        type: 'recommended',
        description: 'Хорошая цена от надёжного магазина',
        isPrimary: true,
    },
    payments: {
        deliveryCard: true,
        deliveryCash: true,
        prepaymentCard: true,
        prepaymentOther: false,
    },
    urls: {
        U_DIRECT_OFFER_CARD_URL: 'http://example.com',
        decrypted: 'http://example.com',
        direct: 'http://example.com',
        encrypted: '',
        geo: 'http://example.com',
        offercard: 'http://example.com',
        pickupGeo: 'http://example.com',
        postomatGeo: 'http://example.com',
        showPhone: 'http://example.com',
        storeGeo: 'http://example.com',
    },
    vendor: {
        id: 1,
        webpageRecommendedShops: '/some-url/',
        name: 'vendor',
        logo: {
            url: 'logo-url',
        },
    },
    cpc: 'YVwBN9ETvPXGDZSiKF2l7yI7ewwo3VPSxwc_i4zikHLQonAxhPCtDEi6sGg0m78tNJMVpses-WvglfZYpW1ZqaxAkk' +
        '6Jkk9okufOs3YBoznAc40Hlkj9wdfSS5fsdZswCS8u1xJXkmg,',
    delivery: {
        shopPriorityRegion: {
            entity: 'region',
            id: 62007514,
            name: 'ea mol',
            lingua: {
                name: {
                    accusative: 'ut aliqua',
                    genitive: 'veniam Excepteur consequat',
                    preposition: 'sit',
                    prepositional: 'nulla amet',
                },
            },
        },
        shopPriorityCountry: {
            entity: 'region',
            id: 59868827,
            name: 'in officia exercitation',
            lingua: {
                name: {
                    accusative: 'anim aute',
                    genitive: 'reprehenderit',
                    preposition: 'dolor ad Duis aliqua sunt',
                    prepositional: 'voluptate cillum',
                },
            },
        },
        region: {
            lingua: {
                name: {
                    accusative: 'anim aute',
                    genitive: 'reprehenderit',
                    preposition: 'dolor ad Duis aliqua sunt',
                    prepositional: 'voluptate cillum',
                },
            },
            title: 'Регион, в который будет осуществляться доставка курьером',
        },
        price: {
            currency: 'reprehenderit ',
            value: 87304050,
        },
        options: [],
    },
};

const feeShow = 'gf45678uikjhgfe4567ujhg';

const offer2 = createOffer({
    ...offerBase,
    feeShow,
    promos: [{
        type: 'blue-set-secondary',
    }],
}, offer2Id);


const product = createProduct({
    deletedId: null,
    categories: createCategories(),
    navnodes: createNavnodes(),
    slug: 'product',
}, productId);

const offer = createOffer({
    ...offerBase,
    promos: [{
        type: 'blue-set',
        itemsInfo: {
            'totalPrice': {
                'currency': 'RUR',
                'value': '1850',
                'discount': {
                    'oldMin': '2000',
                    'percent': '8',
                    'absolute': '150',
                },
            },
            'primaryPrice': {
                'currency': 'RUR',
                'value': '875',
                'discount': {
                    'oldMin': '1000',
                    'percent': '8',
                    'absolute': '125',
                },
            },
            'additionalOffers': [{
                'urls': {
                    'direct': 'https://beru.ru/product/110012?offerid=BlueOffer2-----------w',
                    'cpa': '/safeclick/data=dtype=cpa/fee=0.0300/shop_fee=300/min_fee=0/pof_debug=pof: , cpa-pof: null/uid=04884192001117778888806000/link_id=04884192001117778888800000/pp=18/show_time=488419200/show_block_id=048841920011177788888/reqid=cb8f7a995444ab4dc3215cad33fcd6bb/position=0/host=dev-rep01vd.market.yandex.net/touch=0/yandexuid=1/url_type=6/categid=2035/price=1000/hyper_id=2/hyper_cat_id=13022/nav_cat_id=15819/type_id=0/cpa=1/shop_id=244/onstock=1/geo_id=0/vcluster_id=-1/ware_md5=BlueOffer2-----------w/offer_id=777.shop_sku_2/feed_id=7453/bid_type=minbid/is_price_from=0/vnd_id=19295/promo_type=16777216/rgb=BLUE/is_blue=1/cond=0/msku=110012/supplier_id=777/supplier_type=3',
                    'encrypted': '/redir/dtype=market/cp=2/cb=2/min_bid=2/bd=0/sbid=0/ae=1/cp_vnd=0/cb_vnd=0/dtsrc_id=14676/cpbbc=2/uid=04884192001117778888800000/link_id=04884192001117778888800000/pp=18/show_time=488419200/show_block_id=048841920011177788888/reqid=cb8f7a995444ab4dc3215cad33fcd6bb/position=0/host=dev-rep01vd.market.yandex.net/touch=0/yandexuid=1/url_type=0/categid=2035/price=1000/hyper_id=2/hyper_cat_id=13022/nav_cat_id=15819/type_id=0/cpa=1/shop_id=244/onstock=1/geo_id=0/vcluster_id=-1/ware_md5=BlueOffer2-----------w/offer_id=777.shop_sku_2/feed_id=7453/bid_type=minbid/is_price_from=0/vnd_id=19295/promo_type=16777216/rgb=BLUE/is_blue=1/cond=0/msku=110012/supplier_id=777/supplier_type=3/*?data=url=https%3A%2F%2Fberu.ru%2Fproduct%2F110012%3Fofferid%3DBlueOffer2-----------w&ts=488419200&uid=1',
                },
                'offerId': offer2Id,
                'showUid': '',
                'feeShow': feeShow,
                'promoPrice': {
                    'currency': 'RUR',
                    'value': '1000',
                    'discount': {
                        'oldMin': '2000',
                        'percent': '50',
                        'absolute': '1000',
                    },
                },
                'entity': 'showPlace',
            }],
        },
    }],
}, offerId);

const state = mergeState([
    product,
    offer,
    offer2,
]);

const route = {
    productId,
    slug,
};

export default {
    state,
    route,
};
