'use strict';

import expect from 'expect.js';

import React from 'react';
import ReactDOMServer from 'react-dom/server';

import { addTranslation, i18n, I18N } from 'helpers/i18n';
import { setLang } from 'helpers/lang';

const render = (elem) => ReactDOMServer.renderToStaticMarkup(elem);

setLang('test');
addTranslation('test', { 'yandex_disk_widget_save': { 'test': {
    simple: () => 'text',
    with_params: (params) => '(' + params['text'] + ')',
    with_tag: () => '(<x-tag>-</x-tag>)',
    with_empty_tag: () => '(<x-tag/>)',
}}});

describe('Локализация (helpers/i18n)', function() {
    describe('Функция (i18n)', function() {
        it('simple', function() {
            expect(i18n('test', 'simple')).to.be('text');
        });
        it('with params', function() {
            expect(i18n('test', 'with_params', { text: 'hello' })).to.be('(hello)');
        });
    });
    describe('React-компонент (I18N)', function() {
        it('simple', function() {
            const elem = <I18N keyset="test" loc="simple"/>;
            expect(render(elem)).to.be('<span>text</span>');
        });
        it('with params', function() {
            const elem = <I18N keyset="test" loc="with_params" text="hello"/>;
            expect(render(elem)).to.be('<span>(hello)</span>');
        });
        it('with tag', function() {
            const elem = <I18N keyset="test" loc="with_tag">
                <b ref="tag"/>
            </I18N>;
            expect(render(elem)).to.be('<span>(<b>-</b>)</span>');
        });
        it('with empty tag', function() {
            const elem = <I18N keyset="test" loc="with_empty_tag">
                <b ref="tag">!</b>
            </I18N>;
            expect(render(elem)).to.be('<span>(<b>!</b>)</span>');
        });
    });
});
