import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createShop, createProduct, createOffer} from '@self/platform/spec/hermione/helpers/shopRating';
import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import ShopLogoSuite from '@self/platform/spec/hermione/test-suites/blocks/n-shop-logo';
// page-objects
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import ShopLogo from '@self/platform/spec/page-objects/components/ShopLogo';
import {logo as logoMock} from './fixtures/logo';

const ROUTE = routes.product.galaxyS8plus;

async function createAndSetState(browser, shop = {}) {
    const {productId} = ROUTE;
    const product = createProduct({slug: 'product'}, productId);
    const offer = createOffer({
        shop: createShop(shop),
        benefit: {
            type: 'recommended',
            description: 'Маркет рекомендует',
            isPrimary: true,
        },
        urls: {
            encrypted: '/redir/encrypted',
            decrypted: '/redir/decrypted',
            offercard: '/redir/offercard',
            geo: '/redir/geo',
        },
        delivery: {
            shopPriorityRegion: {
                entity: 'region',
                id: 62007514,
                name: 'ea mol',
                lingua: {
                    name: {
                        accusative: 'ut aliqua',
                        genitive: 'veniam Excepteur consequat',
                        preposition: 'sit',
                        prepositional: 'nulla amet',
                    },
                },
            },
            shopPriorityCountry: {
                entity: 'region',
                id: 59868827,
                name: 'in officia exercitation',
                lingua: {
                    name: {
                        accusative: 'anim aute',
                        genitive: 'reprehenderit',
                        preposition: 'dolor ad Duis aliqua sunt',
                        prepositional: 'voluptate cillum',
                    },
                },
            },
            region: {
                lingua: {
                    name: {
                        accusative: 'anim aute',
                        genitive: 'reprehenderit',
                        preposition: 'dolor ad Duis aliqua sunt',
                        prepositional: 'voluptate cillum',
                    },
                },
                title: 'Регион, в который будет осуществляться доставка курьером',
            },
            price: {
                currency: 'reprehenderit ',
                value: 87304050,
            },
            options: [],
        },
    });

    await browser.setState('report', mergeState([
        product,
        offer,
    ]));
}

export default makeSuite('Дефолтный оффер с логотипом магазина.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    shopLogo: () => this.createPageObject(ShopLogo, {
                        parent: DefaultOffer.root,
                    }),
                });
            },
        },
        prepareSuite(ShopLogoSuite, {
            meta: {
                id: 'marketfront-3015',
                issue: 'MARKETVERSTKA-32024',
            },
            hooks: {
                async beforeEach() {
                    const shopId = '431782';

                    await createAndSetState(this.browser, {
                        id: shopId,
                        logo: logoMock,
                    });

                    return this.browser.yaOpenPage('market:product', ROUTE);
                },
            },
        })
    ),
});
