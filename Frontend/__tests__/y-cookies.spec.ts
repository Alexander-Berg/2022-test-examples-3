import yCookies from '../y-cookies';

const mockedCookie = {
    server: {
        yp: '1800000160.multibServ.1#1800000645.udnServ.cDpcsdcsdcZWFy#1800000161.szmServ.2:1440x900:1139x680',
        ys: 'multibysServ.1#udnysServ.cDpcsdcsdcZWFy#szmysServ.2:1440x900:1139x680',
    },
    client: {
        yp: '1800000160.multib.1#1800000645.udn.cDpcsdcsdcZWFy#1800000161.szm.2:1440x900:1139x680',
        ys: 'multibys.1#udnys.cDpcsdcsdcZWFy#szmys.2:1440x900:1139x680',
    },
};

const defaultYpParams = {
    expires: expect.any(Date),
    domain: '.yandex.tld',
    path: '/',
    secure: true,
};

const defaultYsParams = {
    domain: '.yandex.tld',
    path: '/',
    secure: true,
};
const mockSetCookie = jest.fn();

jest.mock('js-cookie', () => ({
    get: cookieName => mockedCookie.client[cookieName],
    set: (name, value, params) => mockSetCookie(name, value, params),
}));

describe('YCookies', () => {
    const windowLocation = window.location;

    beforeAll(() => {
        delete window.location;

        Object.defineProperty(window, 'location', {
            value: {
                host: 'yandex.tld',
            },
            writable: true,
        });
    });

    afterEach(() => {
        mockSetCookie.mockReset();
    });

    afterAll(() => {
        window.location = windowLocation;
    });

    describe('YCookies.Instance', () => {
        test('YCookies should have required methods', async() => {
            const CookiesProvider = yCookies();

            expect(typeof CookiesProvider.ypRead).toBe('function');
            expect(typeof CookiesProvider.ypWrite).toBe('function');
            expect(typeof CookiesProvider.ypRemove).toBe('function');
            expect(typeof CookiesProvider.yp).toBe('function');
            expect(typeof CookiesProvider.ysRead).toBe('function');
            expect(typeof CookiesProvider.ysWrite).toBe('function');
            expect(typeof CookiesProvider.ys).toBe('function');
            expect(typeof CookiesProvider.set).toBe('function');
            expect(typeof CookiesProvider.get).toBe('function');
        });
    });

    describe('YCookies.YP', () => {
        test('test client YP', () => {
            const CookiesProvider = yCookies();

            expect(CookiesProvider.ypRead()).toStrictEqual({
                multib: {
                    expires: expect.any(Date),
                    value: '1',
                },
                udn: {
                    expires: expect.any(Date),
                    value: 'cDpcsdcsdcZWFy',
                },
                szm: {
                    expires: expect.any(Date),
                    value: '2:1440x900:1139x680',
                },
            });
        });

        test('ypWrite', () => {
            const CookiesProvider = yCookies();

            CookiesProvider.ypWrite({
                multib2: {
                    expires: '1800000260',
                    value: '2',
                },
                multib3: {
                    expires: '1800000360',
                    value: '3',
                },
            });

            expect(mockSetCookie).toHaveBeenCalledWith(
                'yp',
                '1800000260.multib2.2#' + '1800000360.multib3.3',
                defaultYpParams
            );
        });

        test('ypRemove', () => {
            const CookiesProvider = yCookies();

            CookiesProvider.ypRemove('multib');

            expect(mockSetCookie).toHaveBeenCalledWith(
                'yp',
                new Date(1800000645000).toString() +
                    '.udn.cDpcsdcsdcZWFy#' +
                    new Date(1800000161000).toString() +
                    '.szm.2%3A1440x900%3A1139x680',
                defaultYpParams
            );
        });

        test('yp', () => {
            const CookiesProvider = yCookies();

            expect(CookiesProvider.yp('szm')).toEqual('2:1440x900:1139x680');

            CookiesProvider.yp('szm_write', 'szm_write_value', '1800000161');

            expect(mockSetCookie).toHaveBeenCalledWith(
                'yp',
                new Date(1800000160000).toString() +
                    '.multib.1#' +
                    new Date(1800000645000).toString() +
                    '.udn.cDpcsdcsdcZWFy#' +
                    new Date(1800000161000).toString() +
                    '.szm.2%3A1440x900%3A1139x680#' +
                    '1800000161.szm_write.szm_write_value',
                defaultYpParams
            );

            CookiesProvider.yp('szm', 'szm_update_value');

            expect(mockSetCookie).toHaveBeenCalledWith(
                'yp',
                new Date(1800000160000).toString() +
                    '.multib.1#' +
                    new Date(1800000645000).toString() +
                    '.udn.cDpcsdcsdcZWFy#' +
                    new Date(1800000161000).toString() +
                    '.szm.szm_update_value',
                defaultYpParams
            );
        });
    });

    describe('YCookies.YS', () => {
        test('ysRead', () => {
            const CookiesProvider = yCookies();

            expect(CookiesProvider.ysRead()).toStrictEqual({
                multibys: '1',
                szmys: '2:1440x900:1139x680',
                udnys: 'cDpcsdcsdcZWFy',
            });
        });

        test('ysWrite', () => {
            const CookiesProvider = yCookies();

            CookiesProvider.ysWrite({
                multibys2: '2',
            });

            expect(mockSetCookie).toHaveBeenCalledWith('ys', 'multibys2.2', defaultYsParams);
        });

        test('ys', () => {
            const CookiesProvider = yCookies();

            expect(CookiesProvider.ys('szmys')).toEqual('2:1440x900:1139x680');

            CookiesProvider.ys('szmys_write', 'szmys_write_value');

            expect(mockSetCookie).toHaveBeenCalledWith(
                'ys',
                'multibys.1#' +
                    'udnys.cDpcsdcsdcZWFy#' +
                    'szmys.2%3A1440x900%3A1139x680#' +
                    'szmys_write.szmys_write_value',
                defaultYsParams
            );

            CookiesProvider.ys('szmys', 'szmys_update_value');

            expect(mockSetCookie).toHaveBeenCalledWith(
                'ys',
                'multibys.1#' + 'udnys.cDpcsdcsdcZWFy#' + 'szmys.szmys_update_value',
                defaultYsParams
            );
        });
    });
});
