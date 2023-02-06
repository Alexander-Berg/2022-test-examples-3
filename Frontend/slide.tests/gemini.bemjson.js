({
    block: 'b-page',
    title: 'slide',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini-controls',
            content: [
                {
                    block: 'button',
                    mods: {size: 'm', show: 'true', theme: 'normal'},
                    content: 'Show'
                },
                {
                    block: 'button',
                    mods: {size: 'm', hide: 'true', theme: 'normal'},
                    content: 'Hide'
                }
            ]
        },
        {
            block: 'gemini',
            content: [
                {
                    block: 'gemini-top',
                    content: {
                        block: 'slide',
                        js: {
                            rel: [
                                {elem: '.button_show_true', event: 'click', method: 'open'},
                                {elem: '.button_hide_true', event: 'click', method: 'close'}
                            ]
                        },
                        mods: {dur: 0},
                        content: {
                            block: 'gemini-content',
                            content: 'Transition top'
                        }
                    }
                },
                {
                    block: 'gemini-left-right',
                    content: [
                        {
                            block: 'slide',
                            js: {
                                rel: [
                                    {elem: '.button_show_true', event: 'click', method: 'open'},
                                    {elem: '.button_hide_true', event: 'click', method: 'close'}
                                ]
                            },
                            mods: {dir: 'l', dur: 0},
                            content: {
                                block: 'gemini-content',
                                content: 'Transition left'
                            }
                        },
                        {
                            block: 'slide',
                            js: {
                                rel: [
                                    {elem: '.button_show_true', event: 'click', method: 'open'},
                                    {elem: '.button_hide_true', event: 'click', method: 'close'}
                                ]
                            },
                            mods: {dir: 'r', dur: 0},
                            content: {
                                block: 'gemini-content',
                                content: 'Transition right'
                            }
                        }
                    ]
                },
                {
                    block: 'gemini-bottom',
                    content: {
                        block: 'slide',
                        js: {
                            rel: [
                                {elem: '.button_show_true', event: 'click', method: 'open'},
                                {elem: '.button_hide_true', event: 'click', method: 'close'}
                            ]
                        },
                        mods: {dir: 'b', dur: 0},
                        content: {
                            block: 'gemini-content',
                            content: 'Transition bottom'
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
