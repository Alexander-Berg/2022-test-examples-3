import { appendPpQueryParams } from './append-pp-query-params';
import { createApphostContextStub } from './create-apphost-context-stub';

describe('helpers/append-pp-query-params', () => {
    it('Должна получать версию ПП и ОС для Android', () => {
        expect(appendPpQueryParams({}, createApphostContextStub({
            device: {
                browser:
                    {
                        version: '20.73',
                        name: 'YandexSearch',
                        engineVersion: '537.36',
                        engine: 'WebKit',
                        baseVersion: '83.0.4103.116',
                        base: 'Chromium',
                    },
                is_touch: 1,
                type: 'device',
                os: { name: '', version: '10', family: 'Android' },
                is_browser: 1,
                is_robot: 0,
                device: { name: '', model: '', vendor: 'Xiaomi', id: '' },
                is_same_site_supported: 1,
                is_tv: 0,
                is_tablet: 0,
                is_mobile: 1,
                version: 'INIT.device 2.0',
            },
        }))).toEqual({
            app_platform: 'android',
            app_version_name: '20.73',
            os_version: '10',
        });
    });

    it('Должна получать версию ПП и ОС для iOS', () => {
        expect(appendPpQueryParams({}, createApphostContextStub({
            device: {
                browser:
                    {
                        version: '36.00',
                        name: 'YandexSearch',
                        engineVersion: '605.1.15',
                        engine: 'WebKit',
                        baseVersion: '605.1.15',
                        base: 'Safari',
                    },
                is_touch: 1,
                type: 'device',
                os: { name: '', version: '13.6', family: 'iOS' },
                is_browser: 1,
                is_robot: 0,
                device: { name: 'iPhone', model: 'iPhone', vendor: 'Apple', id: '7065' },
                is_same_site_supported: 0,
                is_tv: 0,
                is_tablet: 0,
                is_mobile: 1,
                version: 'INIT.device 2.0',
            },
        }))).toEqual({
            app_platform: 'iphone',
            app_version_name: '36.00',
            os_version: '13.6',
        });
    });

    it('НЕ должна брать параметры других браузеров и ОС', () => {
        expect(appendPpQueryParams({}, createApphostContextStub({
            device: {
                browser:
                    {
                        version: '85.0.4177.0',
                        name: 'Chrome',
                        engineVersion: '537.36',
                        engine: 'WebKit',
                        baseVersion: '85.0.4177.0',
                        base: 'Chromium',
                    },
                is_touch: 0,
                type: 'device',
                os: { name: '', version: '10.15.5', family: 'MacOS' },
                is_browser: 1,
                is_robot: 0,
                device: { name: '', model: '', vendor: '', id: '' },
                is_same_site_supported: 1,
                is_tv: 0,
                is_tablet: 0,
                is_mobile: 0,
                version: 'INIT.device 2.0',
            },
        }))).toEqual({});
    });

    it('НЕ должна перетирать параметры ПП', () => {
        const origParams = {
            manufacturer: 'Xiaomi',
            app_id: 'ru.yandex.searchplugin.dev',
            app_version: '11050000',
            app_build_number: '55809',
            dp: '2.875',
            lang: 'ru-RU',
            size: '1080,2000',
            model: 'Mi A2',
            app_version_name: '11.50',
            os_version: '10',
            app_platform: 'android',
            some_param: '1234',
        };

        expect(appendPpQueryParams({ ...origParams }, createApphostContextStub({
            device: {
                browser:
                    {
                        version: '36.00',
                        name: 'YandexSearch',
                        engineVersion: '605.1.15',
                        engine: 'WebKit',
                        baseVersion: '605.1.15',
                        base: 'Safari',
                    },
                is_touch: 1,
                type: 'device',
                os: { name: '', version: '13.6', family: 'iOS' },
                is_browser: 1,
                is_robot: 0,
                device: { name: 'iPhone', model: 'iPhone', vendor: 'Apple', id: '7065' },
                is_same_site_supported: 0,
                is_tv: 0,
                is_tablet: 0,
                is_mobile: 1,
                version: 'INIT.device 2.0',
            },
        }))).toEqual(origParams);
    });
});
