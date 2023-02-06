/* eslint-disable global-require */

import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mountSearchWidgets, SEARCH_ROOT_WIDGET_PATH} from '@self/root/src/widgets/content/search/__spec__/mocks/mountSearchWidgets';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {NUTS_HID, NUTS_NID} from '@self/root/src/constants/categories';
import {
    mockBasicSearchFunctionality,
    mockNoopBackendFunctionality,
    mockSearchFunctionality,
    makeContext, mockCatalogListFunctionality,
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

describe('Search: Рекомендации на основе истории', () => {
    let container;

    const pageId = PAGE_IDS_COMMON.LIST;
    const initialParams = {hid: NUTS_HID, nid: NUTS_NID};

    beforeEach(async () => {
        await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
        await mockNoopBackendFunctionality({jestLayer, kadavrLayer});
        await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);

        await mockSearchFunctionality({kadavrLayer}, {product: true});
        await mockCatalogListFunctionality({kadavrLayer}, {nuts: true});

        container = (await apiaryLayer.mountWidget(
            SEARCH_ROOT_WIDGET_PATH,
            mountSearchWidgets([
                {widgetName: 'SearchProductsByHistoryEx', props: {}},
            ])
        )).container;
    });

    describe('По умолчанию', () => {
        test('должна встраиваться ленивая карусель', () => {
            expect(container.querySelector('div[data-apiary-widget-name="@marketfront/VisibilityLoaderMarket"]')).toBeVisible();
        });
    });
});
