import {mockIntersectionObserver, mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';

import {
    baseMockFunctionality,
    mockFullCartPageStateWithFashion,
    mockFullCartPageStateWithExpiredOffer,
    mockFullCartPageStateWithSoldOutOffer,
    mockFullCartPageStateWithOneLimitedOffer,
} from '@self/root/src/widgets/content/cart/__spec__/mocks/mockFunctionality';
import {
    fashionInfoTestCase,
    limitWarningVisibleTestCase,
    soldOutBadgeVisibleTestCase,
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

    await jestLayer.doMock(
        require.resolve('@self/root/src/widgets/content/cart/CartDeliveryTermsNotifier'),
        () => ({create: () => Promise.resolve(null)})
    );
});

afterAll(() => {
    mirror.destroy();
});

describe('CartList', () => {
    describe('Актуализация', () => {
        describe('Обнуление стока', () => {
            describe('Несколько офферов в корзине', () => {
                describe('Оффер кончился по стокам', () => {
                    beforeAll(async () => {
                        await jestLayer.backend.runCode(mockFullCartPageStateWithExpiredOffer, []);
                    });

                    test('Бейдж "Разобрали" отображается', () => soldOutBadgeVisibleTestCase(jestLayer, apiaryLayer, mandrelLayer));
                });

                describe('Оффера уже нет в репорте', () => {
                    beforeAll(async () => {
                        await jestLayer.backend.runCode(mockFullCartPageStateWithSoldOutOffer, []);
                    });

                    test('Бейдж "Разобрали" отображается', () => soldOutBadgeVisibleTestCase(jestLayer, apiaryLayer, mandrelLayer));
                });
            });
        });

        describe('Недостаточное количества товара', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(mockFullCartPageStateWithOneLimitedOffer, []);
            });

            test('Надпись "Осталось 2 штуки" отображается на оффере', () => limitWarningVisibleTestCase(jestLayer, apiaryLayer, mandrelLayer));
        });
    });

    describe('Признак примерки у FBS товара', () => {
        describe('с примеркой [marketfront-6111]', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(mockFullCartPageStateWithFashion, []);
            });

            test('Надпись "Примерка" отображается на оффере', () => fashionInfoTestCase(jestLayer, apiaryLayer, mandrelLayer));
        });
    });
});
