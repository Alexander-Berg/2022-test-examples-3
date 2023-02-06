import {screen} from '@testing-library/dom';

import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {baseMockFunctionality, mockFullCartPageStateWithLargeWeight} from '@self/root/src/widgets/content/cart/__spec__/mocks/mockFunctionality';
import {visibleStrategyId} from '@self/root/src/widgets/content/cart/__spec__/mocks/mockData';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

beforeAll(async () => {
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

describe('Тяжёлая корзина', () => {
    beforeAll(async () => {
        await jestLayer.backend.runCode(mockFullCartPageStateWithLargeWeight, []);
    });
    it('отображается плашка для тяжёлых корзины [bluemarket-2759]', async () => {
        await mandrelLayer.initContext();
        await apiaryLayer.mountWidget('..', {
            props: {
                size: 's',
                visibleStrategyId,
                isParcelsDeliveryThreshold: true,
            },
        });

        expect(screen.getByTestId('remainder-description')).toBeVisible();
    });
});
