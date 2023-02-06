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
    productDefaultUniqueOffer,
    productUniqueOffers,
} from '@self/platform/spec/hermione/fixtures/product';
import {getEstimatedDate} from '@self/platform/spec/hermione/test-suites/tops/pages/product/fixtures/getEstimatedDay';


export default makeSuite('Офер на заказ.', {
    story: mergeSuites(
        prepareSuite(DefaultOfferContentSuite, {
            meta: {
                id: 'marketfront-5837',
                issue: 'MARKETFRONT-81052',
                environment: 'kadavr',
            },
            params: {
                deliveryTexts: [`Курьером ${getEstimatedDate(30)} — 30 ₽`],
                showReturnPolicy: true,
                policyText: 'Под заказ, возврату не подлежит',
                policyHintText: 'Вернуть такой товар не получится, если он надлежащего качества. А отменить заказ можно в течение 10 дней после оформления.',
            },
            hooks: {
                async beforeEach() {
                    this.setPageObjects({
                        defaultOffer: () => this.createPageObject(DefaultOffer),
                    });
                    await this.browser.setState('report', productDefaultUniqueOffer);
                    await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                },
            },
        }),
        prepareSuite(ProductOffersSnippetContentSuite, {
            meta: {
                id: 'marketfront-5972',
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
                    await this.browser.setState('report', productUniqueOffers);
                    await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                },
            },
        })
    ),
});
