/* eslint-disable global-require */

import {waitFor, fireEvent, screen} from '@testing-library/dom';
import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {parseTestingUrl} from '@self/root/src/helpers/testament/url';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {FILTER_TYPE} from '@self/root/src/constants/filters';
import {FILTERS} from '@self/root/src/entities/filterSearch/constants';
import SearchSnippetCell from '@self/root/market/src/components/Search/Snippet/Cell/__pageObject';
import {
    mockBasicSearchFunctionality,
    mockNoopBackendFunctionality,
    mockSearchFunctionality,
    makeContext,
    mountSearchWidgets,
    SEARCH_ROOT_WIDGET_PATH,
} from '@self/root/src/widgets/content/search/__spec__/mocks';

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

describe('Search: Фильтры', () => {
    const pageId = PAGE_IDS_COMMON.SEARCH;
    const initialParams = {};

    beforeEach(async () => {
        await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
        await mockNoopBackendFunctionality({jestLayer, kadavrLayer});
        await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);
    });

    const VALUE_TO_TYPE = '1000';
    describe.each([
        [FILTER_TYPE.BOOLEAN, 'filterBoolean', 'click', '1'],
        [FILTER_TYPE.RADIO, 'filterRadio', 'click', '12'],
        [FILTER_TYPE.ENUM, 'filterEnum', 'click', 'blue-cashback'],
        [FILTER_TYPE.RANGE, 'filterRange', 'type', VALUE_TO_TYPE],
    ])('Фильтр типа %s', (type, strategy, action, expectedValue) => {
        let container;
        let filterElement;

        beforeEach(async () => {
            await mockSearchFunctionality({kadavrLayer}, {[strategy]: true, commo: true});

            const mountedWidget = await apiaryLayer.mountWidget(
                SEARCH_ROOT_WIDGET_PATH,
                mountSearchWidgets([
                    {widgetName: 'SearchSerp', props: {emptyType: 'withReset'}},
                    {widgetName: 'SearchUrlUpdater', props: {}},
                    {widgetName: 'SearchFilters', props: {scrollToAnchor: 'serpTop'}},
                ])
            );
            container = mountedWidget.container;
            filterElement = container.querySelector(`div[data-filter-type="${type}"]`);
        });

        // MARKETFRONT-96354
        // Search: Фильтры Фильтр типа boolean По умолчанию должен отображаться
        // Падений 27.54%
        // eslint-disable-next-line jest/no-disabled-tests
        describe.skip('По умолчанию', () => {
            test('должен отображаться', () => {
                screen.logTestingPlaygroundURL();
                expect(filterElement).toBeVisible();
            });
        });

        describe('При применении', () => {
            let filterId;
            let queryParam;

            beforeEach(() => {
                step('Получаем filterId из разметки', () => {
                    filterId = filterElement.getAttribute('data-filter-id');
                });

                let filterInputElement;
                switch (action) {
                    case 'click': {
                        step('Находим доступное для применения значение', () => {
                            filterInputElement = filterElement.querySelector('input:not(:checked)');

                            expect(filterInputElement).toBeVisible();
                        });

                        step('Применяем новое значение фильтра', () => {
                            filterInputElement.click();
                        });

                        queryParam = filterId;

                        break;
                    }
                    case 'type': {
                        step('Находим доступное для применения значение', () => {
                            filterInputElement = filterElement.querySelector('input[type="text"]');

                            expect(filterInputElement).toBeVisible();
                        });

                        step('Применяем новое значение фильтра', () => {
                            filterInputElement.value = VALUE_TO_TYPE;
                            fireEvent.input(filterInputElement);
                        });

                        queryParam = 'pricefrom';

                        break;
                    }
                    default: {
                        throw new Error('Unknown action method in test body!');
                    }
                }
            });

            test('Примененное значение должно появиться в урле', async () => {
                await waitFor(() => {
                    expect(parseTestingUrl(window.location.href).searchParams[queryParam])
                        .toEqual(expectedValue);
                });
            });

            test('Должен появиться тултип', async () => {
                await waitFor(() => {
                    expect(document.querySelector('span[data-auto="filter-found-visible-tooltip"]'))
                        .toBeVisible();
                });
            });
        });
    });

    describe('Работа конкретных фильтров', () => {
        describe('Покупка в кредит', () => {
            let container;

            beforeEach(async () => {
                await mockSearchFunctionality({kadavrLayer}, {creditProduct: true});

                const mountedWidget = await apiaryLayer.mountWidget(
                    SEARCH_ROOT_WIDGET_PATH,
                    mountSearchWidgets([
                        {widgetName: 'SearchSerp', props: {emptyType: 'withReset'}},
                        {widgetName: 'SearchUrlUpdater', props: {}},
                        {widgetName: 'SearchFilters', props: {scrollToAnchor: 'serpTop'}},
                    ])
                );
                container = mountedWidget.container;
            });

            describe('При активации фильтра', () => {
                beforeEach(() => {
                    screen.getByRole('radio', {name: /есть/i}).click();
                });

                test('в урл пробрасывается соответствующий параметр', async () => {
                    await waitFor(() => {
                        expect(parseTestingUrl(window.location.href).searchParams['credit-type'])
                            .toEqual('credit');
                    });
                });

                test('при переходе на КМ пробрасывается параметр', async () => {
                    await waitFor(() => {
                        const modelLink = container.querySelector(SearchSnippetCell.titleLink).href;

                        expect(parseTestingUrl(modelLink).searchParams['credit-type'])
                            .toEqual('credit');
                    });
                });
            });
        });

        describe('Производитель', () => {
            beforeEach(async () => {
                await mockSearchFunctionality({kadavrLayer}, {filterVendor: true});

                await jestLayer.backend.runCode(() => {
                    jest.spyOn(require('@self/root/src/resources/report/fetchPrime'), 'fetchPrime');
                }, []);

                await apiaryLayer.mountWidget(
                    SEARCH_ROOT_WIDGET_PATH,
                    mountSearchWidgets([
                        {widgetName: 'SearchSerp', props: {emptyType: 'withReset'}},
                        {widgetName: 'SearchUrlUpdater', props: {}},
                        {widgetName: 'SearchFilters', props: {scrollToAnchor: 'serpTop'}},
                    ])
                );
            });

            describe('По умолчанию', () => {
                test('отображается', () => {
                    expect(screen.getByText('Производитель')).toBeVisible();
                });

                test('улетает запрос в прайм со значением showVendors=top', async () => {
                    // eslint-disable-next-line jest/valid-expect
                    expect(
                        jestLayer.backend.runCode(
                            () => {
                                const {getLastSpiedModuleCall} = require('@self/root/src/helpers/testament/jestLayer');

                                const lastPrimeCall = getLastSpiedModuleCall(require('@self/root/src/resources/report/fetchPrime'), 'fetchPrime');
                                return lastPrimeCall[1];
                            },
                            []
                        )
                    ).resolves.toEqual(expect.objectContaining({showVendors: 'top'}));
                });
            });

            describe('При активации фильтра', () => {
                beforeEach(() => {
                    screen.getByRole('checkbox', {name: /Apple/i}).click();
                });

                test('в урл пробрасывается соответствующий параметр', async () => {
                    await waitFor(() => {
                        expect(parseTestingUrl(window.location.href).searchParams.glfilter)
                            .toEqual(`${FILTERS.VENDOR}:153043`);
                    });
                });
            });

            describe('При догрузке фильтра', () => {
                beforeEach(() => {
                    screen.getByRole('button', {name: /Показать вс/i}).click();
                });

                test('улетает запрос в Репорт со значением showVendors=all', async () => {
                    // eslint-disable-next-line jest/valid-expect
                    expect(
                        jestLayer.backend.runCode(
                            () => {
                                const {getLastSpiedModuleCall} = require('@self/root/src/helpers/testament/jestLayer');

                                const lastPrimeCall = getLastSpiedModuleCall(require('@self/root/src/resources/report/fetchPrime'), 'fetchPrime');
                                return lastPrimeCall[1];
                            },
                            []
                        )
                    ).resolves.toEqual(expect.objectContaining({showVendors: 'all'}));
                });

                test('полные опции отображаются', () => {
                    screen.findByRole('checkbox', {name: 'Apple'});
                });
            });
        });
    });
});
