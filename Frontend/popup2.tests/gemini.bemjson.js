({
    block: 'b-page',
    title: 'simple popup',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    zoom: true,
    attrs: {style: 'height: 600px'}, // For opera
    content: [

        // Directions
        {
            block: 'test',
            js: true,
            mods: {'case': 'directions'},
            content: [
                {elem: 'anchor', cls: 'gemini-directions-anchor', content: 'anchor'},
                ['bottom-left', 'bottom-center', 'bottom-right', 'top-left', 'top-center', 'top-right',
                    'right-top', 'right-center', 'right-bottom', 'left-top', 'left-center', 'left-bottom'
                ].map(function(direction, i) {
                    return {
                        block: 'popup2',
                        cls: 'gemini-directions-popup-' + i,
                        mix: {block: 'test', elem: 'popup'},
                        mods: {target: 'anchor', theme: 'normal'},
                        directions: [direction],
                        content: direction
                    };
                })
            ]
        },

        {
            block: 'test',
            js: true,
            mods: {'case': 'directions'},
            content: [
                {elem: 'anchor', cls: 'gemini-directions-tail-anchor', content: 'anchor'},
                ['bottom-left', 'bottom-center', 'bottom-right', 'top-left', 'top-center', 'top-right',
                    'right-top', 'right-center', 'right-bottom', 'left-top', 'left-center', 'left-bottom'
                ].map(function(direction, i) {
                    return {
                        block: 'popup2',
                        cls: 'gemini-directions-tail-popup-' + i,
                        mix: {block: 'test', elem: 'popup'},
                        mods: {target: 'anchor', theme: 'normal'},
                        directions: [direction],
                        tailOffset: 5,
                        content: [
                            {elem: 'tail'},
                            direction
                        ]
                    };
                })
            ]
        },

        // Nesting
        {
            block: 'test',
            js: true,
            content: [
                {elem: 'anchor', content: 'anchor', cls: 'gemini-nesting-anchor-1'},
                {
                    block: 'popup2',
                    cls: 'gemini-nesting-popup-1',
                    mix: {block: 'test', elem: 'popup'},
                    mods: {target: 'anchor', theme: 'normal'},
                    directions: ['bottom-left'],
                    content: [
                        'level 1',
                        {
                            block: 'test',
                            js: true,
                            content: [
                                {elem: 'anchor', content: 'anchor', cls: 'gemini-nesting-anchor-2'},
                                {
                                    block: 'popup2',
                                    cls: 'gemini-nesting-popup-2',
                                    mix: {block: 'test', elem: 'popup'},
                                    mods: {target: 'anchor', theme: 'normal'},
                                    directions: ['bottom-left'],
                                    content: 'level 2'
                                }
                            ]
                        }
                    ]
                }
            ]
        },

        // Clear
        {
            block: 'test',
            js: true,
            content: [
                {elem: 'anchor', content: 'anchor', cls: 'gemini-clear-anchor'},
                {
                    block: 'popup2',
                    cls: 'gemini-clear-popup',
                    mix: {block: 'test', elem: 'popup'},
                    mods: {target: 'anchor', theme: 'clear'},
                    directions: ['bottom-left'],
                    content: '_theme_clear'
                }
            ]
        },

        {
            block: 'test',
            mix: {block: 'test', mods: {'case': 'directions'}}, // стили широкого anchor
            js: true,
            content: [
                {elem: 'anchor', cls: 'gemini-default-anchor', content: 'anchor'},
                ['default', 'red', 'grey', 'dark'].map(function(tone, i) {
                    var directions = ['top-center', 'right-center', 'bottom-center', 'left-center'];

                    return {
                        block: 'popup2',
                        cls: 'gemini-popup-default-' + i,
                        mix: {block: 'test', elem: 'popup'},
                        mods: {target: 'anchor', theme: 'normal', view: 'default', tone: tone},
                        directions: [directions[i]],
                        content: tone
                    };
                })
            ]
        },

        {
            block: 'test',
            mix: {block: 'test', mods: {'case': 'directions'}}, // стили широкого anchor
            js: true,
            content: [
                {elem: 'anchor', cls: 'gemini-default-anchor-tails', content: 'anchor'},
                ['default', 'red', 'grey', 'dark'].map(function(tone, i) {
                    var directions = ['top-center', 'right-center', 'bottom-center', 'left-center'];

                    return {
                        block: 'popup2',
                        cls: 'gemini-popup-default-tail-' + i,
                        mix: {block: 'test', elem: 'popup'},
                        mods: {target: 'anchor', theme: 'normal', view: 'default', tone: tone},
                        directions: [directions[i]],
                        content: [
                            {elem: 'tail'},
                            tone,
                            {tag: 'br'},
                            directions[i]
                        ]
                    };
                })
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
