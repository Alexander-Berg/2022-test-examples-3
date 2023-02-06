import React from 'react';
import { parseReactElementToJSON } from './parseReactElement';

describe('Парсинг React элементов в СПАС-джсон', () => {
    it('Корректно парсит инлайн скрипты', () => {
        const elm = (
            <script id="script" dangerouslySetInnerHTML={{ __html: 'console.log(x)' }} async />
        );

        expect(parseReactElementToJSON(elm)).toEqual({
            type: 'script',
            content: 'console.log(x)',
            attrs: {
                id: 'script',
                async: true,
            },
        });
    });

    it('Корректно парсит remote скрипты', () => {
        const elm = (
            <script id="script" src="./script.js" async />
        );

        expect(parseReactElementToJSON(elm)).toEqual({
            type: 'script',
            url: './script.js',
            content: undefined,
            attrs: {
                id: 'script',
                async: true,
            },
        });
    });

    it('Корректно парсит стили', () => {
        const elm = (
            <style dangerouslySetInnerHTML={{ __html: '.div {}' }} />
        );

        expect(parseReactElementToJSON(elm)).toEqual({
            type: 'style',
            content: '.div {}',
            attrs: {},
        });
    });

    it('Корректно парсит meta-тэги', () => {
        const elm = (
            <meta httpEquiv="refresh" content="3;url=https://www.mozilla.org" />
        );

        expect(parseReactElementToJSON(elm)).toEqual({
            tag: 'meta',
            attrs: {
                'http-equiv': 'refresh',
                content: '3;url=https://www.mozilla.org',
            },
        });
    });
});
