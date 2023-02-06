import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {createReportStateByEntities} from '@self/root/src/spec/utils/kadavr';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as vitaminsLowCost from '@self/root/src/spec/hermione/kadavr-mock/report/vitaminsLowCost';
import * as televisor from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import * as fuzzyFilters from '@self/root/src/spec/hermione/kadavr-mock/report/fuzzyFilters';
import * as accessories from '@self/root/src/spec/hermione/kadavr-mock/report/with-accessories';
import * as largeCargoType from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import * as dresser from '@self/root/src/spec/hermione/kadavr-mock/report/dresser';

const product = {
    id: '12345',
    slug: 'skovoroda-biol-eco-optima-semnaia-ruchka',
    categories: [
        {
            entity: 'category',
            id: 90698,
            nid: 18824970,
            name: 'Сковороды и сотейники',
            slug: 'skovorody-i-soteiniki',
            fullName: 'Сковороды и сотейники',
            type: 'guru',
            cpaType: 'cpc_and_cpa',
            isLeaf: true,
            kinds: [],
        },
    ],
    vendor: {
        entity: 'vendor',
        id: 10717670,
        name: 'Биол',
        slug: 'biol',
        website: 'http://biol.com.ua',
        filter: '7893318:10717670',
    },
    filters: [
        {
            id: '12782797',
            type: 'enum',
            name: 'Линейка',
            xslname: 'vendor_line',
            subType: '',
            originalSubType: '',
            kind: 1,
            isGuruLight: true,
            position: 15,
            noffers: 1,
            valuesCount: 1,
            values: [
                {
                    initialFound: 1,
                    popularity: 759,
                    found: 1,
                    value: 'Оптима',
                    vendor: {
                        name: 'Биол',
                        entity: 'vendor',
                        id: 10717670,
                    },
                    id: '18913931',
                },
            ],
            valuesGroups: [
                {
                    type: 'all',
                    valuesIds: [
                        '18913931',
                    ],
                },
            ],
            meta: {},
        },
    ],
    links: [
        {
            type: 'filter',
            hid: '90666',
            filter: '12782797',
            xslname: 'vendor_line',
            values: [
                '18913931',
            ],
        },
    ],
};


const reportStateGoods = createReportStateByEntities([
    {
        sku: kettle.skuMock,
        offer: kettle.offerMock,
    },
    {
        sku: vitaminsLowCost.skuMock,
        offer: vitaminsLowCost.offerMock,
    },
    {
        product: televisor.productMock,
        offer: televisor.offerMock,
    },
    {
        product: fuzzyFilters.productMock,
        offer: fuzzyFilters.offerMock1,
    },
    {
        product: accessories.product,
        offer: accessories.offer,
    },
    {
        product: largeCargoType.productMock,
        offer: largeCargoType.offerMock,
    },
    {
        product: dresser.productMock,
        offer: dresser.offerMock,
    },
]);


const state = mergeReportState([
    createProduct(product, product.id),
    reportStateGoods,
]);

const productId = product.id;
const slug = product.slug;
const filter = product.filters[0];
const filterValue = filter.values[0];
const lineName = filterValue.value;
const hid = product.links[0].hid;
const vendorLineGlFilter = `${filter.id}:${filterValue.id}`;
const vendorGLFilter = product.vendor.filter;

const routeParams = {
    hid,
    glfilter: [vendorLineGlFilter, vendorGLFilter],
};

export {
    state,
    productId,
    slug,
    lineName,
    routeParams,
};
