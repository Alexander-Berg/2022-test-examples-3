const OSDetect = require('../../core/utils/os-detect');

describe('utils os-detect', function() {
    describe('getBrowserVersionRaw', function() {
        it('Если поле BrowserVersionRaw заполнено, то возвращает его', function() {
            const data = {
                reqdata: {
                    ua: 'Mozilla/5.0 (Linux; Android 7.0; SM-G935F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) ' +
                        'Chrome/71.0.3578.99 Mobile Safari/537.36 YaApp_Android/8.70 YaSearchBrowser/8.70',
                    device_detect: {
                        BrowserName: 'YandexSearch',
                        OSFamily: 'Android',
                        BrowserVersionRaw: '8.70',
                    },
                },
            };

            const version = OSDetect.getBrowserVersionRaw(data);

            expect(version).toEqual('8.70');
        });

        it('Если BrowserVersionRaw пустое, парсит его из UserAgent', function() {
            const data = {
                reqdata: {
                    ua: 'Mozilla/5.0 (Linux; Android 7.0; SM-G935F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) ' +
                        'Chrome/71.0.3578.99 Mobile Safari/537.36 YaApp_Android/8.70 YaSearchBrowser/8.70',
                    device_detect: {
                        BrowserName: 'YandexSearch',
                        OSFamily: 'Android',
                        BrowserVersionRaw: '',
                    },
                },
            };

            const version = OSDetect.getBrowserVersionRaw(data);

            expect(version).toEqual('8.70');
        });

        it('Если не удалось получить версию из UserAgent, возвращает пустую строку', function() {
            const data = {
                reqdata: {
                    ua: 'not-supported-user-agent',
                    device_detect: {
                        BrowserName: 'YandexSearch',
                        OSFamily: 'Android',
                        BrowserVersionRaw: '',
                    },
                },
            };

            const version = OSDetect.getBrowserVersionRaw(data);

            expect(version).toEqual('');
        });
    });

    describe('isSearchApp', () => {
        it('Возвращает falsy value, если не передан device', () => {
            expect(OSDetect.isSearchApp()).toBeFalsy();
        });

        it('Возвращает false, если BrowserName отличен от YandexSearch', () => {
            expect(OSDetect.isSearchApp({ BrowserName: 'YandexBrowser' })).toBe(false);
        });

        it('Возвращает true, если BrowserName равен YandexSearch', () => {
            expect(OSDetect.isSearchApp({ BrowserName: 'YandexSearch' })).toBe(true);
        });
    });

    describe('isSearchAppBasedOnYaBro', () => {
        it('Возвращает falsy value, если не передан data', () => {
            expect(OSDetect.isSearchAppBasedOnYaBro()).toBeFalsy();
        });

        it('Возвращает false, если в ua нет BroPP/', () => {
            expect(OSDetect.isSearchAppBasedOnYaBro({ reqdata: { ua: 'some user agent' } })).toBe(false);
        });

        it('Возвращает true, если в ua есть BroPP/', () => {
            expect(
                OSDetect.isSearchAppBasedOnYaBro({
                    reqdata: {
                        ua: 'Mozilla/5.0 (Linux; arm; Android 10; Pixel 2) AppleWebKit/537.36 (KHTML, like Gecko) ' +
                            'Chrome/81.0.4044.5 YaApp_Android/11.40 YaSearchBrowser/11.40 BroPP/1.0 SA/1 Mobile ' +
                            'Safari/537.36',
                    },
                })
            ).toBe(true);
        });
    });
});
