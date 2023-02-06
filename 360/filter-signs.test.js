'use strict';

const filterSigns = require('./filter-signs');

jest.mock('./signature-lang', () => ({
    ids: { 5: 'uk' },
    detect: jest.fn()
}));

const { detect } = require('./signature-lang');

let core;

beforeEach(() => {
    core = {};
});

test('не навернётся при пустом значении', () => {
    const { signs } = filterSigns(core, []);

    expect(signs).toEqual([]);
});

test('преобразует http://webattach в https://webattach (все вхождения)', () => {
    const data = { signs: [ { text: 'http://webattach/someattach http://webattach' } ] };

    const { signs } = filterSigns(core, data.signs);

    expect(signs[0].text).toEqual('https://webattach/someattach https://webattach');
});

test('преобразует http://cache.mail.yandex в https://cache.mail.yandex (все вхождения)', () => {
    const data = { signs: [ { text: 'http://cache.mail.yandex/someattach http://cache.mail.yandex' } ] };

    const { signs } = filterSigns(core, data.signs);

    expect(signs[0].text).toEqual('https://cache.mail.yandex/someattach https://cache.mail.yandex');
});

test('преобразует \u2028 в &#8232; (все вхождения)', () => {
    const data = { signs: [ { text: '&#8232;\u2028&#8232;\u2028' } ] };

    const { signs } = filterSigns(core, data.signs);

    expect(signs[0].text).toEqual('&#8232;&#8232;&#8232;&#8232;');
});

test('не упадёт, если придёт sign без текста', () => {
    const data = { signs: [ {} ] };

    const { signs } = filterSigns(core, data.signs);

    expect(signs).toEqual([]);
});

test('язык сбрасывается на en, если не один из ru,en,uk,be,tr', () => {
    const data = { signs: [ { lang: 'he', text: '' } ] };

    const { signs } = filterSigns(core, data.signs);

    expect(signs[0].lang).toEqual('en');
});

test('userLang выставляется в значение lang подписи', () => {
    const data = { signs: [ { lang: 'he', text: '' } ] };

    const { signs } = filterSigns(core, data.signs);

    expect(signs[0].userLang).toEqual('he');
});

test('если языка нет - берёт из text_traits', () => {
    const data = { signs: [ { text: '', text_traits: { lang: 5 } } ] };

    const { signs } = filterSigns(core, data.signs);

    expect(signs[0].lang).toEqual('uk');
});

test('если языка и text_traits нет - определяет язык по тексту', () => {
    const text = 'какой это язык?';
    const data = { signs: [ { text } ] };

    filterSigns(core, data.signs);

    expect(detect).toBeCalledWith(text);
});

test('is_default - преобразуется в bool', () => {
    const data = { signs: [ { text: '', is_default: 'нет' } ] };

    const { signs } = filterSigns(core, data.signs);

    expect(signs[0].isDefault).toEqual(true);
});

test('associated_emails мапится в emails', () => {
    const emails = [ 'email1@email.com', 'email2' ];
    const data = { signs: [ { text: '', associated_emails: emails } ] };

    const { signs } = filterSigns(core, data.signs);

    expect(signs[0].emails).toEqual(emails);
});
