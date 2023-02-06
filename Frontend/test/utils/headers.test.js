const { setHostOverrideHeader } = require('../../core/utils/headers');
const OSDetect = require('../../core/utils/os-detect');

const cgidata = {
    scheme: 'https',
    hostname: 'yandex.ru',
    args: {},
};

function checkHeader(reportContext, idx = 0) {
    const headerName = reportContext.setResponseHeader.mock.calls[idx][0];
    const headerValue = reportContext.setResponseHeader.mock.calls[idx][1];

    expect(headerName).toEqual('X-Yandex-TurboPage-Override');
    expect(headerValue).toMatchSnapshot();
}

describe('utils headers', function() {
    describe('setHostOverrideHeader', function() {
        it('Устанавливает закодированный в base64 HTTP-заголовок для Android Яндекс.Браузера', function() {
            const data = {
                env: {
                    platform: 'touch-phone',
                },
                reqdata: {
                    unparsed_uri: '/turbo?text=https%3A%2F%2Fregnum.ru%2Fnews%2F2681316.html&utm_referrer=ht' +
                        'tps%3A%2F%2Fm.news.yandex.ru',
                    url: 'https://yandex.ru/turbo?text=https%3A%2F%2Fregnum.ru%2Fnews%2F2681316.html&utm_referrer=ht' +
                        'tps%3A%2F%2Fm.news.yandex.ru',
                },
                doc: {
                    url: 'https://regnum.ru/news/2681316.html',
                },
                cgidata,
            };
            OSDetect.getBrowserName = jest.fn(() => 'yandexbrowser');
            OSDetect.getOSFamily = jest.fn(() => 'android');
            const reportContext = {
                setResponseHeader: jest.fn(),
            };

            setHostOverrideHeader(data, reportContext);

            checkHeader(reportContext);
        });

        it('Устанавливает закодированный в base64 HTTP-заголовок для десктопного Яндекс.Браузера', function() {
            const data = {
                env: {
                    platform: 'desktop',
                },
                reqdata: {
                    unparsed_uri: '/turbo?text=https%3A%2F%2Frussian.rt.com%2Frussia%2Fnews%2F656083-postrad' +
                        'avshie-vzryv-krasnoyarskii-krai&utm_referrer=https%3A%2F%2Fm.news.yandex.ru',
                    url: 'https://yandex.ru/turbo?text=https%3A%2F%2Frussian.rt.com%2Frussia%2Fnews%2F656083-postrad' +
                        'avshie-vzryv-krasnoyarskii-krai&utm_referrer=https%3A%2F%2Fm.news.yandex.ru',
                },
                doc: {
                    url: 'https://russian.rt.com/russia/news/656083-postradavshie-vzryv-krasnoyarskii-krai',
                },
                cgidata,
            };
            OSDetect.getBrowserName = jest.fn(() => 'yandexbrowser');
            OSDetect.getOSFamily = jest.fn(() => 'macos');
            const reportContext = {
                setResponseHeader: jest.fn(),
            };

            setHostOverrideHeader(data, reportContext);

            checkHeader(reportContext);
        });

        it('Устанавливает закодированный в base64 HTTP-заголовок для iOS ПП', function() {
            const data = {
                env: {
                    platform: 'touch-phone',
                },
                reqdata: {
                    unparsed_uri: '/turbo?text=https%3A%2F%2Fregnum.ru%2Fnews%2F2681316.html&utm_referrer=ht' +
                        'tps%3A%2F%2Fm.news.yandex.ru',
                    url: 'https://yandex.ru/turbo?text=https%3A%2F%2Fregnum.ru%2Fnews%2F2681316.html&utm_referrer=ht' +
                        'tps%3A%2F%2Fm.news.yandex.ru',
                },
                doc: {
                    url: 'https://regnum.ru/news/2681316.html',
                },
                cgidata,
            };
            OSDetect.getBrowserName = jest.fn(() => 'yandexsearch');
            OSDetect.getOSFamily = jest.fn(() => 'ios');
            const reportContext = {
                setResponseHeader: jest.fn(),
            };

            setHostOverrideHeader(data, reportContext);

            checkHeader(reportContext);
        });

        it('Устанавливает закодированый в base64 HTTP-заголовок для turbopages.org', () => {
            const data = {
                env: {
                    platform: 'touch-phone',
                },
                reqdata: {
                    unparsed_uri: '/s/example.ru/news/2020/02/06/pogi/',
                    url: 'https://example-ru.turbopages.org/s/example.ru/news/2020/02/06/pogi/',
                },
                doc: {
                    url: 'https://example.ru/news/2020/02/06/pogi',
                },
                cgidata: {
                    scheme: 'https',
                    hostname: 'example-ru.turbopages.org',
                    args: {},
                },
            };
            const reportContext = {
                setResponseHeader: jest.fn(),
            };
            OSDetect.getBrowserName = jest.fn(() => 'yandexbrowser');
            OSDetect.getOSFamily = jest.fn(() => 'android');

            setHostOverrideHeader(data, reportContext);

            checkHeader(reportContext);
        });

        it('Устанавливает закодированый в base64 HTTP-заголовок для turbo.domain.tld', () => {
            const data = {
                env: {
                    platform: 'touch-phone',
                },
                reqdata: {
                    unparsed_uri: '/turbo?text=https%3A%2F%2Fwww.gazeta.ru%2Fsocial%2Fnews%2F2020%2F01%2F27%2Fn_13964162.shtml',
                    url: 'https://yandex.ru/turbo?text=https%3A%2F%2Fwww.gazeta.ru%2Fsocial%2Fnews%2F2020%2F01%2F27%2Fn_13964162.shtml',
                },
                doc: {
                    url: 'https://www.gazeta.ru/social/news/2020/01/27/n_13964162.shtml',
                },
                cgidata: {
                    scheme: 'https',
                    hostname: 'yandex.ru',
                    args: {},
                },
            };
            const reportContext = {
                setResponseHeader: jest.fn(),
            };
            OSDetect.getBrowserName = jest.fn(() => 'yandexbrowser');
            OSDetect.getOSFamily = jest.fn(() => 'android');

            setHostOverrideHeader(data, reportContext);

            checkHeader(reportContext);
        });

        it('Устанавливает закодированый в base64 HTTP-заголовок для turbo.domain.tld, если запрос с turbopages.org', () => {
            const data = {
                env: {
                    platform: 'touch-phone',
                },
                reqdata: {
                    unparsed_uri: '/s/www.gazeta.ru/social/news/2020/02/06/n_14006821.shtml',
                    url: 'https://www-gazeta-ru.turbopages.org/s/www.gazeta.ru/social/news/2020/02/06/n_14006821.shtml',
                },
                doc: {
                    url: 'https://www.gazeta.ru/social/news/2020/01/27/n_13964162.shtml',
                },
                cgidata: {
                    scheme: 'https',
                    hostname: 'www-gazeta-ru.turbopages.org',
                    args: {},
                },
            };
            const reportContext = {
                setResponseHeader: jest.fn(),
            };
            OSDetect.getBrowserName = jest.fn(() => 'yandexbrowser');
            OSDetect.getOSFamily = jest.fn(() => 'android');

            setHostOverrideHeader(data, reportContext);

            checkHeader(reportContext);
        });

        it('Устанавливает закодированый в base64 HTTP-заголовок с "чистым" и "грязным" url', () => {
            const data = {
                env: {
                    platform: 'touch-phone',
                },
                reqdata: {
                    unparsed_uri: '/turbo?text=https%3A%2F%2Fregnum.ru%2Fnews%2F2681316.html&utm_referrer=ht' +
                        'tps%3A%2F%2Fm.news.yandex.ru&parent-reqid=12345',
                    url: 'https://yandex.ru/turbo?text=https%3A%2F%2Fregnum.ru%2Fnews%2F2681316.html&utm_referrer=ht' +
                        'tps%3A%2F%2Fm.news.yandex.ru&parent-reqid=12345',
                },
                doc: {
                    url: 'https://regnum.ru/news/2681316.html',
                },
                cgidata,
            };
            OSDetect.getBrowserName = jest.fn(() => 'yandexsearch');
            OSDetect.getOSFamily = jest.fn(() => 'ios');
            const reportContext = {
                setResponseHeader: jest.fn(),
            };

            setHostOverrideHeader(data, reportContext);

            checkHeader(reportContext);
        });

        it('Устанавливает закодированый в base64 HTTP-заголовок с legacy_path', () => {
            const data = {
                env: {
                    platform: 'touch-phone',
                },
                reqdata: {
                    legacy_path: '/turbo?text=https%3A%2F%2Fexample.ru%2Fnews%2F2020%2F02%2F06%2Fpogi%2F',
                    unparsed_uri: '/s/example.ru/news/2020/02/06/pogi/',
                    url: 'https://example-ru.turbopages.org/s/example.ru/news/2020/02/06/pogi/',
                },
                doc: {
                    url: 'https://example.ru/news/2020/02/06/pogi/',
                },
                cgidata: {
                    scheme: 'https',
                    hostname: 'example-ru.turbopages.org',
                    args: {},
                },
            };
            OSDetect.getBrowserName = jest.fn(() => 'yandexsearch');
            OSDetect.getOSFamily = jest.fn(() => 'ios');
            const reportContext = {
                setResponseHeader: jest.fn(),
            };

            setHostOverrideHeader(data, reportContext);

            checkHeader(reportContext);
        });

        it('Устанавливает закодированый в base64 HTTP-заголовок с legacy_path и корректным tld', () => {
            const data = {
                env: {
                    platform: 'touch-phone',
                },
                reqdata: {
                    legacy_path: '/turbo?text=https%3A%2F%2Fexample.ru%2Fnews%2F2020%2F02%2F06%2Fpogi%2F',
                    unparsed_uri: '/s/example.ru/news/2020/02/06/pogi/',
                    url: 'https://example-ru.turbopages.org/s/example.ru/news/2020/02/06/pogi/',
                },
                doc: {
                    url: 'https://example.ru/news/2020/02/06/pogi/',
                },
                cgidata: {
                    scheme: 'https',
                    hostname: 'example-ru.turbopages.org',
                    args: {},
                },
            };
            OSDetect.getBrowserName = jest.fn(() => 'yandexsearch');
            OSDetect.getOSFamily = jest.fn(() => 'ios');
            const reportContext = {
                setResponseHeader: jest.fn(),
            };

            setHostOverrideHeader({
                ...data,
                httpHeaders: {
                    referer: 'https://yandex.by',
                },
            }, reportContext);

            checkHeader(reportContext);

            setHostOverrideHeader({
                ...data,
                httpHeaders: {
                    referer: 'https://yandex.com.tr',
                },
            }, reportContext);

            checkHeader(reportContext, 1);
        });

        it('Не устанавливает HTTP-заголовок для Android ПП', function() {
            const data = {
                env: {
                    platform: 'touch-phone',
                },
                reqdata: {
                    unparsed_uri: '/turbo?text=https%3A%2F%2Fwww.gazeta.ru%2Fbusiness%2F2019%2F08%2F05%2F125' +
                        '57143.shtml&utm_referrer=https%3A%2F%2Fm.news.yandex.ru',
                    url: 'https://yandex.ru/turbo?text=https%3A%2F%2Fwww.gazeta.ru%2Fbusiness%2F2019%2F08%2F05%2F125' +
                        '57143.shtml&utm_referrer=https%3A%2F%2Fm.news.yandex.ru',
                },
                doc: {
                    url: 'https://www.gazeta.ru/business/2019/08/05/12557143.shtml',
                },
                cgidata,
            };
            OSDetect.getBrowserName = jest.fn(() => 'yandexsearch');
            OSDetect.getOSFamily = jest.fn(() => 'android');
            const reportContext = {
                setResponseHeader: jest.fn(),
            };

            setHostOverrideHeader(data, reportContext);

            expect(reportContext.setResponseHeader).not.toHaveBeenCalled();
        });

        it('Не устанавливает HTTP-заголовок для iOS Яндекс.Браузера', function() {
            const data = {
                env: {
                    platform: 'touch-phone',
                },
                reqdata: {
                    unparsed_uri: '/turbo?text=https%3A%2F%2Frussian.rt.com%2Frussia%2Fnews%2F656083-postrad' +
                        'avshie-vzryv-krasnoyarskii-krai&utm_referrer=https%3A%2F%2Fm.news.yandex.ru',
                    url: 'https://yandex.ru/turbo?text=https%3A%2F%2Frussian.rt.com%2Frussia%2Fnews%2F656083-postrad' +
                        'avshie-vzryv-krasnoyarskii-krai&utm_referrer=https%3A%2F%2Fm.news.yandex.ru',
                },
                doc: {
                    url: 'https://russian.rt.com/russia/news/656083-postradavshie-vzryv-krasnoyarskii-krai',
                },
                cgidata,
            };
            OSDetect.getBrowserName = jest.fn(() => 'yandexbrowser');
            OSDetect.getOSFamily = jest.fn(() => 'ios');
            const reportContext = {
                setResponseHeader: jest.fn(),
            };

            setHostOverrideHeader(data, reportContext);

            expect(reportContext.setResponseHeader).not.toHaveBeenCalled();
        });
    });
});
