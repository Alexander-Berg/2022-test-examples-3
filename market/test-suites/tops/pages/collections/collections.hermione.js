import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import CollectionsSuite from '@self/platform/spec/hermione/test-suites/blocks/w-collections';
import OfferDefaultShopLinkSuite from '@self/platform/spec/hermione/test-suites/blocks/offer-default__shop-link';
// page-objects
import Collections from '@self/platform/spec/page-objects/w-collections';
import Link from '@self/platform/spec/page-objects/link';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница подборок', {
    environment: 'testing',
    story: mergeSuites(
        prepareSuite(CollectionsSuite, {
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage('touch:collections', routes.collections);
                },
            },

            pageObjects: {
                collections() {
                    return this.createPageObject(Collections);
                },
            },
        }),
        prepareSuite(OfferDefaultShopLinkSuite, {
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage('touch:collections', routes.collectionsWithDefaultOffers);
                },
            },

            pageObjects: {
                offerDefaultShopLink() {
                    return this.createPageObject(Link, {
                        root: '.offer-default__shop-link',
                    });
                },
            },
        })
    ),
});
