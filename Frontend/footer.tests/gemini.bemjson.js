({
    block: 'b-page',
    title: 'Яндекс',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            attrs: {style: 'height: 120px;'}, // Для фиксации ширины футера
            content: [
                {
                    block: 'gemini',
                    mix: {block: 'gemini-simple'},
                    attrs: {style: 'margin-top: 15px;'},
                    content: {
                        block: 'footer',
                        start: 2005,
                        tld: 'ru',
                        statUrl: '//stat.yandex.ru/stats.xml?ReportID=-225&ProjectID=1'
                    }
                },
                {
                    block: 'gemini',
                    mix: {block: 'gemini-custom'},
                    attrs: {style: 'margin-top: 15px;'},
                    content: {
                        block: 'footer',
                        content: [
                            {
                                elem: 'column',
                                content: [
                                    {
                                        elem: 'link',
                                        url: '#',
                                        content: 'Первая ссылка'
                                    },
                                    {
                                        elem: 'link',
                                        url: '#',
                                        content: 'Вторая ссылка'
                                    }
                                ]
                            },
                            {
                                elem: 'column',
                                elemMods: {side: 'right'},
                                content: {
                                    block: 'copyright',
                                    content: 'Компания ответственных профессионалов'
                                }
                            }
                        ]
                    }
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
