'use strict';

const expect = require('expect');

const React = require('react');
const ReactDOMServer = require('react-dom/server');
const Tanker = require('../lib');

Tanker.setTankerProjectId('project');
Tanker.addTranslation('ru', require('./fixtures/loc.ru.js'));

const r = ReactDOMServer.renderToStaticMarkup.bind(ReactDOMServer);

describe('react-tanker', function() {
    describe('React Component I18N', function() {
        [
            [
                {
                    key: 'common',
                    params: {},
                    children: []
                },
                'toEqual',
                '<span>Ошибка!</span>'
            ],
            [
                {
                    key: 'with_param',
                    params: { name: 'foo' },
                    children: []
                },
                'toEqual',
                '<span>Не удалось сохранить файл «foo» на Диск.</span>'
            ],
            [
                {
                    key: 'with_param_and_custom_tag',
                    params: { name: 'foo' },
                    children: [
                        React.createElement('a', {
                            'data-ref': 'folder',
                            key: 'folder',
                            href: '#trash'
                        }, 'Корзина')
                    ]
                },
                'toEqual',
                '<span>Файл «foo» перемещен в папку «<a data-ref="folder" href="#trash">Корзина</a>».</span>'
            ],
            [
                {
                    key: 'with_not_self-closed_tag',
                    params: { name: 'bar' },
                    children: [
                        React.createElement('a', {
                            'data-ref': 'folder',
                            key: 'folder',
                            href: '#images'
                        })
                    ]
                },
                'toEqual',
                '<span>Файл «bar» скопирован в папку «<a data-ref="folder" href="#images">Изображения</a>».</span>'
            ],
            [
                {
                    key: 'with_only_tag',
                    params: {},
                    children: [
                        React.createElement('a', {
                            'data-ref': 'link',
                            key: 'link',
                            href: '#link'
                        }, 'Ссылка')
                    ]
                },
                'toEqual',
                '<a data-ref="link" href="#link">Ссылка</a>'
            ],
            [
                {
                    key: 'with_absent_child',
                    params: {},
                    children: []
                },
                'toThrow',
                'LOC child Sasha not found'
            ],
            [
                {
                    key: 'with_not_string_ref',
                    params: {},
                    children: [
                        React.createElement('span', {
                            'data-ref': 1,
                            key: 1
                        }, "Контент")
                    ]
                },
                'toEqual',
                '<span>Странный child.</span>'
            ],
            [
                {
                    key: 'with_deprecated_ref',
                    params: { name: 'foo' },
                    children: [
                        React.createElement('a', {
                            ref: 'folder',
                            key: 'folder',
                            href: '#trash'
                        }, 'Корзина')
                    ]
                },
                'toEqual',
                '<span>Файл перемещен в папку «<a href="#trash">Корзина</a>».</span>'
            ],
            [
                {
                    key: 'with_className',
                    params: {
                        className: 'i18n_class'
                    },
                    children: []
                },
                'toEqual',
                '<span class="i18n_class">Просто текст</span>'
            ],
            [
                {
                    key: 'with_props_tag',
                    params: {
                        tag: 'div'
                    },
                    children: []
                },
                'toEqual',
                '<div>Просто текст</div>'
            ],
            [
                {
                    key: 'with_props_tag_and_className',
                    params: {
                        className: 'test',
                        tag: 'div'
                    },
                    children: []
                },
                'toEqual',
                '<div class="test">Просто текст</div>'
            ],
            [
                {
                    key: 'with_props_tag_and_only_tag',
                    params: {
                        className: 'test',
                        tag: 'div'
                    },
                    children: [
                        React.createElement('a', {
                            'data-ref': 'link',
                            key: 'link',
                            href: '#link'
                        }, 'Ссылка')
                    ]
                },
                'toEqual',
                '<div class="test"><a data-ref="link" href="#link">Ссылка</a></div>'
            ],
            [
                {
                    key: 'double_clone',
                    params: {},
                    children: [
                        React.createElement('kbd', { 'data-ref': 'key' })
                    ]
                },
                'toEqual',
                '<span>Press <kbd data-ref="key">Ctrl</kbd> + <kbd data-ref="key">S</kbd></span>'
            ],
            [
                {
                    key: 'double_br',
                    params: {
                        tag: 'b'
                    },
                    children: []
                },
                'toEqual',
                '<b>1<br/>2<br/>3</b>'
            ]
        ].forEach((item) => {
            it(item[0].key + ' -> ' + item[2], function() {
                const params = item[0].params;
                params.lang = 'ru';
                params.keyset = 'keyset';
                params.loc = item[0].key;

                let expected;

                // в случае toThrow expect принимает не результат вызова функции,
                // а саму функцию, и потом вызывает ее под капотом
                if (item[1] === 'toThrow') {
                    expected = () => r(React.createElement(Tanker.I18N, params, item[0].children));
                } else {
                    expected = r(React.createElement(Tanker.I18N, params, item[0].children));
                }

                expect(expected)[item[1]](item[2]);
            });
        });
    });
    describe('Raw i18n', function() {
        [
            ['common', {}, 'Ошибка!'],
            ['with_param', { name: 'foo' }, 'Не удалось сохранить файл «foo» на Диск.']
        ].forEach((item) => {
            it(item[0] + ' -> ' + item[2], function() {
                expect(Tanker.i18n('ru', 'keyset', item[0], item[1])).toEqual(item[2]);
            });
        });
    });
});
