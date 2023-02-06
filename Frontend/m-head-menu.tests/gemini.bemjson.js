({
    block: 'x-page',
    content: [
        {
            block: 'gemini',
            content: [{
                block: 'm-head',
                mods: {type: 'staff'},
                menuItems: [
                    {name: 'Страница 1', url: '#'},
                    {
                        name: 'Страница 2',
                        current: 'yes',
                        subs: [
                            {name: 'Второй подпункт', url: '#', current: 'yes'},
                            {name: 'Третий подпункт', url: '#'}
                        ]
                    },
                    {
                        more: 'yes',
                        subs: [
                            {name: 'Страница 3', url: '#'},
                            {name: 'Страница 4', url: '#'}
                        ]
                    }
                ]
            }]
        }
    ]
});
