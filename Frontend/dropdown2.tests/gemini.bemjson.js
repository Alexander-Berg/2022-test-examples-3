({
    block: 'x-page',
    title: 'dropdown gemini tests',
    content: [
        {
            block: 'gemini',
            mix: {block: 'gemini-new'},
            content: (function() {
                var tones = ['default', 'red', 'grey', 'dark'];
                var sizes = ['xs', 's', 'm', 'n'];

                return tones.map(function(tone) {
                    return [
                        sizes.map(function(size) {
                            return {
                                block: 'dropdown2',
                                mods: {
                                    switcher: 'button2',
                                    view: 'default',
                                    tone: tone,
                                    theme: 'normal',
                                    size: size
                                },
                                switcher: {block: 'button2', view: 'default', text: 'dropdown2'},
                                popup: {block: 'popup2', view: 'default', content: 'popup'}
                            };
                        }),
                        {tag: 'br'}
                    ];
                });
            })()
        },
        {tag: 'br'},
        {tag: 'br'},
        {
            block: 'dropdown2',
            cls: 'gemini-1-dropdown',
            mods: {switcher: 'button2', theme: 'normal', size: 's', 'has-tick': 'yes'},
            switcher: {block: 'button2', cls: 'gemini-1-switcher', text: 'has-tick'},
            popup: {block: 'popup2', cls: 'gemini-1-popup', content: 'popup'}
        },
        ' ',
        {
            block: 'dropdown2',
            cls: 'gemini-2-dropdown',
            mods: {switcher: 'button2', theme: 'normal', size: 's', 'has-tick': 'yes', 'has-tail': 'yes'},
            switcher: {block: 'button2', cls: 'gemini-2-switcher', text: 'has-tail'},
            popup: {block: 'popup2', cls: 'gemini-2-popup', content: 'popup'}
        },
        {tag: 'div', attrs: {style: 'height: 50px'}} // Распорка для оперы 12
    ]
});
