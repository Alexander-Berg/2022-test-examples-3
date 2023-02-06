/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { IReportContext } from '~/types/IReportContext';

// eslint-disable-next-line
import * as React from 'react';
// eslint-disable-next-line
import * as config from '../server/config';
// eslint-disable-next-line
import * as rum from '../server/rum';
import renderApp from '../index.server';

/** Моки модулей можно поменять, когда понадобиться, сейчас 'мешает' простому тесту */
jest.mock('../server/config', () => {
    return {
        css: 'test',
    };
});

jest.mock('../server/rum', () => {
    return {
        getRumCounterInterfaceContent: jest.fn(),
        getRumCounterImplementationContent: jest.fn(),
        getRumCounterInitParams: jest.fn(),
        isErrorCounterEnabled: jest.fn(),
        getErrorCounterContent: jest.fn(),
        getGlobalBemParams: jest.fn(),
    };
});

jest.mock('../lib/polyfills', () => {
    return {
        getPolyfills: jest.fn(() => []),
    };
});

jest.mock('react', () => Object.assign(
    {},
    jest.requireActual('react'),
    { useLayoutEffect: jest.requireActual('react').useEffect }
));

describe('renderApp', function() {
    describe('Редирект', function() {
        let rrCtx: Partial<IReportContext>;
        let Util = {};
        let data = {};

        beforeEach(function() {
            //@see https://jestjs.io/docs/en/manual-mocks#mocking-methods-which-are-not-implemented-in-jsdom
            Object.defineProperty(window, 'matchMedia', {
                writable: true,
                value: jest.fn().mockImplementation(query => ({
                    matches: false,
                    media: query,
                    onchange: null,
                    addListener: jest.fn(), // deprecated
                    removeListener: jest.fn(), // deprecated
                    addEventListener: jest.fn(),
                    removeEventListener: jest.fn(),
                    dispatchEvent: jest.fn(),
                })),
            });

            rrCtx = {
                setResponseHeader: jest.fn(),
                setResponseStatus: jest.fn(),
            };

            Util = {
                reportError: jest.fn(),
                getTemplatesState: jest.fn(),
                getLog: jest.fn(),
                encrypt: jest.fn(),
                signString: jest.fn(),
            };

            data = {
                reqdata: {
                    passport: { login: '' },
                    cookie: {},
                    // Адресс запроса.
                    unparsed_uri: '/turbo/gipfel.ru?text=random.ru/any-text&srcrwr=SAAS%3ASAAS_ANSWERS&tpid=bd417ec%2Fchrome-phone&random=should-rm',
                    device_detect: {
                        BrowserVersion: '0020.0004.0000.3443',
                        OSFamily: 'MacOS',
                        BrowserName: 'YandexBrowser',
                        BrowserVersionRaw: '20.4.0.3443',
                        OSVersion: '10.14.6',
                        BrowserEngine: 'WebKit',
                        OSName: 'Mac OS X Mojave',
                    },
                    flags: {},
                    reqid: '1598882081354963-407517864524043176000190-production-app-host-man-web-yp-315',
                    headers: {},
                },
                cgidata: {
                    scheme: 'https',
                    hostname: 'yandex.ru',
                    text: 'text=random.ru/any-text&srcrwr=SAAS%3ASAAS_ANSWERS&tpid=bd417ec%2Fchrome-phone&random=should-rm',
                    path: '/turbo/gipfel.ru',
                    args: {
                        srcrwr: [
                            'SAAS:SAAS_ANSWERS',
                        ],
                        random: [
                            'should-rm',
                        ],
                        gimme: [
                            'orig.cgidata',
                        ],
                        tpid: [
                            'bd417ec/chrome-phone',
                        ],
                        text: [
                            'random.ru/any-text',
                        ],
                    },
                },
                env: {
                    expFlags: {},
                },
                app_host: {
                    result: {
                        docs: [
                            {
                                url: 'https://random.ru',
                                feature: 'ecom-morda',
                                content: [],
                                product_info: { turbo_shop_id: 'random.ru' },
                            },
                        ],
                    },
                },
            };
        });

        it('происходит при разных baseUrl в url и данных', function() {
            const result = renderApp(data, rrCtx, Util);

            expect(result, 'Вернулся не пустой ответ').toBe('');
            expect(rrCtx.setResponseHeader, 'Не произошло редиректа на главную страницу приложения')
                .toBeCalledWith(
                    'Location',
                    'https://yandex.ru/turbo/gipfel.ru/n/yandexturbocatalog/main/?morda=1&ecommerce_main_page_preview=1&isTap=1&srcrwr=SAAS%3ASAAS_ANSWERS&tpid=bd417ec%2Fchrome-phone'
                );
            expect(rrCtx.setResponseStatus, 'Не произошло редиректа с кодом 301').toBeCalledWith(301);
        });

        it('не происходит при одинаковых baseUrl в url и данных', function() {
            data.app_host.result.docs[0].product_info.turbo_shop_id = 'gipfel.ru';
            const result = renderApp(data, rrCtx, Util);

            expect(result.length > 0, 'Вернулся пустой ответ').toBeTruthy();
            expect(rrCtx.setResponseStatus, 'Вызывался метод установки кода').toBeCalledTimes(0);

            expect(rrCtx.setResponseHeader, 'Вызывался метод установки заголовков').toBeCalledTimes(1);
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            expect(rrCtx.setResponseHeader!.mock.calls[0][0], 'Выставился неправильный заголовок').toEqual('Content-Security-Policy');
        });

        it('не происходит при отсутствие baseUrl в url', function() {
            data.reqdata.unparsed_uri = '/turbo?text=random.ru/test';
            data.cgidata.path = '/turbo';
            const result = renderApp(data, rrCtx, Util);

            expect(result.length > 0, 'Вернулся пустой ответ').toBeTruthy();
            expect(rrCtx.setResponseHeader, 'Вызывался метод установки заголовков').toBeCalledTimes(1);
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            expect(rrCtx.setResponseHeader!.mock.calls[0][0], 'Выставился неправильный заголовок').toEqual('Content-Security-Policy');
        });

        it('не происходит при отсутствие (схема с красивыми урлами)', function() {
            data.reqdata.unparsed_uri = '/turbo/s/random.ru/test';
            data.cgidata.path = '/turbo';
            const result = renderApp(data, rrCtx, Util);

            expect(result.length > 0, 'Вернулся пустой ответ').toBeTruthy();
            expect(rrCtx.setResponseHeader, 'Вызывался метод установки заголовков').toBeCalledTimes(1);
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            expect(rrCtx.setResponseHeader!.mock.calls[0][0], 'Выставился неправильный заголовок').toEqual('Content-Security-Policy');
        });

        describe('Перед авторизацией на Паспорт', function() {
            const passportUrl = 'https://passport.yandex.ru/auth?random=2';
            beforeEach(function() {
                data.cgidata.path = '/turbo';
                data.reqdata.tld = 'ru';
                data.cgidata.args.passport_redir = [1];
                data.cgidata.args.auth_url = [passportUrl];
            });

            it('происходит при наличии параметра passport_redir и валидного урла auth_url', function() {
                renderApp(data, rrCtx, Util);

                expect(rrCtx.setResponseStatus, 'Не произошло редиректа с кодом 301').toBeCalledWith(301);
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                expect(rrCtx.setResponseHeader!.mock.calls[1][0], 'Выставился неправильный заголовок').toEqual('Location');
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                expect(rrCtx.setResponseHeader!.mock.calls[1][1]).toEqual(passportUrl);
            });

            it('не происходит если не совпадает tld', function() {
                data.reqdata.tld = 'by';
                renderApp(data, rrCtx, Util);

                expect(rrCtx.setResponseStatus, 'Не произошло редиректа с кодом 301').toBeCalledWith(301);
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                expect(rrCtx.setResponseHeader!.mock.calls[1][0], 'Выставился неправильный заголовок').toEqual('Location');
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                expect(rrCtx.setResponseHeader!.mock.calls[1][1] !== passportUrl).toBeTruthy();
            });

            it('не происходит если в параметре auth_url не паспортный урл', function() {
                data.cgidata.args.auth_url = ['https://not-passport.yandex.ru'];
                renderApp(data, rrCtx, Util);

                expect(rrCtx.setResponseStatus, 'Не произошло редиректа с кодом 301').toBeCalledWith(301);
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                expect(rrCtx.setResponseHeader!.mock.calls[1][0], 'Выставился неправильный заголовок').toEqual('Location');
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                expect(rrCtx.setResponseHeader!.mock.calls[1][1] !== passportUrl).toBeTruthy();
            });
        });
    });
});
