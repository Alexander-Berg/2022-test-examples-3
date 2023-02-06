import { YANDEX_QUASAR_JS_API } from 'constants/experimental-flags';

describe('pp/is-pairing-api-supported', () => {
    beforeEach(() => {
        global.storage = {
            experiments: {
                flags: {
                    [YANDEX_QUASAR_JS_API]: true,
                },
                encryptedBoxes: '',
                boxes: '',
            },
        };
    });

    afterEach(() => {
        jest.resetModules();

        window.YandexQuasarApplicationsAPIBackend = undefined;
        window.yandex = undefined;
    });

    it('Должна проходить проверка андроидной версии', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [10, 0],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        window.YandexQuasarApplicationsAPIBackend = {
            playPairingSound: () => {},
            connectApDevice: () => {},
        };

        return import('pp/is-pairing-api-supported').then(({ isPairingApiSupported }) => {
            expect(isPairingApiSupported()).toEqual(true);
        });
    });

    it('Должна проходить проверка андроидной версии 2', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [11, 23],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        window.YandexQuasarApplicationsAPIBackend = {
            playPairingSound: () => {},
            connectApDevice: () => {},
        };

        return import('pp/is-pairing-api-supported').then(({ isPairingApiSupported }) => {
            expect(isPairingApiSupported()).toEqual(true);
        });
    });

    it('Должна НЕ проходить проверка андроидной версии', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [9, 99],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        window.YandexQuasarApplicationsAPIBackend = {
            playPairingSound: () => {},
            connectApDevice: () => {},
        };

        return import('pp/is-pairing-api-supported').then(({ isPairingApiSupported }) => {
            expect(isPairingApiSupported()).toEqual(false);
        });
    });

    it('Должна проходить проверка версии с методами в нэймпейсе yandex.quasar', () => {
        if (!window.yandex) {
            window.yandex = {};
        }

        window.yandex = {
            quasar: {
                playPairingSound: async() => {},
                connectApDevice: async() => true,
            },
        };

        return import('pp/is-pairing-api-supported').then(({ isPairingApiSupported }) => {
            expect(isPairingApiSupported()).toEqual(true);
        });
    });

    it('Должна НЕ проходить проверка без методов JS API', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [12, 99],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        return import('pp/is-pairing-api-supported').then(({ isPairingApiSupported }) => {
            expect(isPairingApiSupported()).toEqual(false);
        });
    });

    it('Должна НЕ проходить проверка без методов JS API 2', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [12, 99],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        window.YandexQuasarApplicationsAPIBackend = {};

        return import('pp/is-pairing-api-supported').then(({ isPairingApiSupported }) => {
            expect(isPairingApiSupported()).toEqual(false);
        });
    });

    it('Должна НЕ проходить проверка без методов JS API 3', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [12, 99],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        window.YandexQuasarApplicationsAPIBackend = {
            playPairingSound: () => {},
        };

        return import('pp/is-pairing-api-supported').then(({ isPairingApiSupported }) => {
            expect(isPairingApiSupported()).toEqual(false);
        });
    });

    it('Должна НЕ проходить проверка в неизвестной платформе', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [100, 500],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => false,
            };
        });

        window.YandexQuasarApplicationsAPIBackend = {
            playPairingSound: () => {},
            connectApDevice: () => {},
        };

        return import('pp/is-pairing-api-supported').then(({ isPairingApiSupported }) => {
            expect(isPairingApiSupported()).toEqual(false);
        });
    });

    it('Должна проходить проверка айосной версии', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [24, 0],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => true,
            };
        });

        window.YandexQuasarApplicationsAPIBackend = {
            playPairingSound: () => {},
            connectApDevice: () => {},
        };

        return import('pp/is-pairing-api-supported').then(({ isPairingApiSupported }) => {
            expect(isPairingApiSupported()).toEqual(true);
        });
    });

    it('Должна проходить проверка айосной версии 2', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [42, 56],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => true,
            };
        });

        window.YandexQuasarApplicationsAPIBackend = {
            playPairingSound: () => {},
            connectApDevice: () => {},
        };

        return import('pp/is-pairing-api-supported').then(({ isPairingApiSupported }) => {
            expect(isPairingApiSupported()).toEqual(true);
        });
    });

    it('Должна НЕ проходить проверка айосной версии', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [23, 99],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => true,
            };
        });

        window.YandexQuasarApplicationsAPIBackend = {
            playPairingSound: () => {},
            connectApDevice: () => {},
        };

        return import('pp/is-pairing-api-supported').then(({ isPairingApiSupported }) => {
            expect(isPairingApiSupported()).toEqual(false);
        });
    });
});
