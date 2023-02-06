import { DeviceType } from 'types/device-type';
import { getPairingModes } from 'utils/speaker/get-pairing-modes';

// TODO: QUASARUI-3267 Починить тесты. Падают из-за отсутствия js api на возможность пилика и ble
// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('pp/is-speaker-pairing-supported', () => {
    beforeEach(() => {
        jest.doMock('pp/is-pairing-api-supported', () => {
            return {
                isPairingApiSupported: () => true,
            };
        });
    });

    afterEach(() => {
        jest.resetModules();
    });

    it('Должна проходить проверка для Станции', () => {
        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION, getPairingModes)).toEqual(true);
        });
    });

    it('Должна проходить проверка для Станции Макс', () => {
        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.SMART_SPEAKER_YANDEX_STATION_2, getPairingModes)).toEqual(true);
        });
    });

    it('Должна проходить проверка для Модуля', () => {
        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.MEDIA_DEVICE_DONGLE_YANDEX_MODULE, getPairingModes)).toEqual(true);
        });
    });

    it('Не должна проходить проверка для Модуля 2', () => {
        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.MEDIA_DEVICE_DONGLE_YANDEX_MODULE_2, getPairingModes)).toEqual(false);
        });
    });

    it('Не должна проходить проверка для устройств УД', () => {
        return import('pp/is-speaker-pairing-supported').then(({ isSpeakerPairingSupported }) => {
            expect(isSpeakerPairingSupported(DeviceType.LIGHT, getPairingModes)).toEqual(false);
            expect(isSpeakerPairingSupported(DeviceType.MEDIA_DEVICE_TV, getPairingModes)).toEqual(false);
        });
    });
});
