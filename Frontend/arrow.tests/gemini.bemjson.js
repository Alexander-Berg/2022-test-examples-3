({
    block: 'b-page',
    title: 'Arrow',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            content: [
                {
                    block: 'gemini-arrow',
                    content: {
                        block: 'arrow',
                        content: {
                            elem: 'text',
                            content: 'Тут какой-то текст'
                        }
                    }
                },
                {block: 'separator', tag: 'br'},
                {
                    block: 'gemini-arrow-search',
                    content: {
                        block: 'arrow',
                        mods: {type: 'search'},
                        content: {
                            block: 'search2',
                            input: {
                                block: 'input',
                                mods: {size: 'm'},
                                content: {
                                    elem: 'control',
                                    attrs: {
                                        name: 'text',
                                        tabindex: 1,
                                        autocomplete: 'off',
                                        maxlength: 400
                                    }
                                }
                            },
                            button: {
                                block: 'button',
                                mods: {size: 'm', theme: 'normal'},
                                type: 'submit',
                                tabindex: 2,
                                content: 'Найти'
                            }
                        }
                    }
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
