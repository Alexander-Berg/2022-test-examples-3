import {makeSuite, makeCase, mergeSuites, prepareSuite} from 'ginny';
import {routes} from '@self/platform/spec/hermione/configs/routes';

// page-objects
import SearchSnippetPrice from '@self/platform/spec/page-objects/containers/SearchSnippet/Price';
import ReferenceUnitPrice from '@self/platform/spec/page-objects/containers/SearchSnippet/ReferenceUnitPrice';

// constants
import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds';

// mock
import * as mocksWithTemplator from '@self/platform/spec/hermione/fixtures/product/productWithJumpTables';
import {getReportState} from './mocks';

const checkPriceSuite = makeSuite('единицы измерения в цене', {
    params: {
        expectedPrice: 'Ожидаемая цена',
    },
    story: {
        'присутсвуют': makeCase({
            async test() {
                const actualPrice = await this.price.getPrice();
                return this.expect(actualPrice)
                    .to.be.equal(this.params.expectedPrice, 'единицы измерения присутсвуют в цене');
            },
        }),
    },
});

const mainAndreferenceUnits = makeSuite('Единицы измерения.', {
    params: {
        view: 'list или grid',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                const snippetConfiguration = mocksWithTemplator.templatorTarantinoMock;
                await this.browser.setState('Tarantino.data.result', [snippetConfiguration]);
                await this.browser.setState('report', getReportState(this.params.view));
                const route = PAGE_IDS_TOUCH.YANDEX_MARKET_SEARCH;
                const routeParams = routes.search.default;
                return this.browser.yaOpenPage(route, routeParams).yaClosePopup(this.regionPopup);
            },
        },
        prepareSuite(checkPriceSuite, {
            suiteName: 'Основные единицы',
            params: {
                expectedPrice: '100₽ / уп',
            },
            pageObjects: {
                price() {
                    return this.createPageObject(SearchSnippetPrice);
                },
            },
        }),
        prepareSuite(checkPriceSuite, {
            suiteName: 'Второстепенные единицы',
            params: {
                expectedPrice: '1 775₽ / м²',
            },
            pageObjects: {
                price() {
                    return this.createPageObject(ReferenceUnitPrice);
                },
            },
        })
    ),
});

export default makeSuite('Единицы измерения.', {
    id: 'm-touch-3955',
    issue: 'MARKETFRONT-78107',
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(mainAndreferenceUnits, {
            suiteName: 'List view.',
            params: {
                view: 'list',
            },
        }),
        prepareSuite(mainAndreferenceUnits, {
            suiteName: 'Grid view.',
            params: {
                view: 'grid',
            },
        })),
});
