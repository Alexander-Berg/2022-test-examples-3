import { getPage, buildURLByPageName } from '..';

jest.mock('../config', () => ({
    routes: [
        {
            name: 'test',
            pattern: '/path/to',
            data: {
                layout: 'getLayout',
            },
        },
        {
            name: 'test-anchor',
            pattern: '/path/from',
            data: {
                requestParams: {
                    anchor: '#scroll-here',
                },
            },
        },
    ],
}));

describe('Router', () => {
    describe('getPage', () => {
        it('должен возвращать распаршенные параметры урла и данные роута', () => {
            const pageDecl = getPage('/path/to?query=1');

            expect(pageDecl).toEqual({
                pageName: 'test',
                pageData: { layout: 'getLayout' },
                parsedParams: {
                    query: '1',
                },
            });
        });

        it('должен кидать исключение если роут страницы не найден', () => {
            const url = 'https://some.com/one/two';

            expect(() => getPage(url)).toThrowError(`Route not found ${url}`);
        });
    });

    describe('buildURLByPageName', () => {
        it('должен корректно находить и строить маршрут по имени', () => {
            const params = { p1: 'param1' };
            const pageName = 'test';

            expect(buildURLByPageName(pageName, params))
                .toEqual('https://m.market.yandex.ru/path/to?p1=param1');
        });

        it('должен корректно добавлять hash если он задан в config', () => {
            const params = { p1: 'param1' };
            const pageName = 'test-anchor';

            expect(buildURLByPageName(pageName, params))
                .toEqual('https://m.market.yandex.ru/path/from?p1=param1#scroll-here');
        });

        it('должен возвращать "" если маршрут c искомым именем не зарегистрирован', () => {
            const pageName = 'unknown';

            // @ts-ignore инорим для теста, так как на входе enum
            expect(buildURLByPageName(pageName)).toEqual('');
        });
    });
});
