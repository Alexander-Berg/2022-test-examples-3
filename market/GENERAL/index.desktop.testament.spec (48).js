/* eslint-disable global-require */

import {waitFor} from '@testing-library/dom';
import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';

import {mountSearchWidgets, SEARCH_ROOT_WIDGET_PATH} from '@self/root/src/widgets/content/search/__spec__/mocks/mountSearchWidgets';
import AdultWarningPO from '@self/root/src/widgets/content/AdultWarning/components/View/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ADULT_HID, ADULT_NID} from '@self/root/src/constants/categories';
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

describe('Search: Подтверждение возраста', () => {
    let container;
    let setCookieSpy;
    let locationReloadSpy;
    let locationReplaceSpy;

    describe('Поиск в категории', () => {
        const pageId = PAGE_IDS_COMMON.LIST;
        const initialParams = {hid: ADULT_HID, nid: ADULT_NID};

        beforeEach(async () => {
            await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
            await mockNoopBackendFunctionality({jestLayer, kadavrLayer});

            await mockSearchFunctionality({kadavrLayer}, {adult: true, product: true});
            await mockCatalogListFunctionality({kadavrLayer}, {adult: true});

            await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);
            setCookieSpy = jest.spyOn(require('@self/root/src/utils/cookie'), 'setCookie')
                .mockImplementation(() => {});

            locationReloadSpy = jest.spyOn(window.location, 'reload');
            locationReplaceSpy = jest.spyOn(window.location, 'replace');

            const mountedWidget = await apiaryLayer.mountWidget(
                SEARCH_ROOT_WIDGET_PATH,
                mountSearchWidgets([
                    {widgetName: 'SearchAdultWarning', props: {}},
                ])
            );
            container = mountedWidget.container;
        });

        describe('По умолчанию', () => {
            test('присутствует на странице', () => {
                expect(container.querySelector(AdultWarningPO.root)).toBeVisible();
            });

            test('присутствует заголовок', async () => {
                expect(
                    container.querySelector(`${AdultWarningPO.root} h2[data-auto="title"]`).textContent
                ).toEqual('Это каталог товаров для взрослых');
            });
        });

        describe('При нажатии на кнопку "Да"', () => {
            beforeEach(() => {
                container.querySelector(AdultWarningPO.acceptButton).click();
            });

            test('должна происходить перезагрузка страницы', async () => {
                expect(locationReloadSpy).toHaveBeenCalled();
            });

            test('должна установиться несессионная кука adult', async () => {
                expect(setCookieSpy).toHaveBeenCalledWith(
                    'adult',
                    expect.stringMatching(/^\d+:\d+:ADULT$/i),
                    expect.anything()
                );
            });
        });

        describe('При нажатии на кнопку "Нет"', () => {
            beforeEach(() => {
                container.querySelector(AdultWarningPO.rejectButton).click();
            });

            test('должен произойти редирект на главную', async () => {
                expect(locationReplaceSpy).toHaveBeenCalled();
            });

            test('должна установиться несессионная кука adult со значением CHILD', async () => {
                expect(setCookieSpy).toHaveBeenCalledWith(
                    'adult',
                    expect.stringMatching(/^\d+:\d+:CHILD$/i),
                    expect.anything()
                );
            });
        });
    });

    describe('Поиск без категории', () => {
        const pageId = PAGE_IDS_COMMON.SEARCH;
        const initialParams = {text: 'запрос'};

        beforeEach(async () => {
            await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
            await mockNoopBackendFunctionality({jestLayer, kadavrLayer});

            await mockSearchFunctionality({kadavrLayer}, {adult: true, product: true});

            await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);
            setCookieSpy = jest.spyOn(require('@self/root/src/utils/cookie'), 'setCookie')
                .mockImplementation(() => {});

            locationReloadSpy = jest.spyOn(window.location, 'reload');
            locationReplaceSpy = jest.spyOn(window.location, 'replace');

            const mountedWidget = await apiaryLayer.mountWidget(
                SEARCH_ROOT_WIDGET_PATH,
                mountSearchWidgets([
                    {widgetName: 'SearchAdultWarning', props: {}},
                ])
            );
            container = mountedWidget.container;
        });

        describe('По умолчанию', () => {
            test('присутствует на странице', async () => {
                expect(container.querySelector(AdultWarningPO.root)).toBeVisible();
            });

            test('присутствует заголовок', async () => {
                expect(
                    container.querySelector(`${AdultWarningPO.root} h2[data-auto="title"]`).textContent
                ).toEqual('Найдены товары, для доступа к которым нужно подтвердить совершеннолетний возраст');
            });
        });

        describe('При нажатии на кнопку "Да"', () => {
            beforeEach(() => {
                container.querySelector(AdultWarningPO.acceptButton).click();
            });

            test('должна происходить перезагрузка страницы', async () => {
                expect(locationReloadSpy).toHaveBeenCalled();
            });

            test('должна установиться несессионная кука adult со значением ADULT', async () => {
                expect(setCookieSpy).toHaveBeenCalledWith(
                    'adult',
                    expect.stringMatching(/^\d+:\d+:ADULT$/i),
                    expect.anything()
                );
            });
        });

        describe('При нажатии на кнопку "Нет"', () => {
            beforeEach(() => {
                container.querySelector(AdultWarningPO.rejectButton).click();
            });

            test('не должен произойти редирект на главную', async () => {
                expect(locationReplaceSpy).not.toHaveBeenCalled();
            });

            test('блок должен быть скрыт', async () => {
                await waitFor(() => {
                    expect(container.querySelector(AdultWarningPO.root)).not.toBeInTheDocument();
                });
            });

            test('должна установиться несессионная кука adult со значением CHILD', async () => {
                expect(setCookieSpy).toHaveBeenCalledWith(
                    'adult',
                    expect.stringMatching(/^\d+:\d+:CHILD$/i),
                    expect.anything()
                );
            });
        });
    });
});
