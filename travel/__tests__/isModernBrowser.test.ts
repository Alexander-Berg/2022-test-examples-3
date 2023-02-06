import IStateUserBrowser from '../../interfaces/state/user/IStateUserBrowser';

import isModernBrowser from '../isModernBrowser';

describe('isModernBrowser', () => {
    it('Для пустого объекта IStateUserBrowser вернет false', () => {
        expect(isModernBrowser({})).toBe(false);
    });

    it('Для неизвестного браузера вернет false', () => {
        expect(
            isModernBrowser({name: 'SuperPuperBrowser', version: '100500'}),
        ).toBe(false);
    });

    it('Для современных браузеров вернет true, для старых false', () => {
        expect(isModernBrowser({name: 'Chrome', version: '70'})).toBe(false);
        expect(isModernBrowser({name: 'Chrome', version: '77'})).toBe(true);
        expect(isModernBrowser({name: 'ChromeMobile', version: '70'})).toBe(
            false,
        );
        expect(isModernBrowser({name: 'ChromeMobile', version: '77'})).toBe(
            true,
        );
        expect(isModernBrowser({name: 'Firefox', version: '70'})).toBe(true);
        expect(isModernBrowser({name: 'Firefox', version: '65'})).toBe(false);
        expect(isModernBrowser({name: 'MobileFirefox', version: '67'})).toBe(
            true,
        );
        expect(isModernBrowser({name: 'MobileFirefox', version: '66'})).toBe(
            false,
        );
        expect(isModernBrowser({name: 'Opera', version: '63'})).toBe(true);
        expect(isModernBrowser({name: 'Opera', version: '62'})).toBe(false);
        expect(isModernBrowser({name: 'OperaMini', version: '62'})).toBe(false);
        expect(isModernBrowser({name: 'OperaMobile', version: '46'})).toBe(
            true,
        );
        expect(isModernBrowser({name: 'OperaMobile', version: '45'})).toBe(
            false,
        );
        expect(isModernBrowser({name: 'Safari', version: '12'})).toBe(true);
        expect(isModernBrowser({name: 'Safari', version: '11'})).toBe(false);
        expect(isModernBrowser({name: 'MobileSafari', version: '11'})).toBe(
            true,
        );
        expect(isModernBrowser({name: 'MobileSafari', version: '10'})).toBe(
            false,
        );
        expect(isModernBrowser({name: 'YandexBrowser', version: '19'})).toBe(
            true,
        );
        expect(isModernBrowser({name: 'YandexBrowser', version: '18'})).toBe(
            false,
        );
        expect(
            isModernBrowser({name: 'Samsung Internet', version: '10.1'}),
        ).toBe(true);
        expect(isModernBrowser({name: 'Samsung Internet', version: '9'})).toBe(
            false,
        );
    });

    it('Поисковое приложение на андройде должно распознаваться как мобильный хром', () => {
        const modernPP: IStateUserBrowser = {
            name: 'YandexSearch',
            base: 'Chromium',
            baseVersion: '77.0.3865.92',
        };

        const oldPP: IStateUserBrowser = {
            name: 'YandexSearch',
            base: 'Chromium',
            baseVersion: '72.0.3865.92',
        };

        expect(isModernBrowser(modernPP)).toBe(true);
        expect(isModernBrowser(oldPP)).toBe(false);
    });

    it('Так как нет нормального способа сопоставить версию браузера поискового приложения на ios с данными для browserlist, вернет false', () => {
        const iosPP: IStateUserBrowser = {
            name: 'YandexSearch',
            base: 'Safari',
            baseVersion: '605.1.15',
        };

        expect(isModernBrowser(iosPP)).toBe(false);
    });
});
