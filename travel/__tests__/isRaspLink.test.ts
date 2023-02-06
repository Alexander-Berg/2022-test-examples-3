import isRaspLink from '../isRaspLink';

describe('isRaspLink', () => {
    it('Is rasp link', () => {
        expect(isRaspLink('https://rasp.yandex.ru')).toBe(true);
        expect(isRaspLink('https://rasp.yandex.by')).toBe(true);
        expect(isRaspLink('https://rasp.yandex.some')).toBe(true);
        expect(isRaspLink('https://rasp.yandex.ru/info/format/')).toBe(true);
        expect(
            isRaspLink('https://testing.morda-front.rasp.common.yandex.ru'),
        ).toBe(true);
        expect(
            isRaspLink('https://testing.morda-front.rasp.common.yandex.by'),
        ).toBe(true);
        expect(
            isRaspLink('https://testing.morda-front.rasp.common.yandex.some'),
        ).toBe(true);
        expect(
            isRaspLink(
                'https://testing.morda-front.rasp.common.yandex.ru/info/format/',
            ),
        ).toBe(true);
        expect(
            isRaspLink(
                'https://experiments3.morda-front.rasp.common.yandex.ru',
            ),
        ).toBe(true);
        expect(
            isRaspLink(
                'https://experiments3.morda-front.rasp.common.yandex.by',
            ),
        ).toBe(true);
        expect(
            isRaspLink(
                'https://experiments3.morda-front.rasp.common.yandex.some',
            ),
        ).toBe(true);
        expect(
            isRaspLink(
                'https://experiments3.morda-front.rasp.common.yandex.ru/info/format/',
            ),
        ).toBe(true);
        expect(isRaspLink('https://t.rasp.yandex.ru')).toBe(true);
        expect(isRaspLink('https://t.rasp.yandex.by')).toBe(true);
        expect(isRaspLink('https://t.rasp.yandex.ru/info/format/')).toBe(true);
    });

    it('Is not rasp link', () => {
        expect(isRaspLink('https://yandex.ru')).toBe(false);
        expect(isRaspLink('https://travel.yandex.ru')).toBe(false);
        expect(isRaspLink('https://t.yandex.ru')).toBe(false);
        expect(isRaspLink('https://t.travel.yandex.ru')).toBe(false);
        expect(
            isRaspLink('https://experiments3.morda-frontrasp.common.yandex.ru'),
        ).toBe(false);
        expect(
            isRaspLink('https://experiments3.morda-front.rasp.commonyandex.ru'),
        ).toBe(false);
    });
});
