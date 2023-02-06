/* eslint-disable global-require */

import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mountSearchWidgets, SEARCH_ROOT_WIDGET_PATH} from '@self/root/src/widgets/content/search/__spec__/mocks/mountSearchWidgets';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {GROCERY_HID, GROCERY_NID} from '@self/root/src/constants/categories';
import {parseTestingUrl} from '@self/root/src/helpers/testament/url';
import {
    makeContext,
    mockBasicSearchFunctionality,
    mockNoopBackendFunctionality,
    mockSearchFunctionality,
    mockCatalogListFunctionality,
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

describe('Search: Уточнение категории', () => {
    const pageId = PAGE_IDS_COMMON.LIST;
    const initialParams = {hid: GROCERY_HID, nid: GROCERY_NID, businessId: '702923'};

    let container;

    beforeEach(async () => {
        await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
        await mockNoopBackendFunctionality({jestLayer, kadavrLayer});

        await mockSearchFunctionality({kadavrLayer}, {product: true, grocery: true});
        await mockCatalogListFunctionality({kadavrLayer}, {grocery: true});

        await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);

        await step('Монтируем виджеты выдачи', async () => {
            const mountedWidget = await apiaryLayer.mountWidget(
                SEARCH_ROOT_WIDGET_PATH,
                mountSearchWidgets([{widgetName: 'SearchClarify', props: {hideTitleWhenOneCategory: true}}])
            );

            container = mountedWidget.container;
        });
    });

    describe('При наличии уточняющих категорий', () => {
        test('визуальный блок должен быть виден', async () => {
            const categoryLinks = Array.from(container.querySelectorAll('div[data-auto="ClarifyCategory"] div[data-auto="item"]'));

            expect(categoryLinks.length > 0).toBeTruthy();
        });
    });

    describe('При использовании магазинных фильтров', () => {
        test('визуальный блок должен обновляться', async () => {
            const categoryLinks = Array.from(container.querySelectorAll('div[data-auto="ClarifyCategory"] div[data-auto="item"]'));

            expect(categoryLinks.length > 0).toBeTruthy();
        });

        test('при клике на карточку категории проставленные фильтры пробрасываются', async () => {
            const anyCategoryLink = container
                .querySelector('div[data-auto="ClarifyCategory"] div[data-auto="item"] a');

            const clarifyLinkTarget = anyCategoryLink.href;
            const clarifyLinkParams = parseTestingUrl(clarifyLinkTarget).searchParams;
            expect(String(clarifyLinkParams.businessId)).toEqual(initialParams.businessId);
        });
    });
});
