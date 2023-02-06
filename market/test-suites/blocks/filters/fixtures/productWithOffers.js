import {
    mergeState,
    createProduct,
    createPriceRange,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import clarifyingCategories from './clarifyingCategories';


const ENUM_FILTER_ID = '19172750';

const enumFilterValuesMock = [{
    slug: '',
    found: 1,
    value: 'enumValue1',
},
{
    found: 1,
    slug: 'smartfon-apple-iphone-xr-64gb-krasnyi',
    value: 'enumValue2',
},
];

const enumFilterMock = {
    isGuruLight: true,
    kind: 2,
    meta: {},
    name: 'Enum filter',
    precision: 2,
    subType: '',
    type: 'enum',
    valuesGroups: [],
};

const COMPILATION_ITEM_FILTER = '7893318:153061';
const COMPILATION_ITEM_HID = 91491;

const product1 = createProduct({
    prices: createPriceRange(3000, 6000, 'RUB'),
    slug: 'test-product1',
    categories: [
        {
            entity: 'category',
            id: COMPILATION_ITEM_HID,
            name: 'Мобильные телефоны',
            fullName: 'Мобильные телефоны',
            type: 'guru',
            isLeaf: true,
        },
    ],
    vendor: {
        entity: 'vendor',
        id: 153043,
        name: 'Apple',
        slug: 'apple',
        filter: COMPILATION_ITEM_FILTER,
    },
}, 1);

const product2 = createProduct({
    prices: createPriceRange(9000, 12000, 'RUB'),
    slug: 'test-product2',

    categories: [
        {
            entity: 'category',
            id: COMPILATION_ITEM_HID,
            name: 'Мобильные телефоны',
            fullName: 'Мобильные телефоны',
            type: 'guru',
            isLeaf: true,
        },
    ],
    vendor: {
        entity: 'vendor',
        id: 144444,
        name: 'Xiaomi',
        slug: 'xiaomi',
        filter: COMPILATION_ITEM_FILTER,
    },
}, 2);

const product3 = createProduct({
    prices: createPriceRange(2000, 5000, 'RUB'),
    slug: 'test-product3',
    categories: [
        {
            entity: 'category',
            id: COMPILATION_ITEM_HID,
            name: 'Мобильные телефоны',
            fullName: 'Мобильные телефоны',
            type: 'guru',
            isLeaf: true,
        },
    ],
    vendor: {
        entity: 'vendor',
        id: 789678,
        name: 'Samsung',
        slug: 'samsung',
        filter: COMPILATION_ITEM_FILTER,
    },
}, 3);

const productCount = 3;

const totalMixin = {
    data: {
        search: {
            total: productCount,
            totalModels: productCount,
        },
        sorts: [{text: 'по популярности'}],
        spellchecker: {
            new: {raw: 'наушники', highlighted: [{value: 'наушники'}]},
            old: 'ноушники',
            probablyTypo: true,
        },
    },
};

const reportState = mergeState([
    product1,
    product2,
    product3,
    totalMixin,
    clarifyingCategories,
]);

export {
    ENUM_FILTER_ID,
    enumFilterValuesMock,
    enumFilterMock,
    COMPILATION_ITEM_FILTER,
    COMPILATION_ITEM_HID,
    reportState,
};
