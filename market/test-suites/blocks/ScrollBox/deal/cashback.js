import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {
    createProduct,
    mergeState,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import CashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/Cashback';
// page-objects
import CashbackDealTerms from '@self/platform/spec/page-objects/components/CashbackDealTerms';
import CashbackInfoTooltip from '@self/platform/spec/page-objects/components/CashbackInfoTooltip';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
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
};
const OFFER = {
    entity: 'offer',
    benefit: {
        type: 'default',
    },
    showUid: OFFER_ID,
    promos: [{
        type: 'blue-cashback',
        value: cashbackAmount,
    }],
    feeShow: 'qwerty',
};

const EXTRA_OFFER = {
    entity: 'offer',
    benefit: {
        type: 'default',
    },
    showUid: OFFER_ID,
    promos: [{
        type: 'blue-cashback',
        value: cashbackAmount,
        tags: ['extra-cashback'],
    }],
    feeShow: 'qwerty',
};

async function preparePage(isExtraCashback) {
    await this.browser.setState(
        'Tarantino.data.result',
        [indexPageMock]
    );

    let offer = isExtraCashback ? EXTRA_OFFER : OFFER;

    const reportState = mergeState([
        createProduct(PRODUCT, PRODUCT_ID),
        createOfferForProduct(offer, PRODUCT_ID, OFFER_ID),
        {
            data: {
                search: {
                    total: 1,
                },
            },
        },
    ]);
    await this.browser.setState('report', reportState);

    await this.browser.yaOpenPage('market:index');
}

export default makeSuite('ScrollBox deals', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    cashbackDealTerms() {
                        return this.createPageObject(CashbackDealTerms, {
                            root: ScrollBox.snippet,
                        });
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
                    await preparePage.call(this, false);
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
            suiteName: 'Повышенный кешбэк.',
            meta: {
                id: 'marketfront-4519',
            },
            hooks: {
                async beforeEach() {
                    await preparePage.call(this, true);
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
