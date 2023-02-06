({
    block: 'b-page',
    head: [
        {elem: 'css', url: '_desktop.css', ie: false},
        {elem: 'css', url: '_desktop', ie: true}
    ],
    content: [
        // Размеры.
        {
            block: 'gemini',
            id: 'size-head',
            content: [
                {
                    elem: 'item',
                    content: [
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: 'head'},
                            text: 'button'
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: 'head'},
                            iconLeft: {mods: {type: 'load'}},
                            text: 'button'
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: 'head'},
                            iconRight: {mods: {type: 'load'}},
                            text: 'button'
                        }
                    ]
                }
            ]
        },
        {tag: 'br'},
        {
            block: 'gemini',
            id: 'other',
            content: [
                {
                    elem: 'item',
                    content: [
                        {
                            block: 'button2',
                            mods: {theme: 'clear', size: 'm', checked: 'yes', hovered: 'yes'},
                            iconLeft: {mods: {type: 'load'}},
                            text: 'button'
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'clear', size: 'm', checked: 'yes', disabled: 'yes'},
                            iconLeft: {mods: {type: 'load'}},
                            text: 'button'
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: 'm', disabled: 'yes'},
                            iconLeft: {mods: {type: 'load'}},
                            text: 'button'
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'action', size: 'm', disabled: 'yes'},
                            iconLeft: {mods: {type: 'load'}},
                            text: 'button'
                        }
                    ]
                }
            ]
        },
        {tag: 'br'},
        {
            block: 'gemini',
            id: 'progress',
            content: ['classic', 'default', 'red', 'grey', 'dark'].map(function(tone) {
                const isClassic = tone === 'classic';
                return [{
                    block: 'button2',
                    mods: {
                        size: isClassic ? 'm' : 'n',
                        tone: !isClassic && tone,
                        view: isClassic ? 'classic' : 'default',
                        theme: 'action',
                        progress: 'yes',
                        disabled: 'yes'
                    },
                    text: 'button'
                }, '\u00a0'];
            })
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
        {elem: 'js', url: '_desktop.js'}
    ]
});
