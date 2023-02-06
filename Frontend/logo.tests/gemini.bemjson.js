var GROUNDS = ['white', 'grey'];

module.exports = {
    block: 'b-page',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            attrs: {
                id: 'static-black'
            },
            content: [
                'ys-ru-64x27',
                'ys-en-66x27',
                'ys-ru-69x28',
                'ys-en-69x28',
                'ys-ru-84x35',
                'ys-en-84x35',
                'ys-ru-86x35',
                'ys-en-87x35',
                'ys-ru-98x42',
                'ys-en-102x42'
            ].map(function(name) {
                return GROUNDS.map(function(bg) {
                    return {
                        elem: 'item',
                        elemMods: {
                            bg: bg
                        },
                        content: {
                            block: 'logo',
                            mods: {
                                name: name
                            }
                        }
                    };
                });
            })
        },
        {
            block: 'gemini',
            attrs: {
                id: 'static-white'
            },
            content: [
                'ys-ru-w-64x27',
                'ys-en-w-66x27',
                'ys-ru-w-69x28',
                'ys-en-w-69x28',
                'ys-ru-w-84x35',
                'ys-en-w-84x35',
                'ys-ru-w-86x35',
                'ys-en-w-87x35',
                'ys-ru-w-98x42',
                'ys-en-w-102x42'
            ].map(function(name) {
                return {
                    elem: 'item',
                    content: {
                        block: 'logo',
                        mods: {
                            name: name
                        }
                    }
                };
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
        {elem: 'js', url: '_gemini.js'}
    ]
};
