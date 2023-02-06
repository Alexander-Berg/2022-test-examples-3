import { DataProvider, ILoadOptions } from '../data-provider';

interface IFakeResponse {
    json: () => Record<string, string>;
}
type TFakeFetch = jest.Mock<Promise<IFakeResponse>, [string]>;

describe('DataProvider', function() {
    let testGlobal = global as NodeJS.Global & { fetch: TFakeFetch };
    let dataProvider: DataProvider<{ html: string }>;
    let cacheGet: jest.Mock;
    let cacheUpdate: jest.Mock;
    let originFetch = testGlobal.fetch;

    function makeFakeResponse(content?: string): Promise<IFakeResponse> {
        return new Promise(resolve => resolve({
            json: () => JSON.parse(content),
        }));
    }

    afterAll(() => {
        testGlobal.fetch = originFetch;
    });

    beforeEach(() => {
        cacheGet = jest.fn(() => Promise.reject());
        cacheUpdate = jest.fn();
        testGlobal.fetch = jest.fn();
        dataProvider = new DataProvider({
            cache: {
                add: jest.fn(),
                remove: jest.fn(),
                get: cacheGet,
                update: cacheUpdate,
            },
            parentReqId: 'req-test-id',
        });
    });

    it('getCacheKey() корректно обрабатывает ссылку', () => {
        const url = 'https://yandex.ru/turbo?parent-reqid=1580661391291960-939131719299062402300116-man1-3469&text=https%3A%2F%2Fyandex.ru%2Fpogoda%2Fmoscow%2Fmonth%2F%3Fvia%3Dcnav';

        expect(DataProvider.getCacheKey(url)).toStrictEqual('https://yandex.ru/turbo?text=https%3A%2F%2Fyandex.ru%2Fpogoda%2Fmoscow%2Fmonth%2F%3Fvia%3Dcnav');
    });

    describe('loadPage()', () => {
        const url = 'https://yandex.ru/turbo';

        let options: ILoadOptions;

        beforeEach(() => {
            options = {
                pageBundles: ['TestBundle'],
                pageIcons: ['test-icon'],
                ttl: 6000,
            };

            testGlobal.fetch.mockReturnValue(makeFakeResponse('{ "html": "<h1>data-from-fetch</h1>"}'));
        });

        it('Возвращает данные из кэша, если они существуют', async() => {
            cacheGet.mockReturnValue(Promise.resolve({ data: { html: '<h1>data-from-cache</h1>' } }));
            const data = await dataProvider.loadPage(url, options);

            expect(data).toStrictEqual({ html: '<h1>data-from-cache</h1>' });
        });

        it('Возвращает данные с ручки, если нет данных в кэше', async() => {
            const data = await dataProvider.loadPage(url, options);

            expect(data).toStrictEqual({ html: '<h1>data-from-fetch</h1>' });
        });

        it('Сохраняет данные с ручки в кэш', async() => {
            await dataProvider.loadPage(url, options);

            expect(cacheUpdate).toBeCalledWith('pages', {
                key: 'https://yandex.ru/turbo',
                data: {
                    html: '<h1>data-from-fetch</h1>',
                },
            }, 6000);
        });

        it('Вызывает fetch с корректными параметрами', async() => {
            await dataProvider.loadPage(url, options);

            expect(fetch).toBeCalledWith('https://yandex.ru/turbo?tap-preload=1&parent-reqid=req-test-id&bundles=%5B%22TestBundle%22%5D&icons=%5B%22test-icon%22%5D');
        });

        it('Возвращает null если нет данных с ручки', async() => {
            testGlobal.fetch.mockReturnValue(makeFakeResponse(undefined));

            const data = await dataProvider.loadPage(url, options);

            expect(data).toStrictEqual(null);
        });

        it('Не сохраняет данные в кэш, если их нет на ручке', async() => {
            testGlobal.fetch.mockReturnValue(makeFakeResponse(undefined));

            await dataProvider.loadPage(url, options);

            expect(cacheUpdate).not.toBeCalled();
        });

        it('Возвращает null если в данных с ручки вернулась строка', async() => {
            testGlobal.fetch.mockReturnValue(makeFakeResponse('Not found'));

            const data = await dataProvider.loadPage(url, options);

            expect(data).toStrictEqual(null);
        });
    });
});
