import { ensureUrl } from './ensureUrl';

describe('ensureUrl', () => {
    it('должна вовзращать валидный URL как есть', () => {
        const testUrl1 = 'https://yandex.ru/a/b/c/?a=b&c=d';
        const testUrl2 = 'http://yandex.ru/a/b/c/?a=b&c=d';

        expect(ensureUrl(testUrl1)).toEqual(testUrl1);
        expect(ensureUrl(testUrl2)).toEqual(testUrl2);
    });

    it('должна подставлять https: в URL, начинающиеся с //', () => {
        const testUrl = '//yandex.ru/a/b/c/?a=b&c=d';

        expect(ensureUrl(testUrl)).toEqual(`https:${testUrl}`);
    });

    it('должна выбрасывать исключения, если URL невалиден', () => {
        const testUrl1 = 'some-strange-url';
        const testUrl2 = '//';

        expect(() => ensureUrl(testUrl1)).toThrow();
        expect(() => ensureUrl(testUrl2)).toThrow();
    });
});
