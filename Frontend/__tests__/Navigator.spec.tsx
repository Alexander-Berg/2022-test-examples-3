import { Navigator } from '../Navigator';
import { Frame } from '../../Root/Frame/Frame';
import { spinnerVisible } from '../../Root/Views/Classic/Classes';
import { frameBody, frame, frameBodyVisible } from '../../Root/Frame/Classes';
import { Config } from '../../utils/config';
import * as overrideHostUtil from '../../utils/addOverrideHostEntries';

import {
    restoreDom,
    getBaseHookData,
    getRootInstance,
    restoreLocation,
    mockLocation,
    clearNamespace,
    mockOverlayInited,
} from '../../__tests__/tests-lib';
import { EFallbackTypes } from '../../emitHook/emitHook';

let root = getRootInstance();
let navigator: Navigator;

const config = new Config({
    urls: [{
        frameUrl: 'https://yandex.ru/turbo?text=https://test_news.com',
        displayUrl: 'https://yandex.ru/turbo?text=https://test_news.com',
        originalUrl: 'https://test_news.com',
    }],
});

const init = () => {
    root.appendFrame('https://yandex.ru/turbo?text=https://test_news.com');
    root.showFrame('https://yandex.ru/turbo?text=https://test_news.com');
    root.showOverlay();

    navigator.configure({
        config,
        depth: 0,
        currentUrl: 'https://yandex.ru/turbo?text=https://test_news.com',
        currentCacheUrl: 'https://yandex.ru/turbo?text=https://test_news.com',
        orig: 'https://test_news.com',
    });
};

describe('Турбо-оверлей', () => {
    describe('Навигатор', () => {
        beforeAll(() => {
            restoreDom();
            mockOverlayInited();
            root = getRootInstance();
        });
        afterEach(() => {
            restoreDom();
            mockOverlayInited();
            root = getRootInstance();
        });
        afterAll(clearNamespace);

        describe('Функции установки и удаления состояния', () => {
            it('Устанавливает корректный current url', () => {
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                navigator.configure({
                    config,
                    depth: 0,
                    currentUrl: 'some-current-url',
                    currentCacheUrl: 'some-current-cache-url',
                });
                expect(navigator.getCurrentCacheUrl()).toBe('some-current-cache-url');

                navigator.configure({ config, depth: 0, currentUrl: 'some-other-current-url' });
                expect(navigator.getCurrentCacheUrl()).toBe('some-current-cache-url');

                navigator.configure({
                    config,
                    depth: 0,
                    currentUrl: 'some-other-current-url',
                    currentCacheUrl: 'some-other-current-cache-url',
                });
                expect(navigator.getCurrentCacheUrl()).toBe('some-other-current-cache-url');
            });
        });

        describe('show()', () => {
            let addOverrideHostEntriesSpy: jest.SpyInstance;
            beforeAll(() => {
                addOverrideHostEntriesSpy = jest.spyOn(overrideHostUtil, 'addOverrideHostEntries')
                    .mockImplementation(() => {});
            });
            afterEach(() => {
                jest.clearAllMocks();
            });
            afterAll(() => {
                jest.restoreAllMocks();
            });

            const checkMessageCallback = (
                onShow?: jest.Mock<unknown>,
                overrideHostData?: { displayHost?: string, displayUrl?: string }
            ) => {
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                // @ts-ignore игнорим модуль, т.к. глупо добавлять его в тайпинги
                window.Ya.turboOverlay.onOverlayContentShown = onShow;
                init();

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.show({
                    title: 'Турбо-страницы',
                    cleanUrl: '/turbo?text=https://test_news.com',
                    fixSwipe: false,
                    url: 'https://yandex.ru/turbo?text=https://test_news.com',
                    originalUrl: 'https://test_news.com',
                    source: {} as MessageEventSource,
                    action: 'show',
                    ...overrideHostData,
                }, {} as MessageEventSource);
            };

            it('Не падает, если показывается оверлей, о котором не знает navigator', () => {
                navigator = new Navigator({ getRootInstance, getBaseHookData });
                navigator.configure({ depth: 0, currentUrl: 'https://yandex.ru/turbo?text=never-exists' });

                expect(() => navigator.show({
                    title: 'Турбо-страницы',
                    cleanUrl: 'https://yandex.ru/turbo?text=https://test_news.com',
                    fixSwipe: false,
                    url: 'https://yandex.ru/turbo?text=https://test_news.com',
                    originalUrl: 'https://test_news.com',
                    source: {} as MessageEventSource,
                    action: 'show',
                }, {} as MessageEventSource)).not.toThrow();
            });

            it('Сохраняет и передает информацию о корректировке свайпа', () => {
                navigator = new Navigator({ getRootInstance, getBaseHookData });
                init();

                expect(navigator.isRecreateMode()).toBe(false);
                navigator.show({
                    title: 'Турбо-страницы',
                    cleanUrl: 'https://yandex.ru/turbo?text=https://test_news.com',
                    fixSwipe: true,
                    url: 'https://yandex.ru/turbo?text=https://test_news.com',
                    originalUrl: 'https://test_news.com',
                    source: {} as MessageEventSource,
                    action: 'show',
                }, {} as MessageEventSource);

                expect(navigator.isRecreateMode()).toBe(true);
            });

            it('Правильно отрабатывает без хуков', () => {
                const length = window.history.length;
                checkMessageCallback();

                expect(document.title, 'Тайтл страницы не изменился').toEqual('Турбо-страницы');
                expect(location.href, 'Урл не изменился').toEqual('https://yandex.ru/turbo?text=https://test_news.com');
                expect(window.history.length).toEqual(length);

                expect(document.documentElement).toMatchSnapshot('Показался первый iframe');
            });

            it('Правильно отрабатывает с хуком не возвращающем ничего', () => {
                const length = window.history.length;
                const onShow = jest.fn();

                checkMessageCallback(onShow);

                expect(document.title, 'Тайтл страницы не изменился').toEqual('Турбо-страницы');
                expect(location.href, 'Урл не изменился').toEqual('https://yandex.ru/turbo?text=https://test_news.com');
                expect(window.history.length).toEqual(length);

                expect(onShow.mock.calls.length, 'Хук не был вызыван ровно один раз').toBe(1);
                expect(onShow.mock.calls[0], 'Хук был вызван с неправильными параметрами')
                    .toEqual([
                        {
                            displayUrl: '/turbo?text=https://test_news.com',
                            title: 'Турбо-страницы',
                            ...getBaseHookData(),
                        },
                    ]);

                expect(document.documentElement).toMatchSnapshot('Показался первый iframe');
            });

            it('Правильно отрабатывает и заменяет тайтл на необходимый', () => {
                const length = window.history.length;
                const onShow = jest.fn().mockReturnValue({ title: 'New Title' });

                checkMessageCallback(onShow);

                expect(document.title, 'Тайтл страницы не изменился').toEqual('New Title');
                expect(location.href, 'Урл не изменился').toEqual('https://yandex.ru/turbo?text=https://test_news.com');
                expect(window.history.length).toEqual(length);

                expect(onShow.mock.calls.length, 'Хук не был вызыван ровно один раз').toBe(1);
                expect(onShow.mock.calls[0], 'Хук был вызван с неправильными параметрами')
                    .toEqual([
                        {
                            displayUrl: '/turbo?text=https://test_news.com',
                            title: 'Турбо-страницы',
                            ...getBaseHookData(),
                        },
                    ]);

                expect(document.documentElement).toMatchSnapshot('Показался первый iframe');
            });

            it('Правильно отрабатывает и заменяет урл на необходимый', () => {
                const length = window.history.length;
                const onShow = jest.fn().mockReturnValue({
                    displayUrl: 'https://yandex.ru/?sidebar-href=text=https://test_news.com',
                });

                checkMessageCallback(onShow);

                expect(document.title, 'Тайтл страницы не изменился').toEqual('Турбо-страницы');
                expect(location.href, 'Урл не изменился').toEqual('https://yandex.ru/?sidebar-href=text=https://test_news.com');
                expect(window.history.length).toEqual(length);

                expect(onShow.mock.calls.length, 'Хук не был вызыван ровно один раз').toBe(1);
                expect(onShow.mock.calls[0], 'Хук был вызван с неправильными параметрами')
                    .toEqual([
                        {
                            displayUrl: '/turbo?text=https://test_news.com',
                            title: 'Турбо-страницы',
                            ...getBaseHookData(),
                        },
                    ]);

                expect(document.documentElement).toMatchSnapshot('Показался первый iframe');
            });

            it('Правильно отрабатывает и заменяет урл и тайтл на необходимый', () => {
                const length = window.history.length;
                const onShow = jest.fn().mockReturnValue({
                    displayUrl: 'https://yandex.ru/?sidebar-href=text=https://test_news.com',
                    title: 'New Title',
                });

                checkMessageCallback(onShow);

                expect(document.title, 'Тайтл страницы не изменился').toEqual('New Title');
                expect(location.href, 'Урл не изменился').toEqual('https://yandex.ru/?sidebar-href=text=https://test_news.com');
                expect(window.history.length).toEqual(length);

                expect(onShow.mock.calls.length, 'Хук не был вызыван ровно один раз').toBe(1);
                expect(onShow.mock.calls[0], 'Хук был вызван с неправильными параметрами')
                    .toEqual([
                        {
                            displayUrl: '/turbo?text=https://test_news.com',
                            title: 'Турбо-страницы',
                            ...getBaseHookData(),
                        },
                    ]);

                expect(document.documentElement).toMatchSnapshot('Показался первый iframe');
            });

            it('Не вызывает addOverrideHostEntries, если не пришли данные для подмены', () => {
                checkMessageCallback();

                expect(addOverrideHostEntriesSpy, 'Вызвался метод подмены домена, хотя данных для этого не пришло')
                    .toHaveBeenCalledTimes(0);
            });

            it('Вызывает addOverrideHostEntries, если пришли данные для подмены', () => {
                const onShow = jest.fn();
                const overrideData = {
                    displayUrl: 'https://test.example.com/1',
                    displayHost: 'test.example.com',
                };

                checkMessageCallback(onShow, overrideData);

                expect(addOverrideHostEntriesSpy, 'Метод подмены домена не вызвался несмотря на то, что данные пришли')
                    .toHaveBeenCalledTimes(1);
                expect(addOverrideHostEntriesSpy, 'Метод подмены домена был вызван с неверным набором параметров')
                    .toHaveBeenCalledWith({
                        keyUrl: 'https://yandex.ru/turbo?text=https://test_news.com',
                        displayUrl: 'https://test.example.com/1',
                        displayHost: 'test.example.com',
                    });
            });

            it('Вызывает addOverrideHostEntries с данными, модифицированными в хуках', () => {
                const onShow = jest.fn().mockReturnValue({
                    displayUrl: '/?sidebar-href=text=https://test_news.com',
                    title: 'New Title',
                });
                const overrideData = {
                    displayUrl: 'https://test.example.com/1',
                    displayHost: 'test.example.com',
                };

                checkMessageCallback(onShow, overrideData);

                expect(addOverrideHostEntriesSpy, 'Метод подмены домена не вызвался несмотря на то, что данные пришли')
                    .toHaveBeenCalledTimes(1);
                expect(addOverrideHostEntriesSpy, 'Метод подмены домена был вызван с неверным набором параметров')
                    .toHaveBeenCalledWith({
                        keyUrl: 'https://yandex.ru/?sidebar-href=text=https://test_news.com',
                        displayUrl: 'https://yandex.ru/?sidebar-href=text=https://test_news.com',
                        displayHost: 'test.example.com',
                    });
            });
        });

        describe('openNewWindow()', () => {
            afterEach(restoreLocation);

            it('Открытие для текущих страниц', () => {
                const { hrefFnMock } = mockLocation();
                navigator = new Navigator({ getRootInstance, getBaseHookData });
                navigator.openNewWindow({ url: 'https://kp.ru' });

                expect(hrefFnMock.mock.calls.length, 'Перехода в истории не произошло').toBe(1);
                expect(hrefFnMock.mock.calls[0], 'Редирект произошел не туда').toEqual(['https://kp.ru']);
            });

            it('Открытие для поискового приложения', () => {
                const { hrefFnMock } = mockLocation();
                const availableFn = jest.fn();
                const unavailableFn = jest.fn();
                const openFn = jest.fn();
                jest.useFakeTimers();

                const AppsApi = {
                    whenAvailable: availableFn,
                    whenUnavailable: unavailableFn,
                    verticalServices: {
                        open: openFn,
                    },
                };

                // @ts-ignore неполный объект
                window.Ya = { AppsApi };

                navigator = new Navigator({ getRootInstance, getBaseHookData });
                navigator.openNewWindow({ url: 'https://kp.ru' });

                expect(availableFn.mock.calls.length, 'Apps api не вызвался').toBe(1);
                availableFn.mock.calls[0][0](AppsApi);
                jest.runTimersToTime(1000);

                expect(openFn.mock.calls.length, 'Не вызвалось открытие в новой вкладке приложения').toBe(1);
                expect(openFn.mock.calls[0]).toEqual(['search', 'https://kp.ru', true]);
                expect(hrefFnMock.mock.calls.length, 'Вызвался нативный переход в истории').toBe(0);

                unavailableFn.mock.calls[0][0]();

                expect(hrefFnMock.mock.calls.length, 'Перехода в истории не произошло').toBe(1);
                expect(hrefFnMock.mock.calls[0], 'Редирект произошел не туда').toEqual(['https://kp.ru']);
            });
        });

        describe('doFallback()', () => {
            afterEach(restoreLocation);

            it('Редиректит на оригинал страницы, используя метод open-new-tab', () => {
                const { historyBackMock } = mockLocation();
                navigator = new Navigator({ getRootInstance, getBaseHookData });
                const openNewWindowMock = jest.fn();

                // мокаем имплементацию, которую проверяем в тесте выше
                navigator.openNewWindow = openNewWindowMock;

                navigator.configure({
                    config,
                    depth: 0,
                    currentUrl: 'https://yandex.ru/turbo/s/kp.ru',
                    currentCacheUrl: 'https://yandex.ru/turbo/s/kp.ru',
                    orig: 'https://kp.ru',
                });

                // показываем фрейм (для непоказанных не происходит перехода назад)
                root.appendFrame('https://yandex.ru/turbo/s/kp.ru');
                const frame = root.getFrame('https://yandex.ru/turbo/s/kp.ru');
                frame.show();

                navigator.doFallback({ type: EFallbackTypes.messageFallback, url: 'https://yandex.ru/turbo/s/kp.ru' });

                expect(historyBackMock.mock.calls.length, 'Не было шага назад').toBe(1);

                expect(openNewWindowMock.mock.calls.length, 'Перехода в истории не произошло').toBe(1);
                expect(openNewWindowMock.mock.calls[0], 'Редирект произошел не туда').toEqual([{ url: 'https://kp.ru' }]);
            });

            it('Не редиректит на оригинал страницы, используя метод open-new-tab под хуком', () => {
                const { historyBackMock } = mockLocation();

                const hookMock = jest.fn().mockReturnValueOnce({ preventDefault: true });
                // @ts-ignore
                window.Ya.turboOverlay.onOverlayFallback = hookMock;

                navigator = new Navigator({ getRootInstance, getBaseHookData });
                const mock = jest.fn();
                navigator.openNewWindow = mock;

                navigator.configure({
                    config,
                    depth: 0,
                    currentUrl: 'https://yandex.ru/turbo/s/kp.ru',
                    currentCacheUrl: 'https://yandex.ru/turbo/s/kp.ru',
                    orig: 'https://kp.ru',
                });

                // показываем фрейм (для непоказанных не происходит перехода назад)
                root.appendFrame('https://yandex.ru/turbo/s/kp.ru');
                const frame = root.getFrame('https://yandex.ru/turbo/s/kp.ru');
                frame.show();

                navigator.doFallback({ type: EFallbackTypes.messageFallback, url: 'https://yandex.ru/turbo/s/kp.ru' });

                expect(mock.mock.calls.length, 'Произошел переход в истории').toBe(0);
                expect(historyBackMock.mock.calls.length, 'Был шаг назад').toBe(0);

                expect(hookMock.mock.calls.length).toBe(1);
                expect(hookMock.mock.calls[0]).toEqual([{
                    ...getBaseHookData(),
                    originalUrl: 'https://kp.ru',
                    fallbackType: 'message',
                    callback: expect.any(Function),
                }]);
            });

            it('Отправляет правильный хук под таймаутом по времени', () => {
                jest.useFakeTimers();

                const { historyBackMock } = mockLocation();
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                init();

                const hookMock = jest.fn();
                // @ts-ignore
                window.Ya.turboOverlay.onOverlayFallback = hookMock;

                // показываем фрейм (для непоказанных не происходит перехода назад)
                root.appendFrame('http://www.yandex.ru');
                const frame = root.getFrame('http://www.yandex.ru');
                frame.show();

                navigator.startFallbackCounter('http://www.yandex.ru');
                jest.runTimersToTime(5000);

                expect(historyBackMock.mock.calls.length, 'Не было шага назад').toBe(1);

                expect(hookMock.mock.calls.length).toBe(1);
                expect(hookMock.mock.calls[0]).toEqual([{
                    ...getBaseHookData(),
                    originalUrl: 'https://test_news.com',
                    fallbackType: 'timeout',
                    callback: expect.any(Function),
                }]);
            });

            jest.useRealTimers();
        });

        describe('navigateTurboInTurbo()', () => {
            it('Простая навигация', () => {
                const historyLength = window.history.length;
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                init();

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.navigateTurboInTurbo({
                    url: '/turbo/s/kp.ru?parent-reqid=1',
                    fromUrl: '/turbo?text=about',
                    orig: 'https://kp.ru',
                });

                expect(document.querySelector(`.${frameBodyVisible}`), 'Iframe не спрятан').toBeNull();
                expect(document.querySelectorAll(`.${frameBody}`).length, 'Есть лишние iframe').toBe(1);
                expect(document.querySelector(`.${spinnerVisible}`), 'Спиннер не показался').not.toBeNull();
                expect(window.history.length, 'При нативных переходах размер истории не должен увеличиваться')
                    .toBe(historyLength);

                expect(document.documentElement).toMatchSnapshot('Состояние после навигации');

                // @ts-ignore лезем внутрь приватных свойств
                expect(Object.keys(root.frameCache), 'Не обновился кэш рута')
                    .toEqual(['turbo?text=https://test_news.com']);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'], 'Не обновился кэш рута')
                    .toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelector(`.${frame}`));
            });

            it('Навигация для ios12', () => {
                const historyLength = window.history.length;
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                // @ts-ignore — изменяем приватное свойство
                navigator.recreateMode = true;

                init();

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.navigateTurboInTurbo({
                    url: '/turbo/s/kp.ru?parent-reqid=1',
                    fromUrl: '/turbo?text=about',
                    orig: 'https://kp.ru',
                });

                expect(document.querySelector(`.${frameBodyVisible}`), 'Iframe не спрятан').toBeNull();
                expect(document.querySelectorAll(`.${frameBody}`).length, 'Есть лишние iframe').toBe(1);
                expect(document.querySelector(`.${spinnerVisible}`), 'Спиннер не показался').not.toBeNull();
                expect(window.history.length, 'При нативных переходах размер истории не должен увеличиваться')
                    .toBe(historyLength + 1);
                expect(location.href, 'Урл не изменился').toBe('https://yandex.ru/turbo/s/kp.ru?parent-reqid=1');

                expect(document.documentElement).toMatchSnapshot('Состояние после навигации');

                // @ts-ignore лезем внутрь приватных свойств
                expect(Object.keys(root.frameCache), 'Не обновился кэш рута')
                    .toEqual(['turbo?text=https://test_news.com']);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'], 'Не обновился кэш рута')
                    .toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelector(`.${frame}`));
            });

            it('Навигация для ios12 c хуком, меняющиим урл', () => {
                const historyLength = window.history.length;
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                const mock = jest.fn().mockReturnValueOnce({ displayUrl: 'https://yandex.ru/?sidebar_href=123' });
                // @ts-ignore
                window.Ya.turboOverlay.onOverlayNavigate = mock;

                // @ts-ignore — изменяем приватное свойство
                navigator.recreateMode = true;

                init();

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.navigateTurboInTurbo({
                    url: '/turbo/s/kp.ru?parent-reqid=1',
                    fromUrl: '/turbo?text=about',
                    orig: 'https://kp.ru',
                });

                expect(document.querySelector(`.${frameBodyVisible}`), 'Iframe не спрятан').toBeNull();
                expect(document.querySelectorAll(`.${frameBody}`).length, 'Есть лишние iframe').toBe(1);
                expect(document.querySelector(`.${spinnerVisible}`), 'Спиннер не показался').not.toBeNull();
                expect(window.history.length, 'При нативных переходах размер истории не должен увеличиваться')
                    .toBe(historyLength + 1);
                expect(location.href, 'Урл не изменился').toBe('https://yandex.ru/?sidebar_href=123');

                expect(document.documentElement).toMatchSnapshot('Состояние после навигации');

                // @ts-ignore лезем внутрь приватных свойств
                expect(Object.keys(root.frameCache), 'Не обновился кэш рута')
                    .toEqual(['turbo?text=https://test_news.com']);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'], 'Не обновился кэш рута')
                    .toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelector(`.${frame}`));

                expect(mock.mock.calls.length).toBe(1);
                expect(mock.mock.calls[0]).toEqual([{
                    ...getBaseHookData(),
                    to: '/turbo/s/kp.ru?parent-reqid=1',
                    from: '/turbo?text=about',
                }]);
            });
        });

        describe('relocateTurboInTurbo()', () => {
            it('Изменение урла без хуков', () => {
                const historyLength = window.history.length;
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                init();

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.relocateTurboInTurbo({
                    url: '/turbo/s/kp.ru?parent-reqid=1',
                    fromUrl: '/turbo?text=about',
                    orig: 'https://kp.ru',
                });

                expect(document.querySelector(`.${frameBodyVisible}`), 'Iframe не спрятан').toBeNull();
                expect(document.querySelectorAll(`.${frameBody}`).length, 'Есть лишние iframe').toBe(1);
                expect(document.querySelector(`.${spinnerVisible}`), 'Спиннер не показался').not.toBeNull();
                expect(window.history.length, 'При измении урла размер истории не должен увеличиваться')
                    .toBe(historyLength);

                expect(location.href, 'Локация не изменилась')
                    .toBe('https://yandex.ru/turbo/s/kp.ru?parent-reqid=1');
                expect(document.documentElement).toMatchSnapshot('Состояние после навигации');

                // @ts-ignore лезем внутрь приватных свойств
                expect(Object.keys(root.frameCache), 'Не обновился кэш рута')
                    .toEqual(['turbo?text=https://test_news.com']);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'], 'Не обновился кэш рута')
                    .toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelector(`.${frame}`));
            });

            it('Изменение урла с хуком на другой урл', () => {
                const historyLength = window.history.length;
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                const mock = jest.fn().mockReturnValueOnce({ displayUrl: 'https://yandex.ru?sidebar_href=/turbo?text=&a' });
                // @ts-ignore
                window.Ya.turboOverlay.onOverlayRelocate = mock;

                init();

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.relocateTurboInTurbo({
                    url: '/turbo/s/kp.ru?parent-reqid=1',
                    fromUrl: '/turbo?text=about',
                    orig: 'https://kp.ru',
                });

                expect(document.querySelector(`.${frameBodyVisible}`), 'Iframe не спрятан').toBeNull();
                expect(document.querySelectorAll(`.${frameBody}`).length, 'Есть лишние iframe').toBe(1);
                expect(document.querySelector(`.${spinnerVisible}`), 'Спиннер не показался').not.toBeNull();
                expect(window.history.length, 'При измении урла размер истории не должен увеличиваться')
                    .toBe(historyLength);

                expect(location.href, 'Локация не изменилась на правильную')
                    .toBe('https://yandex.ru/?sidebar_href=/turbo?text=&a');
                expect(document.documentElement).toMatchSnapshot('Состояние после навигации');

                // @ts-ignore лезем внутрь приватных свойств
                expect(Object.keys(root.frameCache), 'Не обновился кэш рута')
                    .toEqual(['turbo?text=https://test_news.com']);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'], 'Не обновился кэш рута')
                    .toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelector(`.${frame}`));

                expect(mock.mock.calls.length).toBe(1);
                expect(mock.mock.calls[0]).toEqual([{
                    ...getBaseHookData(),
                    to: '/turbo/s/kp.ru?parent-reqid=1',
                    from: '/turbo?text=about',
                }]);
            });
        });

        describe('changePage()', () => {
            it('Изменение страницы без хуков', () => {
                const historyLength = window.history.length;
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                init();

                root.appendFrame('/turbo/s/kp.ru?parent-reqid=1');

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.changePage({
                    url: '/turbo/s/kp.ru?parent-reqid=1',
                    fromUrl: '/turbo?text=about',
                    orig: 'https://kp.ru',
                }, '/turbo/s/kp.ru?parent-reqid=1');

                expect(document.querySelector(`.${frameBodyVisible}`), 'Iframe был спрятан').not.toBeNull();
                expect(document.querySelectorAll(`.${frameBody}`).length, 'Неверное колчиество iframe на странице')
                    .toBe(2);
                expect(document.querySelector(`.${spinnerVisible}`), 'Спиннер появился').toBeNull();
                expect(window.history.length, 'При перелистывании размер истории не должен увеличиваться')
                    .toBe(historyLength);
                expect(location.href, 'Локация не изменилась')
                    .toBe('https://yandex.ru/turbo/s/kp.ru?parent-reqid=1');

                expect(document.documentElement).toMatchSnapshot('Состояние после навигации');

                // @ts-ignore лезем внутрь приватных свойств
                expect(Object.keys(root.frameCache), 'Не обновился кэш рута')
                    .toEqual(['turbo?text=https://test_news.com', 'turbo/s/kp.ru?']);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'], 'Не обновился кэш рута')
                    .toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo/s/kp.ru?'], 'Не обновился кэш рута').toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelectorAll(`.${frame}`)[0]);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo/s/kp.ru?'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelectorAll(`.${frame}`)[1]);
            });

            it('Изменение страницы с хуком на другой урл', () => {
                const historyLength = window.history.length;
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                const mock = jest.fn().mockReturnValueOnce({ displayUrl: 'https://yandex.ru?sidebar_href=/turbo?text=&a' });
                // @ts-ignore
                window.Ya.turboOverlay.onOverlayPageChanged = mock;

                init();

                root.appendFrame('/turbo/s/kp.ru?parent-reqid=1');

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.changePage({
                    url: '/turbo/s/kp.ru?parent-reqid=1',
                    fromUrl: '/turbo?text=about',
                    orig: 'https://kp.ru',
                }, '/turbo/s/kp.ru?parent-reqid=1');

                expect(document.querySelector(`.${frameBodyVisible}`), 'Iframe был спрятан').not.toBeNull();
                expect(document.querySelectorAll(`.${frameBody}`).length, 'Неверное колчиество iframe на странице')
                    .toBe(2);
                expect(document.querySelector(`.${spinnerVisible}`), 'Спиннер появился').toBeNull();
                expect(window.history.length, 'При перелистывании размер истории не должен увеличиваться')
                    .toBe(historyLength);
                expect(location.href, 'Локация не изменилась на правильную')
                    .toBe('https://yandex.ru/?sidebar_href=/turbo?text=&a');

                expect(document.documentElement).toMatchSnapshot('Состояние после навигации');

                // @ts-ignore лезем внутрь приватных свойств
                expect(Object.keys(root.frameCache), 'Не обновился кэш рута')
                    .toEqual(['turbo?text=https://test_news.com', 'turbo/s/kp.ru?']);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'], 'Не обновился кэш рута').toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo/s/kp.ru?'], 'Не обновился кэш рута').toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelectorAll(`.${frame}`)[0]);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo/s/kp.ru?'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelectorAll(`.${frame}`)[1]);

                expect(mock.mock.calls.length).toBe(1);
                expect(mock.mock.calls[0]).toEqual([{
                    ...getBaseHookData(),
                    to: '/turbo/s/kp.ru?parent-reqid=1',
                    from: '/turbo?text=about',
                }]);
            });
        });

        describe('activeDocumentChanged()', () => {
            it('Просто изменение урла', () => {
                const historyLength = window.history.length;
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                init();

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.activeDocumentChanged({
                    url: '/turbo/s/kp.ru?parent-reqid=1',
                    cleanUrl: '/turbo/s/kp.ru',
                    orig: 'https://kp.ru',
                    title: 'кп ру',
                });

                expect(window.history.length, 'При изменении урла размер истории не должен увеличиваться')
                    .toBe(historyLength);
                expect(location.href, 'Урл не изменился').toBe('https://yandex.ru/turbo/s/kp.ru');
                expect(document.title).toBe('кп ру');

                expect(document.documentElement).toMatchSnapshot('Состояние после навигации');

                // @ts-ignore лезем внутрь приватных свойств
                expect(Object.keys(root.frameCache), 'Не обновился кэш рута')
                    .toEqual(['turbo?text=https://test_news.com']);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'], 'Не обновился кэш рута')
                    .toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelector(`.${frame}`));
            });

            it('Изменение с хуком, заменяющим урл', () => {
                const historyLength = window.history.length;
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                const mock = jest.fn().mockReturnValueOnce({ displayUrl: 'https://yandex.ru/?sidebar_href=123' });
                // @ts-ignore
                window.Ya.turboOverlay.onOverlayActiveDocumentChanged = mock;

                init();

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.activeDocumentChanged({
                    url: '/turbo/s/kp.ru?parent-reqid=1',
                    cleanUrl: '/turbo/s/kp.ru',
                    orig: 'https://kp.ru',
                    title: 'кп ру',
                });

                expect(window.history.length, 'При изменении урла размер истории не должен увеличиваться')
                    .toBe(historyLength);
                expect(location.href, 'Урл не изменился').toBe('https://yandex.ru/?sidebar_href=123');
                expect(document.title).toBe('кп ру');

                expect(document.documentElement).toMatchSnapshot('Состояние после навигации');

                // @ts-ignore лезем внутрь приватных свойств
                expect(Object.keys(root.frameCache), 'Не обновился кэш рута')
                    .toEqual(['turbo?text=https://test_news.com']);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'], 'Не обновился кэш рута')
                    .toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelector(`.${frame}`));

                expect(mock.mock.calls.length).toBe(1);
                expect(mock.mock.calls[0]).toEqual([{
                    ...getBaseHookData(),
                    displayUrl: '/turbo/s/kp.ru',
                    title: 'кп ру',
                }]);
            });

            it('Изменение с хуком, заменяющим тайтл', () => {
                const historyLength = window.history.length;
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                const mock = jest.fn().mockReturnValueOnce({ title: 'some new title' });
                // @ts-ignore
                window.Ya.turboOverlay.onOverlayActiveDocumentChanged = mock;

                init();

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.activeDocumentChanged({
                    url: '/turbo/s/kp.ru?parent-reqid=1',
                    cleanUrl: '/turbo/s/kp.ru',
                    orig: 'https://kp.ru',
                    title: 'кп ру',
                });

                expect(window.history.length, 'При изменении урла размер истории не должен увеличиваться')
                    .toBe(historyLength);
                expect(location.href, 'Урл не изменился').toBe('https://yandex.ru/turbo/s/kp.ru');
                expect(document.title).toBe('some new title');

                expect(document.documentElement).toMatchSnapshot('Состояние после навигации');

                // @ts-ignore лезем внутрь приватных свойств
                expect(Object.keys(root.frameCache), 'Не обновился кэш рута')
                    .toEqual(['turbo?text=https://test_news.com']);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'], 'Не обновился кэш рута')
                    .toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelector(`.${frame}`));

                expect(mock.mock.calls.length).toBe(1);
                expect(mock.mock.calls[0]).toEqual([{
                    ...getBaseHookData(),
                    displayUrl: '/turbo/s/kp.ru',
                    title: 'кп ру',
                }]);
            });

            it('Изменение с хуком, заменяющим урл и тайтл', () => {
                const historyLength = window.history.length;
                navigator = new Navigator({ getRootInstance, getBaseHookData });

                const mock = jest.fn()
                    .mockReturnValueOnce({ title: 'some new title', displayUrl: 'https://yandex.ru/?sidebar_href=123' });
                // @ts-ignore
                window.Ya.turboOverlay.onOverlayActiveDocumentChanged = mock;

                init();

                expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

                navigator.activeDocumentChanged({
                    url: '/turbo/s/kp.ru?parent-reqid=1',
                    cleanUrl: '/turbo/s/kp.ru',
                    orig: 'https://kp.ru',
                    title: 'кп ру',
                });

                expect(window.history.length, 'При изменении урла размер истории не должен увеличиваться')
                    .toBe(historyLength);
                expect(location.href, 'Урл не изменился').toBe('https://yandex.ru/?sidebar_href=123');
                expect(document.title).toBe('some new title');

                expect(document.documentElement).toMatchSnapshot('Состояние после навигации');

                // @ts-ignore лезем внутрь приватных свойств
                expect(Object.keys(root.frameCache), 'Не обновился кэш рута')
                    .toEqual(['turbo?text=https://test_news.com']);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'], 'Не обновился кэш рута')
                    .toBeInstanceOf(Frame);

                // @ts-ignore лезем внутрь приватных свойств
                expect(root.frameCache['turbo?text=https://test_news.com'].getElement(), 'Не обновился кэш рута')
                    .toEqual(document.querySelector(`.${frame}`));

                expect(mock.mock.calls.length).toBe(1);
                expect(mock.mock.calls[0]).toEqual([{
                    ...getBaseHookData(),
                    displayUrl: '/turbo/s/kp.ru',
                    title: 'кп ру',
                }]);
            });
        });

        describe('getAndroidFallbackPackage()', () => {
            afterEach(restoreLocation);

            it('Должен возвращать имя неустановленного приложения', () => {
                navigator = new Navigator({ getRootInstance, getBaseHookData });
                // @ts-ignore private method
                mockLocation(location.href, navigator.getDeeplinkAndroidFallbackHash('ru.beru.android'));
                expect(navigator.getAndroidFallbackPackage(), 'Вернулось не ожидаемое приложение').toBe('ru.beru.android');
            });

            it('Должен возвращать пустую строку, если фолбек не сработал', () => {
                navigator = new Navigator({ getRootInstance, getBaseHookData });
                expect(navigator.getAndroidFallbackPackage(), 'Вернулась не пустая строка').toBe('');
            });
        });

        describe('navigateAndroidDeeplink()', () => {
            afterEach(restoreLocation);

            it('Смена адреса на deeplink', () => {
                const { hrefFnMock } = mockLocation();
                navigator = new Navigator({ getRootInstance, getBaseHookData });
                navigator.navigateAndroidDeeplink({
                    url: 'https://beru.ru/product/100608245897',
                    package: 'ru.beru.android',
                });

                expect(hrefFnMock.mock.calls.length, 'Перехода в истории не произошло').toBe(1);
                expect(hrefFnMock.mock.calls[0], 'Редирект произошел не туда').toEqual([
                    'https://beru.ru/product/100608245897' +
                    ';package=ru.beru.android' +
                    ';scheme=https' +
                    ';S.browser_fallback_url=' + encodeURIComponent('https://yandex.ru/#deeplink-android-fallback:ru.beru.android') +
                    ';end',
                ]);
            });

            it('Не должно быть попытки открытия приложения, если известно, что оно не установлено', () => {
                const { hrefFnMock } = mockLocation();
                navigator = new Navigator({ getRootInstance, getBaseHookData });
                navigator.androidApplicationExistence['ru.beru.android'] = false;
                navigator.navigateAndroidDeeplink({
                    url: 'https://beru.ru/product/100608245897',
                    package: 'ru.beru.android',
                });

                expect(hrefFnMock.mock.calls.length, 'Произошёл избыточный переход').toBe(0);
            });
        });
    });
});
