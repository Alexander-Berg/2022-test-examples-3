import {
    mergeState,
    createProduct,
    createOffer,
    createFilter,
    createFilterValue,
    createEntityFilterValue,
    createSku,
    createEntityFilter,
    createEntityPicture,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import productWithColoredPicturesFixture from './productWithColoredPictures';

const THUMBNAILS = [{
    containerWidth: 50,
    containerHeight: 50,
    width: 24,
    height: 50,
}, {
    containerWidth: 55,
    containerHeight: 70,
    width: 34,
    height: 70,
}, {
    containerWidth: 60,
    containerHeight: 80,
    width: 39,
    height: 80,
}, {
    containerWidth: 74,
    containerHeight: 100,
    width: 49,
    height: 100,
}, {
    containerWidth: 75,
    containerHeight: 75,
    width: 37,
    height: 75,
}, {
    containerWidth: 90,
    containerHeight: 120,
    width: 59,
    height: 120,
}, {
    containerWidth: 100,
    containerHeight: 100,
    width: 49,
    height: 100,
}, {
    containerWidth: 120,
    containerHeight: 160,
    width: 79,
    height: 160,
}, {
    containerWidth: 150,
    containerHeight: 150,
    width: 74,
    height: 150,
}, {
    containerWidth: 180,
    containerHeight: 240,
    width: 119,
    height: 240,
}, {
    containerWidth: 190,
    containerHeight: 250,
    width: 124,
    height: 250,
}, {
    containerWidth: 200,
    containerHeight: 200,
    width: 99,
    height: 200,
}, {
    containerWidth: 240,
    containerHeight: 320,
    width: 159,
    height: 320,
}, {
    containerWidth: 300,
    containerHeight: 300,
    width: 149,
    height: 300,
}, {
    containerWidth: 300,
    containerHeight: 400,
    width: 198,
    height: 400,
}];

const getSkuPictures = img => ({
    entity: 'picture',
    original: {
        containerWidth: 254,
        containerHeight: 511,
        url: `//avatars.mds.yandex.net/get-mpic/${img}/orig`,
        width: 254,
        height: 511,
    },
    thumbnails: THUMBNAILS.map(thumbnail => ({
        ...thumbnail,
        url: `//avatars.mds.yandex.net/get-mpic/${img}/${thumbnail.containerWidth}x${thumbnail.containerHeight}`,
    })),
});

const {productMock} = productWithColoredPicturesFixture;
const {id: productId} = productMock;
const product = createProduct(productMock, productId);

const skuYellowId = '100210863681';
const ENUM_FILTER_ID = '19172750';

const enumFilterValuesMock = [{
    slug: '',
    found: 1,
    value: 'enumValue1',
    marketSku: skuYellowId,
}, {
    found: 1,
    slug: 'smartfon-apple-iphone-xr-64gb-krasnyi',
    value: 'enumValue2',
    marketSku: skuYellowId,
}];


const enumFilterValues = enumFilterValuesMock.map((enumFilterValue, i) => {
    const id = String(i + 1);
    return createFilterValue({...enumFilterValue, marketSku: id}, ENUM_FILTER_ID, id);
});

const enumFilterValuesWithCheckedValue = enumFilterValuesMock.map((enumFilterValue, i) => {
    const id = String(i + 1);
    return createFilterValue({...enumFilterValue, marketSku: id, checked: i === 0}, ENUM_FILTER_ID, id);
});

const enumFilterMock = {
    isGuruLight: true,
    kind: 2,
    meta: {},
    name: 'Enum filter',
    precision: 2,
    type: 'enum',
    valuesGroups: [],
    subType: 'radio',
};

const enumFilter = createFilter(enumFilterMock, String(ENUM_FILTER_ID));

const colorFilterId = '14871214';
const colorFilterMock = {
    id: '14871214',
    type: 'enum',
    name: 'Цвет товара',
    xslname: 'color_vendor',
    subType: 'image_picker',
    kind: 2,
    position: 1,
    noffers: 242,
    valuesCount: 5,
    valuesGroups: [],
};
const colorFilter = createFilter(colorFilterMock, colorFilterId);

const SKU_TITLE = 'Смартфон Samsung Galaxy S8, желтый';

const skuYellow = createSku({
    ...productMock,
    titles: {
        'raw': SKU_TITLE,
    },
    product: {
        ...productMock,
    },
    filters: [colorFilterId],
}, skuYellowId);

const avatarsPrefix = '//avatars.mds.yandex.net/get-mpic/364668/model_option-picker-';

const skuBlueId = '100210863684';
const colorFilterValueBlueId = '14898165';
const colorFilterValueBlueMock = {
    value: 'голубой',
    image: `${avatarsPrefix}1722193751-14898165--8fc6f04ed732d2fc6b9b9043447a7260/orig`,
    found: 15,
    marketSku: '100210863684',
};
const colorFilterValueBlue = createFilterValue(colorFilterValueBlueMock, colorFilterId, colorFilterValueBlueId);

const colorFilterValueYellowId = '15278711';
const colorFilterValueYellowMock = {
    value: 'желтый топаз',
    image: `${avatarsPrefix}1722193751-15278711--8a7e57690d31d8639d33d12be12ef704/orig`,
    found: 25,
    marketSku: skuYellowId,
};
const colorFilterValueYellow = createFilterValue(colorFilterValueYellowMock, colorFilterId, colorFilterValueYellowId);


const payments = {
    deliveryCard: true,
    deliveryCash: true,
    prepaymentCard: true,
    prepaymentOther: false,
};

const offer = {
    product: {
        id: productId,
    },
    isCutPrice: false,
    payments,
    prices: {
        min: '600',
        currency: 'RUR',
    },
};

const generateOffer = (offerId, price, i, shopName) => createOffer({
    ...offer,
    prices: {
        min: price,
        currency: 'RUR',
    },
    bundleCount: 50,
    shop: {
        name: `${shopName} ${i + 1}`,
    },
    cpc: offerId,
    benefit: !i ? {
        isPrimary: true,
        type: 'default',
    } : undefined,
}, offerId);

const intiOfferId = 1001;
const initOffersCount = 3;

const initOffers = Array(initOffersCount).fill(null).map((_, i) =>
    generateOffer(String(intiOfferId + i), 5000 + (i * 250), i, 'Shop'), []);

const skuOfferId = 2001;
const skuOffersCount = 50;

const skuOffers = Array(skuOffersCount).fill(null).reduce((acc, _, i) => {
    const offerId = String(skuOfferId + i);
    const price = 1000 + (i * 150);

    return [
        ...acc,
        createEntityFilter(colorFilterMock, 'offer', offerId, colorFilterId),
        createEntityFilterValue(colorFilterValueYellowMock, offerId, colorFilterId, colorFilterValueYellowId),
        createEntityFilter(enumFilterMock, 'offer', offerId, ENUM_FILTER_ID),
        createEntityFilterValue(enumFilterValues[0], offerId, ENUM_FILTER_ID, '1'),
        generateOffer(offerId, price, i, 'SkuShop'),
    ];
}, []);

const initState = mergeState([
    product,
    colorFilter,
    colorFilterValueBlue,
    colorFilterValueYellow,
    enumFilter,
    ...enumFilterValues,
    ...initOffers,
    createEntityPicture(getSkuPictures('195452/img_id4783826773129378920'), 'product', productId, '80001'),
    createEntityPicture(getSkuPictures('466729/img_id1424119088555673128'), 'product', productId, '80002'),
    createEntityPicture(getSkuPictures('331398/img_id6221223021850607033'), 'product', productId, '80003'),
    {
        data: {
            search: {
                total: 9000,
                totalOffers: 9000,
                totalOffersBeforeFilters: 9000,
            },
        },
    },
]);

const skuState = mergeState([
    skuYellow,
    colorFilter,
    colorFilterValueBlue,
    colorFilterValueYellow,
    enumFilter,
    ...enumFilterValuesWithCheckedValue,
    createEntityPicture(getSkuPictures('1927699/img_id4447051930764154992.jpeg'), 'sku', skuYellowId, '90001'),
    createEntityPicture(getSkuPictures('466729/img_id382919798489486397'), 'sku', skuYellowId, '90002'),
    ...skuOffers,
]);

const route = {
    productId,
    slug: productMock.slug,
};

export default {
    skuYellowId,
    SKU_TITLE,
    ENUM_FILTER_ID,
    skuBlueId,
    initState,
    skuState,
    route,
    skuPicture: 'https://avatars.mds.yandex.net/get-mpic/1927699/img_id4447051930764154992.jpeg/orig',
    skuThumbs: [
        'https://avatars.mds.yandex.net/get-mpic/1927699/img_id4447051930764154992.jpeg/50x50',
        'https://avatars.mds.yandex.net/get-mpic/466729/img_id382919798489486397/50x50',
    ],
};
