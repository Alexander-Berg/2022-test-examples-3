const OverrideHostDataProcessor = require('../../core/utils/override-host-data-processor');
const { SERVICES } = require('../../core/utils/config');

describe('OverrideHostDataProcessor', () => {
    test('Корректно забирает url источника из данных', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            doc: { url: 'https://example.com' },
        });

        expect(overrideHostInstance.originalDocUrl).toEqual('https://example.com');
    });

    test('Корректно забирает url источника из reqdata с legacy схемой урлов', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://yandex.ru/turbo?text=https://example.com' },
        });

        expect(overrideHostInstance.originalDocUrl).toEqual('https://example.com');
    });

    test('Корректно забирает url источника из reqdata с turbopages.org', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://ria-ru.turbopages.org/s/ria.ru/20200409/1569820014.html' },
        });

        expect(overrideHostInstance.originalDocUrl).toEqual('https://ria.ru/20200409/1569820014.html');
    });

    test('Корректно забирает url источника из reqdata с красивыми урлами', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://yandex.ru/turbo/s/ria.ru/20200409/1569820014.html' },
        });

        expect(overrideHostInstance.originalDocUrl).toEqual('https://ria.ru/20200409/1569820014.html');
    });

    test('Корректно забирает url источника из reqdata с turbopages.org для автоморд', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://ria-ru.turbopages.org/automainhttp/newsru.com' },
        });

        expect(overrideHostInstance.originalDocUrl).toEqual('http://newsru.com');
    });

    test('Корректно забирает url источника из reqdata с красивыми урлами для автоморд', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://yandex.ru/turbo/automainhttp/newsru.com' },
        });

        expect(overrideHostInstance.originalDocUrl).toEqual('http://newsru.com');
    });

    const schemes = {
        s: 'https',
        h: 'http',
        n: '',
        automainhttp: 'http',
        automainhttps: 'https',
    };

    Object.keys(schemes).forEach(prefix => {
        test(`Корректно обрабатывает технический параметр ${prefix} при парсинге`, () => {
            const overrideHostInstance = new OverrideHostDataProcessor({
                reqdata: { url: `https://yandex.ru/turbo/${prefix}/newsru.com` },
            });

            const scheme = schemes[prefix];
            const protocol = prefix === 'n' ? '' : `${scheme}://`;
            expect(overrideHostInstance.originalDocUrl).toEqual(`${protocol}newsru.com`);
        });
    });

    test('getDisplayUrl корректно возвращает переданный в данных turbopages_url', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://example.ru/news/2020/01/14/chilli/' },
            serviceName: SERVICES.PUBLISHERS,
            turbopages_url: 'https://example-ru.turbopages.org/example.ru/s/news/2020/01/14/chilli/',
        });

        expect(overrideHostInstance.getDisplayUrl())
            .toEqual('https://example-ru.turbopages.org/example.ru/s/news/2020/01/14/chilli/');
    });

    test('getDisplayUrl возвращает кастомный домен в приоритете над другими', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            doc: {
                url: 'https://www.gazeta.ru/social/news/2020/03/17/n_14168641.shtml',
            },
            reqdata: { url: 'https://yandex.ru/turbo?text=https%3A%2F%2Fwww.gazeta.ru%2Fsocial%2Fnews%2F2020%2F03%2F17%2Fn_14168641.shtml' },
        });

        expect(overrideHostInstance.getDisplayUrl())
            .toEqual('https://turbo.gazeta.ru/social/news/2020/03/17/n_14168641.shtml');
    });

    test('getDisplayUrl возвращает ссылку на turbopages.org из данных', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://yandex.ru/turbo?text=https%3A%2F%2Fexample.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F' },
            serviceName: SERVICES.PUBLISHERS,
            turbopages_url: 'https://example-ru.turbopages.org/s/example.ru/news/2020/01/14/chilli/',
        });

        expect(overrideHostInstance.getDisplayUrl())
            .toEqual('https://example-ru.turbopages.org/s/example.ru/news/2020/01/14/chilli/');
    });

    test('getDisplayUrl не возвращает ссылку на turbopages.org из данных если это не PUBLISHER', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://yandex.ru/turbo?text=https%3A%2F%2Fexample.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F' },
            turbopages_url: 'https://example-ru.turbopages.org/s/example.ru/news/2020/01/14/chilli/',
        });

        expect(overrideHostInstance.getDisplayUrl())
            .toEqual('https://yandex.ru/turbo?text=https%3A%2F%2Fexample.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F');
    });

    test('getDisplayUrl возвращает ссылку на turbopages.org из данных и пробрасывает параметры', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://yandex.ru/turbo?text=https%3A%2F%2Fexample.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F&exp_flags=shiny_flag=1' },
            serviceName: SERVICES.PUBLISHERS,
            turbopages_url: 'https://example-ru.turbopages.org/s/example.ru/news/2020/01/14/chilli/?pcgi=foo=bar',
        });

        expect(overrideHostInstance.getDisplayUrl())
            .toEqual('https://example-ru.turbopages.org/s/example.ru/news/2020/01/14/chilli/?exp_flags=shiny_flag%3D1&pcgi=foo%3Dbar');
    });

    test('getDisplayUrl возвращает ссылку на turbopages.org из данных и вырезает check_swipe', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://yandex.ru/turbo?text=https%3A%2F%2Fexample.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F&check_swipe=1' },
            serviceName: SERVICES.PUBLISHERS,
            turbopages_url: 'https://example-ru.turbopages.org/s/example.ru/news/2020/01/14/chilli/',
        });

        expect(overrideHostInstance.getDisplayUrl())
            .toEqual('https://example-ru.turbopages.org/s/example.ru/news/2020/01/14/chilli/');
    });

    test('getDisplayUrl вырезает check_swipe', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://yandex.ru/turbo?text=https%3A%2F%2Fexample.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F&check_swipe=1' },
        });

        expect(overrideHostInstance.getDisplayUrl())
            .toEqual('https://yandex.ru/turbo?text=https%3A%2F%2Fexample.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F');
    });

    test('getDisplayHost возвращает хост из похостовых', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            doc: { url: 'https://www.gazeta.ru/social/news/2020/03/17/n_14168641.shtml' },
            reqdata: {
                hostSettings: {
                    host: 'test.host',
                },
            },
        });

        expect(overrideHostInstance.getDisplayHost())
            .toEqual('test.host');
    });

    test('getDisplayHost возвращает хост из оригинального урла', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            doc: { url: 'https://gazeta.ru/social/news/2020/03/17/n_14168641.shtml' },
            reqdata: {},
        });

        expect(overrideHostInstance.getDisplayHost())
            .toEqual('gazeta.ru');
    });

    test('getDisplayHost канонизирует хост', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            doc: { url: 'https://www.m.gazeta.ru/social/news/2020/03/17/n_14168641.shtml' },
            reqdata: {},
        });

        expect(overrideHostInstance.getDisplayHost())
            .toEqual('gazeta.ru');
    });

    test('getKeys возвращает грязный и очищенные урлы', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://yandex.ru/turbo?text=https%3A%2F%2Flenta.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F&turbo_uid=131231' },
        });

        expect(overrideHostInstance.getKeys())
            .toEqual([
                'https://yandex.ru/turbo?text=https%3A%2F%2Flenta.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F&turbo_uid=131231',
                'https://yandex.ru/turbo?text=https%3A%2F%2Flenta.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F',
            ]);
    });

    test('getKeys возвращает один урл, если грязный и очищенный совпадают', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: { url: 'https://yandex.ru/turbo?text=https%3A%2F%2Flenta.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F' },
        });

        expect(overrideHostInstance.getKeys())
            .toEqual([
                'https://yandex.ru/turbo?text=https%3A%2F%2Flenta.ru%2Fnews%2F2020%2F01%2F14%2Fchilli%2F',
            ]);
    });

    test('getKeys возвращает URL для серпа', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: {
                url: 'https://lenta-ru.turbopages.org/s/lenta.ru/news/2020/01/14/chilli/',
            },
        });

        expect(overrideHostInstance.getKeys())
            .toEqual([
                'https://lenta-ru.turbopages.org/s/lenta.ru/news/2020/01/14/chilli/',
                'https://yandex.ru/turbo/s/lenta.ru/news/2020/01/14/chilli/',
                'https://lenta-ru.turbopages.org/turbo/s/lenta.ru/news/2020/01/14/chilli/',
            ]);
    });

    test('getKeys возвращает URL для серпа с правильным tld', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: {
                url: 'https://lenta-ru.turbopages.org/s/lenta.ru/news/2020/01/14/chilli/',
            },
            httpHeaders: {
                referer: 'https://yandex.ua',
            },
        });

        expect(overrideHostInstance.getKeys())
            .toEqual([
                'https://lenta-ru.turbopages.org/s/lenta.ru/news/2020/01/14/chilli/',
                'https://yandex.ua/turbo/s/lenta.ru/news/2020/01/14/chilli/',
                'https://lenta-ru.turbopages.org/turbo/s/lenta.ru/news/2020/01/14/chilli/',
            ]);
    });

    test('getKeys обрабатывает турецкий tld из referer при создании serpUrl', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: {
                url: 'https://lenta-ru.turbopages.org/s/lenta.ru/news/2020/01/14/chilli/',
            },
            httpHeaders: {
                referer: 'https://yandex.com.tr',
            },
        });

        expect(overrideHostInstance.getKeys())
            .toEqual([
                'https://lenta-ru.turbopages.org/s/lenta.ru/news/2020/01/14/chilli/',
                'https://yandex.com.tr/turbo/s/lenta.ru/news/2020/01/14/chilli/',
                'https://lenta-ru.turbopages.org/turbo/s/lenta.ru/news/2020/01/14/chilli/',
            ]);
    });

    test('getKeys отрезает /turbo/ из урла для turbopages.org', () => {
        const overrideHostInstance = new OverrideHostDataProcessor({
            reqdata: {
                url: 'https://lenta-ru.turbopages.org/turbo/s/lenta.ru/news/2020/01/14/chilli/',
            },
        });

        expect(overrideHostInstance.getKeys())
            .toEqual([
                'https://lenta-ru.turbopages.org/s/lenta.ru/news/2020/01/14/chilli/',
                'https://yandex.ru/turbo/s/lenta.ru/news/2020/01/14/chilli/',
                'https://lenta-ru.turbopages.org/turbo/s/lenta.ru/news/2020/01/14/chilli/',
            ]);
    });
});
