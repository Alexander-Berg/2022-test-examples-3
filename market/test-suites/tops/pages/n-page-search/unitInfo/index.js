import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
// suites
import referenceUnitSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitInfo/referenceUnit';
import mainUnitSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitInfo/mainUnit';
// page-objects
import SearchSnippetCard from '@self/project/src/components/Search/Snippet/Card/__pageObject';
import SearchSnippetCell from '@self/project/src/components/Search/Snippet/Cell/__pageObject';
// mocks
import {reportOfferState} from './fixtures/offer';
import {reportProductState} from './fixtures/product';
import {GRID_VIEW_DATA_STATE, LIST_VIEW_DATA_STATE} from '../../n-page-catalog-list/constants';

/**
 * Тесты на единицы измерения продажи товара в выдаче
 * @param {PageObject.SnippetCard2} SearchSnippetCard
 * @param {PageObject.SnippetCell2} SearchSnippetCell
 */
export default makeSuite('Единицы измерения продажи товара в сниппетах.', {
    environment: 'kadavr',
    story: {
        'Листовой список офферов.': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState('report', mergeState([reportOfferState, LIST_VIEW_DATA_STATE]));

                    return this.browser.yaOpenPage('market:search', routes.search.list);
                },
                afterEach() {
                    return this.browser.deleteCookie('viewtype');
                },
            },
            prepareSuite(referenceUnitSuite, {
                meta: {
                    id: 'marketfront-5363',
                    issue: 'MARKETFRONT-73649',
                },
                pageObjects: {
                    parent() {
                        return this.createPageObject(SearchSnippetCard);
                    },
                },
            }),
            prepareSuite(mainUnitSuite, {
                meta: {
                    id: 'marketfront-5363',
                    issue: 'MARKETFRONT-73649',
                },
                pageObjects: {
                    parent() {
                        return this.createPageObject(SearchSnippetCard);
                    },
                },
            })
        ),
        'Гридовый список офферов.': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState('report', mergeState([reportOfferState, GRID_VIEW_DATA_STATE]));

                    return this.browser.yaOpenPage('market:search', routes.search.grid);
                },
                afterEach() {
                    return this.browser.deleteCookie('viewtype');
                },
            },
            prepareSuite(referenceUnitSuite, {
                meta: {
                    id: 'marketfront-5363',
                    issue: 'MARKETFRONT-73649',
                },
                pageObjects: {
                    parent() {
                        return this.createPageObject(SearchSnippetCell);
                    },
                },
            }),
            prepareSuite(mainUnitSuite, {
                meta: {
                    id: 'marketfront-5363',
                    issue: 'MARKETFRONT-73649',
                },
                pageObjects: {
                    parent() {
                        return this.createPageObject(SearchSnippetCell);
                    },
                },
            })
        ),
        'Листовой список продуктов.': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState('report', mergeState([reportProductState, LIST_VIEW_DATA_STATE]));

                    return this.browser.yaOpenPage('market:search', routes.search.list);
                },
                afterEach() {
                    return this.browser.deleteCookie('viewtype');
                },
            },
            prepareSuite(referenceUnitSuite, {
                meta: {
                    id: 'marketfront-5363',
                    issue: 'MARKETFRONT-73649',
                },
                pageObjects: {
                    parent() {
                        return this.createPageObject(SearchSnippetCard);
                    },
                },
            }),
            prepareSuite(mainUnitSuite, {
                meta: {
                    id: 'marketfront-5363',
                    issue: 'MARKETFRONT-73649',
                },
                pageObjects: {
                    parent() {
                        return this.createPageObject(SearchSnippetCard);
                    },
                },
            })
        ),
        'Гридовый список продуктов.': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState('report', mergeState([reportProductState, GRID_VIEW_DATA_STATE]));

                    return this.browser.yaOpenPage('market:search', routes.search.grid);
                },
                afterEach() {
                    return this.browser.deleteCookie('viewtype');
                },
            },
            prepareSuite(referenceUnitSuite, {
                meta: {
                    id: 'marketfront-5363',
                    issue: 'MARKETFRONT-73649',
                },
                pageObjects: {
                    parent() {
                        return this.createPageObject(SearchSnippetCell);
                    },
                },
            }),
            prepareSuite(mainUnitSuite, {
                meta: {
                    id: 'marketfront-5363',
                    issue: 'MARKETFRONT-73649',
                },
                pageObjects: {
                    parent() {
                        return this.createPageObject(SearchSnippetCell);
                    },
                },
            })
        ),
    },
});
