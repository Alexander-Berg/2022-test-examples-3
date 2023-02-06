import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createShop, createProduct, createOffer} from '@self/platform/spec/hermione/helpers/shopRating';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import ShopLogoSuite from '@self/platform/spec/hermione/test-suites/blocks/n-shop-logo';
import TopOffers from '@self/platform/spec/page-objects/widgets/content/TopOffers';
import ShopLogo from '@self/platform/spec/page-objects/components/ShopLogo';

import {logo as logoMock} from './fixtures/logo';

const ROUTE = routes.product.galaxyS8plus;

async function createAndSetState(browser, shop = {}) {
    const {productId} = ROUTE;
    const product = createProduct({slug: 'product'}, productId);
    const offer1 = createOffer({
        shop: createShop(shop),
        isCutPrice: false,
        cpc: 'DqqPjIrWS5xITq',
        urls: {
            encrypted: '/redir/encrypted',
            decrypted: '/redir/decrypted',
            offercard: '/redir/offercard',
            geo: '/redir/geo',
        },
        payments: {
            deliveryCard: true,
            deliveryCash: true,
            prepaymentCard: true,
            prepaymentOther: false,
        },
    });
    const offer2 = createOffer({
        shop: createShop({
            id: 2,
            overallGradesCount: 1,
        }),
        isCutPrice: false,
        cpc: 'DqqPjIrWS5xITw',
        urls: {
            encrypted: '/redir/encrypted',
            decrypted: '/redir/decrypted',
            offercard: '/redir/offercard',
            geo: '/redir/geo',
        },
        payments: {
            deliveryCard: true,
            deliveryCash: true,
            prepaymentCard: true,
            prepaymentOther: false,
        },
    });

    await browser.setState('report', mergeState([
        {
            data: {
                search: {
                    // нужно чтобы отобразился блок топ-6
                    totalOffersBeforeFilters: 2,
                },
            },
        },
        product,
        offer1,
        offer2,
    ]));
}

export default makeSuite('Топ-6. Сниппет оффера с логотипом магазина.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    shopLogo: () => this.createPageObject(ShopLogo, {
                        parent: TopOffers.root,
                    }),
                });
            },
        },
        prepareSuite(ShopLogoSuite, {
            meta: {
                id: 'marketfront-3014',
                issue: 'MARKETVERSTKA-32025',
            },
            hooks: {
                async beforeEach() {
                    const shopId = '431782';

                    await createAndSetState(this.browser, {
                        id: shopId,
                        logo: logoMock,
                    });

                    return this.browser.yaOpenPage('market:product-spec', ROUTE);
                },
            },
        })
    ),
});
