import {mockIntersectionObserver, mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';

import {
    baseMockFunctionality,
    mockFullCartPageStateWithOneExpiredOffer,
    mockFullCartPageStateWithOneSoldOutOffer,
    mockFullCartPageStateWithExpiredOffer,
    mockFullCartPageStateWithOneLimitedOffer,
    mockFullCartPageStateWithSoldOutOffer,
} from './mockFunctionality';
import {
    includeCorrectCartTitle,
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
    mirror = await makeMirrorDesktop({
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

describe('CartHeader', () => {
    describe('Актуализация', () => {
        describe('Обнуление стока', () => {
            describe('Один оффер в корзине', () => {
                describe('Оффер кончился по стокам', () => {
                    beforeAll(async () => {
                        await jestLayer.backend.runCode(mockFullCartPageStateWithOneExpiredOffer, []);
                    });

                    test('Заголовок корзины содержит текст "Корзина"', () => includeCorrectCartTitle(jestLayer, apiaryLayer, mandrelLayer));
                });

                describe('Оффера уже нет в репорте', () => {
                    beforeAll(async () => {
                        await jestLayer.backend.runCode(mockFullCartPageStateWithOneSoldOutOffer, []);
                    });

                    test('Заголовок корзины содержит текст "Корзина"', () => includeCorrectCartTitle(jestLayer, apiaryLayer, mandrelLayer));
                });
            });

            describe('Несколько офферов в корзине', () => {
                describe('Оффер кончился по стокам', () => {
                    beforeAll(async () => {
                        await jestLayer.backend.runCode(mockFullCartPageStateWithExpiredOffer, []);
                    });

                    test('Заголовок корзины содержит текст "Корзина"', () => includeCorrectCartTitle(jestLayer, apiaryLayer, mandrelLayer));
                });

                describe('Оффера уже нет в репорте', () => {
                    beforeAll(async () => {
                        await jestLayer.backend.runCode(mockFullCartPageStateWithSoldOutOffer, []);
                    });

                    test('Заголовок корзины содержит текст "Корзина"', () => includeCorrectCartTitle(jestLayer, apiaryLayer, mandrelLayer));
                });
            });
        });

        describe('Недостаточное количества товара', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(mockFullCartPageStateWithOneLimitedOffer, []);
            });

            test('Заголовок корзины содержит текст "Корзина"', () => includeCorrectCartTitle(jestLayer, apiaryLayer, mandrelLayer));
        });
    });
});
