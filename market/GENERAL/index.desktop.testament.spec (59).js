/* eslint-disable global-require */

import {screen} from '@testing-library/dom';
import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mountSearchWidgets, SEARCH_ROOT_WIDGET_PATH} from '@self/root/src/widgets/content/search/__spec__/mocks/mountSearchWidgets';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {GROCERY_HID, GROCERY_NID} from '@self/root/src/constants/categories';
import {
    makeContext,
    mockBasicSearchFunctionality,
    mockCatalogListFunctionality,
    mockNoopBackendFunctionality,
    mockSearchFunctionality,
} from '../../__spec__/mocks';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

beforeAll(async () => {
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    jestLayer = mirror.getLayer('jest');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
});

describe('Search: Заголовок', () => {
    const pageId = PAGE_IDS_COMMON.LIST;
    const initialParams = {hid: GROCERY_HID, nid: GROCERY_NID};

    let container;

    beforeEach(async () => {
        await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
        await mockNoopBackendFunctionality({jestLayer, kadavrLayer});
        await mockSearchFunctionality({kadavrLayer}, {product: true, grocery: true});
        await mockCatalogListFunctionality({kadavrLayer}, {grocery: true});
    });

    describe('Для Москвы', () => {
        beforeEach(async () => {
            await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);

            await step('Монтируем виджеты выдачи', async () => {
                const mountedWidget = await apiaryLayer.mountWidget(
                    SEARCH_ROOT_WIDGET_PATH,
                    mountSearchWidgets([
                        {widgetName: 'SearchTitle', props: {}},
                    ])
                );

                container = mountedWidget.container;
            });
        });

        test('заголовок не содержит фразы "в Москве"', async () => {
            const titleNode = container.querySelector('div[data-grabber="SearchTitle"] [data-auto="title"]');
            screen.logTestingPlaygroundURL();
            step('Заголовок соответствует заданному', () => {
                expect(titleNode.textContent).toEqual('Вода');
            });
        });
    });

    describe('Не для Москвы', () => {
        beforeEach(async () => {
            await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams, {
                region: {
                    geo: {coordinates: {longitude: 0, latitude: 0}, zoom: 0},
                    id: 47,
                    entity: 'region',
                    name: 'Нижний Новгород',
                    country: undefined,
                    parentId: undefined,
                    linguistics: {
                        preposition: 'в',
                        prepositional: 'Нижнем Новгороде',
                        accusative: 'Нижний Новгород',
                    },
                    isCapital: false,
                },
            });

            await step('Монтируем виджеты выдачи', async () => {
                const mountedWidget = await apiaryLayer.mountWidget(
                    SEARCH_ROOT_WIDGET_PATH,
                    mountSearchWidgets([
                        {widgetName: 'SearchTitle', props: {}},
                    ])
                );

                container = mountedWidget.container;
            });
        });

        test('заголовок содержит фразу "в Нижнем Новгороде"', async () => {
            const titleNode = container.querySelector('div[data-grabber="SearchTitle"] [data-auto="title"]');

            step('Заголовок соответствует заданному', () => {
                expect(titleNode.textContent).toEqual('Вода в Нижнем Новгороде');
            });
        });
    });
});
