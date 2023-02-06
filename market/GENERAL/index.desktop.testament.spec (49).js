/* eslint-disable global-require */

import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mountSearchWidgets, SEARCH_ROOT_WIDGET_PATH} from '@self/root/src/widgets/content/search/__spec__/mocks/mountSearchWidgets';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {NUTS_HID, NUTS_NID} from '@self/root/src/constants/categories';
import {
    mockBasicSearchFunctionality,
    mockNoopBackendFunctionality,
    mockSearchFunctionality,
    makeContext,
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

describe('Search: Хлебные крошки', () => {
    let container;

    describe('По умолчанию', () => {
        const pageId = PAGE_IDS_COMMON.LIST;
        const initialParams = {hid: NUTS_HID, nid: NUTS_NID};

        beforeEach(async () => {
            await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
            await mockNoopBackendFunctionality({jestLayer, kadavrLayer});
            await mockCatalogListFunctionality({kadavrLayer}, {nuts: true});
            await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);

            await mockSearchFunctionality({kadavrLayer}, {product: true});

            const mountedWidget = await apiaryLayer.mountWidget(
                SEARCH_ROOT_WIDGET_PATH,
                mountSearchWidgets([
                    {widgetName: 'SearchBreadcrumbs', props: {}},
                ])
            );
            container = mountedWidget.container;
        });

        describe('должны содержать элементы сео-разметки', () => {
            test('обертка itemScope', () => {
                expect(container.querySelector('ol[itemType="https://schema.org/BreadcrumbList"]')).toBeInTheDocument();
            });

            test('элементы itemListElement', () => {
                expect(container.querySelector('li[itemProp="itemListElement"]')).toBeInTheDocument();
            });

            test('крошки как itemProp', () => {
                expect(container.querySelector('a[itemProp="item"]')).toBeInTheDocument();
                expect(container.querySelector('span[itemProp="name"]')).toBeInTheDocument();
                expect(container.querySelector('meta[itemProp="position"]')).toBeInTheDocument();
            });
        });

        describe('должны иметь категории совпадающие с категориями', () => {
            test('по тексту', () => {
                expect(
                    Array.from(container.querySelectorAll('a')).map(node => node.textContent)
                ).toEqual(['Продукты', 'Снеки', 'Орехи']);
            });

            test('по ссылкам', () => {
                expect(
                    Array.from(container.querySelectorAll('a')).map(node => node.href)
                ).toEqual([
                    "market:catalog_{'hid':['91307'],'nid':['54434'],'slug':'produkty'}",
                    "market:catalog_{'hid':['15714670'],'nid':['73823'],'slug':'sneki'}",
                    "market:list_{'hid':['15714682'],'nid':['73827'],'slug':'orehi'}",
                ]);
            });
        });
    });
});
