import { DeviceType } from 'types/device-type';
import { getClassnameForDeviceType } from 'utils/get-classname-for-device-type';

describe('utils/get-classname-for-device-type', () => {
    it('Должен вернуть имя класса для устройства УД', () => {
        expect(getClassnameForDeviceType(DeviceType.LIGHT)).toEqual('light');
    });

    it('Должен вернуть имя класса для устройства УД 2', () => {
        expect(getClassnameForDeviceType(DeviceType.MEDIA_DEVICE_TV_BOX)).toEqual('media-device-tv-box');
    });

    it('Должен вернуть имя класса для общего типа колонки', () => {
        expect(getClassnameForDeviceType(DeviceType.SMART_SPEAKER)).toEqual('smart-speaker');
    });

    it('Должен вернуть имя класса для Станции Лайт', () => {
        expect(getClassnameForDeviceType(DeviceType.SMART_SPEAKER_YANDEX_STATION_MICRO)).toEqual('station-micro');
    });

    it('Должен вернуть имя класса для колонки LG', () => {
        expect(getClassnameForDeviceType(DeviceType.SMART_SPEAKER_LG_XBOOM_WK7Y)).toEqual('lg-xboom-wk7y');
    });

    it('Должен вернуть имя класса для Модуля', () => {
        expect(getClassnameForDeviceType(DeviceType.MEDIA_DEVICE_DONGLE_YANDEX_MODULE)).toEqual('module');
    });

    it('Должен вернуть имя класса для Модуля 2', () => {
        expect(getClassnameForDeviceType(DeviceType.MEDIA_DEVICE_DONGLE_YANDEX_MODULE_2)).toEqual('module-2');
    });
});
