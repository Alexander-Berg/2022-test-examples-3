var services = [
    'images',
    'weather',
    'news',
    'mail',
    'video',
    'translate',
    'browser',
    'afisha',
    'disk'
];

({
    block: 'b-page',
    title: 'Блок service. Ссылки с иконками.',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    attrs: {style: 'padding: 20px'},
    content: [
        {
            block: 'gemini',
            content: [
                {
                    block: 'gemini-icon',
                    content: services.map(function(service) {
                        return [
                            {
                                block: 'service',
                                service: service,
                                iconMods: {color: '56'}
                            },
                            {block: 'separator'}
                        ];
                    })
                },
                {
                    block: 'gemini-link',
                    content: services.map(function(service) {
                        return [
                            {
                                block: 'service',
                                service: service,
                                icon: false
                            },
                            {block: 'separator'}
                        ];
                    })
                }
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
