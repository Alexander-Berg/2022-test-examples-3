const noop = () => {};
const FAKE_QUASAR_API = {
    getBleStatus: noop,
    requestBlePermissions: noop,
    startDiscovery: noop,
    stopDiscovery: noop,
    provideConfig: noop,
    provideAuthOnlyConfig: noop,
    getBleDeviceWiFiList: noop,
};

describe('pp/is-ble-pairing-supported', () => {
    beforeEach(() => {
        window.YandexQuasarApplicationsAPIBackend = FAKE_QUASAR_API;

        jest.doMock('pp/get-pp-params', () => {
            return {
                getPpParams: () => ({ os_version: '10.0' }),
            };
        });
    });

    afterEach(() => {
        jest.resetModules();

        window.YandexQuasarApplicationsAPIBackend = undefined;
    });

    it('Должна проходить проверка андроидной версии', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [21, 0],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(true);
        });
    });

    it('Должна проходить проверка андроидной версии 2', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [20, 120],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(true);
        });
    });

    it('Должна проходить проверка андроидной версии 3', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [20, 112],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(true);
        });
    });

    it('Должна проходить проверка андроидной версии 4', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [20, 113],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(true);
        });
    });

    it('Должна НЕ проходить проверка андроидной версии', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [20, 111],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(false);
        });
    });

    it('Должна НЕ проходить проверка андроидной версии 2', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [19, 999],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(false);
        });
    });

    it('Должна НЕ проходить проверка андроидной версии 3', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [20, 113],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });
        jest.doMock('pp/get-pp-params', () => {
            return {
                getPpParams: () => ({ os_version: '6.9' }),
            };
        });

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(false);
        });
    });

    it('Должна НЕ проходить проверка без методов JS API', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [99, 99],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        window.YandexQuasarApplicationsAPIBackend = undefined;

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(false);
        });
    });

    it('Должна НЕ проходить проверка без методов JS API 2', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [99, 99],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        // @ts-ignore
        window.YandexQuasarApplicationsAPIBackend = {};

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(false);
        });
    });

    it('Должна НЕ проходить проверка без методов JS API 3', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [99, 99],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        // @ts-ignore
        window.YandexQuasarApplicationsAPIBackend = {
            provideConfig: () => {},
        };

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(false);
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

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(false);
        });
    });

    it('Должна проходить проверка айосной версии', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [48, 0],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => true,
            };
        });

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(true);
        });
    });

    it('Должна проходить проверка айосной версии 2', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [48, 56],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => true,
            };
        });

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(true);
        });
    });

    it('Должна проходить проверка айосной версии 3', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [49, 0],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => true,
            };
        });

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(true);
        });
    });

    it('Должна НЕ проходить проверка айосной версии', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [47, 99],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => true,
            };
        });

        return import('pp/is-ble-pairing-supported').then(({ isBlePairingSupported }) => {
            expect(isBlePairingSupported()).toEqual(false);
        });
    });
});
