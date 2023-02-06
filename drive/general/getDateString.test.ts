import { I18nLocale } from 'shared/consts/I18nLocale';
import { getDateString } from 'shared/helpers/getDateString/getDateString';

describe('getDateString', function () {
    it('works with empty params', function () {
        expect(getDateString(null, I18nLocale['en-US'])).toBeUndefined();
    });

    it('works with full params', function () {
        const options: Intl.DateTimeFormatOptions = {
            day: '2-digit',
            month: '2-digit',
            year: '2-digit',
        };

        expect(getDateString(new Date('2022-01-01'), I18nLocale['en-US'], options)).toStrictEqual('01/01/22');
        expect(getDateString(new Date('2022-01-01'), I18nLocale['ru-RU'], options)).toStrictEqual('01.01.22');
    });
});
