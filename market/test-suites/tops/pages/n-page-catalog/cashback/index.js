import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import CashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/Cashback';
import CashbackDealTerms from '@self/platform/spec/page-objects/components/CashbackDealTerms';
import CashbackInfoTooltip from '@self/platform/spec/page-objects/components/CashbackInfoTooltip';
import {createShop, createProduct, createOffer} from '@self/platform/spec/hermione/helpers/shopRating';
import Text from '@self/root/src/uikit/components/Text/__pageObject';

const cashbackAmount = 100;

const CASHBACK_PROMO = {
    type: 'blue-cashback',
    value: cashbackAmount,
};

const EXTRA_CASHBACK_PROMO = {
    type: 'blue-cashback',
    value: cashbackAmount,
    tags: ['extra-cashback'],
};

async function createAndSetState(browser, shop = {}, isExtraCashback) {
    const offerId = 'fFftggdaFshwfg3gFfregW';
    const productId = 1722193751;
    const productSlug = 'smartfon-samsung-galaxy-s8';

    const product = createProduct({slug: productSlug}, productId);
    const offer = createOffer({
        shop: createShop(shop),
        urls: {
            encrypted: '/redir/encrypted',
            decrypted: '/redir/decrypted',
            offercard: '/redir/offercard',
            geo: '/redir/geo',
        },
        promos: isExtraCashback ? [EXTRA_CASHBACK_PROMO] : [CASHBACK_PROMO],
    }, offerId);

    await browser.setState('report', mergeState([
        {
            data: {
                search: {
                    // нужно чтобы отображался список
                    total: 1,
                },
            },
        },
        product,
        offer,
    ]));
}

export default makeSuite('Блок с кешбэком', {
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
                    const shopId = '431782';
                    const slug = 'shopSlug';

                    await createAndSetState(this.browser, {
                        id: shopId,
                        slug,
                    }, false);

                    return this.browser.yaOpenPage('market:catalog', routes.list.phones);
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
                    const shopId = '431782';
                    const slug = 'shopSlug';

                    await createAndSetState(this.browser, {
                        id: shopId,
                        slug,
                    }, true);

                    return this.browser.yaOpenPage('market:catalog', routes.list.phones);
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
