module.exports = {
    block: 'b-page',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        ['normal', 'pseudo', 'clear'].map(getThemeBemjson),
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

function getThemeBemjson(theme) {
    var sizes = ['xs', 's', 'm', 'l', 'head'],
        mods = [{}, {
            pale: 'yes'
        }, {
            checked: 'yes'
        }, {
            disabled: 'yes'
        }, {
            pressed: 'yes'
        }, {
            hovered: 'yes'
        }, {
            checked: 'yes',
            disabled: 'yes'
        }, {
            checked: 'yes',
            pressed: 'yes'
        }, {
            checked: 'yes',
            hovered: 'yes'
        }, {
            pressed: 'yes',
            hovered: 'yes'
        }];

    return {
        block: 'gemini-sample-theme',
        content: [{
        block: 'gemini-sample-theme-' + theme,
       content: [
            {
                tag: 'h3',
                content: 'theme: ' + theme
            },
            sizes.map(function(size) {
                return {
                    block: 'gemini-sample-theme-size',
                    content: [{
                    block: 'gemini-sample-' + theme + '-' + size,
                    content: [{
                            tag: 'h4',
                            content: [
                                size + ':',
                                {tag: 'br'}
                            ]
                        },
                        mods.map(function(mod) {
                            var fullmode = Object.assign({}, {
                                    size: size,
                                    theme: theme
                                }, mod),
                                onlyIconMode = Object.assign({}, {
                                    size: size,
                                    theme: theme,
                                    'only-icon': 'yes'
                                }, mod);

                            return {
                                elem: 'item',
                                tag: 'span',
                                content: ['\u00a0', '\u00a0', {
                                        block: 'check-button',
                                        name: theme + '-' + size + '-simple',
                                        value: 10,
                                        mods: fullmode,
                                        mix: [{
                                            block: 'gemini-sample-' + theme + '-' + size,
                                            elem: 'check-button'
                                        }],
                                        content: 'btn'
                                    },
                                    '\u00a0', {
                                        block: 'gemini-bg-color',
                                        content: {
                                            block: 'check-button',
                                            name: theme + '-' + size + '-bg-color',
                                            value: 10,
                                            mods: fullmode,
                                            mix: [{
                                                block: 'gemini-sample-' + theme + '-' + size,
                                                elem: 'check-button'
                                            }],
                                            content: 'btn'
                                        }
                                    },
                                    '\u00a0', {
                                        block: 'check-button',
                                        name: theme + '-' + size + '-icon-right',
                                        value: 10,
                                        mods: fullmode,
                                        mix: [{
                                            block: 'gemini-sample-' + theme + '-' + size,
                                            elem: 'check-button'
                                        }],
                                        content: [{
                                            elem: 'text',
                                            content: 'right'
                                        }, {
                                            block: 'icon',
                                            mods: {
                                                type: 'close'
                                            },
                                            mix: {
                                                block: 'check-button',
                                                elem: 'icon'
                                            }
                                        }]
                                    },
                                    '\u00a0', {
                                        block: 'check-button',
                                        name: theme + '-' + size + 'icon-left',
                                        value: 10,
                                        mods: fullmode,
                                        mix: [{
                                            block: 'gemini-sample-' + theme + '-' + size,
                                            elem: 'check-button'
                                        }],
                                        content: [{
                                            block: 'icon',
                                            mods: {
                                                type: 'close'
                                            },
                                            mix: [{
                                                block: 'check-button',
                                                elem: 'icon'
                                            }]
                                        }, {
                                            elem: 'text',
                                            content: 'left'
                                        }]
                                    },
                                    '\u00a0', {
                                        block: 'check-button',
                                        name: theme + '-' + size + '-only-icon',
                                        value: 10,
                                        mods: onlyIconMode,
                                        mix: [{
                                            block: 'gemini-sample-' + theme + '-' + size,
                                            elem: 'check-button'
                                        }],
                                        content: [{
                                            block: 'icon',
                                            mods: {
                                                type: 'close'
                                            },
                                            mix: [{
                                                block: 'check-button',
                                                elem: 'icon'
                                            }]
                                        }]
                                    },
                                    {tag: 'br'}
                                ]
                            };
                        })
                    ]
                }]
                };
            })
        ]
        }]
    };
}
