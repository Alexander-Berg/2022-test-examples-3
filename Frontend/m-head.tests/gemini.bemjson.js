({
    block: 'x-page',
    content: [
        {
            block: 'gemini',
            attrs: {id: 'basic'},
            content: [{
                block: 'm-head',
                service: 'service',
                title: 'Сервис'
            }]
        },
        {
            block: 'gemini',
            attrs: {id: 'menu-items'},
            content: [{
                block: 'm-head',
                service: 'service',
                title: 'menu-items',
                menuItems: [
                    {name: 'Страница 1', url: '#'},
                    {name: 'Страница 2', current: 'yes'},
                    {name: 'Страница 3'}
                ]
            }]
        },
        {
            block: 'gemini',
            attrs: {id: 'full-of-stuff'},
            content: [{
                block: 'm-head',
                mods: {type: 'staff', 'over-paranja': 'yes'},
                service: 'staff',
                title: 'Стафф',
                menuItems: [
                    {name: 'Страница 1', url: '#'},
                    {name: 'Страница 2', current: 'yes'},
                    {name: 'Страница 3'}
                ],
                action: 'Новое действие',
                search: true,
                user: 'lego-changelogger'
            }]
        }
    ]
});
