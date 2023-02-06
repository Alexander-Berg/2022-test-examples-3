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
    id: goldId,
    value: 'золотистый',
    code: '#FFD700',
    initialFound: 5,
    found: 5,
};

const blackId = '14896255';
const blackFixture = {
    id: blackId,
    value: 'черный',
    code: '#000000',
    initialFound: 5,
    found: 5,
};

const filterId = '14871214';
const filterFixture = {
    id: filterId,
    type: 'enum',
    name: 'Цвет товара',
    xslname: 'color_vendor',
    subType: 'image_picker',
    kind: 2,
    position: 4,
    noffers: 1,
    valuesGroups: [
        {
            type: 'all',
            valuesIds: [goldId, blackId],
        },
    ],
};

const category = {
    entity: 'category',
    id: 54545,
    nid: 54545,
    name: 'Планшеты',
    slug: 'planshety',
    fullName: 'Планшеты',
    type: 'guru',
    cpaType: 'cpa_with_cpc_pessimization',
    isLeaf: true,
    kinds: [],
};

const navnode = {
    category,
    entity: 'navnode',
    id: 54545,
    name: 'Планшеты',
    slug: 'planshety',
    fullName: 'Планшеты',
    isLeaf: true,
    rootNavnode: {
        entity: 'navnode',
        id: 54432,
    },
};

const productId = 13905590;
const productFixture = {
    deletedId: null,
    entity: 'product',
    slug,
    categories: [category],
    navnodes: [navnode],
    filters: [filterId],
    meta: {},
    type: 'model',
    id: productId,
    offers: {
        count: 11,
    },
};

// Создаем продукт
const product = createProduct(productFixture, productId);

// Создаем общий фильтр с двумя значениями
const colorFilter = createFilter(filterFixture, filterId);
const goldValue = createFilterValue(goldFixture, filterId, goldId);
const blackValue = createFilterValue(blackFixture, filterId, blackId);

// Создаем оффера с фильтрами
const _generateOffer = (shopId, bundleCount) => ({
    entity: 'offer',
    slug,
    categories: [category],
    navnodes: [navnode],
    shop: {
        entity: 'shop',
        id: shopId,
        name: 'Mobile Mega',
        slug: 'mobile-mega',
        status: 'actual',
        cutoff: '',
    },
    prices: {
        currency: 'RUR',
        value: '23590',
        isDeliveryIncluded: false,
        rawValue: '23590',
    },
    isAdult: false,
    isCutPrice: false,
    isDailyDeal: false,
    isFulfillment: false,
    isRecommendedByVendor: false,
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
    cpc: `${bundleCount}DqqPjIrWS5xIT`,
    vendor: {
        id: 2222,
        entity: 'vendor',
        name: 'some_vendor_name',
    },
    bundleCount,
});

const topOfferIds = [10, 103, 23];

const createOfferStates = offerIds =>
    map(
        offerId => {
            const fixture = _generateOffer(offerId + 100, offerId);
            const offer = createOffer(fixture, offerId);
            const filter = createEntityFilter(filterFixture, 'offer', offerId, filterId);
            const filterValueFixture = offerId % 2 === 0 ? goldFixture : blackFixture;
            const filterValue = createEntityFilterValue(
                filterValueFixture,
                offerId,
                filterId,
                filterValueFixture.id
            );

            return mergeState([offer, filter, filterValue]);
        },
        [...offerIds]
    );

const topOffers = createOfferStates(topOfferIds);

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
    state: topOffersState,
    route: {
        productId,
        slug,
    },
};
