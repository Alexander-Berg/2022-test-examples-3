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

/* eslint-disable max-len */
import MockScrollBoxAttractiveModels from '@self/root/src/spec/hermione/kadavr-mock/tarantino/scrollbox/attractiveModels';
import MockScrollBoxCommonlyPurchasedProducts from '@self/root/src/spec/hermione/kadavr-mock/tarantino/scrollbox/commonlyPurchasedProducts';
import MockScrollBoxHistory from '@self/root/src/spec/hermione/kadavr-mock/tarantino/scrollbox/history';
import MockScrollBoxPopularProducts from '@self/root/src/spec/hermione/kadavr-mock/tarantino/scrollbox/popularProducts';
import MockProductsGridsDeals from '@self/root/src/spec/hermione/kadavr-mock/tarantino/productsGrids/deals';
import MockProductsGridsGroupSkuByIds from '@self/root/src/spec/hermione/kadavr-mock/tarantino/productsGrids/groupSkuByIds.touch';
import MockRollAttractiveModels from '@self/root/src/spec/hermione/kadavr-mock/tarantino/roll/attractiveModels';
/* eslint-enable max-len */
import {setReportState, setTarantinoState} from '@self/root/src/spec/hermione/scenarios/kadavr';

import schemas from './schemas';
import scrollboxMetrikaSuite from './scrollBox';
import productsGridsMetrikaSuite from './productsGrids';
import rollMetrikaSuite from './roll';

/**
 * Добавляем параметры тача в десктопную схему
 * Используем если настройки виджета для десктопа и тача не отличаются
 * Иначе используем отдельный мок тарантино
 */
const withPlatform = tarantinoMock => ({
    ...tarantinoMock,
    type: 'mp_morda_touch',
    name: 'Главная Синий Тач',
});

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
                issue: 'BLUEMARKET-5615',
                id: 'bluemarket-2894',
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
                        state: {data: {result: [withPlatform(MockScrollBoxAttractiveModels)]}},
                    });
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        }),

        prepareSuite(scrollboxMetrikaSuite, {
            suiteName: '"Часто бывает нужно".',
            meta: {
                issue: 'BLUEMARKET-5616',
                id: 'bluemarket-2895',
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
                        state: {data: {result: [withPlatform(MockScrollBoxCommonlyPurchasedProducts)]}},
                    });
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        }),

        prepareSuite(scrollboxMetrikaSuite, {
            suiteName: '"Мои недавние находки".',
            meta: {
                issue: 'BLUEMARKET-4498',
                id: 'bluemarket-2893',
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
                        state: {data: {result: [withPlatform(MockScrollBoxHistory)]}},
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
                issue: 'BLUEMARKET-4502',
                id: 'bluemarket-2889',
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
                        state: {data: {result: [withPlatform(MockScrollBoxPopularProducts)]}},
                    });
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        }),

        prepareSuite(productsGridsMetrikaSuite, {
            suiteName: '"Все со скидкой".',
            meta: {
                issue: 'BLUEMARKET-4501',
                id: 'bluemarket-2897',
            },
            params: {
                selector: selectByZoneData(getTitleFromMock(MockProductsGridsDeals)),
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
                        state: {data: {result: [withPlatform(MockProductsGridsDeals)]}},
                    });
                    // для получения одной цели в метрике ProductGrids добавляем фильтр по продукту
                    this.params.productId = productMock.id.toString();
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        }),

        prepareSuite(productsGridsMetrikaSuite, {
            suiteName: '"Только сегодня".',
            meta: {
                issue: 'BLUEMARKET-4500',
                id: 'bluemarket-2896',
            },
            params: {
                selector: selectByZoneData(getTitleFromMock(MockProductsGridsGroupSkuByIds)),
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
                        state: {data: {result: [withPlatform(MockProductsGridsGroupSkuByIds)]}},
                    });
                    // для получения одной цели в метрике ProductGrids добавляем фильтр по продукту
                    this.params.productId = productMock.id.toString();
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        }),

        prepareSuite(rollMetrikaSuite, {
            suiteName: '"Как для меня выбрано".',
            meta: {
                issue: 'BLUEMARKET-5620',
                id: 'bluemarket-2898',
            },
            params: {
                selector: selectByZoneData(getTitleFromMock(MockRollAttractiveModels)),
                payloadWidgetSchema: {
                    ...schemas.AttractiveModels.root,
                    cmsPageId: Number,
                },
                payloadSnippetSchema: schemas.AttractiveModels.snippet,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, setReportState, {
                        state: createState({product: productMock, offer: offerMock}, 10),
                    });
                    await this.browser.yaScenario(this, setTarantinoState, {
                        state: {data: {result: [withPlatform(MockRollAttractiveModels)]}},
                    });
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, getHidePopupQueryParam());
                },
            },
        })
    ),
});
