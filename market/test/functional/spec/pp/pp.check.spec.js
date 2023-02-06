/* eslint-disable max-len */
const Ajv = require('ajv');

const LOGS = require('./../../data/logs');
const Client = require('./../../lib/client');
const ppCheckMiddleware = require('./../../../../middleware/pp/pp-check');

const ajv = new Ajv();

describe('partnership program, check middleware', () => {
    describe('OK', () => {
        test('should be returned if Sovetnik is not installed, device is unrecognized (desktop) and name of browser is known', async () => {
            const userAgents = [
                /**
                 * Not mobile device -> Firefox
                 */
                'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:53.0) Gecko/20100101 Firefox/53.0',
                'Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:54.0) Gecko/20100101 Firefox/54.0',
                /**
                 * Not mobile device -> Chrome
                 */
                'Mozilla/5.0 (X11; Fedora; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36',
                'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36',
                'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36',
                /**
                 * Not mobile device -> Yandex
                 */
                'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 YaBrowser/15.12.1.6474 Safari/537.36',
                'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 YaBrowser/17.6.0.1633 Yowser/2.5 Safari/537.36',
                /**
                 * Not mobile device -> Opera
                 */
                'Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 OPR/45.0.2552.898',
                /**
                 * Mobile -> Android -> Yandex
                 */
                'Mozilla/5.0 (Linux; Android 4.4.2; HUAWEI Y541-U02 Build/HUAWEIY541-U02) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.137 YaBrowser/17.4.0.544.00 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 5.0; ALE-L21 Build/HuaweiALE-L21) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.137 YaBrowser/17.4.0.544.00 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 6.0; FS407 Build/MRA58K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 YaBrowser/16.7.0.2777.00 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 7.0; SM-A310F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.137 YaBrowser/17.4.0.544.00 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 5.1; m3 note Build/LMY47I) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 YaBrowser/17.1.1.359.00 Mobile Safari/537.36',
                /**
                 * Tablet -> Android -> Yandex
                 */
                'Mozilla/5.0 (Linux; Android 6.0.1; SGP621 Build/23.5.A.1.291) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.137 YaBrowser/17.4.0.544.01 Safari/537.36',
            ];

            const client = new Client();

            userAgents.forEach(async (userAgent) => {
                const params = {
                    headers: {
                        'user-agent': userAgent,
                    },
                    query: {
                        clid: 'clid',
                        aff_id: 'aff_id',
                    },
                };

                const actual = await client.request(ppCheckMiddleware, params);
                expect(actual.response.status).toBe('OK');

                let isValidate = ajv.validate(LOGS.PP.COMMON, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();

                isValidate = ajv.validate(LOGS.PP.CHECK, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();
            });
        });
    });

    describe('UNSUPPORTED_MOBILE', () => {
        test('should be returned if Sovetnik is not installed, device is mobile and OS is not Android', () => {
            const userAgents = [
                /**
                 * Mobile -> iOS -> WebKit
                 */
                'Mozilla/5.0 (iPhone; CPU iPhone OS 10_2_1 like Mac OS X) AppleWebKit/602.4.6 (KHTML, like Gecko) Mobile/14D27',
                'Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_2 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13F69',
                /**
                 * Tablet -> iOS -> Safari
                 */
                'Mozilla/5.0 (iPad; CPU OS 10_3_2 like Mac OS X) AppleWebKit/602.1.50 (KHTML, like Gecko) GSA/28.0.157793287 Mobile/14F89 Safari/602.1',
                'Mozilla/5.0 (iPad; CPU OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Coast/5.04.110603 Mobile/14E304 Safari/7534.48.3',
                /**
                 * Tablet -> iOS -> Mobile Safari
                 */
                'Mozilla/5.0 (iPad; CPU OS 10_3_2 like Mac OS X) AppleWebKit/603.2.4 (KHTML, like Gecko) Version/10.0 Mobile/14F89 Safari/602.1',
                /**
                 * Mobile -> iOS -> Mobile Safari
                 */
                'Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_2 like Mac OS X) AppleWebKit/603.2.4 (KHTML, like Gecko) Version/10.0 Mobile/14F89 Safari/602.1',
                'Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.21 (KHTML, like Gecko) Version/10.0 Mobile/15A5278f Safari/602.1',
                'Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_5 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13G36 Safari/601.1',
                /**
                 * Android -> Chrome
                 */
                'Mozilla/5.0 (Linux; Android 6.0.1; Lenovo P1a42 Build/MMB29M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 6.0.1; Redmi 3S Build/MMB29M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 7.0; E6553 Build/32.3.A.2.33) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 4.4.4; Nexus 5 Build/KTU84P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 5.0.2; LG-V490 Build/LRX22G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Safari/537.36',
                'Mozilla/5.0 (Linux; Android 5.1; MX5 Build/LMY47I) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36',
                /**
                 * Android -> Opera
                 */
                'Mozilla/5.0 (Linux; Android 6.0.1; Redmi Note 3 Build/MMB29M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Mobile Safari/537.36 OPR/42.7.2246.114996',
                'Mozilla/5.0 (Linux; Android 5.1.1; Redmi 3 Build/LMY47V) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Mobile Safari/537.36 OPR/42.7.2246.114996',
                'Mozilla/5.0 (Linux; Android 6.0; Lenovo TB3-850M Build/MRA58K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Safari/537.36 OPR/42.7.2246.114996',
                'Mozilla/5.0 (Linux; Android 7.0; Mi-4c Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Mobile Safari/537.36 OPR/42.7.2246.114996',
                /**
                 * Tablet -> Android -> Android Browser
                 */
                'Mozilla/5.0 (Linux; Android 6.0.1; SAMSUNG SM-T805 Build/MMB29K) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/4.0 Chrome/44.0.2403.133 Safari/537.36',
                /**
                 * Mobile -> Android -> Android Browser
                 */
                'Mozilla/5.0 (Linux; Android 5.0; SAMSUNG SM-N900 Build/LRX21V) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/4.0 Chrome/44.0.2403.133 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 6.0.1; SAMSUNG SM-A720F Build/MMB29K) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/5.4 Chrome/51.0.2704.106 Mobile Safari/537.36',
                /**
                 * Tablet -> Android -> Chrome WebView
                 */
                'Mozilla/5.0 (Linux; Android 6.0.1; Nexus 7 Build/MOB30X; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/58.0.3029.83 Safari/537.36',
                /**
                 * Mobile -> Android -> Chrome WebView
                 */
                'Mozilla/5.0 (Linux; Android 6.0.1; SM-G900F Build/MMB29M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/58.0.3029.83 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 6.0.1; SM-G900F Build/MMB29M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/54.0.2840.85 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 6.0.1; SM-A720F Build/MMB29K; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/54.0.2840.85 Mobile Safari/537.36 YandexSearch/5.45',
                'Mozilla/5.0 (Linux; Android 6.0.1; Redmi Note 4 Build/MMB29M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/55.0.2883.91 Mobile Safari/537.36',
                /**
                 * Android -> MIUI Browser
                 */
                'Mozilla/5.0 (Linux; U; Android 6.0; ru-ru; Redmi Pro Build/MRA58K) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/46.0.2490.85 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.1.4',
                'Mozilla/5.0 (Linux; U; Android 6.0.1; en-us; Redmi 3S Build/MMB29M) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.8.7',
                'Mozilla/5.0 (Linux; U; Android 6.0.1; ru-ru; Redmi 3S Build/MMB29M) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.8.6',
                'Mozilla/5.0 (Linux; U; Android 5.1.1; ru-ru; Redmi 3 Build/LMY47V) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/46.0.2490.85 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.2.6',
                'Mozilla/5.0 (Linux; U; Android 6.0.1; en-us; MI 5 Build/MXB48T) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.146 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.9.4',
                /**
                 * Android -> Firefox
                 */
                'Mozilla/5.0 (Android 6.0; Mobile; rv:53.0) Gecko/53.0 Firefox/53.0',
                'Mozilla/5.0 (Android 4.4.2; Mobile; rv:54.0) Gecko/54.0 Firefox/54.0',
                /**
                 * Tablet -> Android -> Firefox
                 */
                'Mozilla/5.0 (Android 5.0.1; Tablet; rv:54.0) Gecko/54.0 Firefox/54.0',
            ];

            const client = new Client();

            userAgents.forEach(async (userAgent) => {
                const params = {
                    headers: {
                        'user-agent': userAgent,
                    },
                    query: {
                        clid: 'clid',
                        aff_id: 'aff_id',
                    },
                };

                const actual = await client.request(ppCheckMiddleware, params);
                expect(actual.response.status).toBe('UNSUPPORTED_MOBILE');

                let isValidate = ajv.validate(LOGS.PP.COMMON, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();

                isValidate = ajv.validate(LOGS.PP.CHECK, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();
            });
        });
    });

    describe('UNSUPPORTED_DESKTOP', () => {
        test('should be returned if Sovetnik is not installed, device is not mobile and name of browser is unrecognized', async () => {
            const userAgents = [
                'Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)',
                'Mozilla/5.0 (Windows NT 6.1; WOW64; rv:53.0)',
            ];

            const client = new Client();

            userAgents.forEach(async (userAgent) => {
                const params = {
                    headers: {
                        'user-agent': userAgent,
                    },
                    query: {
                        clid: 'clid',
                        aff_id: 'aff_id',
                    },
                };

                const actual = await client.request(ppCheckMiddleware, params);
                expect(actual.response.status).toBe('UNSUPPORTED_DESKTOP');

                let isValidate = ajv.validate(LOGS.PP.COMMON, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();

                isValidate = ajv.validate(LOGS.PP.CHECK, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();
            });
        });

        test('should be returned if Sovetnik is not installed and Opera major version is less than 12', () => {
            const userAgents = [
                'Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 OPR/12.0 (Edition Yx)',
                'Opera/9.80 (Windows NT 6.0; U; en) Presto/2.10.229 Version/11.61',
                'Opera/9.80 (Windows NT 6.1; U; Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0); en) Presto/2.10.229 Version/11.61',
            ];

            const client = new Client();

            userAgents.forEach(async (userAgent) => {
                const params = {
                    headers: {
                        'user-agent': userAgent,
                    },
                    query: {
                        clid: 'clid',
                        aff_id: 'aff_id',
                    },
                };

                const actual = await client.request(ppCheckMiddleware, params);
                expect(actual.response.status).toBe('UNSUPPORTED_DESKTOP');

                let isValidate = ajv.validate(LOGS.PP.COMMON, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();

                isValidate = ajv.validate(LOGS.PP.CHECK, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();
            });
        });
    });

    describe('ALREADY_EXISTS', () => {
        test('should be returned if Sovetnik is installed and has button', () => {
            const userAgents = [
                /**
                 * Not mobile device -> Firefox
                 */
                'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:53.0) Gecko/20100101 Firefox/53.0',
                'Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:54.0) Gecko/20100101 Firefox/54.0',
                /**
                 * Not mobile device -> Chrome
                 */
                'Mozilla/5.0 (X11; Fedora; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36',
                'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36',
                'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36',
                /**
                 * Not mobile device -> Yandex
                 */
                'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 YaBrowser/15.12.1.6474 Safari/537.36',
                'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 YaBrowser/17.6.0.1633 Yowser/2.5 Safari/537.36',
                /**
                 * Not mobile device -> Opera
                 */
                'Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 OPR/45.0.2552.898',
            ];

            const partnerSettings = {};
            const userSettings = { offerAccepted: null };

            const client = new Client(partnerSettings, userSettings, true);

            userAgents.forEach(async (userAgent) => {
                const params = {
                    headers: {
                        'user-agent': userAgent,
                    },
                    query: {
                        clid: 'clid',
                        aff_id: 'aff_id',
                    },
                };

                const actual = await client.request(ppCheckMiddleware, params);
                expect(actual.response.status).toBe('ALREADY_EXISTS');

                let isValidate = ajv.validate(LOGS.PP.COMMON, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();

                isValidate = ajv.validate(LOGS.PP.CHECK, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();
            });
        });

        test('should be returned is Sovetnik is installed and has mobile view', () => {
            const userAgents = [
                /**
                 * Mobile -> Android -> Yandex
                 */
                'Mozilla/5.0 (Linux; Android 4.4.2; HUAWEI Y541-U02 Build/HUAWEIY541-U02) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.137 YaBrowser/17.4.0.544.00 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 5.0; ALE-L21 Build/HuaweiALE-L21) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.137 YaBrowser/17.4.0.544.00 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 6.0; FS407 Build/MRA58K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 YaBrowser/16.7.0.2777.00 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 7.0; SM-A310F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.137 YaBrowser/17.4.0.544.00 Mobile Safari/537.36',
                'Mozilla/5.0 (Linux; Android 5.1; m3 note Build/LMY47I) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 YaBrowser/17.1.1.359.00 Mobile Safari/537.36',
                /**
                 * Tablet -> Android -> Yandex
                 */
                'Mozilla/5.0 (Linux; Android 6.0.1; SGP621 Build/23.5.A.1.291) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.137 YaBrowser/17.4.0.544.01 Safari/537.36',
            ];

            const partnerSettings = {};
            const userSettings = { offerAccepted: null };

            const client = new Client(partnerSettings, userSettings);

            userAgents.forEach(async (userAgent) => {
                const params = {
                    headers: {
                        'user-agent': userAgent,
                    },
                    query: {
                        clid: 'clid',
                        aff_id: 'aff_id',
                    },
                };

                const actual = await client.request(ppCheckMiddleware, params);
                expect(actual.response.status).toBe('ALREADY_EXISTS');

                let isValidate = ajv.validate(LOGS.PP.COMMON, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();

                isValidate = ajv.validate(LOGS.PP.CHECK, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();
            });
        });
    });

    describe('WITHOUT_BUTTON', () => {
        test('should be returned if Sovetnik is installed but does not have button', () => {
            const userAgents = [
                /**
                 * Not mobile device -> Firefox
                 */
                'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:53.0) Gecko/20100101 Firefox/53.0',
                'Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:54.0) Gecko/20100101 Firefox/54.0',
                /**
                 * Not mobile device -> Chrome
                 */
                'Mozilla/5.0 (X11; Fedora; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36',
                'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36',
                'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36',
                /**
                 * Not mobile device -> Yandex
                 */
                'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 YaBrowser/15.12.1.6474 Safari/537.36',
                'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 YaBrowser/17.6.0.1633 Yowser/2.5 Safari/537.36',
                /**
                 * Not mobile device -> Opera
                 */
                'Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 OPR/45.0.2552.898',
            ];

            const partnerSettings = {};
            const userSettings = { offerAccepted: null };

            const client = new Client(partnerSettings, userSettings);

            userAgents.forEach(async (userAgent) => {
                const params = {
                    headers: {
                        'user-agent': userAgent,
                    },
                    query: {
                        clid: 'clid',
                        aff_id: 'aff_id',
                    },
                };

                const actual = await client.request(ppCheckMiddleware, params);
                expect(actual.response.status).toBe('WITHOUT_BUTTON');

                let isValidate = ajv.validate(LOGS.PP.COMMON, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();

                isValidate = ajv.validate(LOGS.PP.CHECK, actual.logs && actual.logs.pp);
                expect(isValidate).toBeTruthy();
            });
        });
    });
});
