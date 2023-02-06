import { getDefaultExpPath } from '.';

describe('getDefaultExpPath', () => {
    it('yandex.ru', () => {
        expect(getDefaultExpPath('yandex.ru')).toEqual('https://yandex.ru/ecoo');
    });
    it('yandex-team.ru', () => {
        expect(getDefaultExpPath('mail.yandex-team.ru')).toEqual('https://ecoo.n.yandex-team.ru/ecoo');
    });
    it('yandex.com.tr', () => {
        expect(getDefaultExpPath('yandex.com.tr')).toEqual('https://yandex.com.tr/ecoo');
    });
    it('hamster.yandex.ru', () => {
        expect(getDefaultExpPath('hamster.yandex.ru')).toEqual('https://yandex.ru/ecoo');
    });
    it('Unmatched url', () => {
        expect(getDefaultExpPath('')).toEqual('https://yandex.ru/ecoo');
    });
});
