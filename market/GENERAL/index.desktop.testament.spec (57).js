/* eslint-disable global-require */

import {screen, waitFor} from '@testing-library/dom';

import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mountSearchWidgets, SEARCH_ROOT_WIDGET_PATH} from '@self/root/src/widgets/content/search/__spec__/mocks/mountSearchWidgets';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {parseTestingUrl} from '@self/root/src/helpers/testament/url';
import {
    mockBasicSearchFunctionality,
    mockNoopBackendFunctionality,
    mockSearchFunctionality,
    makeContext,
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

describe('Search: Панель сортировок', () => {
    describe.each([
        ['Выдача без параметров', PAGE_IDS_COMMON.SEARCH, {}],
        ['Хаб отзывов', PAGE_IDS_COMMON.SEARCH, {'show-reviews': '1'}],
    ])('Окружение "%s"', (_, pageId, initialParams) => {
        let container;

        beforeEach(async () => {
            await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
            await mockNoopBackendFunctionality({jestLayer, kadavrLayer});
            await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);
        });

        describe('Параметр "Сначала мой регион"', () => {
            describe('Во включенном состоянии', () => {
                beforeEach(async () => {
                    await mockSearchFunctionality({kadavrLayer}, {product: true});

                    const mountedWidget = await apiaryLayer.mountWidget(
                        SEARCH_ROOT_WIDGET_PATH,
                        mountSearchWidgets([
                            {widgetName: 'SearchControls', props: {hideIfEmptySearchResults: true, sortViewType: 'row'}},
                            {widgetName: 'SearchUrlUpdater', props: {}},
                        ])
                    );
                    container = mountedWidget.container;

                    container.querySelector('input').click();
                });

                test('отображается', () => {
                    expect(screen.getByText('Сначала мой регион')).toBeVisible();
                });

                test('добавляет параметр local-offers-first=1 в урл', async () => {
                    await waitFor(() => {
                        const localOffersFirst = parseTestingUrl(window.location.href).searchParams['local-offers-first'];
                        expect(localOffersFirst).toEqual('1');
                    });
                });
            });
        });
    });
});
