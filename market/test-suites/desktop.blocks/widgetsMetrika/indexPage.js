import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';
// eslint-disable-next-line no-restricted-imports
import _ from 'lodash';

import {skuMock, productMock, offerMock} from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import {getHidePopupQueryParam} from '@self/root/src/spec/utils/pageParams';
import {createState, createUserHistory} from '@self/root/src/spec/utils/kadavr';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {selectByZoneData} from '@self/root/src/spec/utils/metrika';
import {setReportState, setTarantinoState} from '@self/root/src/spec/hermione/scenarios/kadavr';

/* eslint-disable max-len */
import MockScrollBoxAttractiveModels from '@self/root/src/spec/hermione/kadavr-mock/tarantino/scrollbox/attractiveModels';
import MockScrollBoxCommonlyPurchasedProducts from '@self/root/src/spec/hermione/kadavr-mock/tarantino/scrollbox/commonlyPurchasedProducts';
import MockScrollBoxHistory from '@self/root/src/spec/hermione/kadavr-mock/tarantino/scrollbox/history';
import MockScrollBoxPopularProducts from '@self/root/src/spec/hermione/kadavr-mock/tarantino/scrollbox/popularProducts';
import MockScrollBoxDeals from '@self/root/src/spec/hermione/kadavr-mock/tarantino/scrollbox/deals';
// import MockProductsGridsGroupSkuByIds from '@self/root/src/spec/hermione/kadavr-mock/tarantino/productsGrids/groupSkuByIds.desktop';
import MockRollAttractiveModels from '@self/root/src/spec/hermione/kadavr-mock/tarantino/roll/attractiveModels';
/* eslint-enable max-len */

import schemas from './schemas';
import scrollboxMetrikaSuite from './scrollBox';
// import productsGridsMetrikaSuite from './productsGrids';
import rollMetrikaSuite from './roll';

const getTitleFromMock = tarantinoMock => _.get(tarantinoMock, 'content.rows[0].nodes[0].nodes[0].props.title', '');

module.exports = makeSuite('Метрика', {
    feature: 'Метрика',
    environment: 'kadavr',
    defaultParams: {
        goalNamePrefix: 'CMS-PAGE',
    },
    story: mergeSuites(
        prepareSuite(scrollboxMetrikaSuite, {
            suiteName: '"Стоит приглядеться".',
            meta: {
                issue: 'BLUEMARKET-5618',
                id: 'bluemarket-2601',
            },
            params: {
                selector: selectByZoneData(getTitleFromMock(MockScrollBoxAttractiveModels)),
                payloadWidgetSchema: {
                    ...schemas.AttractiveModels.root,
                    cmsPageId: Number,
                    cmsWidgetId: Number,
                },
                payloadSnippetSchema: schemas.AttractiveModels.snippet,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setReportState, {
                        state: createState({product: productMock, offer: offerMock}, 6),
                    });
                    await this.browser.yaScenario(this, setTarantinoState, {
                        state: {data: {result: [MockScrollBoxAttractiveModels]}},
                    });
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        }),

        prepareSuite(scrollboxMetrikaSuite, {
            suiteName: '"Часто бывает нужно".',
            meta: {
                issue: 'BLUEMARKET-5617',
                id: 'bluemarket-2600',
            },
            params: {
                selector: selectByZoneData(getTitleFromMock(MockScrollBoxCommonlyPurchasedProducts)),
                payloadWidgetSchema: {
                    ...schemas.CommonlyPurchasedProducts.root,
                    cmsPageId: Number,
                    cmsWidgetId: Number,
                },
                payloadSnippetSchema: schemas.CommonlyPurchasedProducts.snippet,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setReportState, {
                        state: createState({product: productMock, offer: offerMock}, 6),
                    });
                    await this.browser.yaScenario(this, setTarantinoState, {
                        state: {data: {result: [MockScrollBoxCommonlyPurchasedProducts]}},
                    });
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        }),

        prepareSuite(scrollboxMetrikaSuite, {
            suiteName: '"Мои недавние находки".',
            meta: {
                issue: 'BLUEMARKET-4412',
                id: 'bluemarket-2509',
            },
            params: {
                selector: selectByZoneData(getTitleFromMock(MockScrollBoxHistory)),
                payloadWidgetSchema: {
                    ...schemas.History.root,
                    cmsPageId: Number,
                    cmsWidgetId: Number,
                },
                payloadSnippetSchema: schemas.History.snippet,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setReportState, {
                        state: createState({sku: skuMock, product: productMock, offer: offerMock}, 6),
                    });
                    await this.browser.yaScenario(this, setTarantinoState, {
                        state: {data: {result: [MockScrollBoxHistory]}},
                    });
                    await this.browser.yaScenario(this, 'kadavr.setHistoryState', {
                        state: {data: createUserHistory(skuMock.id, 6)},
                    });
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        }),

        prepareSuite(scrollboxMetrikaSuite, {
            suiteName: '"Многим нравится".',
            meta: {
                issue: 'BLUEMARKET-4416',
                id: 'bluemarket-2513',
            },
            params: {
                selector: selectByZoneData(getTitleFromMock(MockScrollBoxPopularProducts)),
                payloadWidgetSchema: {
                    ...schemas.PopularProducts.root,
                    cmsPageId: Number,
                    cmsWidgetId: Number,
                },
                payloadSnippetSchema: schemas.PopularProducts.snippet,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setReportState, {
                        state: createState({product: productMock, offer: offerMock}, 6),
                    });
                    await this.browser.yaScenario(this, setTarantinoState, {
                        state: {data: {result: [MockScrollBoxPopularProducts]}},
                    });
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        }),

        prepareSuite(scrollboxMetrikaSuite, {
            suiteName: '"Все со скидкой".',
            meta: {
                issue: 'BLUEMARKET-4415',
                id: 'bluemarket-2512',
            },
            params: {
                selector: selectByZoneData(getTitleFromMock(MockScrollBoxDeals)),
                payloadWidgetSchema: {
                    ...schemas.Deals.root,
                    cmsPageId: Number,
                    cmsWidgetId: Number,
                },
                payloadSnippetSchema: schemas.Deals.snippet,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setReportState, {
                        state: createState({product: productMock, offer: offerMock}, 6),
                    });
                    await this.browser.yaScenario(this, setTarantinoState, {
                        state: {data: {result: [MockScrollBoxDeals]}},
                    });
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        }),

        /* prepareSuite(productsGridsMetrikaSuite, {
            suiteName: '"Только сегодня".',
            meta: {
                issue: 'BLUEMARKET-4414',
                id: 'bluemarket-2511',
            },
            params: {
                selector: '[data-zone-data*="GroupSkuByIds"]',
                payloadWidgetSchema: {
                    ...schemas.GroupSkuByIds.root,
                    cmsPageId: Number,
                    cmsWidgetId: Number,
                },
                payloadSnippetSchema: schemas.GroupSkuByIds.snippet,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setReportState, {
                        state: createState({sku: skuMock, product: productMock, offer: offerMock}, 11),
                    });
                    await this.browser.yaScenario(this, setTarantinoState, {
                        state: {data: {result: [MockProductsGridsGroupSkuByIds]}},
                    });
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        }), */

        prepareSuite(rollMetrikaSuite, {
            suiteName: '"Как для меня выбрано".',
            meta: {
                issue: 'BLUEMARKET-4410',
                id: 'bluemarket-2473',
            },
            params: {
                selector: selectByZoneData(getTitleFromMock(MockRollAttractiveModels)),
                payloadWidgetSchema: {
                    ...schemas.AttractiveModels.root,
                    name: 'Roll',
                    cmsPageId: Number,
                },
                payloadSnippetSchema: {
                    ...schemas.AttractiveModels.snippet,
                    name: 'Roll',
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setReportState, {
                        state: createState({product: productMock, offer: offerMock}, 6),
                    });
                    await this.browser.yaScenario(this, setTarantinoState, {
                        state: {data: {result: [MockRollAttractiveModels]}},
                    });
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        })
    ),
});
