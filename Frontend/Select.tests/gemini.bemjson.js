({
    block: 'b-page',
    title: 'select',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini-max-width',
            js: true,
            content: {
                block: 'select',
                name: 'mail',
                mods: {size: 'm', theme: 'normal'},
                content: [
                    {
                        block: 'button',
                        mods: {theme: 'normal', size: 'm'},
                        content: 'Отправленные'
                    },
                    {
                        elem: 'control',
                        content: [
                            {
                                elem: 'option',
                                attrs: {value: 'send'},
                                content: 'Отправленные'
                            },
                            {
                                elem: 'option',
                                attrs: {value: 'draft'},
                                content: 'Черновики'
                            },
                            {
                                elem: 'option',
                                attrs: {value: 'del'},
                                content: 'УдаленныеУдаленныеУдаленныеУдаленныеУдаленныеУдаленныеУдаленныеУдаленные'
                            },
                            {
                                elem: 'option-group',
                                attrs: {label: 'Option group'},
                                content: [
                                    {
                                        elem: 'option',
                                        attrs: {value: 'tvc'},
                                        content: 'ТВЦ'
                                    },
                                    {
                                        elem: 'option',
                                        attrs: {value: 'ng'},
                                        content: 'National Geographic'
                                    }
                                ]
                            },
                            {
                                elem: 'option-group',
                                attrs: {label: 'Еще группа'},
                                content: [
                                    {
                                        elem: 'option',
                                        attrs: {value: 'poisoned'},
                                        content: 'Отравленные'
                                    },
                                    {
                                        elem: 'option',
                                        attrs: {value: 'exploded'},
                                        content: 'Взорванные'
                                    }
                                ]
                            },
                            {
                                elem: 'option',
                                attrs: {value: 'unknown'},
                                content: 'Непознанные'
                            }
                        ]
                    }
                ]
            }
        },
        {block: 'example-workaround'},
        {
            block: 'gemini-long-select',
            js: true,
            content: {
                block: 'select',
                name: 'country',
                mods: {size: 'm', theme: 'normal'},
                rows: 4,
                js: {
                    rows: 4,
                    popupParams: {
                        directions: [
                            {to: 'bottom', axis: 'left', offset: {top: 8}}
                        ]
                    }
                },
                content: [
                    {
                        block: 'button',
                        mods: {theme: 'normal', size: 'm'},
                        content: 'Опция 99'
                    },
                    {
                        elem: 'control',
                        content: (function() {
                            var i = 100,
                            arr = [];
                            while(i--) {
                                arr.push({
                                    elem: 'option',
                                    attrs: {value: i},
                                    content: 'Опция ' + i
                                });
                            }

                            return arr;
                        })()
                    }
                ]
            }
        },
        {block: 'wrapper'},
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
