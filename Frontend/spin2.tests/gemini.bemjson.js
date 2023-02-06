({
    block: 'b-page',
    title: 'Размеры preloader',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        'xxs xs s m l'.split(' ').map(function(size) {
            return {
                block: 'gemini',
                mods: {size: size},
                content: [
                    {
                        block: 'button2',
                        mods: {theme: 'normal', size: size === 'xxs' ? 'xs' : size, type: 'check'},
                        text: size
                    },
                    {
                        block: 'spin2',
                        mods: {size: size}
                    }
                ]
            };
        }),
        {
            block: 'gemini',
            mods: {
                position: 'center'
            },
            content: 'xxs xs s m l'.split(' ').map(function(size) {
                return {
                    block: 'gemini',
                    elem: 'spin-container',
                    content: {
                        block: 'spin2',
                        mods: {
                            size: size,
                            position: 'center',
                            progress: 'yes'
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
});
