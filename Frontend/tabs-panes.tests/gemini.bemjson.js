({
    block: 'x-page',
    content: [
        {
            block: 'gemini-simple-tabs',
            content: [
                {
                    block: 'tabs',
                    mods: {
                        control: 'menu',
                        size: 'm',
                        theme: 'normal',
                        layout: 'horiz'
                    },
                    panes: 'menu-tabs',
                    content: [
                        {elem: 'tab', content: 'Tab 1', elemMods: {active: 'yes'}},
                        {elem: 'tab', content: 'Tab 2', elemMods: {normal: 'yes'}},
                        {elem: 'tab', content: 'Tab 3', elemMods: {disabled: 'yes'}},
                        {elem: 'tab', content: 'Tab 4'}
                    ]
                },
                {
                    block: 'tabs-panes',
                    id: 'menu-tabs',
                    content: [
                        {elem: 'pane', content: 'Pane 1', elemMods: {active: 'yes'}},
                        {elem: 'pane', content: 'Pane 2'},
                        {elem: 'pane', content: 'Pane 3'},
                        {elem: 'pane', content: 'Pane 4'}
                    ]
                }
            ]
        },
        {
            block: 'x-deps',
            content: [
                {
                    block: 'tabs-menu',
                    mods: {
                        size: 'm',
                        theme: 'normal',
                        layout: 'horiz'
                    }
                }
            ]
        }
    ]
});
