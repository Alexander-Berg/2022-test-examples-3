import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import CashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchSnippet/cashback';
import CashbackInfoTooltip from '@self/platform/spec/page-objects/components/CashbackInfoTooltip';
import SearchSnippetPrice from '@self/platform/spec/page-objects/containers/SearchSnippet/Price';

import {offerMock} from '../fixtures/offer.mock';

const cashbackAmount = 100;

const CASHBACK_PROMO = [{
    type: 'blue-cashback',
    value: cashbackAmount,
}];

const EXTRA_CASHBACK_PROMO = [{
    type: 'blue-cashback',
    value: cashbackAmount,
    tags: ['extra-cashback'],
}];

async function prepareAndSetState(isExtraCashback) {
    const offer = createOffer({
        ...offerMock,
        promos: isExtraCashback ? EXTRA_CASHBACK_PROMO : CASHBACK_PROMO,
    }, offerMock.wareId);
    await this.browser.setState('report', offer);
    return this.browser.yaOpenPage('touch:list', {
        nid: offerMock.navnodes[0].id,
        slug: offerMock.navnodes[0].slug,
    });
}

export default makeSuite('Блок с кешбэком.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    snippetPrice() {
                        return this.createPageObject(SearchSnippetPrice);
                    },
                    cashbackInfoTooltip() {
                        return this.createPageObject(CashbackInfoTooltip);
                    },
                });
            },
        },
        prepareSuite(CashbackDealTermSuite, {
            meta: {
                id: 'marketfront-4176',
            },
            hooks: {
                async beforeEach() {
                    await prepareAndSetState.call(this, false);
                },
            },
            params: {
                cashbackAmount,
                cashbackFormat: 'short',
                isTooltipOnHover: false,
                isExtraCashback: false,
            },
        }),
        prepareSuite(CashbackDealTermSuite, {
            suiteName: 'Повышенный кешбэк.',
            meta: {
                id: 'marketfront-4500',
            },
            hooks: {
                async beforeEach() {
                    await prepareAndSetState.call(this, true);
                },
            },
            params: {
                cashbackAmount,
                cashbackFormat: 'short',
                isTooltipOnHover: false,
                isExtraCashback: true,
            },
        })
    ),
});
