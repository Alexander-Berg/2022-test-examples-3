import {mockIntersectionObserver, mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import {
    baseMockFunctionality,
    mockFullCartPageStateWithOneExpiredOffer,
    mockFullCartPageStateWithOneSoldOutOffer,
    mockFullCartPageStateWithOneLimitedOffer,
    mockFullCartPageStateWithExpiredOffer,
    mockFullCartPageStateWithSoldOutOffer,
} from './mockFunctionality';
import {
    summaryDisplayedCorrectTestCase,
    summaryIsNotVisibleTestCase,
} from './testCases';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

beforeAll(async () => {
    mockLocation();
    mockIntersectionObserver();
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    await jestLayer.backend.runCode(baseMockFunctionality, []);
});

afterAll(() => {
    mirror.destroy();
});

describe('CartTotalPrice', () => {
    describe('Актуализация', () => {
        describe('Обнуление стока', () => {
            describe('Один оффер в корзине', () => {
                describe('Оффер кончился по стокам', () => {
                    beforeAll(async () => {
                        await jestLayer.backend.runCode(mockFullCartPageStateWithOneExpiredOffer, []);
                    });

                    test('Саммари корзины не отображается', () => summaryIsNotVisibleTestCase(jestLayer, apiaryLayer, mandrelLayer));
                });

                describe('Оффера уже нет в репорте', () => {
                    beforeAll(async () => {
                        await jestLayer.backend.runCode(mockFullCartPageStateWithOneSoldOutOffer, []);
                    });

                    test('Саммари корзины не отображается', () => summaryIsNotVisibleTestCase(jestLayer, apiaryLayer, mandrelLayer));
                });
            });

            describe('Несколько офферов в корзине', () => {
                describe('Оффер кончился по стокам', () => {
                    beforeAll(async () => {
                        await jestLayer.backend.runCode(mockFullCartPageStateWithExpiredOffer, []);
                    });

                    test('Сумма и количество товаров в саммари отображается корректно', () => summaryDisplayedCorrectTestCase(jestLayer, apiaryLayer, mandrelLayer));
                });

                describe('Оффера уже нет в репорте', () => {
                    beforeAll(async () => {
                        await jestLayer.backend.runCode(mockFullCartPageStateWithSoldOutOffer, []);
                    });

                    test('Сумма и количество товаров в саммари отображается корректно', () => summaryDisplayedCorrectTestCase(jestLayer, apiaryLayer, mandrelLayer));
                });
            });
        });

        describe('Недостаточное количества товара', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(mockFullCartPageStateWithOneLimitedOffer, []);
            });

            test('Сумма и количество товаров в саммари отображается с учетом доступного количества', () => summaryDisplayedCorrectTestCase(jestLayer, apiaryLayer, mandrelLayer));
        });
    });
});
