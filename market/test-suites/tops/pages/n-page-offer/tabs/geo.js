import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

// suites
import TitleSuite from '@self/platform/spec/hermione/test-suites/blocks/n-geo-snippet/__title';
import TypeOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/n-geo-snippet/_type_offer';
import OfferCardSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-default-offer/offer-card';
// page-objects
import DefaultOfferMini from '@self/platform/components/DefaultOfferMini/__pageObject';
import ClickoutButton from '@self/platform/spec/page-objects/components/ClickoutButton';
import GeoSnippet from '@self/platform/spec/page-objects/n-geo-snippet';
import {offer, offerId} from '../fixtures/offerWithoutModel';

export default makeSuite('Контент вкладки "Карта"', {
    id: 'marketfront-3485',
    issue: 'MARKETVERSTKA-34567',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                // eslint-disable-next-line market/ginny/no-skip
                return this.skip('MARKETFRONT-9428: Починка гео-тестов на КО');
                // asvasilenko@: я ребейзил это и тут был конфликт
                // возможно этот скип не актуален

                /* eslint-disable no-unreachable */
                await this.browser.setState('report', offer);
                await this.browser.yaOpenPage('market:offer-geo', {offerId});
                /* eslint-enable no-unreachable */
            },
        },
        prepareSuite(TitleSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.waitForVisible(GeoSnippet.simple);
                },
            },
            pageObjects: {
                geoSnippet() {
                    return this.createPageObject(GeoSnippet);
                },
            },
        }),
        prepareSuite(TypeOfferSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.waitForVisible(GeoSnippet.simple);
                },
            },
            pageObjects: {
                geoSnippet() {
                    return this.createPageObject(GeoSnippet);
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

        /** MARKETFRONT-9428: Починка гео-тестов на КО
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
