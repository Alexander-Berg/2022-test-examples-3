const { iso88591toUTF8 } = require('../../../src/server/utils/iso88591-to-utf8');

describe('iso88591toUTF8', () => {
    it('Должен перевести текст с кодировкой iso-8859-1 в UTF-8', () => {
        const str = 'ÑÐ²Ð°Ð»ÑÐ´';
        const expectedValue = 'ывалыд';

        assert.equal(iso88591toUTF8(str), expectedValue);
    });
});
