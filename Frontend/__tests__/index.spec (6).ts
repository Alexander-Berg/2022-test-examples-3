import { getRouteData, buildUrl } from '..';

let mockRoute: object;
let mockFindFirst: ReturnType<typeof jest.fn>;
let mockAddRoute: ReturnType<typeof jest.fn>;
let mockGetRouteByName: ReturnType<typeof jest.fn>;

jest.mock('../routes', () => {
    mockRoute = { name: 'test', path: '/path/to' };

    return {
        routes: [mockRoute],
    };
});

jest.mock('susanin', () => {
    return jest.fn().mockImplementation(() => {
        mockFindFirst = jest.fn();
        mockAddRoute = jest.fn();
        mockGetRouteByName = jest.fn();

        return {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            addRoute: (arg: any) => mockAddRoute(arg),
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            findFirst: (arg: any) => mockFindFirst(arg),
            getRouteByName: (name: string) => mockGetRouteByName(name),
        };
    });
});

describe('Router', () => {
    beforeEach(() => {
        mockFindFirst.mockReturnValue([
            {
                getData() {
                    return { test: true };
                },
                getName() {
                    return 'page:test';
                },
            },
            {
                one: 'one',
                two: 'two',
            },
        ]);
    });

    afterEach(() => {
        mockFindFirst.mockReset();
        mockAddRoute.mockReset();
        mockFindFirst.mockReset();
        mockGetRouteByName.mockReset();
    });

    describe('getRouteData', () => {
        it('роуты должны корректно регистрироваться в сусанине', () => {
            expect(mockAddRoute).toHaveBeenCalledTimes(1);
            expect(mockAddRoute).toHaveBeenCalledWith(mockRoute);
        });

        it('должен возвращать распаршенные параметры урла, данные роута и id страницы', () => {
            const pageDecl = getRouteData('https://some.com/path/to?a=b');

            expect(mockFindFirst).toHaveBeenCalledWith('/path/to?a=b');
            expect(pageDecl).toEqual({
                routeData: { test: true },
                pageId: 'page:test',
                parsedParams: {
                    one: 'one',
                    two: 'two',
                },
            });
        });

        it('должен кидать исключение если роут страницы не найден', () => {
            const url = 'https://some.com/one/two';

            mockFindFirst.mockReset();

            expect(() => getRouteData(url)).toThrowError(`Route not found ${url}`);
        });
    });

    describe('buildUrl', () => {
        const pageName = 'unknown';
        const expectedPath = '/path/to';
        const route = {
            build() {
                return expectedPath;
            },
        };
        const buildSpy = jest.spyOn(route, 'build');
        let params: Record<string, string>;

        beforeEach(() => {
            params = { p1: 'param1' };
            mockGetRouteByName.mockReturnValue(route);
        });

        afterEach(() => {
            buildSpy.mockClear();
        });

        it('должен корректно находить и строить маршрут по имени', () => {
            // @ts-ignore маршруты замоканы в тесте
            expect(buildUrl(pageName, params)).toEqual(`https://m.pokupki.market.yandex.ru${expectedPath}`);
            expect(buildSpy).toHaveBeenCalledWith(params);
            expect(mockGetRouteByName).toHaveBeenCalledWith(pageName);
        });

        it('должен строить ссылку без префикса "m.", если передана опция toDesktop', () => {
            // @ts-ignore маршруты замоканы в тесте
            expect(buildUrl(pageName, params, { toDesktop: true })).toEqual(`https://pokupki.market.yandex.ru${expectedPath}`);
            // @ts-ignore маршруты замоканы в тесте
            expect(buildUrl(pageName, params, { toDesktop: true, turboLink: true })).toEqual(`/turbo?text=${encodeURIComponent(`https://pokupki.market.yandex.ru${expectedPath}`)}`);
        });

        it('должен корректно строить ссылку на турбо страницу', () => {
            // @ts-ignore маршруты замоканы в тесте
            expect(buildUrl(pageName, params, { turboLink: true }))
                .toEqual(`/turbo?text=${encodeURIComponent(`https://m.pokupki.market.yandex.ru${expectedPath}`)}`);
            expect(buildSpy).toHaveBeenCalledWith(params);
            expect(mockGetRouteByName).toHaveBeenCalledWith(pageName);
        });

        it('должен корректно строит external ссылку', () => {
            // @ts-ignore маршруты замоканы в тесте
            expect(buildUrl('external:test'), params).toEqual('/path/to');
        });

        it('должен возвращать null если маршрут c искомым именем не зарегистрирован', () => {
            // @ts-ignore
            mockGetRouteByName.mockReturnValue(null);

            // @ts-ignore инорим для теста, на входе enum, а такой страницы нет
            expect(buildUrl(pageName)).toEqual(null);
        });
    });
});
