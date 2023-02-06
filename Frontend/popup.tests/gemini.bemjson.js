({
    block: 'b-page',
    title: 'popup',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'left',
            content: [
                {
                    block: 'gemini',
                    mix: {block: 'gemini-bottom'},
                    js: true,
                    content: [
                        {
                            block: 'button',
                            mods: {size: 'm', theme: 'normal'},
                            content: 'Я.Owner'
                        },
                        {
                            block: 'popup',
                            attrs: {style: 'width:200px', name: 'gemini-bottom'},
                            mods: {adaptive: 'yes'},
                            /*
                             * Можно обойтись без указания направлений
                             * Раскрытие вниз производится по умолчанию
                             * см islands-popups/common.blocks/popup/popup.js:30
                             */
                            js: {
                                directions: 'bottom'
                            },
                            content: [
                                {elem: 'tail'},
                                {
                                    elem: 'content',
                                    content: [
                                        'Душа моя озарена неземной радостью, как эти чудесные весенние утра, ',
                                        'которыми я наслаждаюсь от всего сердца. Я совсем один и ' +
                                        'блаженствую в здешнем краю, ',
                                        'словно созданном для таких, как я. Я так счастлив, мой друг, ',
                                        'так упоен ощущением покоя, что искусство мое страдает от этого.'
                                    ].join('')
                                }
                            ]
                        }
                    ]
                },
                {
                    block: 'gemini',
                    mix: {block: 'gemini-top'},
                    js: true,
                    content: [
                        {
                            block: 'button',
                            mods: {size: 'm', theme: 'normal'},
                            content: 'Я.Owner'
                        },
                        {
                            block: 'popup',
                            attrs: {style: 'width:200px', name: 'gemini-top'},
                            mods: {adaptive: 'yes'},
                            js: {
                                directions: 'top'
                            },
                            content: [
                                {elem: 'tail'},
                                {
                                    elem: 'content',
                                    content: [
                                        'Душа моя озарена неземной радостью, как эти чудесные весенние утра, ',
                                        'которыми я наслаждаюсь от всего сердца. Я совсем один и ' +
                                        'блаженствую в здешнем краю, ',
                                        'словно созданном для таких, как я. Я так счастлив, мой друг, ',
                                        'так упоен ощущением покоя, что искусство мое страдает от этого.'
                                    ].join('')
                                }
                            ]
                        }
                    ]
                },
                {
                    block: 'gemini',
                    mix: {block: 'gemini-right'},
                    js: true,
                    content: [
                        {
                            block: 'button',
                            mods: {size: 'm', theme: 'normal'},
                            content: 'Я.Owner'
                        },
                        {
                            block: 'popup',
                            attrs: {style: 'width:200px', name: 'gemini-right'},
                            mods: {adaptive: 'yes'},
                            js: {
                                directions: 'right'
                            },
                            content: [
                                {elem: 'tail'},
                                {
                                    elem: 'content',
                                    content: [
                                        'Душа моя озарена неземной радостью, как эти чудесные весенние утра, ',
                                        'которыми я наслаждаюсь от всего сердца. Я совсем один и ' +
                                        'блаженствую в здешнем краю, ',
                                        'словно созданном для таких, как я. Я так счастлив, мой друг, ',
                                        'так упоен ощущением покоя, что искусство мое страдает от этого.'
                                    ].join('')
                                }
                            ]
                        }
                    ]
                },
                {
                    block: 'gemini',
                    mix: {block: 'gemini-left'},
                    js: true,
                    content: [
                        {
                            block: 'button',
                            mods: {size: 'm', theme: 'normal'},
                            content: 'Я.Owner'
                        },
                        {
                            block: 'popup',
                            attrs: {style: 'width:200px', name: 'gemini-left'},
                            mods: {adaptive: 'yes'},
                            js: {
                                directions: 'left'
                            },
                            content: [
                                {elem: 'tail'},
                                {
                                    elem: 'content',
                                    content: [
                                        'Душа моя озарена неземной радостью, как эти чудесные весенние утра, ',
                                        'которыми я наслаждаюсь от всего сердца. Я совсем один и ' +
                                        'блаженствую в здешнем краю, ',
                                        'словно созданном для таких, как я. Я так счастлив, мой друг, ',
                                        'так упоен ощущением покоя, что искусство мое страдает от этого.'
                                    ].join('')
                                }
                            ]
                        }
                    ]
                }
            ]
        },
        {
            block: 'right',
            content: [
                {
                    block: 'gemini',
                    mix: {block: 'gemini-offset'},
                    js: true,
                    content: [
                        {
                            block: 'button',
                            mods: {size: 'm', theme: 'normal'},
                            content: 'Я.Owner'
                        },
                        {
                            block: 'popup',
                            attrs: {style: 'width:200px', name: 'gemini-offset'},
                            mods: {adaptive: 'yes'},
                            js: {
                                directions: [{
                                    to: 'bottom',
                                    offset: {
                                        top: -10
                                    }
                                }, {
                                    to: 'top',
                                    offset: 10
                                }]
                            },
                            content: [
                                {elem: 'tail'},
                                {
                                    elem: 'content',
                                    content: 'Этот попап смещён вверх отрицательным смещением в js-параметрах.'
                                }
                            ]
                        }
                    ]
                },
                {
                    block: 'gemini',
                    mix: {block: 'gemini-nested'},
                    js: true,
                    content: [
                        {
                            block: 'button',
                            mods: {size: 'm', theme: 'normal'},
                            content: 'owner'
                        },
                        {
                            block: 'popup',
                            zIndex: 123,
                            js: {
                                directions: 'right-top'
                            },
                            attrs: {style: 'width: 200px', name: 'gemini-nested'},
                            content: [
                                {elem: 'tail'},
                                {
                                    elem: 'content',
                                    content: [
                                        [
                                            'Далеко-далеко за словесными горами в стране гласных и согласных',
                                            ' живут рыбные тексты. Вдали от всех живут они в буквенных ' +
                                            'домах на берегу ',
                                            'Семантика большого языкового океана. Маленький ручеек Даль журчит по ',
                                            'всей стране и обеспечивает ее всеми необходимыми правилами.'
                                        ].join(''),
                                        {
                                            block: 'gemini',
                                            js: true,
                                            content: [
                                                {
                                                    block: 'button',
                                                    mods: {size: 'm', theme: 'normal'},
                                                    content: 'popup2 owner'
                                                },
                                                {
                                                    block: 'popup',
                                                    mods: {theme: 'ffffff', color: 'drunktankpink'},
                                                    js: {
                                                        directions: 'right-top'
                                                    },
                                                    attrs: {style: 'width:200px', name: 'gemini-nested'},
                                                    content: [
                                                        {elem: 'tail'},
                                                        {
                                                            elem: 'content',
                                                            content: [
                                                                'Далеко-далеко за словесными горами в ' +
                                                                'стране гласных и ',
                                                                ' согласных живут рыбные тексты. Вдали ' +
                                                                'от всех живут они  ',
                                                                'в буквенных домах на берегу Семантика ' +
                                                                'большого языкового ',
                                                                'океана. Маленький ручеек Даль журчит по ' +
                                                                'всей стране и ',
                                                                'обеспечивает ее всеми необходимыми правилами.'
                                                            ].join('')
                                                        }
                                                    ]
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                },
                {
                    block: 'gemini',
                    mix: {block: 'gemini-tail-offset'},
                    js: true,
                    content: [
                        {
                            block: 'button',
                            mods: {size: 'm', theme: 'normal'},
                            content: 'Я.Owner'
                        },
                        {
                            block: 'popup',
                            attrs: {style: 'width:200px', name: 'gemini-tail-offset'},
                            mods: {adaptive: 'yes'},
                            js: {
                                directions: {
                                    to: 'bottom',
                                    tail: {
                                        offset: {
                                            left: 30
                                        }
                                    }
                                }
                            },
                            content: [
                                {elem: 'tail'},
                                {
                                    elem: 'content',
                                    content: 'Хвост этого попапа смещён вправо относительно центра owner.'
                                }
                            ]
                        }
                    ]
                }
            ]
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {elem: 'js', url: 'https://yastatic.net/es5-shims/0.0.1/es5-shims.min.js'}
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {block: 'i-jquery', mods: {version: '1.8.3'}}
        },
        {
            elem: 'cc',
            condition: 'gt IE 8',
            others: true,
            content: {block: 'i-jquery', mods: {version: 'default'}}
        },
        {elem: 'js', url: '_gemini.js'}
    ]
});
