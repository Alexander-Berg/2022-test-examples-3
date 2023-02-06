import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import SchemaOrgOrganization from '@self/platform/spec/page-objects/components/SchemaOrg/Organization';
import SchemaOrgPostalAddress from '@self/platform/spec/page-objects/components/SchemaOrg/PostalAddress';
import SchemaOrgAggregateRating from '@self/platform/spec/page-objects/components/SchemaOrg/AggregateRating';

import {SHOP_NAME, ADDRESS} from './__mock__/';

const widgetPath = '../';

const ORGANIZATION_ITEM_TYPE_VALUE = 'https://schema.org/Organization';
const POSTAL_ADDRESS_ITEM_TYPE_VALUE = 'https://schema.org/PostalAddress';
const RATING_ITEM_TYPE_VALUE = 'https://schema.org/AggregateRating';
const POSTAL_ADDRESS_ITEM_PROP_VALUE = 'address';
const RATING_ITEM_PROP_VALUE = 'aggregateRating';
const WORST_RATING_VALUE = 1;
const BEST_RATING_VALUE = 5;

let mirror;
let jestLayer;
let mandrelLayer;
let apiaryLayer;

async function makeContext({requestParams}) {
    return mandrelLayer.initContext({
        request: {
            params: requestParams,
        },
    });
}

beforeAll(async () => {
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
});

beforeAll(async () => {
    await jestLayer.doMock(
        require.resolve('@self/platform/resolvers/shops/info'),
        () => {
            const {SHOP} = require('./__mock__');
            return {
                getShopInfoWithBrand: () => Promise.resolve(SHOP),
                getReportShopInfo: () => Promise.resolve(SHOP),
            };
        }
    );
    await jestLayer.doMock(
        require.resolve('@self/platform/resolvers/shops'),
        () => {
            const {SHOP} = require('./__mock__');
            return {
                getShopRating: () => Promise.resolve(SHOP),
            };
        }
    );
    await makeContext({requestParams: {shopId: '1'}});
});

afterAll(() => {
    mirror.destroy();
});

let schemaOrganization;

describe('Widgets: ShopCard', () => {
    describe('Микроразметка страницы. Schema.org для организации.', () => {
        describe('По умолчанию', () => {
            beforeAll(async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    props: {shopId: 1},
                });
                schemaOrganization = container.querySelector(SchemaOrgOrganization.root);
            });
            test('имеет все нужные атрибуты на основном элементе разметки.', () => {
                expect(schemaOrganization.getAttribute('itemscope')).toBe('');
                expect(schemaOrganization.getAttribute('itemtype')).toBe(ORGANIZATION_ITEM_TYPE_VALUE);
            });
            test('содержит имя организации.', () => {
                const shopNameMeta = schemaOrganization.querySelector('meta[itemprop="name"]');
                expect(shopNameMeta.getAttribute('content')).toBe(SHOP_NAME);
            });
            test('содержит разметку адреса организации c верными атрибутами.', () => {
                const addressMeta = schemaOrganization.querySelector(SchemaOrgPostalAddress.root);
                expect(schemaOrganization.getAttribute('itemscope')).toBe('');
                expect(addressMeta.getAttribute('itemprop')).toBe(POSTAL_ADDRESS_ITEM_PROP_VALUE);
                expect(addressMeta.getAttribute('itemtype')).toBe(POSTAL_ADDRESS_ITEM_TYPE_VALUE);
            });
            test('содержит в разметке адрес организации.', () => {
                const addressMeta = schemaOrganization.querySelector(SchemaOrgPostalAddress.root);
                const address = addressMeta.querySelector('meta[itemprop="streetAddress"]').getAttribute('content');
                expect(address).toBe(ADDRESS);
            });
            test('содержит разметку рейтинга организации c верными атрибутами.', () => {
                const ratingMeta = schemaOrganization.querySelector(SchemaOrgAggregateRating.root);

                expect(schemaOrganization.getAttribute('itemscope')).toBe('');
                expect(ratingMeta.getAttribute('itemprop')).toBe(RATING_ITEM_PROP_VALUE);
                expect(ratingMeta.getAttribute('itemtype')).toBe(RATING_ITEM_TYPE_VALUE);
            });
            test('содержит в разметке рейтинга организации всю необходимую информацию.', () => {
                const ratingMeta = schemaOrganization.querySelector(SchemaOrgAggregateRating.root);

                const worstRating = ratingMeta.querySelector('meta[itemprop="worstRating"]').getAttribute('content');
                expect(Number(worstRating)).toBe(WORST_RATING_VALUE);

                const bestRating = ratingMeta.querySelector('meta[itemprop="bestRating"]').getAttribute('content');
                expect(Number(bestRating)).toBe(BEST_RATING_VALUE);

                const ratingValue = ratingMeta.querySelector('meta[itemprop="ratingValue"]').getAttribute('content');
                expect(Number(ratingValue)).toBeGreaterThanOrEqual(WORST_RATING_VALUE);
                expect(Number(ratingValue)).toBeLessThan(BEST_RATING_VALUE);
                expect(ratingValue).toMatch(/\d+/gi);

                const reviewCount = ratingMeta.querySelector('meta[itemprop="reviewCount"]').getAttribute('content');
                expect(reviewCount).toMatch(/\d+/gi);
            });
        });
    });
});
