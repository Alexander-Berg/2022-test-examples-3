'use strict';

const proxyquire = require('proxyquire');
const sinon = require('sinon');
const { assert } = require('chai');
const Collection = require('../helpers/Collection');

sinon.assert.expose(assert, { prefix: '' });

const gitStub = {};

const cssConsts = proxyquire('../linters/css-consts.js', {
    '../lib/vcs': gitStub,
    chalk: {
        red: () => '# error',
        yellow: () => '# warning',
        green: () => '# text'
    }
});

describe('githooks / cssConsts', () => {
    function test(diff, files = ['foo.scss']) {
        const log = sinon.stub();

        gitStub.diffIn = sinon.stub();
        gitStub.diffIn.returns(diff);

        cssConsts(new Collection(files), { log });

        assert.notCalled(log);
    }

    it('Проверяет файлы scss', () => {
        assert.throws(function() {
            test('+ color: red;', ['foo.scss']);
        });
    });

    it('Распознаёт КАПС', () => {
        assert.throws(function() {
            test('+  COLOR: RED');
        });
    });

    it('Пропускает изменения без изменений (???)', () => {
        assert.doesNotThrow(function() {
            test('');
        });
    });

    it('Пропускает удаления', () => {
        assert.doesNotThrow(function() {
            test('- color: red');
        });
    });

    it('Пропускает изменения в файлах не стилей', () => {
        assert.doesNotThrow(function() {
            test('+ color: red', ['foo.js']);
        });
    });

    describe('colors', () => {
        it('Распознает экспериментальный уровень', () => {
            assert.throws(function() {
                test('+  color: red;', ['experiments/beauty_font/blocks-common/b-page/foo.scss']);
            });

            assert.throws(function() {
                test('+  color: red;', ['src/experiments/beauty_font/blocks-common/b-page/foo.scss']);
            });
        });

        it('Распознаёт keyword-значения цветов', () => {
            assert.throws(function() {
                test('+  color: red;');
            });

            assert.throws(function() {
                test('+  color: fuchsia;');
            });
        });

        it('Распознаёт RGB-значения цветов', () => {
            assert.throws(function() {
                test('+  color: rgb(255,0,153)');
            });

            assert.throws(function() {
                test('+  background: 1px solid rgba(1e2, .5e1, .5e0, +.25e2%)');
            });
        });

        it('Распознаёт Hex-значения цветов', () => {
            assert.throws(function() {
                test('+  color: #ff0000');
            });

            assert.throws(function() {
                test('+  background: 1px solid #ff00ff');
            });
        });

        it('Распознаёт HSL-значения цветов', () => {
            assert.throws(function() {
                test('+  color: hsl(270,60%,70%)');
            });

            assert.throws(function() {
                test('+  background: 1px solid hsla(240 100% 50% / .05)');
            });
        });

        it('Распознаёт свойство background-color', () => {
            assert.throws(function() {
                test('+  background-color: red ');
            });
        });

        it('Распознаёт свойство background', () => {
            assert.throws(function() {
                test('+ background: 1px solid red;');
            });
        });

        it('Распознаёт свойства border и border-*', () => {
            assert.throws(function() {
                test('+ border: 1px solid red;');
            });

            assert.throws(function() {
                test('+ border-bottom: 1px solid red;');
            });
        });

        it('Распознаёт свойство -webkit-tap-highlight-color', () => {
            assert.throws(function() {
                test('+ -webkit-tap-highlight-color: #ccc;');
            });
        });

        it('Распознаёт цвета вместе с константами', () => {
            assert.throws(function() {
                test('+  border-color: $color-black-10 #fff $color-black-24;');
            });
        });

        it('Пропускает transparent, none, inherit, currentColor', () => {
            assert.doesNotThrow(function() {
                test('+  color: transparent;');
                test('+  color: none;');
                test('+  color: inherit;');
                test('+  color: currentColor;');
            });
        });

        it('Пропускает константы из дизайн-системы', () => {
            assert.doesNotThrow(function() {
                test('+  background-color: var(--color-white)');
                test('+  color: var(--color-green-url)');
                test('+  border-bottom: 1px solid var(--color-g-stroke)');

                test('+  background-color: $color-red-hover;');
                test('+  color: $color-black-text');
                test('+  border-bottom: 1px solid $color-yellow;');
            });
        });

        it('Пропускает несколько констант', () => {
            assert.doesNotThrow(function() {
                test('+  border-color: $color-black-10 $color-black-8 $color-black-24;');
            });
        });

        it('Пропускает вхождения в комментариях', () => {
            assert.doesNotThrow(function() {
                test('+  background: $color-neoblue-5; // #eeeff2');
                test('+  background: $color-neoblue-5; // rgba(0 0 0 / 10%);');
                test('+  background: $color-neoblue-5; // rgba(0, 0, 0, .1);');
                test('+  background: $color-neoblue-5; // rgba(#fff, .1);');
                test('+  background: $color-neoblue-5; // hsl(0, 0, 0);2');
                test('+  background: $color-neoblue-5; // rgba($color-white, .1);');
                test('+  background: $color-neoblue-5; // orange;');
                test('+  box-shadow: $color-neoblue-5; // 0 1px 0 $color-border, 0 4px 6px $color-border;');
                test('+  box-shadow: $color-neoblue-5; // $color-border; // $color-border');

                test('+  background: var(--color-neoblue-5); // #eeeff2');
                test('+  background: var(--color-neoblue-5); // rgba(0 0 0 / 10%);');
                test('+  background: var(--color-neoblue-5); // rgba(0, 0, 0, .1);');
                test('+  background: var(--color-neoblue-5); // rgba(#fff, .1);');
                test('+  background: var(--color-neoblue-5); // hsl(0, 0, 0);2');
                test('+  background: var(--color-neoblue-5); // rgba($color-white, .1);');
                test('+  background: var(--color-neoblue-5); // orange;');
                test('+  box-shadow: var(--color-neoblue-5); // 0 1px 0 $color-border, 0 4px 6px $color-border;');
                test('+  box-shadow: var(--color-neoblue-5); // $color-border; // $color-border');
            });
        });

        it('Пропускает названия стилей с похожими на цвет комбинации', () => {
            assert.doesNotThrow(function() {
                test('+ .my_color_red_riding_hood');
                test('+ .color:before');
                test('+ .color:active');
            });
        });

        it('Пропускает объявление констант с цветами', () => {
            assert.doesNotThrow(function() {
                test('+ $color-black-83: #222;');
                test('+ $color-black-85-transparent: rgba(0, 0, 0, .85);');
                test('+ $color-video-gradient: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, .1) 50%, rgba(0, 0, 0, .3) 100%);');
                test('+ $color-lightblue-20-no-border: 0 0 0 0 rgba(13, 35, 67, .092), 0 3.3px 10px -4px rgba(13, 35, 67, .4);');
            });
            assert.doesNotThrow(function() {
                test('+ --color-black-83: #222;');
                test('+ --color-black-85-transparent: rgba(0, 0, 0, .85);');
                test('+ --color-video-gradient: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, .1) 50%, rgba(0, 0, 0, .3) 100%);');
                test('+ --color-lightblue-20-no-border: 0 0 0 0 rgba(13, 35, 67, .092), 0 3.3px 10px -4px rgba(13, 35, 67, .4);');
            });
        });
    });

    describe('typography & radius', () => {
        it('Пропускает экспериментальный уровень', () => {
            assert.doesNotThrow(function() {
                test('+  font-size: 16px;', ['experiments/beauty_font/blocks-common/b-page/foo.scss']);
            });

            assert.doesNotThrow(function() {
                test('+  font-size: 16px;', ['src/experiments/beauty_font/blocks-common/b-page/foo.scss']);
            });
        });

        it('Распознаёт keyword-значения, px, em, rem, %', () => {
            assert.throws(function() {
                test('+  font-size: xx-large');
            });

            assert.throws(function() {
                test('+  font-size: 16px');
            });

            assert.throws(function() {
                test('+  font-size: .8em');
            });

            assert.throws(function() {
                test('+  font-size: 1.6rem');
            });

            assert.throws(function() {
                test('+  font-size: 120%');
            });
        });

        it('Распознаёт свойство font-size', () => {
            assert.throws(function() {
                test('+  font-size: 16px');
            });
        });

        it('Распознаёт свойство line-height', () => {
            assert.throws(function() {
                test('+  line-height: 16px');
            });
        });

        it('Распознаёт свойство font', () => {
            assert.throws(function() {
                test('+  font: 1.2em "Fira Sans", sans-serif');
            });
        });

        it('Распознаёт свойство border-radius и border-*-radius', () => {
            assert.throws(function() {
                test('+  border-radius: 10% 30% 50% 70%;');
            });

            assert.throws(function() {
                test('+  border-top-right-radius: 3px 4px;');
            });
        });

        it('Пропускает значения 0, none, inherit', () => {
            assert.doesNotThrow(function() {
                test('+  font-size: 0;');
                test('+  font-size: none;');
                test('+  font-size: inherit;');
            });
        });

        it('Пропускает константы из дизайн-системы', () => {
            assert.doesNotThrow(function() {
                test('+  font-size: var(--text-s)');
                test('+  font: var(--text-s) "Fira Sans", sans-serif');

                test('+  font-size: $text-s;');
                test('+  font: $text-s "Fira Sans", sans-serif;');
            });
        });

        it('Пропускает константы вместе с 0', () => {
            assert.doesNotThrow(function() {
                test('+  border-radius: 0 var(--size-border-radius-m) 0 0;');
                test('+  border-radius: 0 $size-border-radius-m 0 0;');
            });
        });
    });

    describe('shadows', () => {
        it('Распознаёт цвета', () => {
            assert.throws(function() {
                test('+  box-shadow: 10px 5px 5px red;');
            });

            assert.throws(function() {
                test('+  box-shadow: inset 5em 1em gold;');
            });

            assert.throws(function() {
                test('+  box-shadow: 3px 3px red, -1em 0 .4em olive;');
            });
        });

        it('Распознает несколько констант', () => {
            assert.throws(function() {
                test('+  box-shadow: 0 1px 0 var(--color-g-stroke), 0 4px 6px $color-border');
            });
        });

        it('Пропускает константы из дизайн-системы', () => {
            assert.doesNotThrow(function() {
                test('+  box-shadow: var(--shadow-action-10)');
                test('+  box-shadow: var(--shadow-g-lightblue-10);');
            });
        });
    });

    describe('animations', () => {
        it('Распознаёт тайминги s и ms', () => {
            assert.throws(function() {
                test('+  transition-duration: 10s');
            });

            assert.throws(function() {
                test('+  transition: margin-right 4s, color 1s;');
            });
        });

        it('Распознаёт тайминг-функции', () => {
            assert.throws(function() {
                test('+  animation-timing-function: linear');
            });

            assert.throws(function() {
                test('+  animation-timing-function: steps(4, jump-end)');
            });

            assert.throws(function() {
                test('+  animation-timing-function: cubic-bezier(0.1, 0.7, 1.0, 0.1)');
            });
        });

        it('Распознаёт свойства animation и transition', () => {
            assert.throws(function() {
                test('+  transition: margin-right 4s ease-in-out 1s');
            });

            assert.throws(function() {
                test('+  animation: slidein 3s linear 1s infinite running');
            });
        });

        it('Распознаёт свойства animation-timing-function и transition-timing-function', () => {
            assert.throws(function() {
                test('+  animation-timing-function: linear');
            });

            assert.throws(function() {
                test('+  transition-timing-function: linear');
            });
        });

        it('Распознаёт свойства animation-duration и transition-duration', () => {
            assert.throws(function() {
                test('+  animation-duration: 10s');
            });

            assert.throws(function() {
                test('+  transition-duration: 10s');
            });
        });

        it('Распознаёт свойства animation-delay и transition-delay', () => {
            assert.throws(function() {
                test('+  animation-delay: 2s');
            });

            assert.throws(function() {
                test('+  transition-delay: 2s');
            });
        });

        it('Пропускает initial, unset, inherit, 0', () => {
            assert.doesNotThrow(function() {
                test('+  transition-duration: initial;');
                test('+  transition-duration: unset;');
                test('+  transition-duration: inherit;');
                test('+  transition-duration: 0;');
            });
        });

        it('Пропускает константы из дизайн-системы', () => {
            assert.doesNotThrow(function() {
                test('+  animation-duration: var(--timing-m)');
                test('+  animation-delay: var(--timing-s)');
                test('+  animation-timing-function: var(--ease-default)');
                test('+  animation: slidein var(--timing-s) var(--ease-default) var(--timing-m) infinite running');

                test('+  animation-duration: $timing-xs;');
                test('+  animation-delay: $timing-xs;');
                test('+  animation-timing-function: $ease-hide;');
                test('+  animation: slidein $timing-xs $ease-hide $timing-xs infinite running');
            });
        });
    });
});
