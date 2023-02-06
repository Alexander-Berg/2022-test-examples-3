/* eslint-disable no-unused-expressions */

'use strict';

const { isBrowserValid, removeEmojis } = require('../../server/lib/helper.js');

const { expect } = require('chai');

describe('isBrowserValid', () => {
    const emojisSupportBrowsers = [
        { name: 'chrom', version: 51 },
        { name: 'opera', version: 36 },
        { name: 'firefox', version: 52 },
        { name: 'yandex', version: 16 },
        { name: 'safari', version: 12 }
    ];

    it('should return true for exact name and version', () => {
        const uatraits = { BrowserName: 'opera', BrowserVersion: '36.6.1.768' };
        const isValid = isBrowserValid(uatraits, emojisSupportBrowsers);

        expect(isValid).to.be.true;
    });

    it('should return true for not exact name and correct version', () => {
        const uatraits = { BrowserName: 'YandexBrowser', BrowserVersion: '16.6.1.768' };
        const isValid = isBrowserValid(uatraits, emojisSupportBrowsers);

        expect(isValid).to.be.true;
    });

    it('should return false for exact name and version bellow required', () => {
        const uatraits = { BrowserName: 'opera', BrowserVersion: '15.6.1.768' };
        const isValid = isBrowserValid(uatraits, emojisSupportBrowsers);

        expect(isValid).to.be.false;
    });

    it('should return false for browser which not supports emojis', () => {
        const uatraits = { BrowserName: 'NoEmojiBrowser', BrowserVersion: '16.6.1.768' };
        const isValid = isBrowserValid(uatraits, emojisSupportBrowsers);

        expect(isValid).to.be.false;
    });
});

describe('removeEmojis', () => {
    it('should remove emoji', () => {
        const result = removeEmojis('heart❤');
        const expected = 'heart';

        expect(result).to.be.equal(expected);
    });

    it('should not change string without emoji', () => {
        /* eslint-disable max-len */
        const str = 'Обновления в Яндекс.Директе, Яндекс.Аудиториях, медийной рекламе и других рекламных сервисах Яндекса, исследования рынка рекламы и практические советы — читайте, делитесь в соцсетях и подписывайтесь на наши рассылки, чтобы не пропустить важное.';
        /* eslint-enable max-len */
        const result = removeEmojis(str);

        expect(result).to.be.equal(str);
    });

    it('should remove different  emojis', () => {
        const result = removeEmojis('👑❤🎥❓⁉👽♂');
        const expected = '';

        expect(result).to.be.equal(expected);
    });

    it('should not remove special symbols and html tags', () => {
        const result = removeEmojis('<a href="yandex.ru/adv/?tag=2#pow">!heart❤?❓</a>');
        const expected = '<a href="yandex.ru/adv/?tag=2#pow">!heart?</a>';

        expect(result).to.be.equal(expected);
    });
});
