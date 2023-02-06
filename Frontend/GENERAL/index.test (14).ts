import { assert } from 'chai';

import { getGreenUrlText } from '.';

describe('OrgsOverlayAjax', () => {
    describe('getGreenUrlText', () => {
        it('should remove proto from url', () => {
            assert.equal(getGreenUrlText('http://ya.ru/search?text=hello&l10n=ru'), 'ya.ru/search?text=hello&l10n=ru');
            assert.equal(getGreenUrlText('https://ya.ru/search?text=hello&l10n=ru'), 'ya.ru/search?text=hello&l10n=ru');
        });

        it('should remove `www` prefix from url', () => {
            assert.equal(getGreenUrlText('https://www.ya.ru/search?text=hello&l10n=ru'), 'ya.ru/search?text=hello&l10n=ru');
        });

        it('should support non-ascii domains (without punycode transformation)', () => {
            assert.equal(getGreenUrlText('https://кто.рф/search?text=hello&l10n=ru'), 'кто.рф/search?text=hello&l10n=ru');
        });

        it('should remove utm-paramteters', () => {
            assert.equal(getGreenUrlText('https://ya.ru/search?text=hello&utm_source=zen'), 'ya.ru/search?text=hello');
            assert.equal(getGreenUrlText('https://ya.ru/search?utm_source=zen&text=hello'), 'ya.ru/search?text=hello');
            assert.equal(getGreenUrlText('https://ya.ru/search?utm_source=zen'), 'ya.ru/search');
        });

        it('should remove trailing slash', () => {
            assert.equal(getGreenUrlText('https://ya.ru/search/'), 'ya.ru/search');
        });

        it('should return `undefined` on error', () => {
            assert.equal(getGreenUrlText('http://ya.ru/%E0%A4%A'), undefined);
        });
    });
});
