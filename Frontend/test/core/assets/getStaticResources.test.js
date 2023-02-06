const { baseConfig } = require('./data');

// делаем небольшой трюк: устанавливаем конфиг до запроса assets;
// это нужно потому, что в ряде модулей он статичен и не меняется во время запроса
const runtimeConfig = require('../../../core/assets/runtimeConfig');
runtimeConfig.setConfig(baseConfig);

// получаем assets после установки конфига
const assets = require('../../../core/assets/assets');

describe('AssetsApi', () => {
    beforeEach(() => assets.setConfig(baseConfig));
    afterEach(() => assets.setConfig(require('../../../configs/current/config')()));

    describe('getStaticResources', () => {
        it('Возвращает правильную ссылку на основной бандл бэма для десктопов', () => {
            assets.setPlatform('desktop');
            expect(assets.getMainBundlePath()).toEqual('/turbo-static-path/main-bundle_desktop_hash.js');
        });

        it('Возвращает правильную ссылку на основной бандл бэма для тачей', () => {
            assets.setPlatform('touch-phone');
            expect(assets.getMainBundlePath()).toEqual('/turbo-static-path/main-bundle_phone_hash.js');
        });

        it('Возвращает правильную ссылку на основной бандл реакта', () => {
            expect(assets.getReactBundlePath()).toEqual('/turbo-static-path/bundles-common_hash.js');
        });

        it('полный объект для декстопов', () => {
            assets.setPlatform('desktop');
            const expected = {
                reactUrl: 'https://yandex.ru/common-static-path/react.js',
                mainBundle: '/turbo-static-path/main-bundle_desktop_hash.js',
                reactBundle: '/turbo-static-path/bundles-common_hash.js',
            };

            expect(assets.getStaticResources()).toEqual(expected);
        });

        it('полный объект для тачей', () => {
            assets.setPlatform('touch-phone');
            const expected = {
                reactUrl: 'https://yandex.ru/common-static-path/react.js',
                mainBundle: '/turbo-static-path/main-bundle_phone_hash.js',
                reactBundle: '/turbo-static-path/bundles-common_hash.js',
            };

            expect(assets.getStaticResources()).toEqual(expected);
        });
    });

    describe('getPrefetchStaticResources', () => {
        it('полный объект для тачей', () => {
            assets.setPlatform('touch-phone');
            const expected = {
                reactUrl: 'https://yandex.ru/common-static-path/react.js',
                mainBundle: '/turbo-static-path/main-bundle_phone_hash.js',
                reactBundle: '/turbo-static-path/bundles-common_hash.js',
            };

            expect(assets.getPrefetchStaticResources()).toEqual(expected);
        });
    });
});
