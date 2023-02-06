import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createShop, createProduct, createOffer} from '@self/platform/spec/hermione/helpers/shopRating';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import ShopLogoSuite from '@self/platform/spec/hermione/test-suites/blocks/n-shop-logo';
import SnippetList from '@self/platform/widgets/content/productOffers/Results/__pageObject';
import ShopLogo from '@self/platform/spec/page-objects/components/ShopLogo';

import {logo as logoMock} from './fixtures/logo';

const ROUTE = routes.product.galaxyS8plus;

async function createAndSetState(browser, shop = {}) {
    const {productId} = ROUTE;
    const product = createProduct({slug: 'product'}, productId);
    const offer = createOffer({
        isCutPrice: false,
        shop: createShop(shop),
        urls: {
            encrypted: '/redir/encrypted',
            decrypted: '/redir/decrypted',
            offercard: '/redir/offercard',
            geo: '/redir/geo',
        },
    });

    await browser.setState('report', mergeState([
        product,
        offer,
        {
            data: {
                search: {
                    // нужно чтобы отображался список
                    total: 1,
                },
            },
        },
    ]));
}

export default makeSuite('Сниппет оффера с логотипом магазина.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    shopLogo: () => this.createPageObject(ShopLogo, {parent: SnippetList.root}),
                });
            },
        },
        prepareSuite(ShopLogoSuite, {
            meta: {
                id: 'marketfront-3016',
                issue: 'MARKETVERSTKA-32026',
            },
            hooks: {
                async beforeEach() {
                    const shopId = '431782';

                    await createAndSetState(this.browser, {
                        id: shopId,
                        logo: logoMock,
                    });

                    return this.browser.yaOpenPage('market:product-offers', ROUTE);
                },
            },
        })
    ),
});
