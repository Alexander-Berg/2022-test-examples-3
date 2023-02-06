import { noop } from 'lodash';

import { Overlay } from '../Overlay';
import { Frame } from '../Root/Frame/Frame';
import { restoreDom, clearNamespace, mockLocation, mockWindow, restoreLocation, restoreWindow } from './tests-lib';
import * as baseCls from '../Root/Views/Base/Classes';
import * as frameCls from '../Root/Frame/Classes';
import * as classicCls from '../Root/Views/Classic/Classes';
import * as createRootsModule from '../Root/createRoots';
import * as userAgentParams from '../utils/userAgent';
import * as overrideHostUtil from '../utils/addOverrideHostEntries';
import * as market from '../utils/isInMarket';
import * as getScriptParamUtil from '../utils/getScriptParam';

interface IPostMsgParams {
    data: object;
    origin?: string;
    source?: MessageEventSource;
}
const postMsg = ({ data, origin = location.origin, source = window as MessageEventSource }: IPostMsgParams) => new Promise(res => {
    const cb = () => {
        window.removeEventListener('message', cb);
        res();
    };

    const event = new MessageEvent('message', { data, origin, source });

    window.addEventListener('message', cb);
    window.dispatchEvent(event);
});

const postHistoryEvent = (state: object) => new Promise(res => {
    const cb = () => {
        window.removeEventListener('popstate', cb);
        res();
    };
    const event = new PopStateEvent('popstate', {
        state,
    });

    window.addEventListener('popstate', cb);
    window.dispatchEvent(event);
});

const urls = [{
    originalUrl: 'test_news',
    displayUrl: 'https://yandex.ru/turbo?text=test_news',
    frameUrl: 'https://yandex.ru/turbo?text=test_news&some-more-data',
}];

const expectedUrls = [{
    originalUrl: 'test_news',
    displayUrl: 'https://yandex.ru/turbo?text=test_news',
    frameUrl: 'https://yandex.ru/turbo?text=test_news&some-more-data&new_overlay=1',
}];

const multipleUrls = [
    {
        originalUrl: 'https://original-host1.ru/article/1',
        displayUrl: 'https://yandex.ru/turbo?text=test_news',
        frameUrl: 'https://yandex.ru/turbo?text=test_news&some-more-data',
    },
    {
        originalUrl: 'https://original-host2.ru/article/2',
        displayUrl: 'https://yandex.ru/turbo?text=test_news',
        frameUrl: 'https://yandex.ru/turbo?text=test_news&some-more-data2',
    },
];

const marketUrls = [
    {
        frameUrl: '//market-click2.yandex.ru/redir/hash?text=https%3A%2F%2Ffarkop.ru%2Fcatalog',
        originalUrl: 'https://farkop.ru/catalog',
        displayUrl: 'https://yandex.ru/farkop.ru/turbo?text=category',
    },
];

let overlay: Overlay;

function getCurrentFrameSource() {
    return Frame.instances[0].getElement().contentWindow;
}

describe('Турбо-оверлей', () => {
    beforeEach(() => { mockWindow(); restoreDom() });
    afterEach(() => { overlay.destroy(); restoreDom(); restoreLocation() });
    afterAll(() => { clearNamespace(); restoreWindow() });

    it('Создается без ошибок', () => {
        overlay = new Overlay();
        expect(document.documentElement).toMatchSnapshot();
    });

    it('Показывается без записи в историю при открытии с флагом', () => {
        const initialHistoryLength = window.history.length;
        overlay = new Overlay();

        const onOverlayOpen = jest.fn();
        // @ts-ignore
        window.Ya.turboOverlay.onOverlayOpen = onOverlayOpen;

        expect(overlay.getIsOverlayOpen(), 'Оверлей сообщает, что открыт, пока еще нет').toBe(false);

        overlay.openOverlay({ urls }, false);

        expect(document.documentElement).toMatchSnapshot();
        expect(window.history.length, 'Есть записи в историю после показа оверлея')
            .toEqual(initialHistoryLength);

        expect(overlay.getIsOverlayOpen(), 'Оверлей не сообщает, что открыт').toBe(true);
        expect(onOverlayOpen.mock.calls.length, 'Хук не вызвался').toBe(1);
        expect(onOverlayOpen.mock.calls[0], 'Хук вызвался с неправильными параметрами').toEqual([{
            root: document.querySelector(`.${baseCls.block}`),
            urls: expectedUrls,
            index: 0,
        }]);
    });

    it('Открывается с записью в историю', () => {
        const initialHistoryLenght = window.history.length;
        overlay = new Overlay();
        expect(overlay.getIsOverlayOpen(), 'Оверлей сообщает, что открыт, пока еще нет').toBe(false);

        const onOverlayOpen = jest.fn();
        // @ts-ignore
        window.Ya.turboOverlay.onOverlayOpen = onOverlayOpen;

        overlay.openOverlay({ urls });

        expect(document.documentElement).toMatchSnapshot();
        expect(window.location.href, 'Не изменился урл после открытия оверлея')
            .toEqual('https://yandex.ru/turbo?text=test_news');
        expect(window.history.length, 'Нет записи в историю после открытия оверлея')
            .toEqual(initialHistoryLenght + 1);

        expect(overlay.getIsOverlayOpen(), 'Оверлей не сообщает, что открыт').toBe(true);
        expect(onOverlayOpen.mock.calls.length, 'Хук не вызвался').toBe(1);
        expect(onOverlayOpen.mock.calls[0], 'Хук вызвался с неправильными параметрами').toEqual([{
            root: document.querySelector(`.${baseCls.block}`),
            urls: expectedUrls,
            index: 0,
        }]);
    });

    it('Записывает в историю переданный урл, если он относительный', () => {
        const initialHistoryLenght = window.history.length;
        overlay = new Overlay();
        expect(overlay.getIsOverlayOpen(), 'Оверлей сообщает, что открыт, пока еще нет').toBe(false);

        const urls = [{
            originalUrl: 'test_news',
            displayUrl: '/turbo?text=test_news2',
            frameUrl: 'https://yandex.ru/turbo?text=test_news2&some-more-data',
        }];

        overlay.openOverlay({ urls });

        expect(window.location.href, 'Не изменился урл после открытия оверлея')
            .toEqual('https://yandex.ru/turbo?text=test_news2');
        expect(window.history.length, 'Нет записи в историю после открытия оверлея')
            .toEqual(initialHistoryLenght + 1);
    });

    it('Записывает в историю текущий урл, если хосты страницы и переданного урла отличаются', () => {
        const initialHistoryLenght = window.history.length;
        overlay = new Overlay();
        expect(overlay.getIsOverlayOpen(), 'Оверлей сообщает, что открыт, пока еще нет').toBe(false);

        const urls = [{
            originalUrl: 'hot_news',
            displayUrl: 'https://publisher-ru.turbopages.org/s/publisher.ru/hot-news',
            frameUrl: 'https://publisher-ru.turbopages.org/s/publisher.ru/hot-news&some-more-data',
        }];

        overlay.openOverlay({ urls });

        expect(window.history.length, 'Нет записи в историю после открытия оверлея')
            .toEqual(initialHistoryLenght + 1);
        expect(window.location.href, 'В историю записался не текущий урл')
            .toEqual('https://yandex.ru/');
    });

    it('Возращает корректные данные для хуков', () => {
        overlay = new Overlay();

        const meta = { record: 'some-meta-information' };
        overlay.openOverlay({ urls, meta });

        // @ts-ignore
        expect(overlay.getBaseHookData()).toEqual({
            root: document.querySelector(`.${baseCls.block}`),
            urls: expectedUrls,
            meta,
            index: 0,
        });
    });

    it('Скрывает и показывает шапку', async() => {
        overlay = new Overlay();

        // открываем оверлей
        overlay.openOverlay({ urls });

        expect(document.documentElement).toMatchSnapshot('Изначальное состояние');
        await postMsg({ data: { action: 'hide-overlay-header' }, source: getCurrentFrameSource() });

        expect(document.querySelector(`.${classicCls.overlayHeadVisible}`), 'Шапка не спряталась').toBe(null);
        expect(document.documentElement).toMatchSnapshot('Шапка спрятана');

        await postMsg({ data: { action: 'show-overlay-header' }, source: getCurrentFrameSource() });
        expect(document.querySelector(`.${classicCls.overlayHeadVisible}`), 'Шапка не показалась').not.toBe(null);
        expect(document.documentElement).toMatchSnapshot('Шапка показана');
    });

    it('Не вызывает сообщения, если ориджин некорректный', async() => {
        overlay = new Overlay();

        // открываем оверлей
        overlay.openOverlay({ urls });

        expect(document.documentElement).toMatchSnapshot('Изначальное состояние');
        await postMsg({ data: { action: 'hide-overlay-header' }, origin: 'https://google.com' });

        expect(document.querySelector(`.${classicCls.overlayHeadVisible}`), 'Шапка спряталась (хотя ничего не должно было произойти)')
            .not.toBe(null);
    });

    it('Открывается в режиме мультислайдера', () => {
        overlay = new Overlay();

        overlay.openOverlay({ urls: multipleUrls });

        expect(document.documentElement).toMatchSnapshot();
    });

    it('Открывает страницу с переданным index в режиме мультислайдера', () => {
        overlay = new Overlay();

        overlay.openOverlay({ urls: multipleUrls, index: 1 });

        expect(document.documentElement).toMatchSnapshot();
    });

    it('Устанавливает переданный title на место хоста в режиме мультислайдера', () => {
        overlay = new Overlay();

        overlay.openOverlay({ urls: multipleUrls, title: 'Заголовок' });

        expect(document.documentElement).toMatchSnapshot();
    });

    it('Передает события в навигатор', async() => {
        const navigator = {
            show: jest.fn(),
            navigateTurboInTurbo: jest.fn(),
            relocateTurboInTurbo: jest.fn(),
            doFallback: jest.fn(),
            activeDocumentChanged: jest.fn(),
            navigateAndroidDeeplink: jest.fn(),
            getCurrentCacheUrl: jest.fn().mockReturnValue('https://yandex.ru/turbo?text=test_news&some-more-data'),

            // функции, которые оверлей будет дергать, оставляем вместо них noop
            configure: noop,
            startFallbackCounter: noop,
        };

        // @ts-ignore мы не хотим реализовывать полный стаб навигатора
        overlay = new Overlay(navigator);
        overlay.openOverlay({ urls });

        const messageSource = (document.querySelector(`.${frameCls.frame}`) as HTMLFrameElement).contentWindow;
        const url = urls[0].frameUrl;

        const showParams = { action: 'show', hi: 'there', url };
        await postMsg({ data: showParams, source: messageSource });
        expect(navigator.show.mock.calls.length, 'Show не вызвался').toBe(1);
        expect(navigator.show.mock.calls[0], 'Show не вызвался').toEqual([showParams, messageSource]);

        const navigateParams = { action: 'navigate-turbo-link', hi: 'there1', url };
        await postMsg({ data: navigateParams, source: messageSource });
        expect(navigator.navigateTurboInTurbo.mock.calls.length, 'Navigate не вызвался').toBe(1);
        expect(navigator.navigateTurboInTurbo.mock.calls[0], 'Navigate не вызвался')
            .toEqual([navigateParams, messageSource]);

        const relocateParams = { action: 'replace-turbo-link', hi: 'there2', url };
        await postMsg({ data: relocateParams, source: messageSource });
        expect(navigator.relocateTurboInTurbo.mock.calls.length, 'Relocate не вызвался').toBe(1);
        expect(navigator.relocateTurboInTurbo.mock.calls[0], 'Relocate не вызвался')
            .toEqual([relocateParams, messageSource]);

        const fallbackParams = { action: 'fallback', hi: 'there3', url };
        await postMsg({ data: fallbackParams, source: messageSource });
        expect(navigator.doFallback.mock.calls.length, 'Fallback не вызвался').toBe(1);
        expect(navigator.doFallback.mock.calls[0], 'Fallback не вызвался').toEqual([fallbackParams, messageSource]);

        const documentChangeParams = { action: 'active-document-changed', hi: 'there4', url };
        await postMsg({ data: documentChangeParams, source: messageSource });
        expect(navigator.activeDocumentChanged.mock.calls.length, 'DocumentChange не вызвался').toBe(1);
        expect(navigator.activeDocumentChanged.mock.calls[0], 'DocumentChange не вызвался')
            .toEqual([documentChangeParams, messageSource]);

        const navigateAndroidDeeplinkParams = { action: 'navigate-android-deeplink', hi: 'there5', url };
        await postMsg({ data: navigateAndroidDeeplinkParams, source: messageSource });
        expect(navigator.navigateAndroidDeeplink.mock.calls.length, 'NavigateAndroidDeeplink не вызвался').toBe(1);
        expect(navigator.navigateAndroidDeeplink.mock.calls[0], 'NavigateAndroidDeeplink не вызвался')
            .toEqual([navigateAndroidDeeplinkParams, messageSource]);
    });

    it('Передает в навигатор события active-document-changed только из текущего iframe', async() => {
        const navigator = {
            activeDocumentChanged: jest.fn(),
            getCurrentCacheUrl: jest.fn().mockReturnValue('https://yandex.ru/turbo?text=test_news&some-more-data'),

            // функции, которые оверлей будет дергать, оставляем вместо них noop
            configure: noop,
            startFallbackCounter: noop,
        };

        // @ts-ignore мы не хотим реализовывать полный стаб навигатора
        overlay = new Overlay(navigator);
        overlay.openOverlay({ urls });

        const documentChangeParams = { action: 'active-document-changed', hi: 'there4' };

        const fakeMessageSource = {} as MessageEventSource;

        await postMsg({ data: documentChangeParams, source: fakeMessageSource });
        expect(navigator.activeDocumentChanged.mock.calls.length, 'DocumentChange вызвался для нетекущего фрейма')
            .toBe(0);

        const currentFrame = (document.querySelector(`.${frameCls.frame}`) as HTMLFrameElement);
        const currentFrameMessageSource = currentFrame.contentWindow;

        await postMsg({ data: documentChangeParams, source: currentFrameMessageSource });
        expect(navigator.activeDocumentChanged.mock.calls.length, 'DocumentChange не вызвался')
            .toBe(1);
        expect(navigator.activeDocumentChanged.mock.calls[0], 'DocumentChange не вызвался')
            .toEqual([documentChangeParams, currentFrameMessageSource]);
    });

    it('Реагирует на события истории на закрытие и открытие оверлея', async() => {
        overlay = new Overlay();
        expect(overlay.getIsOverlayOpen(), 'Оверлей сообщает, что открыт, пока еще нет').toBe(false);
        expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

        mockLocation('https://yandex.ru/turbo');
        await postHistoryEvent({ turboOverlayUrls: urls });
        expect(overlay.getIsOverlayOpen(), 'Не был открыт по событию в истории').toBe(true);
        expect(document.documentElement).toMatchSnapshot('Состояние после открытия');
        restoreLocation();

        mockLocation('https://yandex.ru');
        await postHistoryEvent({});
        expect(overlay.getIsOverlayOpen(), 'Не был закрыт по событию в истории').toBe(false);
        expect(document.documentElement).toMatchSnapshot('Состояние после открытия');
        restoreLocation();

        // не падает на null
        await postHistoryEvent(null);
        expect(overlay.getIsOverlayOpen(), 'Не был закрыт по событию в истории').toBe(false);
    });

    it('Фолбэчит по времени навигации', () => {
        jest.useFakeTimers();

        const { hrefFnMock, historyBackMock } = mockLocation();
        overlay = new Overlay();

        overlay.openOverlay({ urls });

        jest.runTimersToTime(5000);

        expect(historyBackMock.mock.calls.length).toBe(1);
        expect(historyBackMock.mock.calls[0]).toEqual([]);

        expect(hrefFnMock.mock.calls.length).toBe(1);
        expect(hrefFnMock.mock.calls[0]).toEqual(['test_news']);

        jest.useRealTimers();
    });

    it('Воссоздает оверлеи по событию в истории', async() => {
        overlay = new Overlay();
        expect(overlay.getIsOverlayOpen(), 'Оверлей сообщает, что открыт, пока еще нет').toBe(false);
        expect(document.documentElement).toMatchSnapshot('Изначальное состояние');

        // @ts-ignore
        overlay.navigatorInstance.recreateMode = true;

        overlay.openOverlay({ urls });

        await postHistoryEvent({ turboOverlayUrls: urls, activeUrl: 'https://yandex.ru/turbo?text=https://overlay.some' });
        expect(overlay.getIsOverlayOpen(), 'Не был открыт по событию в истории').toBe(true);
        expect(document.querySelector(`.${frameCls.frame}`)).not.toBeNull();
        expect(document.querySelector(`.${frameCls.frame}`).getAttribute('src'))
            .toBe('https://yandex.ru/turbo?text=https://overlay.some');
        expect(document.documentElement).toMatchSnapshot();

        await postHistoryEvent({ turboOverlayUrls: urls, activeUrl: 'https://yandex.ru/turbo?text=https://overlay.some-other' });
        expect(overlay.getIsOverlayOpen(), 'Не был открыт по событию в истории').toBe(true);
        expect(document.querySelector(`.${frameCls.frame}`)).not.toBeNull();
        expect(document.querySelector(`.${frameCls.frame}`).getAttribute('src'))
            .toBe('https://yandex.ru/turbo?text=https://overlay.some-other');
        expect(document.documentElement).toMatchSnapshot();
    });

    it('Показывает и прячет кнопку закрытия', () => {
        overlay = new Overlay();

        expect(window.history.state).toBeNull();
        expect(document.querySelector(`.${classicCls.closeButtonVisible}`), 'Крестик показан изначально').toBeNull();
        expect(document.documentElement).toMatchSnapshot();

        overlay.showOverlayCloseButton();
        expect(document.querySelector(`.${classicCls.closeButtonVisible}`), 'Крестик не показался').not.toBeNull();
        expect(document.documentElement).toMatchSnapshot();
        expect(window.history.state).toEqual({ closeButton: true });

        overlay.hideOverlayCloseButton();
        expect(document.querySelector(`.${classicCls.closeButtonVisible}`), 'Крестик не скрылся').toBeNull();
        expect(window.history.state).toEqual({ closeButton: false });

        expect(document.documentElement).toMatchSnapshot();
    });

    it('Показывает и скрывает крестик по событиям в истории', async() => {
        overlay = new Overlay();
        overlay.openOverlay({ urls: [{
            frameUrl: 'https://yandex.ru/turbo',
            displayUrl: 'https://yandex.ru/turbo',
            originalUrl: '',
        }] });

        expect(document.querySelector(`.${classicCls.closeButtonVisible}`), 'Крестик показан изначально').toBeNull();
        expect(document.documentElement).toMatchSnapshot();

        await postHistoryEvent({ turboOverlayUrls: [], closeButton: true });
        expect(document.querySelector(`.${classicCls.closeButtonVisible}`), 'Крестик не показался').not.toBeNull();
        expect(document.documentElement).toMatchSnapshot();

        await postHistoryEvent({ turboOverlayUrls: [] });
        expect(document.querySelector(`.${classicCls.closeButtonVisible}`), 'Крестик не скрылся').toBeNull();

        expect(document.documentElement).toMatchSnapshot();
    });

    it('Добавляет событие на клик по кнопке назад', async() => {
        overlay = new Overlay();
        const { historyBackMock } = mockLocation();

        overlay.openOverlay({ urls });

        (document.querySelector(`.${classicCls.backButton}`) as HTMLElement).click();
        expect(historyBackMock.mock.calls.length, 'функция не вызвалась').toBe(1);
    });

    it('Добавляет событие на клик по кнопке-крестику', async() => {
        overlay = new Overlay();
        const { historyGoMock } = mockLocation();

        overlay.openOverlay({ urls });
        overlay.showOverlayCloseButton();

        (document.querySelector(`.${classicCls.closeButton}`) as HTMLElement).click();
        expect(historyGoMock.mock.calls.length, 'функция не вызвалась').toBe(1);
        expect(historyGoMock.mock.calls[0], 'функция вызвалась с неправильными параметрами').toEqual([-1]);
    });

    it('Закрывает несколько турбо-страниц на клик по кнопке-крестику', async() => {
        overlay = new Overlay();
        const { historyGoMock } = mockLocation();

        overlay.openOverlay({ urls });
        overlay.showOverlayCloseButton();

        await postMsg({ data: { action: 'show', depth: 3, cleanUrl: urls[0].displayUrl, url: urls[0].frameUrl }, source: getCurrentFrameSource() });

        (document.querySelector(`.${classicCls.closeButton}`) as HTMLElement).click();
        expect(historyGoMock.mock.calls.length, 'функция не вызвалась').toBe(1);
        expect(historyGoMock.mock.calls[0], 'функция вызвалась с неправильными параметрами').toEqual([-4]);
    });

    it('Вызывает хуки при клике по кнопке назад', async() => {
        overlay = new Overlay();
        const { historyBackMock } = mockLocation();

        const hookMock = jest.fn();
        // @ts-ignore
        window.Ya.turboOverlay.onOverlayBackButtonClick = hookMock;

        overlay.openOverlay({ urls, meta: {} });

        (document.querySelector(`.${classicCls.backButton}`) as HTMLElement).click();
        expect(historyBackMock.mock.calls.length, 'функция не вызвалась').toBe(1);

        expect(hookMock.mock.calls.length).toEqual(1);
        expect(hookMock.mock.calls[0]).toEqual([{
            root: document.querySelector(`.${baseCls.block}`),
            urls: expectedUrls,
            index: 0,
            meta: {},
            depth: 0,
        }]);
    });

    it('Превентит шаг назад при вызове хуков по клику по кнопке назад', async() => {
        overlay = new Overlay();
        const { historyBackMock } = mockLocation();

        const hookMock = jest.fn().mockReturnValueOnce({ preventDefault: true });
        // @ts-ignore
        window.Ya.turboOverlay.onOverlayBackButtonClick = hookMock;

        overlay.openOverlay({ urls, meta: {} });

        (document.querySelector(`.${classicCls.backButton}`) as HTMLElement).click();
        expect(historyBackMock.mock.calls.length, 'функция вызвалась, хотя должна была запревенчина').toBe(0);

        expect(hookMock.mock.calls.length).toEqual(1);
        expect(hookMock.mock.calls[0]).toEqual([{
            root: document.querySelector(`.${baseCls.block}`),
            urls: expectedUrls,
            index: 0,
            meta: {},
            depth: 0,
        }]);
    });

    it('Показывает правильную глубину при вызове хуков по клику по кнопке назад', async() => {
        overlay = new Overlay();
        const { historyBackMock } = mockLocation();

        const hookMock = jest.fn();
        // @ts-ignore
        window.Ya.turboOverlay.onOverlayBackButtonClick = hookMock;

        overlay.openOverlay({ urls, meta: {} });

        await postMsg({ data: { action: 'show', depth: 3, cleanUrl: urls[0].displayUrl, url: urls[0].frameUrl }, source: getCurrentFrameSource() });

        (document.querySelector(`.${classicCls.backButton}`) as HTMLElement).click();
        expect(historyBackMock.mock.calls.length, 'функция не вызвалась').toBe(1);

        expect(hookMock.mock.calls.length).toEqual(1);
        expect(hookMock.mock.calls[0]).toEqual([{
            root: document.querySelector(`.${baseCls.block}`),
            urls: expectedUrls,
            index: 0,
            meta: {},
            depth: 3,
        }]);
    });

    it('Вызывает хуки при клике по крестику', async() => {
        overlay = new Overlay();
        const { historyGoMock } = mockLocation();

        const hookMock = jest.fn();
        // @ts-ignore
        window.Ya.turboOverlay.onOverlayCloseButtonClick = hookMock;

        overlay.openOverlay({ urls, meta: {} });

        (document.querySelector(`.${classicCls.closeButton}`) as HTMLElement).click();
        expect(historyGoMock.mock.calls.length, 'функция не вызвалась').toBe(1);
        expect(historyGoMock.mock.calls[0], 'функция вызвалась с неправильными параметрами').toEqual([-1]);

        expect(hookMock.mock.calls.length).toEqual(1);
        expect(hookMock.mock.calls[0]).toEqual([{
            root: document.querySelector(`.${baseCls.block}`),
            urls: expectedUrls,
            index: 0,
            meta: {},
            depth: 0,
        }]);
    });

    it('Превентит шаг назад при вызове хуков по клику по крестику', async() => {
        overlay = new Overlay();
        const { historyGoMock } = mockLocation();

        const hookMock = jest.fn().mockReturnValueOnce({ preventDefault: true });
        // @ts-ignore
        window.Ya.turboOverlay.onOverlayCloseButtonClick = hookMock;

        overlay.openOverlay({ urls, meta: {} });

        (document.querySelector(`.${classicCls.closeButton}`) as HTMLElement).click();
        expect(historyGoMock.mock.calls.length, 'функция вызвалась, хотя должна была запревенчина').toBe(0);

        expect(hookMock.mock.calls.length).toEqual(1);
        expect(hookMock.mock.calls[0]).toEqual([{
            root: document.querySelector(`.${baseCls.block}`),
            urls: expectedUrls,
            index: 0,
            meta: {},
            depth: 0,
        }]);
    });

    it('Показывает правильную глубину при вызове хуков по клику по крестику', async() => {
        overlay = new Overlay();
        const { historyGoMock } = mockLocation();

        const hookMock = jest.fn();
        // @ts-ignore
        window.Ya.turboOverlay.onOverlayCloseButtonClick = hookMock;

        overlay.openOverlay({ urls, meta: {} });

        await postMsg({ data: { action: 'show', depth: 3, cleanUrl: urls[0].displayUrl, url: urls[0].frameUrl }, source: getCurrentFrameSource() });

        (document.querySelector(`.${classicCls.closeButton}`) as HTMLElement).click();
        expect(historyGoMock.mock.calls.length, 'функция не вызвалась').toBe(1);
        expect(historyGoMock.mock.calls[0], 'функция вызвалась с неправильными параметрами').toEqual([-4]);

        expect(hookMock.mock.calls.length).toEqual(1);
        expect(hookMock.mock.calls[0]).toEqual([{
            root: document.querySelector(`.${baseCls.block}`),
            urls: expectedUrls,
            index: 0,
            meta: {},
            depth: 3,
        }]);
    });

    it('Перезагружает страницу, если видит что запись в истории битая', async() => {
        overlay = new Overlay();
        history.replaceState({}, '', 'https://yandex.ru/turbo?text=about');

        const { replaceMock } = mockLocation();

        await postHistoryEvent({});
        expect(replaceMock.mock.calls, 'reload не вызвался').toEqual([['https://yandex.ru/turbo?text=about']]);
    });

    it('Вызывает хук и не перезагружает страницу, если хук вернул preventDefault', async() => {
        overlay = new Overlay();
        history.replaceState({}, '', 'https://yandex.ru/turbo?text=about');

        const { replaceMock } = mockLocation();

        const hookMock = jest.fn().mockReturnValueOnce({ preventDefault: true });
        // @ts-ignore
        window.Ya.turboOverlay.onOverlayBrokenHistoryRecord = hookMock;

        await postHistoryEvent({});
        expect(replaceMock.mock.calls.length, 'reload вызвался').toBe(0);
    });

    it('Вызывает addOverrideHostEntries по сообщению override-host', async() => {
        const addOverrideHostEntriesSpy = jest.spyOn(overrideHostUtil, 'addOverrideHostEntries')
            .mockImplementation(() => {});

        overlay = new Overlay();

        overlay.openOverlay({ urls });

        expect(addOverrideHostEntriesSpy, 'Метод подмены домена не ожидаемо был вызван до поступления сообщения')
            .toHaveBeenCalledTimes(0);

        const overrideHostParams = {
            action: 'override-host',
            cleanUrl: '/turbo/text=123',
            displayUrl: 'https://test.example.com/1',
            displayHost: 'test.example.com',
        };
        await postMsg({ data: overrideHostParams, source: getCurrentFrameSource() });

        expect(addOverrideHostEntriesSpy, 'Метод подмены домена не вызвался несмотря на пришедшее сообщение')
            .toHaveBeenCalledTimes(1);
        expect(addOverrideHostEntriesSpy, 'Метод подмены домена был вызван с неверным набором параметров')
            .toHaveBeenCalledWith({
                keyUrl: 'https://yandex.ru/turbo/text=123',
                displayUrl: 'https://test.example.com/1',
                displayHost: 'test.example.com',
            });

        addOverrideHostEntriesSpy.mockRestore();
    });

    describe('Платформоспецефичные фичи', () => {
        const originalUserAgentParams = userAgentParams;

        afterEach(() => { Object.assign(userAgentParams, originalUserAgentParams) });

        it('Добавляет скрипт для API в Android ПП', () => {
            userAgentParams.isAndroid = true;
            userAgentParams.isSearchApp = true;

            overlay = new Overlay();

            const searchAppScript = document.querySelector(
                'script[src*="yastatic.net/yandex-apps-api/__webview-intercepted/_api.js"]'
            );

            expect(searchAppScript, 'Не добавился скрипт для API в Android ПП').not.toBe(null);
        });

        it('Не добавляет скрипт для API платформы не в Android ПП', () => {
            userAgentParams.isAndroid = false;
            userAgentParams.isSearchApp = false;

            overlay = new Overlay();

            const searchAppScript = document.querySelector(
                'script[src*="yastatic.net/yandex-apps-api/__webview-intercepted/_api.js"]'
            );

            expect(searchAppScript, 'Добавился скрипт для API НЕ в Android ПП').toBe(null);
        });

        it('Не добавляет скрипт для API платформы в iOS ПП', () => {
            userAgentParams.isAndroid = false;
            userAgentParams.isIos = true;
            userAgentParams.isSearchApp = true;

            overlay = new Overlay();

            const searchAppScript = document.querySelector(
                'script[src*="yastatic.net/yandex-apps-api/__webview-intercepted/_api.js"]'
            );

            expect(searchAppScript, 'Добавился скрипт для API в iOS ПП').toBe(null);
        });

        it('Не добавляет скрипт для API платформы не ПП на Android', () => {
            userAgentParams.isAndroid = true;
            userAgentParams.isSearchApp = false;

            overlay = new Overlay();

            const searchAppScript = document.querySelector(
                'script[src*="yastatic.net/yandex-apps-api/__webview-intercepted/_api.js"]'
            );

            expect(searchAppScript, 'Добавился скрипт для API в не ПП на Android').toBe(null);
        });

        it('Не добавляет скрипт для API платформы ПП на Android на базе Я.Бро', () => {
            userAgentParams.isAndroid = true;
            userAgentParams.isSearchApp = true;
            userAgentParams.isSearchAppBasedOnYaBro = true;

            overlay = new Overlay();

            const searchAppScript = document.querySelector(
                'script[src*="yastatic.net/yandex-apps-api/__webview-intercepted/_api.js"]'
            );

            expect(searchAppScript, 'Добавился скрипт для API в ПП на Android на базе Я.Бро').toBe(null);
        });

        it('Вызывает API входа и выхода из fullscreen при открытии и закрытии Оверлея, если API доступно', async() => {
            window.YandexApplicationsAPIBackend = {
                openFullScreen: jest.fn(),
                closeFullScreen: jest.fn(),
            };

            userAgentParams.isAndroid = true;
            userAgentParams.isSearchApp = true;

            overlay = new Overlay();

            expect(
                window.YandexApplicationsAPIBackend.openFullScreen,
                'Был открыт fullscreen до открытия Оверлея'
            ).toHaveBeenCalledTimes(0);
            expect(
                window.YandexApplicationsAPIBackend.closeFullScreen,
                'Был вызван метод выхода из fullscreen до открытия Оверлея'
            ).toHaveBeenCalledTimes(0);

            overlay.openOverlay({ urls }, false);

            expect(
                window.YandexApplicationsAPIBackend.openFullScreen,
                'После открытия Оверлея не был вызван метод перехода в fullscreen'
            ).toHaveBeenCalledTimes(1);
            expect(
                window.YandexApplicationsAPIBackend.closeFullScreen,
                'Был вызван метод выхода из fullscreen до закрытия оверлея'
            ).toHaveBeenCalledTimes(0);

            await postHistoryEvent({});

            expect(
                window.YandexApplicationsAPIBackend.openFullScreen,
                'После закрытия Оверлея метод перехода в fullscreen был вызван лишний раз'
            ).toHaveBeenCalledTimes(1);
            expect(
                window.YandexApplicationsAPIBackend.closeFullScreen,
                'После закрытия Оверлея не был вызван метод выхода из fullscreen'
            ).toHaveBeenCalledTimes(1);

            delete window.YandexApplicationsAPIBackend;
        });
    });

    describe('Открытие оверлея на iOS', () => {
        const originalUserAgentParams = userAgentParams;

        beforeEach(() => {
            userAgentParams.isIos = true;
            market.isInMarket = false;
        });

        afterEach(() => { Object.assign(userAgentParams, originalUserAgentParams) });

        it('На iOS 12.0 - 12.3 открывает страницу в отдельном окне с корректным урлом, ', () => {
            userAgentParams.isBuggyIos12 = true;

            overlay = new Overlay();

            overlay.openOverlay({ urls, openIos12InNewTab: true }, true);

            expect(window.open).toBeCalledWith('https://yandex.ru/turbo?text=test_news&some-more-data&new_overlay=1', '_blank');
        });

        it('На других версиях iOS не открывает страницу в окне', () => {
            overlay = new Overlay();

            overlay.openOverlay({ urls }, true);

            expect(window.open).not.toBeCalled();
        });

        it('На других версиях iOS в Маркете открывает страницу в отдельном окне', () => {
            market.isInMarket = true;

            overlay = new Overlay();

            overlay.openOverlay({ urls: marketUrls }, true);

            expect(window.open).toBeCalledWith('//market-click2.yandex.ru/redir/hash?text=https%3A%2F%2Ffarkop.ru%2Fcatalog&new_overlay=1', '_blank');
        });
    });

    describe('В ПП', () => {
        const originalUserAgentParams = userAgentParams;

        beforeEach(() => {
            userAgentParams.isSearchApp = true;
            market.isInMarket = false;
        });

        afterEach(() => { Object.assign(userAgentParams, originalUserAgentParams) });

        it('Открывает страницу в отдельном окне в Маркете', () => {
            market.isInMarket = true;

            overlay = new Overlay();

            overlay.openOverlay({ urls }, true);

            expect(window.open).toBeCalledWith('https://yandex.ru/turbo?text=test_news&some-more-data&new_overlay=1', '_blank');
        });

        it('Открывает страницу в оверлее на других сервисах', () => {
            overlay = new Overlay();

            overlay.openOverlay({ urls }, true);

            expect(window.open).not.toBeCalled();
        });
    });

    describe('Режим Drawer-шторки (у тега script есть атрибут view-type="drawer")', () => {
        let createRootsSpy: jest.SpyInstance;

        beforeAll(() => {
            jest.spyOn(getScriptParamUtil, 'getScriptParam').mockImplementation(() => 'drawer');
            createRootsSpy = jest.spyOn(createRootsModule, 'createRoots');
        });
        afterEach(() => { jest.clearAllMocks() });
        afterAll(() => { jest.restoreAllMocks() });

        it('В разметке рендерятся два вида отображения', () => {
            overlay = new Overlay();
            expect(document.documentElement).toMatchSnapshot();
        });

        it('Показывает и прячет кнопку закрытия в обоих отображениях', () => {
            overlay = new Overlay();

            expect(createRootsSpy).toHaveBeenCalledTimes(1);
            const { singlepage, multipage } = createRootsSpy.mock.results[0].value;

            const singlepageShowCloseButtonElementSpy = jest.spyOn(singlepage, 'showCloseButtonElement');
            const singlepageHideCloseButtonElementSpy = jest.spyOn(singlepage, 'hideCloseButtonElement');

            const multipageShowCloseButtonElementSpy = jest.spyOn(multipage, 'showCloseButtonElement');
            const multipageHideCloseButtonElementSpy = jest.spyOn(multipage, 'hideCloseButtonElement');

            expect(singlepageShowCloseButtonElementSpy).toHaveBeenCalledTimes(0);
            expect(multipageShowCloseButtonElementSpy).toHaveBeenCalledTimes(0);
            expect(singlepageHideCloseButtonElementSpy).toHaveBeenCalledTimes(0);
            expect(multipageHideCloseButtonElementSpy).toHaveBeenCalledTimes(0);

            overlay.showOverlayCloseButton();

            expect(singlepageShowCloseButtonElementSpy).toHaveBeenCalledTimes(1);
            expect(multipageShowCloseButtonElementSpy).toHaveBeenCalledTimes(1);
            expect(singlepageHideCloseButtonElementSpy).toHaveBeenCalledTimes(0);
            expect(multipageHideCloseButtonElementSpy).toHaveBeenCalledTimes(0);

            overlay.hideOverlayCloseButton();

            expect(singlepageShowCloseButtonElementSpy).toHaveBeenCalledTimes(1);
            expect(multipageShowCloseButtonElementSpy).toHaveBeenCalledTimes(1);
            expect(singlepageHideCloseButtonElementSpy).toHaveBeenCalledTimes(1);
            expect(multipageHideCloseButtonElementSpy).toHaveBeenCalledTimes(1);
        });

        it('Задействует отображение singlepage, если передан один url в openOverlay', () => {
            overlay = new Overlay();

            expect(createRootsSpy).toHaveBeenCalledTimes(1);
            const { singlepage, multipage } = createRootsSpy.mock.results[0].value;

            const singlepageSetConfigSpy = jest.spyOn(singlepage, 'setConfig');
            const singlepageAppendFrameSpy = jest.spyOn(singlepage, 'appendFrame');
            const singlepageShowFrameSpy = jest.spyOn(singlepage, 'showFrame');
            const singlepageShowOverlaySpy = jest.spyOn(singlepage, 'showOverlay');

            const multipageShowFrameSpy = jest.spyOn(multipage, 'showFrame');
            const multipageAppendFrameSpy = jest.spyOn(multipage, 'appendFrame');
            const multipageSetConfigSpy = jest.spyOn(multipage, 'setConfig');
            const multipageShowOverlaySpy = jest.spyOn(multipage, 'showOverlay');

            overlay.openOverlay({ urls });

            expect(singlepageSetConfigSpy).toHaveBeenCalledTimes(1);
            expect(singlepageAppendFrameSpy).toHaveBeenCalledTimes(1);
            expect(singlepageShowFrameSpy).toHaveBeenCalledTimes(1);
            expect(singlepageShowOverlaySpy).toHaveBeenCalledTimes(1);
            expect(multipageShowFrameSpy).toHaveBeenCalledTimes(0);
            expect(multipageAppendFrameSpy).toHaveBeenCalledTimes(0);
            expect(multipageSetConfigSpy).toHaveBeenCalledTimes(0);
            expect(multipageShowOverlaySpy).toHaveBeenCalledTimes(0);
        });

        it('Задействует отображение multipage, если передано несколько url\'ов в openOverlay', () => {
            overlay = new Overlay();

            expect(createRootsSpy).toHaveBeenCalledTimes(1);
            const { singlepage, multipage } = createRootsSpy.mock.results[0].value;

            const singlepageSetConfigSpy = jest.spyOn(singlepage, 'setConfig');
            const singlepageAppendFrameSpy = jest.spyOn(singlepage, 'appendFrame');
            const singlepageShowFrameSpy = jest.spyOn(singlepage, 'showFrame');
            const singlepageShowOverlaySpy = jest.spyOn(singlepage, 'showOverlay');

            const multipageShowFrameSpy = jest.spyOn(multipage, 'showFrame');
            const multipageAppendFrameSpy = jest.spyOn(multipage, 'appendFrame');
            const multipageSetConfigSpy = jest.spyOn(multipage, 'setConfig');
            const multipageShowOverlaySpy = jest.spyOn(multipage, 'showOverlay');

            overlay.openOverlay({ urls: multipleUrls });

            expect(singlepageSetConfigSpy).toHaveBeenCalledTimes(0);
            expect(singlepageAppendFrameSpy).toHaveBeenCalledTimes(0);
            expect(singlepageShowFrameSpy).toHaveBeenCalledTimes(0);
            expect(singlepageShowOverlaySpy).toHaveBeenCalledTimes(0);
            expect(multipageShowFrameSpy).toHaveBeenCalledTimes(1);
            expect(multipageAppendFrameSpy).toHaveBeenCalledTimes(1);
            expect(multipageSetConfigSpy).toHaveBeenCalledTimes(1);
            expect(multipageShowOverlaySpy).toHaveBeenCalledTimes(1);
        });
    });
});
