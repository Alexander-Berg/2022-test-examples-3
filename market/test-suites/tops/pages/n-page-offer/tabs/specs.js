import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

// suites
import HeaderSuite from '@self/platform/spec/hermione/test-suites/blocks/n-offer-card-specs/__header';
import OfferCardSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-default-offer/offer-card';
// page-objects
import DefaultOfferMini from '@self/platform/components/DefaultOfferMini/__pageObject';
import ClickoutButton from '@self/platform/spec/page-objects/components/ClickoutButton';
import OfferCardSpecs from '@self/platform/spec/page-objects/OfferCardSpecs';

import {offer, offerId} from '../fixtures/offerWithoutModel';

export default makeSuite('Контент вкладки "Характеристики"', {
    id: 'marketfront-3483',
    issue: 'MARKETVERSTKA-34565',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', offer);
                await this.browser.yaOpenPage('market:offer-spec', {offerId});
            },
        },
        prepareSuite(HeaderSuite, {
            pageObjects: {
                offerCardSpecs() {
                    return this.createPageObject(OfferCardSpecs);
                },
            },
        }),
        prepareSuite(OfferCardSuite, {
            pageObjects: {
                productDefaultOfferActionButton() {
                    return this.createPageObject(ClickoutButton, {
                        parent: DefaultOfferMini.root,
                    });
                },
                productDefaultOffer() {
                    return this.createPageObject(DefaultOfferMini);
                },
            },
        })
        /** MARKETFRONT-58885: Скип автотестов в релизе 2021.375.0
         prepareSuite(WishlistControlSuite, {
            pageObjects: {
                wishlistControl() {
                    return this.createPageObject(WishlistTumbler, {
                        parent: MiniCard.root,
                    });
                },
            },
        })
        */
    ),
});
