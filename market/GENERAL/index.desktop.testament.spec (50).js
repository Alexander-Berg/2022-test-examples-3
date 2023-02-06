/* eslint-disable global-require */

import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {screen} from '@testing-library/dom';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {NUTS_HID, NUTS_NID} from '@self/root/src/constants/categories';
import {
    mockBasicSearchFunctionality,
    mockNoopBackendFunctionality,
    mockSearchFunctionality,
    makeContext,
    mockCatalogListFunctionality,
    mountSearchWidgets,
    SEARCH_ROOT_WIDGET_PATH,
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

describe('Search: Точки входа на категорийные вопросы', () => {
    const pageId = PAGE_IDS_COMMON.LIST;
    const initialParams = {hid: NUTS_HID, nid: NUTS_NID};

    beforeEach(async () => {
        await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
        await mockNoopBackendFunctionality({jestLayer, kadavrLayer});

        await mockSearchFunctionality({kadavrLayer}, {product: true});
        await mockCatalogListFunctionality({kadavrLayer}, {nuts: true});

        await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);

        await apiaryLayer.mountWidget(
            SEARCH_ROOT_WIDGET_PATH,
            mountSearchWidgets([
                {widgetName: 'SearchCategoryQuestionsEntrypoint', props: {}},
            ])
        );
    });

    describe('По умолчанию', () => {
        test('присутствует на странице', () => {
            expect(screen.getByText(/вопросы о товарах/i)).toBeVisible();
        });

        test('указано количество категорий', async () => {
            expect(screen.getByTestId('count').textContent).toEqual('12345');
        });
    });
});
