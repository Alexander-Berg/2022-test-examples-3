import { DeviceType } from 'types/device-type';

describe('pp/is-speaker-pairing-supported', () => {
    beforeEach(() => {
        window.YandexQuasarApplicationsAPIBackend = {
            playPairingSound: () => {},
            connectApDevice: () => {},
        };
    });

    afterEach(() => {
        jest.resetModules();

        window.YandexQuasarApplicationsAPIBackend = undefined;
    });

    it('Должна проходить проверка андроидной версии для Станции', () => {
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

        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION)).toEqual(true);
        });
    });

    it('Должна проходить проверка андроидной версии для Станции Макс', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [20, 94],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION_2)).toEqual(true);
        });
    });

    it('Должна проходить проверка андроидной версии для Станции Макс 2', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [20, 110],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION_2)).toEqual(true);
        });
    });

    it('Должна проходить проверка андроидной версии для Станции Макс 3', () => {
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

        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION_2)).toEqual(true);
        });
    });

    it('Должна НЕ проходить проверка андроидной версии для Станции Макс', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [20, 93],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => true,
                isIos: () => false,
            };
        });

        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION_2)).toEqual(false);
        });
    });

    it('Должна НЕ проходить проверка андроидной версии для Станции Макс 2', () => {
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

        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION_2)).toEqual(false);
        });
    });

    it('Должна проходить проверка айосной версии для Станции', () => {
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

        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION)).toEqual(true);
        });
    });

    it('Должна проходить проверка айосной версии для Станции Макс', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [44, 0],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => true,
            };
        });

        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION_2)).toEqual(true);
        });
    });

    it('Должна проходить проверка айосной версии для Станции Макс 2', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [44, 1],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => true,
            };
        });

        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION_2)).toEqual(true);
        });
    });

    it('Должна проходить проверка айосной версии для Станции Макс 3', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [45, 0],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => true,
            };
        });

        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION_2)).toEqual(true);
        });
    });

    it('Должна НЕ проходить проверка айосной версии для Станции Макс', () => {
        jest.doMock('pp/get-parsed-app-version', () => {
            return {
                getParsedAppVersion: () => [43, 999],
            };
        });
        jest.doMock('pp/platform', () => {
            return {
                isAndroid: () => false,
                isIos: () => true,
            };
        });

        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION_2)).toEqual(false);
        });
    });
});
