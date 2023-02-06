import { createRoots } from '../Root/createRoots';
import { Navigator } from '../Navigator/Navigator';

export let roots = createRoots();
export const getRootInstance = () => {
    return roots.singlepage;
};
export const getBaseHookData = () => ({
    urls: [{
        frameUrl: 'https://yandex.ru/turbo?text=https://test_news.com&parent-reqid=1000',
        displayUrl: 'https://yandex.ru/turbo?text=https://test_news.com',
        originalUrl: 'https://test_news.com',
    }],
    root: roots.singlepage.getRootNode(),
    index: 100,
});
export const navigator = new Navigator({ getRootInstance, getBaseHookData });

let oldLocation = window.location;
let oldHistoryBack = window.history.back;
let oldHistoryGo = window.history.go;

let oldWindowOpen = window.open;

export const mockLocation = (
    mockedHref: string = location.href,
    mockedHash: string = location.hash,
) => {
    const hrefFnMock = jest.fn();
    const hashFnMock = jest.fn();
    const historyBackMock = jest.fn();
    const historyGoMock = jest.fn();
    const reloadMock = jest.fn();
    const replaceMock = jest.fn();

    oldLocation = window.location;
    oldHistoryBack = window.history.back;
    oldHistoryGo = window.history.go;

    delete window.location;

    // @ts-ignore делаем стремную магию, так как location.href нет в jsdom
    window.location = {
        origin: oldLocation.origin,
        replace: replaceMock,
        reload: reloadMock,
        set href(data: string) { hrefFnMock(data) },
        get href() { return mockedHref },
        set hash(data: string) { hashFnMock(data) },
        get hash() { return mockedHash },
    };
    window.history.back = historyBackMock;
    window.history.go = historyGoMock;

    return { hrefFnMock, hashFnMock, historyBackMock, historyGoMock, reloadMock, replaceMock };
};

export const restoreLocation = () => {
    window.location = oldLocation;
    window.history.back = oldHistoryBack;
    window.history.go = oldHistoryGo;
};

export const mockOverlayInited = () => {
    roots = createRoots();
};

export const restoreDom = () => {
    restoreLocation();

    // @ts-ignore
    global.jsdom.reconfigure({
        url: 'https://yandex.ru/',
    });

    document.body.innerHTML = '';
    document.head.innerHTML = '';
    window.history.replaceState(null, 'Яндекс', 'https://yandex.ru');
    window.Ya = {};
    window.Ya.turboOverlay = {};
    document.title = 'Яндекс';
};

export const clearNamespace = () => {
    delete window.Ya.turboOverlay;
};

export const mockWindow = () => {
    oldWindowOpen = window.open;
    window.open = jest.fn();

    window.matchMedia = jest.fn().mockReturnValue({ matches: false });

    window.yandex = { publicFeature: {} };
};

export const restoreWindow = () => {
    delete window.matchMedia;
    delete window.yandex;

    window.open = oldWindowOpen;
};
