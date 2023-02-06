import {
    makeSuite,
    prepareSuite,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// scenarios
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';

// pageObjects

// suites
import TreshholdGroupStateSuite from './treshholdGroupStateSuite';

import {
    getDsbsCart,
    getExpressCart,
    getFulfilmentCart,
} from './helpers';


module.exports = makeSuite('Группировка посылок по трешхолдам', {
    environment: 'kadavr',
    params: {
        carts: 'Посылки',
    },
    story: {
        async beforeEach() {
            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                this.params.carts
            );
            await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});
            await this.browser.yaScenario(this, waitForCartActualization);
        },

        'Цена меньше трешхолда': {
            'с YaPlus': {
                'Один экспресс': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getExpressCart({yaPlus: true}),
                        ],
                        isTresholdVisible: false,
                        parcelsCount: 1,
                        tresholdParcelsCount: 0,
                        onlyExpress: true,
                    },
                }),
                'Экспресс и dsbs': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getDsbsCart({yaPlus: true, number: 2, cartIndex: 0}),
                            getExpressCart({yaPlus: true, number: 2, cartIndex: 1}),
                        ],
                        isTresholdVisible: true,
                        parcelsCount: 2,
                        tresholdParcelsCount: 2,
                    },
                }),
                'Экспресс, фулфилмент и dsbs': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getDsbsCart({yaPlus: true, number: 3, cartIndex: 0}),
                            getExpressCart({yaPlus: true, number: 3, cartIndex: 1}),
                            getFulfilmentCart({yaPlus: true, number: 3, cartIndex: 2}),
                        ],
                        isTresholdVisible: true,
                        parcelsCount: 3,
                        tresholdParcelsCount: 3,
                    },
                }),
            },
            'без YaPlus': {
                'Один экспресс': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getExpressCart({yaPlus: false}),
                        ],
                        isTresholdVisible: false,
                        parcelsCount: 1,
                        tresholdParcelsCount: 0,
                        onlyExpress: true,
                    },
                }),
                'Экспресс и dsbs': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getDsbsCart({yaPlus: false, number: 2, cartIndex: 0}),
                            getExpressCart({yaPlus: false, number: 2, cartIndex: 1}),
                        ],
                        isTresholdVisible: true,
                        parcelsCount: 2,
                        tresholdParcelsCount: 2,
                    },
                }),
                'Экспресс, фулфилмент и dsbs': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getDsbsCart({yaPlus: false, number: 3, cartIndex: 0}),
                            getExpressCart({yaPlus: false, number: 3, cartIndex: 1}),
                            getFulfilmentCart({yaPlus: false, number: 3, cartIndex: 2}),
                        ],
                        isTresholdVisible: false,
                        parcelsCount: 3,
                        tresholdParcelsCount: 3,
                    },
                }),
            },
        },
        'Цена больше трешхолда': {
            'с YaPlus': {
                'Один экспресс': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getExpressCart({more: true, yaPlus: true}),
                        ],
                        isTresholdVisible: false,
                        parcelsCount: 1,
                        tresholdParcelsCount: 0,
                        onlyExpress: true,
                    },
                }),
                'Экспресс и dsbs': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getDsbsCart({more: true, yaPlus: true, number: 2, cartIndex: 0}),
                            getExpressCart({more: true, yaPlus: true, number: 2, cartIndex: 1}),
                        ],
                        isTresholdVisible: true,
                        parcelsCount: 2,
                        tresholdParcelsCount: 2,
                    },
                }),
                'Экспресс, фулфилмент и dsbs': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getDsbsCart({more: true, yaPlus: true, number: 3, cartIndex: 0}),
                            getExpressCart({more: true, yaPlus: true, number: 3, cartIndex: 1}),
                            getFulfilmentCart({more: true, yaPlus: true, number: 3, cartIndex: 2}),
                        ],
                        isTresholdVisible: true,
                        parcelsCount: 3,
                        tresholdParcelsCount: 3,
                    },
                }),
            },
            'без YaPlus': {
                'Один экспресс': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getExpressCart({more: true, yaPlus: false}),
                        ],
                        isTresholdVisible: false,
                        parcelsCount: 1,
                        tresholdParcelsCount: 0,
                        onlyExpress: true,
                    },
                }),
                'Экспресс и dsbs': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getDsbsCart({more: true, yaPlus: false, number: 2, cartIndex: 0}),
                            getExpressCart({more: true, yaPlus: false, number: 2, cartIndex: 1}),
                        ],
                        isTresholdVisible: true,
                        parcelsCount: 2,
                        tresholdParcelsCount: 2,
                    },
                }),
                'Экспресс, фулфилмент и dsbs': prepareSuite(TreshholdGroupStateSuite, {
                    params: {
                        carts: [
                            getDsbsCart({more: true, yaPlus: false, number: 3, cartIndex: 0}),
                            getExpressCart({more: true, yaPlus: false, number: 3, cartIndex: 1}),
                            getFulfilmentCart({more: true, yaPlus: false, number: 3, cartIndex: 2}),
                        ],
                        isTresholdVisible: true,
                        parcelsCount: 3,
                        tresholdParcelsCount: 3,
                    },
                }),
            },
        },
    },
});
