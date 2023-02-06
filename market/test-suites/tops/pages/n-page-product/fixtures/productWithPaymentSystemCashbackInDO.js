import {mergeState, createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';
import {paymentSystemExtraCashbackPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

const shopId = '431782';
const productId = '123';
const offerId = '456';
const slug = 'onetwothree';
export const CASHBACK_AMOUNT = 100;
export const PAYMENT_SYSTEM_CASHBACK_AMOUNT = 200;

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

const product = createProduct({
    deletedId: null,
    categories: createCategories(),
    navnodes: createNavnodes(),
    slug: 'product',
    type: 'model',
}, productId);

const createBaseOffer = promos => ({
    entity: 'offer',
    showUid: '15592162537669963376206001',
    id: offerId,
    wareId: offerId,
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
    shop: {
        id: shopId,
        entity: 'shop',
        name: 'Тестовый магазин проекта Фулфиллмент',
        status: 'actual',
        slug: 'slag-suag-swag',
        logo: 'shop-logo',
        feed: {
            id: 123123,
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
    promos,
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
});

const route = {
    productId,
    slug,
};

export async function createAndSetPaymentSystemCashbackState({
    isPromoAvailable,
    isPromoProduct,
    hasCashback,
}) {
    const promos = [];

    if (isPromoProduct) {
        promos.push({
            id: '1',
            key: '1',
            type: 'blue-cashback',
            value: PAYMENT_SYSTEM_CASHBACK_AMOUNT,
            tags: ['payment-system-promo'],
        });
    }

    if (hasCashback) {
        promos.push({
            id: '2',
            key: '2',
            type: 'blue-cashback',
            value: CASHBACK_AMOUNT,
        });
    }

    const baseOffer = createBaseOffer(promos);
    const offer = createOffer(baseOffer, offerId);

    const state = mergeState([
        product,
        offer,
    ]);

    await this.browser.setState('Loyalty.collections.perks', isPromoAvailable ? [paymentSystemExtraCashbackPerk] : []);
    await this.browser.setState('report', state);
    return this.browser.yaOpenPage('market:product', route);
}
