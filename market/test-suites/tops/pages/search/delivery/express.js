import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import ExpressBadgeSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOffer/expressBadgeSuite';
import EmptyDeliverySuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOffer/emptyDeliverySuite';
import SearchSnippetDelivery from '@self/platform/spec/page-objects/containers/SearchSnippet/Delivery';
import {cpaType3POfferMock} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import createProductWithCPADO from '@self/platform/spec/hermione/test-suites/tops/pages/product/fixtures/productWithCPADO';
import {region} from '@self/root/src/spec/hermione/configs/geo';

const createSuite = ({queryParams, meta, offerMock}) =>
    mergeSuites(
        {
            async beforeEach() {
                offerMock.delivery.isExpress = true;
                const state = createProductWithCPADO(offerMock);
                await this.browser.setState('report', state);
                return this.browser.yaOpenPage('touch:search', queryParams);
            },
        },
        prepareSuite(ExpressBadgeSuite, {
            meta,
            pageObjects: {
                expressBadge() {
                    return this.createPageObject(SearchSnippetDelivery, {root: SearchSnippetDelivery.expressBadge});
                },
            },
        }),
        prepareSuite(EmptyDeliverySuite, {
            meta,
            pageObjects: {
                deliveryInfo() {
                    return this.createPageObject(SearchSnippetDelivery, {root: SearchSnippetDelivery.deliveryInfo});
                },
            },
        })
    );

export default makeSuite('Экспресс бейдж.', {
    story: {
        'КМ с FBY-оффером.': {
            'Москва.': createStories([
                {
                    description: 'Срок доставки: экспресс',
                    queryParams: {...routes.search.default, lr: region['Москва']},
                    meta: {
                        id: 'm-touch-3667',
                        issue: 'MARKETFRONT-50563',
                    },
                    deliveryOptions: {
                        dayFrom: 0,
                        dayTo: 0,
                    },
                    offerMock: cpaType3POfferMock,
                },

            ], createSuite),
        },
    },
});
