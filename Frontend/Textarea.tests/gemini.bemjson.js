var sizes = ['xs', 's', 'm'],
    data = [
    '',
    [
        'Вполне вероятно, что это даже многострочный текст получится.',
        'Нельзя быть абсолютно уверенным, но такое возможно.'
    ].join(' ')
];

({
    block: 'b-page',
    title: 'textarea',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            attrs: {id: 'base'},
            content: sizes.map(size => {
                const mods = Object.assign({theme: 'normal'}, {size: size});
                return {
                    elem: 'capture',
                    elemMods: {size: size},
                    content: [
                        {},
                        {disabled: 'yes'},
                        {'has-clear': 'yes'},
                        {'has-clear': 'yes', disabled: 'yes'}
                    ].map(state => {
                        const extend = Object.assign({}, mods, state);
                        return {
                            elem: 'variation',
                            elemMods: state,
                            content: data.map(text => {
                                return {
                                    elem: 'item',
                                    elemMods: {'has-value': text && 'yes'},
                                    content: {
                                        block: 'textarea',
                                        mods: extend,
                                        text: text
                                    }
                                };
                            })
                        };
                    })
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
