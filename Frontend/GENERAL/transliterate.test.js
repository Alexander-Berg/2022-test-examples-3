const { describe, it } = require('mocha');
const { expect } = require('chai');

const transliterate = require('./transliterate');

describe('transliterate', () => {
    it('ru', () => {
        const actual = transliterate('абвгдезиклмнопрстуфхъьцыАБВГДЕЗИКЛМНОПРСТУФХЪЬЦЫйёжчшщэюяЙЁЖЧШЩЭЮЯ');
        const expected = 'abvgdeziklmnoprstufxcyabvgdeziklmnoprstufxcyjjjozhchshshhjejujajjjozhchshshhjejuja';

        expect(actual).to.eql(expected);
    });

    it('en', () => {
        const actual = transliterate('abcdefghijklmoprqstuvwxyzABCDEFGHIJKLMOPRQSTUVWXYZ');
        const expected = 'abcdefghijklmoprqstuvwxyzabcdefghijklmoprqstuvwxyz';

        expect(actual).to.eql(expected);
    });

    it('digit', () => {
        const digits = '0123456789';
        const actual = transliterate(digits);
        const expected = digits;

        expect(actual).to.eql(expected);
    });

    it('punct', () => {
        const actual = transliterate(',./;\'\\[]§!@#$%^&*()_+-=±`~<>?:"|{}');
        const expected = '/-';

        expect(actual).to.eql(expected);
    });
});
