import { isBeautifulUrl } from '../isBeautifulUrl';

describe('isBeautifulUrl', () => {
    it('Если в queryString нет ни text=, ни stub=, то урл в новом формате', () => {
        expect(isBeautifulUrl('?utm_referrer=zen&srcrwr=RF:yandex.ru:1:2'))
            .toStrictEqual(true);
    });

    it('Если в queryString есть text=, то урл в старом формате', () => {
        expect(isBeautifulUrl('?text=https%3A%2F%2Ffarkop.ru&srcrwr=RF:yandex.ru:1:2'))
            .toStrictEqual(false);
    });

    it('Если в queryString есть stub=, то урл в старом формате', () => {
        expect(isBeautifulUrl('?utm_referrer=zen&stub=image/default.json'))
            .toStrictEqual(false);
    });

    it('Если пустой queryString, то урл в новом формате', () => {
        expect(isBeautifulUrl('')).toStrictEqual(true);
    });
});
