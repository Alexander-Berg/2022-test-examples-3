import isRelativeLink from '../isRelativeLink';

describe('isRelativeLink', () => {
    it('Is relative link', () => {
        expect(isRelativeLink('/something')).toBe(true);
        expect(isRelativeLink('/as/was/das?p=1')).toBe(true);
    });

    it('Is not relative link', () => {
        expect(isRelativeLink('https://yandex.ru')).toBe(false);
        expect(isRelativeLink('https://yandex.ru/stuff')).toBe(false);
        expect(isRelativeLink('https://travel.yandex.ru')).toBe(false);
        expect(isRelativeLink('https://rasp.yandex.ru')).toBe(false);
        expect(isRelativeLink('https://t.travel.yandex.ru')).toBe(false);
        expect(
            isRelativeLink(
                'https://experiments3.morda-front.rasp.common.yandex.ru',
            ),
        ).toBe(false);
        expect(isRelativeLink('https://google.com/something')).toBe(false);
    });
});
