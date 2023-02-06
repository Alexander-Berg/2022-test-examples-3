/* eslint-disable global-require */

import {screen, waitFor} from '@testing-library/dom';
import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mountSearchWidgets, SEARCH_ROOT_WIDGET_PATH} from '@self/root/src/widgets/content/search/__spec__/mocks/mountSearchWidgets';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {NUTS_HID, NUTS_NID} from '@self/root/src/constants/categories';
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

describe('Search: Дерево интентов', () => {
    const pageId = PAGE_IDS_COMMON.LIST;
    const initialParams = {hid: NUTS_HID, nid: NUTS_NID};

    let container;

    beforeEach(async () => {
        await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
        await mockNoopBackendFunctionality({jestLayer, kadavrLayer});

        await mockSearchFunctionality({kadavrLayer}, {product: true, filterBoolean: true, grocery: true});
        await mockCatalogListFunctionality({kadavrLayer}, {nuts: true});

        await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);

        await step('Монтируем виджеты выдачи', async () => {
            const mountedWidget = await apiaryLayer.mountWidget(
                SEARCH_ROOT_WIDGET_PATH,
                mountSearchWidgets([
                    {widgetName: 'SearchIntents', props: {shallow: true, maxDepth: 0}},
                    {widgetName: 'SearchFilters', props: {scrollToAnchor: 'serpTop'}},
                ])
            );

            container = mountedWidget.container;
        });
    });

    describe('По умолчанию', () => {
        test('отображается', () => {
            const intentElement = screen.getByTestId('intent-link');

            expect(intentElement.textContent).toEqual('Продукты');
            expect(parseTestingUrl(intentElement.href).searchParams).toEqual({
                cvredirect: 3,
                glfilter: [null],
                hid: 91307,
                nid: 54434,
                slug: 'produkty-pitaniia',
                track: 'srch_ddl',
            });
        });
    });

    describe('При применении фильтров', () => {
        beforeEach(() => {
            screen.getByText('Гарантия производителя').click();
        });

        test('ссылки на категории должны сохранять фильтры', async () => {
            await waitFor(() => {
                const categoryLinks = container.querySelectorAll('a[data-auto="intent-link"]');

                const targets = Array.from(categoryLinks).map(node => node.href);

                expect(targets.length > 0).toBeTruthy();
                expect(
                    targets.every(target => parseTestingUrl(target).searchParams.manufacturer_warranty === '1')
                ).toBeTruthy();
            });
        });
    });
});
