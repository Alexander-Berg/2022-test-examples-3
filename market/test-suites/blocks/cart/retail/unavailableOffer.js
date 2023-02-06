import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// mocks
import * as express from '@self/root/src/spec/hermione/kadavr-mock/report/express';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

// suites
import headerSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/header';
import summarySuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/summary';
import cartListSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/cartList';

// pageObjects
import SoldOutOverlay
    from '@self/root/src/widgets/content/cart/CartList/containers/CartOfferPictureContainer/__pageObject';

const SHOP_ID = express.offerExpressMock.shop.id;
const SHOP_NAME = express.offerExpressMock.shop.name;
const SHOP_SLUG = express.offerExpressMock.shop.slug;
const BUSINESS_ID = express.offerExpressMock.shop.business_id;

const dsbsCarts = [
    buildCheckouterBucket({
        shopId: SHOP_ID,
        items: [{
            skuMock: express.skuExpressMock,
            offerMock: {...express.offerExpressMock, foodtechType: 'retail'},
            count: 1,
            features: ['eats_retail'],
            isExpired: true,
            isSkippedInReport: true,
        }],
        deliveryOptions: [{
            ...deliveryDeliveryMock,
            deliveryPartnerType: 'SHOP',
        }],
    }),
];

module.exports = makeSuite('Для retail офера. Оффер недоступен в репорте', {
    environment: 'kadavr',

    story: mergeSuites(
        {
            async beforeEach() {
                const existingReportState = mergeState([{collections: {shop: {
                    [SHOP_ID]: {id: SHOP_ID, name: SHOP_NAME, slug: SHOP_SLUG, business_id: BUSINESS_ID, entity: 'shop'},
                }}}]);

                await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    dsbsCarts,
                    {existingReportState}
                );

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});
                await this.browser.yaScenario(this, waitForCartActualization);
            },
        },

        prepareSuite(headerSuite, {
            params: {
                availableItemsCount: 0,
                unavailableItemsCount: 1,
                cartTitle: SHOP_NAME,
                businessId: BUSINESS_ID,
                businessSlug: SHOP_SLUG,
                thresholds: '',
            },
        }),

        prepareSuite(summarySuite, {
            params: {
                shouldShowSummary: false,
            },
        }),

        prepareSuite(cartListSuite, {
            hooks: {
                async beforeEach() {
                    this.setPageObjects({
                        soldOutOverlay: () => this.createPageObject(SoldOutOverlay),
                    });
                },
            },
            params: {
                shouldShowBadge: true,
            },
        })
    ),
});
