import {isValidPartnerLink} from 'projects/partners/utilities/urls/validate';

describe('isValidPartnerLink', () => {
    it('falsy параметр', () => {
        expect(isValidPartnerLink()).toBe(false);
        expect(isValidPartnerLink('')).toBe(false);
    });

    it('ссылка на сторонний ресурс', () => {
        expect(isValidPartnerLink('https://weather.yandex.ru')).toBe(false);
    });

    it('ссылка на неподдерживаемую нацверсию', () => {
        expect(isValidPartnerLink('https://travel.yandex.uz')).toBe(false);
    });

    it('валидная ссылка', () => {
        expect(isValidPartnerLink('https://travel.yandex.ru/hotels')).toBe(
            true,
        );
    });
});
