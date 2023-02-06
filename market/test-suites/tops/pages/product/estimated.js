import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

// page-objects
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import ProductOffersSnippetList from '@self/platform/containers/ProductOffersSnippetList/__pageObject';
import ProductOffersSnippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';

// suites
import DefaultOfferContentSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer/content';
import ProductOffersSnippetContentSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippet/content';

// fixtures
import {
    phoneProductRoute,
    productDefaultEstimatedOffer,
    productEstimatedOffers,
} from '@self/platform/spec/hermione/fixtures/product';
import {getEstimatedDate} from '@self/platform/spec/hermione/test-suites/tops/pages/product/fixtures/getEstimatedDay';


export default makeSuite('Офер с неточной датой.', {
    story: mergeSuites(
        prepareSuite(DefaultOfferContentSuite, {
            meta: {
                id: 'marketfront-5952',
                issue: 'MARKETFRONT-81052',
                environment: 'kadavr',
            },
            params: {
                deliveryTexts: [`Курьером ${getEstimatedDate(30)} — 30 ₽`],
                showReturnPolicy: false,
            },
            hooks: {
                async beforeEach() {
                    this.setPageObjects({
                        defaultOffer: () => this.createPageObject(DefaultOffer),
                    });
                    await this.browser.setState('report', productDefaultEstimatedOffer);
                    await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                },
            },
        }),
        prepareSuite(ProductOffersSnippetContentSuite, {
            meta: {
                id: 'marketfront-5973',
                issue: 'MARKETFRONT-81052',
                environment: 'kadavr',
            },
            params: {
                deliveryTexts: [`Курьером ${getEstimatedDate(60)} — 30 ₽`],
            },
            hooks: {
                async beforeEach() {
                    this.setPageObjects({
                        productOffersSnippet: () => this.createPageObject(ProductOffersSnippet, {
                            root: `${ProductOffersSnippetList.item(0)} ${ProductOffersSnippet.root}`,
                        }),
                    });
                    await this.browser.setState('report', productEstimatedOffers);
                    await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                },
            },
        })
    ),
});
