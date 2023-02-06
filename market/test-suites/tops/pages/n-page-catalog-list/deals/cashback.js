import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import CashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/Cashback';
import CashbackDealTerms from '@self/platform/spec/page-objects/components/CashbackDealTerms';
import CashbackInfoTooltip from '@self/platform/spec/page-objects/components/CashbackInfoTooltip';
import Text from '@self/root/src/uikit/components/Text/__pageObject';

import {offerMock} from '../fixtures/offer.mock';

const cashbackAmount = 100;

const CASHBACK_PROMO = {
    id: '1',
    key: '1',
    type: 'blue-cashback',
    value: cashbackAmount,
};

const EXTRA_CASHBACK_PROMO = {
    id: '2',
    key: '2',
    type: 'blue-cashback',
    value: cashbackAmount,
    tags: ['extra-cashback'],
};


async function createAndSetState(isExtraCashback) {
    const offer = createOffer({
        ...offerMock,
        promos: isExtraCashback ? [EXTRA_CASHBACK_PROMO] : [CASHBACK_PROMO],
    }, offerMock.wareId);
    const dataMixin = {
        data: {
            search: {
                total: 1,
                totalOffers: 1,
            },
        },
    };
    const state = mergeState([
        offer,
        dataMixin,
    ]);
    await this.browser.setState('report', state);
    return this.browser.yaOpenPage('market:list', {
        nid: offerMock.navnodes[0].id,
        slug: offerMock.navnodes[0].slug,
        viewtype: 'list',
    });
}

export default makeSuite('Блок с кешбэком.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    cashbackDealTerms() {
                        return this.createPageObject(CashbackDealTerms);
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
                id: 'marketfront-4176',
            },
            hooks: {
                async beforeEach() {
                    await createAndSetState.call(this, false);
                },
            },
            params: {
                cashbackAmount,
                cashbackFormat: 'full',
                isTooltipOnHover: true,
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
                    await createAndSetState.call(this, true);
                },
            },
            params: {
                cashbackAmount,
                cashbackFormat: 'full',
                isTooltipOnHover: true,
                isExtraCashback: true,
            },
        })
    ),
});
