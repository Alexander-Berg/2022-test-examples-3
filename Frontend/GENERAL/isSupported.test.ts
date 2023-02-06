import { assert } from 'chai';
import { OsFamily } from '@yandex-int/frontend-apphost-context';

import { isSupported } from './isSupported';

describe('isSupported()', () => {
    describe('должен возвращать true для поддерживаемых браузеров', () => {
        it('YandexBrowser на новых iOS', () => {
            assert.isTrue(isSupported(
                { family: OsFamily.iOS, version: '12.0' },
                { name: 'YandexBrowser', version: '17.9.0.2031.10' },
            ));
        });

        it('Относительно новый Safari', () => {
            assert.isTrue(isSupported(
                { family: OsFamily.iOS, version: '9.1' },
                { name: 'MobileSafari', version: '9.0' },
            ));
        });

        it('Относительно новый ChromeMobile', () => {
            assert.isTrue(isSupported(
                { family: OsFamily.Android, version: '8.0.0' },
                { name: 'ChromeMobile', version: '73.0.3638' },
            ));
        });

        it('Свежий firefox в Android', () => {
            assert.isTrue(isSupported(
                { family: OsFamily.Android, version: '10.0' },
                { name: 'MobileFirefox', version: '40.0' },
            ));
        });

        it('Свежий firefox в iOS', () => {
            assert.isTrue(isSupported(
                { family: OsFamily.iOS, version: '9.1' },
                { name: 'MobileFirefox', version: '34.0' },
            ));
        });

        it('iOS без версии браузера', () => {
            // Пример https://error.yandex-team.ru/projects/web4/errors/17122801674520681167/useragents?filter=runtime%20==%20nodejs
            assert.isTrue(isSupported(
                { family: OsFamily.iOS, version: '13.4.1' },
                { name: 'MobileSafari', version: '' },
            ));
        });
    });

    describe('должен возвращать false для неподдерживаемых браузеров', () => {
        it('YandexBrowser на старых iOS', () => {
            assert.isFalse(isSupported(
                { family: OsFamily.iOS, version: '8.4.1' },
                { name: 'YandexBrowser', version: '17.9.0.2031.10' },
            ));
        });

        it('Старый Safari', () => {
            assert.isFalse(isSupported(
                // Тут специально версия выше, чтобы убедиться, что проверка выполняется и по версии браузера
                { family: OsFamily.iOS, version: '9.1' },
                { name: 'MobileSafari', version: '8.0' },
            ));
        });

        it('Старый AndroidBrowser', () => {
            assert.isFalse(isSupported(
                { family: OsFamily.Android, version: '4.4.4' },
                { name: 'AndroidBrowser', version: '4.4.4' },
            ));
        });

        it('Старые версии Android неподдерживаются, не смотря на браузер', () => {
            assert.isFalse(isSupported(
                { family: OsFamily.Android, version: '4.1' },
                { name: 'ChromeMobile', version: '73.0.3638' },
            ));
        });

        it('Все версии Internet Explorer', () => {
            assert.isFalse(isSupported(
                { family: OsFamily.Windows, version: '10.0' },
                { name: 'MSIE', version: '11.0' },
            ));
        });

        it('Старый firefox', () => {
            assert.isFalse(isSupported(
                { family: OsFamily.Android, version: '10.0' },
                { name: 'MobileFirefox', version: '39.0' },
            ));
        });

        it('Без названия ОС', () => {
            assert.isFalse(isSupported(
                { version: '7.1.2' },
                { name: 'YandexBrowser', version: '17.9.0.2031.10' },
            ));
        });

        it('Без названия браузера', () => {
            assert.isFalse(isSupported(
                { family: OsFamily.Android, version: '10.0.0' },
                { name: '', version: '17.9.0' },
            ));
        });
    });
});
