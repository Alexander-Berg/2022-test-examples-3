/* eslint-disable global-require */

import {screen, waitFor} from '@testing-library/dom';

import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mountSearchWidgets, SEARCH_ROOT_WIDGET_PATH} from '@self/root/src/widgets/content/search/__spec__/mocks/mountSearchWidgets';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {
    makeContext,
    mockBasicSearchFunctionality,
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

describe('Search: Пагинация', () => {
    const pageId = PAGE_IDS_COMMON.SEARCH;
    const initialParams = {page: 2};

    let container;

    beforeEach(async () => {
        await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
        await mockNoopBackendFunctionality({jestLayer, kadavrLayer});
        await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);

        await mockSearchFunctionality({kadavrLayer}, {
            multiplePages: true,
            filterRadio: true,
        });

        await step('Монтируем виджеты выдачи', async () => {
            const mountedWidget = await apiaryLayer.mountWidget(
                SEARCH_ROOT_WIDGET_PATH,
                mountSearchWidgets([
                    {widgetName: 'SearchPager', props: {scrollToAnchor: 'serpTop'}},
                    {widgetName: 'SearchControls', props: {hideIfEmptySearchResults: true, sortViewType: 'row'}},
                    {widgetName: 'SearchFilters', props: {scrollToAnchor: 'serpTop'}},
                ])
            );

            container = mountedWidget.container;
        });
    });

    // SKIPPED MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('По умолчанию', () => {
        test('должна быть указана текущая страница', () => {
            step('На пагинаторе отображается вторая страница', () => {
                expect(container.querySelector('.currentButton')).toHaveTextContent('2');
            });
        });
    });

    describe('Страница должна переключиться на первую', () => {
        // SKIPPED MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        test.skip('при применении сортировки по цене', async () => {
            await step('Применяем сортировку по цене', async () => {
                await screen.getByText('по цене').click();
            });

            await step('Счетчик в пагинаторе должен измениться на первую страницу', async () => {
                await waitFor(
                    () => expect(container.querySelector('.currentButton')).toHaveTextContent('1')
                );
            });
        });

        test('при применении фильтра local-offers-first', async () => {
            await step('Применяем фильтр local-offer-first', async () => {
                await screen.getByText('Сначала мой регион').click();
            });

            await step('Счетчик в пагинаторе должен измениться на первую страницу', async () => {
                await waitFor(
                    () => expect(container.querySelector('.currentButton')).toHaveTextContent('1')
                );
            });
        });

        // SKIPPED MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        test.skip('при изменении фильтра цены', async () => {
            await step('Кликаем по фильтру по скорости доставки', async () => {
                await screen.getByRole('radio', {name: /до 5 дней/i}).click();
            });

            await step('Счетчик в пагинаторе должен измениться на первую страницу', async () => {
                await waitFor(
                    () => expect(container.querySelector('.currentButton')).toHaveTextContent('1')
                );
            });
        });
    });

    // SKIPPED MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('Страница не должна переключиться на первую', () => {
        test('при изменении параметра отображения viewtype', async () => {
            step('Кликаем по переключателю grid/list', () => {
                container.querySelector('input[type="radio"][value="list"][name="viewType"]').click();
            });

            await step('Счетчик в пагинаторе не должен измениться на первую страницу', async () => {
                await waitFor(
                    () => expect(container.querySelector('.currentButton')).toHaveTextContent('2')
                );
            });
        });
    });
});
