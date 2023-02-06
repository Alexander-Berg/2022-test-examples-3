import isYandexLink from '../isYandexLink';

describe('isYandexLink', () => {
    it('Is yandex link', () => {
        expect(isYandexLink('https://rasp.yandex.ru')).toBe(true);
        expect(isYandexLink('https://rasp.yandex.by')).toBe(true);
        expect(isYandexLink('https://rasp.yandex.some')).toBe(true);
        expect(isYandexLink('https://rasp.yandex.ru/info/format/')).toBe(true);
        expect(
            isYandexLink('https://testing.morda-front.rasp.common.yandex.ru'),
        ).toBe(true);
        expect(
            isYandexLink('https://testing.morda-front.rasp.common.yandex.by'),
        ).toBe(true);
        expect(
            isYandexLink('https://testing.morda-front.rasp.common.yandex.some'),
        ).toBe(true);
        expect(
            isYandexLink(
                'https://testing.morda-front.rasp.common.yandex.ru/info/format/',
            ),
        ).toBe(true);
        expect(
            isYandexLink(
                'https://experiments3.morda-front.rasp.common.yandex.ru',
            ),
        ).toBe(true);
        expect(
            isYandexLink(
                'https://experiments3.morda-front.rasp.common.yandex.by',
            ),
        ).toBe(true);
        expect(
            isYandexLink(
                'https://experiments3.morda-front.rasp.common.yandex.some',
            ),
        ).toBe(true);
        expect(
            isYandexLink(
                'https://experiments3.morda-front.rasp.common.yandex.ru/info/format/',
            ),
        ).toBe(true);
        expect(isYandexLink('https://t.rasp.yandex.ru')).toBe(true);
        expect(isYandexLink('https://t.rasp.yandex.by')).toBe(true);
        expect(isYandexLink('https://t.rasp.yandex.ru/info/format/')).toBe(
            true,
        );
        expect(isYandexLink('https://yandex.ru')).toBe(true);
        expect(isYandexLink('https://travel.yandex.ru')).toBe(true);
        expect(isYandexLink('https://t.yandex.ru')).toBe(true);
        expect(isYandexLink('https://t.travel.yandex.ru')).toBe(true);
        expect(isYandexLink('/something')).toBe(true);
    });

    it('Is not yandex link', () => {
        expect(isYandexLink('https://google.com')).toBe(false);
        expect(isYandexLink('google.com')).toBe(false);
        expect(
            isYandexLink(
                'https://experiments3.morda-front.rasp.commonyandex.ru',
            ),
        ).toBe(false);
    });
});
