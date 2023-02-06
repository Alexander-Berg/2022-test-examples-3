import { assert } from 'chai';
import VHCookies from './index';

declare const global: {
    document: {}
};

describe('VH-Cookies', function() {
    describe('get()', function() {
        beforeAll(function() {
            let cookie = '';

            Object.defineProperty(global.document, 'cookie', {
                set(val) {
                    cookie = val;
                },
                get() {
                    return cookie;
                },
                enumerable: true,
            });
        });

        it('возвращает значение куки', function() {
            document.cookie = 'mda=0; _ym_uid=22222x; yandexuid=1234567890; yandex_gid=213; yc=1551712746.zen.cach%3A1551457142';

            assert.strictEqual(VHCookies.get('yandexuid'), '1234567890');
        });

        it('возвращает корректное значение частично закодированной куки', function() {
            document.cookie = ('bar=test%3Byandexuid%3D111%3Afoo; yandexuid=123');
            assert.strictEqual(VHCookies.get('yandexuid'), '123');
        });
    });
});
