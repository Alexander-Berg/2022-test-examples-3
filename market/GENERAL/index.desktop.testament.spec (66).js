import {screen, waitFor} from '@testing-library/dom';
import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';

import {baseMockFunctionality} from './mockFunctionality';

const WIDGET_PATH = '../';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

async function makeContext(user = {}, businessId, slug, express) {
    return mandrelLayer.initContext({
        user,
        request: {
            params: {
                businessId,
                slug,
                express,
            },
        },
    });
}

beforeAll(async () => {
    mockLocation();
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

    await baseMockFunctionality(jestLayer);
});

afterAll(() => {
    mirror.destroy();
});

describe('Шапка ШиШа на десктопе.', () => {
    describe('У магазина ритейла Еды.', () => {
        beforeEach(async () => {
            await makeContext({}, 3088667, 'lenta', true);
        });
        test('Кнопка графика работы отображается.', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH);
            await waitFor(async () => {
                expect(await screen.findByTestId('schedule-button')).toBeVisible();
            });
        });
        test('Кнопка экспресс доставки отображается.', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH);
            await waitFor(async () => {
                expect(await screen.findByTestId('express-delivery-button')).toBeVisible();
            });
        });
        test('Кнопка адреса доставки отображается', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH);
            await waitFor(async () => {
                expect(await screen.findByTestId('address-button')).toBeVisible();
            });
        });
        test('Кнопка рейтинга скрыта', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH);
            await waitFor(async () => {
                expect(await screen.queryByTestId('rating-button')).toBeNull();
            });
        });
        test('Меню навигации скрыто', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH);
            await waitFor(async () => {
                const toggler = await screen.findByTestId('nav-toggler');
                expect(toggler.childNodes.length).toBe(0);
            });
        });
    });
});
