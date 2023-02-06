let assert = require('assert');
let _ = require('lodash');
let parser = require('../ext/parsers/bem_serp');
let isObject = _.isObject;

describe('Серповый парсер должен корректно отрабатывать когда', function() {
    it('невозможно определеть кейсет', function() {
        let key = parser({
            type: 'js',
            data: 'BEM.I18N("block" + smth, "ключ");',
        })[0];

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === null, 'key.single !== null');
        assert(key.keyset === null, 'key.keyset !== null');
        assert(key.key === 'ключ', 'key.key !== "ключ"');
        assert(key.value === 'ключ', 'key.value !== "ключ"');
        assert(key.params === false, 'key.params !== false');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });

    it('невозможно определеть кейсет и ключ', function() {
        let key = parser({
            type: 'js',
            data: 'BEM.I18N("block" + smth, anything);',
        })[0];

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === null, 'key.single !== null');
        assert(key.keyset === null, 'key.keyset !== null');
        assert(key.key === null, 'key.key !== null');
        assert(key.value === null, 'key.value !== null');
        assert(key.params === false, 'key.params !== false');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });

    it('невозможно определеть кейсет и ключ, при этом переданы параметры', function() {
        let key = parser({
            type: 'js',
            data: 'BEM.I18N("block" + smth, anything, { param: "pampam" });',
        })[0];

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === null, 'key.single !== null');
        assert(key.keyset === null, 'key.keyset !== null');
        assert(key.key === null, 'key.key !== null');
        assert(key.value === null, 'key.value !== null');
        assert(isObject(key.params), '!isObject(key.params)');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });

    it('ключ на английском', function() {
        let key = parser({
            type: 'js',
            data: 'BEM.I18N("block", "key", { param: "pampam" });',
        })[0];

        assert(key.upload === false, 'key.upload !== false');
        assert(key.single === true, 'key.single !== true');
        assert(key.keyset === 'block', 'key.keyset !== "block"');
        assert(key.key === 'key', 'key.key !== "key"');
        assert(key.value === null, 'key.value !== null');
        assert(isObject(key.params), '!isObject(key.params)');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });

    it('ключ не на английском', function() {
        let key = parser({
            type: 'js',
            data: 'BEM.I18N("block", "ключ", { param: "pampam" });',
        })[0];

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === true, 'key.single !== true');
        assert(key.keyset === 'block', 'key.keyset !== "block"');
        assert(key.key === 'ключ', 'key.key !== "ключ"');
        assert(key.value === 'ключ', 'key.value !== "ключ"');
        assert(isObject(key.params), '!isObject(key.params)');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });

    it('ключ содержит комментарий и контекст', function() {
        let key = parser({
            type: 'js',
            data: 'BEM.I18N("block", "ключ", { comment: "коммент", context: "контекст" });',
        })[0];

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === true, 'key.single !== true');
        assert(key.keyset === 'block', 'key.keyset !== "block"');
        assert(key.key === 'ключ', 'key.key !== "ключ"');
        assert(key.value === 'ключ', 'key.value !== "ключ"');
        assert(isObject(key.params), '!isObject(key.params)');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === 'коммент', 'key.comment !== "коммент"');
        assert(key.context === 'контекст', 'key.context !== "контекст"');
    });

    it('ключ содержит параметры', function() {
        let key = parser({
            type: 'js',
            data: 'BEM.I18N("block", "Фрукты: {frt}. Овощи: {vgt}", { frt: "яблоки", vgt: "картофель" });',
        })[0];

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === true, 'key.single !== true');
        assert(key.keyset === 'block', 'key.keyset !== "block"');
        assert(key.key === 'Фрукты: {frt}. Овощи: {vgt}', 'Incorrect key');
        assert(key.value === 'Фрукты: <i18n:param>frt</i18n:param>. Овощи: <i18n:param>vgt</i18n:param>', 'Incorrect value');
        assert(isObject(key.params), '!isObject(key.params)');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });

    it('ключ в нескольких склонениях', function() {
        let key = parser({
            type: 'js',
            data: 'BEM.I18N("block", "Апельсин", { some: "Апельсина", many: "Апельсинов", count: 45 });',
        })[0];
        let expectedValue = [
            '<i18n:dynamic project="tanker" keyset="dynamic" key="plural_adv">',
            '<i18n:count><i18n:param>count</i18n:param></i18n:count>',
            '<i18n:one>Апельсин</i18n:one>',
            '<i18n:some>Апельсина</i18n:some>',
            '<i18n:many>Апельсинов</i18n:many>',
            '<i18n:none>Апельсинов</i18n:none>',
            '</i18n:dynamic>',
        ].join('');

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === true, 'key.single !== true');
        assert(key.keyset === 'block', 'key.keyset !== "block"');
        assert(key.key === 'Апельсин', 'key.key !== "Апельсин"');
        assert(key.value, expectedValue);
        assert(isObject(key.params), '!isObject(key.params)');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });

    it('ключ параметризованный и в нескольких склонениях', function() {
        let key = parser({
            type: 'js',
            data: 'BEM.I18N("block", "Яблоко{end}", { some: "Яблока{end}", many: "Яблок{end}", none: "Яблок{end}", end: "!", count: 10 });',
        })[0];

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === true, 'key.single !== true');
        assert(key.keyset === 'block', 'key.keyset !== "block"');
        assert(key.key === 'Яблоко{end}', 'key.key !== "Яблоко{end}"');
        assert(
            key.value === [
                '<i18n:dynamic project="tanker" keyset="dynamic" key="plural_adv">',
                '<i18n:count><i18n:param>count</i18n:param></i18n:count>',
                '<i18n:one>Яблоко<i18n:param>end</i18n:param></i18n:one>',
                '<i18n:some>Яблока<i18n:param>end</i18n:param></i18n:some>',
                '<i18n:many>Яблок<i18n:param>end</i18n:param></i18n:many>',
                '<i18n:none>Яблок<i18n:param>end</i18n:param></i18n:none>',
                '</i18n:dynamic>',
            ].join(''),
            'Incorrect value'
        );

        assert(isObject(key.params), '!isObject(key.params)');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });

    it('ключ параметризованный и в нескольких склонениях, параметр count используется в т.ч. как подстановочный', function() {
        let key = parser({
            type: 'js',
            data: 'BEM.I18N("block", "{count} яблоко{end}", { some: "{count} яблока{end}", many: "{count} яблок{end}", end: "?", count: 10 });',
        })[0];

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === true, 'key.single !== true');
        assert(key.keyset === 'block', 'key.keyset !== "block"');
        assert(key.key === '{count} яблоко{end}', 'key.key !== "{count} яблоко{end}"');

        assert(
            key.value === [
                '<i18n:dynamic project="tanker" keyset="dynamic" key="plural_adv">',
                '<i18n:count><i18n:param>count</i18n:param></i18n:count>',
                '<i18n:one><i18n:param>count</i18n:param> яблоко<i18n:param>end</i18n:param></i18n:one>',
                '<i18n:some><i18n:param>count</i18n:param> яблока<i18n:param>end</i18n:param></i18n:some>',
                '<i18n:many><i18n:param>count</i18n:param> яблок<i18n:param>end</i18n:param></i18n:many>',
                '<i18n:none><i18n:param>count</i18n:param> яблок<i18n:param>end</i18n:param></i18n:none>',
                '</i18n:dynamic>',
            ].join(''),
            'Incorrect value'
        );

        assert(isObject(key.params), '!isObject(key.params)');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });

    it('проверяем корректную работу spread оператора', function() {
        let key = parser({
            type: 'js',
            data: `
            const key = { ...{} };
            BEM.I18N("block", "{count} яблоко{end}", { some: "{count} яблока{end}", many: "{count} яблок{end}", end: "?", count: 10 });
            `,
        })[0];

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === true, 'key.single !== true');
        assert(key.keyset === 'block', 'key.keyset !== "block"');
        assert(key.key === '{count} яблоко{end}', 'key.key !== "{count} яблоко{end}"');

        assert(
            key.value === [
                '<i18n:dynamic project="tanker" keyset="dynamic" key="plural_adv">',
                '<i18n:count><i18n:param>count</i18n:param></i18n:count>',
                '<i18n:one><i18n:param>count</i18n:param> яблоко<i18n:param>end</i18n:param></i18n:one>',
                '<i18n:some><i18n:param>count</i18n:param> яблока<i18n:param>end</i18n:param></i18n:some>',
                '<i18n:many><i18n:param>count</i18n:param> яблок<i18n:param>end</i18n:param></i18n:many>',
                '<i18n:none><i18n:param>count</i18n:param> яблок<i18n:param>end</i18n:param></i18n:none>',
                '</i18n:dynamic>',
            ].join(''),
            'Incorrect value'
        );

        assert(isObject(key.params), '!isObject(key.params)');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });

    it('проверяем корректную работу jsx', function() {
        let key = parser({
            type: 'js',
            data: `
            const key = { ...{} };
            const text = BEM.I18N("block", "яблоко{end}", { end: "?" });
            const MyLink = () => (<link>{text}</link>);
            `,
        })[0];

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === true, 'key.single !== true');
        assert(key.keyset === 'block', 'key.keyset !== "block"');
        assert(key.key === 'яблоко{end}', 'key.key !== "яблоко{end}"');

        assert(
            key.value === 'яблоко<i18n:param>end</i18n:param>',
            'Incorrect value'
        );

        assert(isObject(key.params), '!isObject(key.params)');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });

    it('проверяем корректную работу sourceType module', function() {
        let key = parser({
            type: 'js',
            data: `
            import test from './test';
            const key = { ...{} };
            const text = BEM.I18N("block", "яблоко{end}", { end: "?" });
            const MyLink = () => (<link>{text}</link>);
            `,
        })[0];

        assert(key.upload === null, 'key.upload !== null');
        assert(key.single === true, 'key.single !== true');
        assert(key.keyset === 'block', 'key.keyset !== "block"');
        assert(key.key === 'яблоко{end}', 'key.key !== "яблоко{end}"');

        assert(
            key.value === 'яблоко<i18n:param>end</i18n:param>',
            'Incorrect value'
        );

        assert(isObject(key.params), '!isObject(key.params)');
        assert(key.plural === false, 'key.plural !== false');
        assert(key.comment === '', 'key.comment !== ""');
        assert(key.context === '', 'key.context !== ""');
    });
});
