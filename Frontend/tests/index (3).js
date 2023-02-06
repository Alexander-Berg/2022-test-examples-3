'use strict';
/* eslint-env mocha */
const assert = require('chai').assert;
const parsePatterns = require('../server/lib/tanker-parser').parsePatterns;

describe('Тесты для tanker parser`а.', () => {
    it('Верно определяет расположение изображений', done => {
        let res = parsePatterns('Чтобы перейти к расширениям, нажмите ${inline, a.jpg} ' +
            'в браузере и выберите ${block, a.jpg} и выберите ');
        assert.deepEqual(res, [
            'Чтобы перейти к расширениям, нажмите ',
            { block: 'image', mods: { type: 'inline' }, imageName: 'a.jpg' },
            ' в браузере и выберите ',
            { block: 'image', mods: { type: 'block' }, imageName: 'a.jpg' },
            ' и выберите '
        ]);
        done();
    });

    it('Верно определяет расположение изображения, если оно последнее', done => {
        let res = parsePatterns('Чтобы перейти к расширениям, нажмите ${inline, a.jpg} ' +
            'в браузере и выберите ${block, a.jpg}');
        assert.deepEqual(res, [
            'Чтобы перейти к расширениям, нажмите ',
            { block: 'image', mods: { type: 'inline' }, imageName: 'a.jpg' },
            ' в браузере и выберите ',
            { block: 'image', mods: { type: 'block' }, imageName: 'a.jpg' }
        ]);
        done();
    });

    it('Верно определяет расположение изображения, если оно в самом начале', done => {
        let res = parsePatterns('${inline, a.jpg}Чтобы перейти к расширениям, нажмите ' +
            'в браузере и выберите ${block, a.jpg}');
        assert.deepEqual(res, [
            { block: 'image', mods: { type: 'inline' }, imageName: 'a.jpg' },
            'Чтобы перейти к расширениям, нажмите в браузере и выберите ',
            { block: 'image', mods: { type: 'block' }, imageName: 'a.jpg' }
        ]);
        done();
    });

    it('Верно определяет расположение двух последовательных изображений', done => {
        let res = parsePatterns('Чтобы перейти к расширениям, нажмите ' +
            '${inline, a.jpg}${block, a.jpg} в браузере и выберите');
        assert.deepEqual(res, [
            'Чтобы перейти к расширениям, нажмите ',
            { block: 'image', mods: { type: 'inline' }, imageName: 'a.jpg' },
            { block: 'image', mods: { type: 'block' }, imageName: 'a.jpg' },
            ' в браузере и выберите'
        ]);
        done();
    });

    it('Допустимые имена изображений', done => {
        let res = parsePatterns('${inline, a.jpg}');
        assert.deepEqual(res, [{ block: 'image', mods: { type: 'inline' }, imageName: 'a.jpg' }]);
        res = parsePatterns('${inline,/some-folder/a.jpg}');
        assert.deepEqual(res, [{ block: 'image', mods: { type: 'inline' }, imageName: '/some-folder/a.jpg' }]);
        res = parsePatterns('${inline,  some_folder/a.jpg}');
        assert.deepEqual(res, [{ block: 'image', mods: { type: 'inline' }, imageName: 'some_folder/a.jpg' }]);
        done();
    });

    it('Не парсит недопустимые конструкции', done => {
        let res = parsePatterns('Чтобы перейти к расширениям, нажмите ${inline, /some-folder/a.jpg}' +
            '${block,some_folder/a.jpg} в браузере и выберите${block}${block,$/a.jpg}${block,some_folder/a.jpg}');
        assert.deepEqual(res, [
            'Чтобы перейти к расширениям, нажмите ',
            { block: 'image', mods: { type: 'inline' }, imageName: '/some-folder/a.jpg' },
            { block: 'image', mods: { type: 'block' }, imageName: 'some_folder/a.jpg' },
            ' в браузере и выберите', '${block}', '${block,', '$/a.jpg}',
            { block: 'image', mods: { type: 'block' }, imageName: 'some_folder/a.jpg' }]);
        done();
    });

    it('Корректно работает, если в начале строки знак $', done => {
        let res = parsePatterns('${Чтобы перейти к расширениям, нажмите ${inline, /some-folder/a.jpg}' +
            '${block,some_folder/a.jpg} в браузере и выберите${block}${block,$/a.jpg}${block,some_folder/a.jpg}');
        assert.deepEqual(res, [
            '${Чтобы перейти к расширениям, нажмите ',
            { block: 'image', mods: { type: 'inline' }, imageName: '/some-folder/a.jpg' },
            { block: 'image', mods: { type: 'block' }, imageName: 'some_folder/a.jpg' },
            ' в браузере и выберите', '${block}', '${block,', '$/a.jpg}',
            { block: 'image', mods: { type: 'block' }, imageName: 'some_folder/a.jpg' }]);
        done();
    });

    it('Корректно парсит инструкционный блок', done => {
        let res = parsePatterns('Слова${browsers,win.browsers}<p></p>');
        assert.deepEqual(res, [
            'Слова',
            { block: 'keyset', type: 'browsers', content: 'win.browsers' },
            '<p></p>'
        ]);
        done();
    });

    it('Корректно парсит browsers список', done => {
        let res = parsePatterns('Слова${instructions,win.browsers}<p></p>');
        assert.deepEqual(res, [
            'Слова',
            { block: 'keyset', type: 'instructions', content: 'win.browsers' },
            '<p></p>'
        ]);
        done();
    });
});
