({
    block: 'x-page',
    title: 'Tabs-menu',
    content: [
        {
            // Для Opera, которая не умеет ставить фокус на .tabs-menu__tab.
            tag: 'input',
            attrs: {id: 'focus-holder'}
        },
        ['m', 's'].map(function(size) {
            return [
                // Так как блок инлайновый, отступами не отделаться.
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'gemini',
                    cls: 'gemini-tabs-menu-' + size,
                    content: {
                        block: 'tabs-menu',
                        mods: {size: size, theme: 'normal', layout: 'horiz'},
                        content: [
                            {
                                elem: 'tab',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'normal'},
                                    content: 'Tab 1'
                                },
                                elemMods: {interactive: 'yes', active: 'yes'}
                            },
                            {
                                elem: 'tab',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'normal'},
                                    content: 'Tab 1'
                                },
                                elemMods: {interactive: 'yes', active: 'yes', disabled: 'yes'}
                            },
                            {
                                elem: 'tab',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'normal'},
                                    content: 'Tab 2'
                                },
                                elemMods: {interactive: 'yes', normal: 'yes'}
                            },
                            {
                                elem: 'tab',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'normal'},
                                    content: 'Tab 3'
                                },
                                elemMods: {interactive: 'yes', disabled: 'yes'}
                            }
                        ]
                    }
                }
            ];
        })
    ]
});
