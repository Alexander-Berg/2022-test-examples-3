import {
    createFilter,
    createFilterValue,
    mergeState,
    createProduct,
    createEntityFilter,
    createEntityFilterValue,
} from '@yandex-market/kadavr/mocks/Report/helpers';

export const filterId = '13478167';
export const filterName = 'Filter Name';
export const productId = '12345';

const product = {
    id: productId,
    showUid: '15917784352105375839016014',
    entity: 'product',
    slug: 'noutbuk-sony-vaio-svt1312z1r',
    categories: [
        {
            entity: 'category',
            id: 91013,
            nid: 54544,
            name: 'Ноутбуки',
            slug: 'noutbuki',
            fullName: 'Ноутбуки',
            type: 'guru',
            cpaType: 'cpc_and_cpa',
            isLeaf: true,
            kinds: [],
        },
    ],
    cpc: 'R3Ya1A58L',
    navnodes: [
        {
            entity: 'navnode',
            id: 54544,
            name: 'Ноутбуки',
            slug: 'noutbuki',
            fullName: 'Ноутбуки',
            isLeaf: false,
            rootNavnode: {},
        },
    ],
    filters: [filterId],
    meta: {},
    type: 'group',
    modelCreator: 'market',
    modificationsCount: 1,
    offers: {
        count: 19,
        cutPriceCount: 0,
    },
};

const booleanFilterValues = [
    {
        initialFound: 11,
        found: 11,
        value: '1',
        priceMin: {
            currency: 'RUR',
            value: '100',
        },
        id: '1',
    },
    {
        initialFound: 22,
        found: 22,
        value: '0',
        priceMin: {
            currency: 'RUR',
            value: '112',
        },
        id: '0',
    },
];

const data = {
    data: {
        search: {
            total: 123,
            totalOffers: 123,
        },
    },
};

const filterValues = booleanFilterValues.map(filterValue =>
    createFilterValue(filterValue, filterId, filterValue.id)
);
const filter = createFilter(
    {
        type: 'boolean',
        kind: 2,
        name: 'Filter Name',
        xslname: 'FilterName',
        position: 1,
        noffers: 379,
        isGuruLight: true,
        hasBoolNo: false,
    },
    filterId
);

const entityFilter = createEntityFilter(
    {
        initialFound: 11,
        found: 11,
        value: '1',
        priceMin: {
            currency: 'RUR',
            value: '100',
        },
        id: '1',
    },
    'product',
    productId,
    filterId
);

const entityFilterValue = createEntityFilterValue(
    {
        initialFound: 11,
        found: 11,
        value: '1',
        priceMin: {
            currency: 'RUR',
            value: '100',
        },
        id: '1',
    },
    productId,
    filterId,
    '1'
);

export const state = mergeState([
    createProduct(product, productId),
    filter,
    data,
    ...filterValues,
    entityFilter,
    entityFilterValue,
]);
