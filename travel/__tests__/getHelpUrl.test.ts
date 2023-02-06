import Tld from '../../../interfaces/Tld';

import getHelpUrl from '../getHelpUrl';

describe('getHelpUrl', () => {
    it('Если домен поддерживается справкой, то вернет его', () => {
        expect(getHelpUrl(Tld.uz)).toBe('https://yandex.uz/support/rasp/');
    });

    it('Если домен не поддерживается справкой, то вернет ru', () => {
        expect(getHelpUrl(Tld.by)).toBe('https://yandex.ru/support/rasp/');
    });
});
