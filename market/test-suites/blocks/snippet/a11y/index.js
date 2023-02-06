import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

// helpers
import {
    mergeState,
    createProduct,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';
// pageObjects
import ScrollBox from '@self/root/src/components/ScrollBox/__pageObject';
// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import indexPageMock from './fixtures/tarantino';
// suites
import raitingSuite from './rating';
import reviewsSuite from './reviews';

const OPINIONS = 25;
const PRODUCT_WITH_OPINIONS = {
    ...kettle.productMock,
    opinions: OPINIONS,
};

async function preparePage() {
    await this.browser.setState(
        'Tarantino.data.result',
        [indexPageMock]
    );

    const reportState = mergeState([
        createProduct(PRODUCT_WITH_OPINIONS, PRODUCT_WITH_OPINIONS.id),
        createOfferForProduct(kettle.offerMock, PRODUCT_WITH_OPINIONS.id, kettle.offerMock.wareId),

        {
            data: {
                search: {
                    results: [{
                        schema: 'product',
                        id: PRODUCT_WITH_OPINIONS.id,
                    }],
                    totalOffers: 1,
                    total: 1,
                },
            },
        },
    ]);
    await this.browser.setState('report', reportState);

    await this.browser.yaOpenPage('market:index');
}

export default makeSuite('Доступность элементов сниппета.', {
    issue: 'MARKETFRONT-73034',
    feature: 'Доступность. Сниппет с оффером/моделью',
    environment: 'kadavr',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                scrollBox: () => this.createPageObject(ScrollBox),
            });
        },
        'Карусель сниппетов': mergeSuites(
            {
                async beforeEach() {
                    await preparePage.call(this, false);
                },
            },
            prepareSuite(raitingSuite, {
                meta: {
                    id: 'marketfront-5629',
                },
                params: {
                    starsCount: PRODUCT_WITH_OPINIONS.rating,
                },
            }),
            prepareSuite(reviewsSuite, {
                meta: {
                    id: 'marketfront-5630',
                },
                params: {
                    reviewsCount: PRODUCT_WITH_OPINIONS.opinions,
                },
            })
        ),
    },
});
