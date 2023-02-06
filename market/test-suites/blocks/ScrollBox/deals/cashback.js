import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {
    createProduct,
    mergeState,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import CashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/Cashback';
// page-objects
import CashbackInfo from '@self/root/src/components/CashbackInfos/CashbackInfo/__pageObject';
import CashbackInfoTooltip from '@self/platform/spec/page-objects/components/CashbackInfoTooltip';
import Text from '@self/root/src/uikit/components/Text/__pageObject';

// mocks
import indexPageMock from '../fixtures/index-page';

const cashbackAmount = 100;
const PRODUCT_ID = 1;
const OFFER_ID = 2;
const SLUG = 'telephone';
const PRODUCT = {
    slug: SLUG,
    type: 'model',
    categories: [
        {
            entity: 'category',
            id: 90639,
            nid: 59601,
            name: 'Телевизоры',
            slug: 'televizory',
            fullName: 'Телевизоры',
            type: 'guru',
            isLeaf: true,
        },
    ],
};
const CASHBACK_PROMO = {
    type: 'blue-cashback',
    value: cashbackAmount,
};

const EXTRA_CASHBACK_PROMO = {
    type: 'blue-cashback',
    value: cashbackAmount,
    tags: ['extra-cashback'],
};

async function prepareAndSetState(isExtraCashback) {
    await this.browser.setState(
        'Tarantino.data.result',
        [indexPageMock]
    );

    const reportState = mergeState([
        createProduct(PRODUCT, PRODUCT_ID),
        createOfferForProduct({
            entity: 'offer',
            benefit: {
                type: 'default',
            },
            showUid: OFFER_ID,
            promos: isExtraCashback ? [EXTRA_CASHBACK_PROMO] : [CASHBACK_PROMO],
            feeShow: 'qwerty',
        }, PRODUCT_ID, OFFER_ID),
        {
            data: {
                search: {
                    total: 1,
                },
            },
        },
    ]);
    await this.browser.setState('report', reportState);

    await this.browser.yaOpenPage('touch:index');
}

export default makeSuite('ScrollBox deals', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    cashbackDealTerms() {
                        return this.createPageObject(CashbackInfo);
                    },
                    cashbackInfoTooltip() {
                        return this.createPageObject(CashbackInfoTooltip);
                    },
                    cashbackDealText() {
                        return this.createPageObject(Text, {
                            parent: this.cashbackDealTerms,
                        });
                    },
                });
            },
        },
        prepareSuite(CashbackDealTermSuite, {
            meta: {
                id: 'marketfront-4182',
            },
            hooks: {
                async beforeEach() {
                    await prepareAndSetState.call(this, false);
                },
            },
            params: {
                cashbackAmount,
                cashbackFormat: 'long',
                isTooltipOnHover: false,
                isExtraCashback: false,
            },
        }),
        prepareSuite(CashbackDealTermSuite, {
            suiteName: 'Повышенный кешбэк',
            meta: {
                id: 'marketfront-4519',
            },
            hooks: {
                async beforeEach() {
                    await prepareAndSetState.call(this, true);
                },
            },
            params: {
                cashbackAmount,
                cashbackFormat: 'long',
                isTooltipOnHover: false,
                isExtraCashback: true,
            },
        })
    ),
});
