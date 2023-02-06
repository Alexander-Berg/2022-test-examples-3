({
    block: 'x-page',
    title: 'm-footer',
    content: [
        {block: 'title', content: 'Минимальная обвязка'},
        {
            block: 'gemini',
            mix: {block: 'gemini', mods: {view: 'minimal'}},
            content: {
                block: 'm-footer',
                email: 'wiki@yandex-team.ru'
            }
        },

        {block: 'title', content: 'Футер в Вики'},
        {
            block: 'gemini',
            mix: {block: 'gemini', mods: {view: 'wiki'}},
            content: {
                block: 'm-footer',
                email: 'wiki@yandex-team.ru',

                vodstvo: '/WackoWiki/WikiSintaksis',
                about: '/WackoWiki',

                additionalData: [// jshint ignore:line
                    {
                        title: 'Эта страница в старом интерфейсе',
                        url: 'https://old.wiki.yandex-team.ru'
                    }
                ]
            }
        },

        {block: 'title', content: 'Футер в Этушке'},
        {
            block: 'gemini',
            mix: {block: 'gemini', mods: {view: 'atushka'}},
            content: {
                block: 'm-footer',
                email: 'atushka@yandex-team.ru',

                mobile: 'https://m.at.yandex-team.ru/?no-detect=no',
                about: 'https://wiki.yandex-team.ru/intranet/atushka/'
            }
        }
    ]
});
