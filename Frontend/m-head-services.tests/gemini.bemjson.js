({
    block: 'x-page',
    content: [
        {
            block: 'gemini',
            cls: 'gemini-list',
            content: [{
                block: 'm-head',
                title: 'Стафф',
                mods: {type: 'staff'},
                service: 'staff',
                menuItems: [
                    {name: 'Страница 1', url: '#'},
                    {
                        name: 'Страница 2',
                        current: 'yes',
                        subs: [
                            {name: 'Второй подпункт', url: '#', current: 'yes'},
                            {name: 'Третий подпункт', url: '#', mods: {modname: 'modval'}}
                        ]
                    }
                ]
            }]
        },
        {tag: 'br'},
        {
            block: 'gemini',
            cls: 'gemini-error',
            content: [{
                block: 'm-head',
                title: 'Стафф',
                mods: {error: 'yes', type: 'staff'},
                menuItems: [
                    {name: 'Страница 1', url: '#'},
                    {
                        name: 'Страница 2',
                        current: 'yes',
                        subs: [
                            {name: 'Второй подпункт', url: '#', current: 'yes'},
                            {name: 'Третий подпункт', url: '#', mods: {modname: 'modval'}}
                        ]
                    }
                ]
            }]
        }
    ]
});
