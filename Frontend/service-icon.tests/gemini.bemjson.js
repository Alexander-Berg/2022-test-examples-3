({
    block: 'b-page',
    title: 'service-icon',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini-line',
            content: {
                block: 'service-icon',
                content: [
                    'afisha', 'auto', 'avia', 'browser', 'disk', 'images', 'mail', 'maps', 'market', 'money',
                    'music', 'news', 'slovari', 'taxi', 'translate', 'video', 'weather', 'advertising', 'direct',
                    'metrika', 'site', 'webmaster', 'gorod', 'master', 'metro', 'rabota', 'time', 'tv', 'books',
                    'kinopoisk', 'rasp', 'tickets', 'kassa', 'autoru'
                ].map(function(name) {
                    return {
                        elem: name,
                        elemMods: {color: 56}
                    };
                })
            }
        },
        {
            block: 'gemini-line',
            content: {
                block: 'service-icon',
                content: [
                    'afisha', 'appsearch', 'auto', 'avia', 'blogs', 'browser', 'fresh', 'gorod', 'images',
                    'mail', 'maps', 'market', 'music', 'news', 'peoplesearch', 'rabota', 'rasp', 'realty', 'review',
                    'search', 'slovari', 'translate', 'tv', 'video', 'weather', 'www', 'autoru'
                ].map(function(name) {
                    return {
                        elem: name,
                        elemMods: {self: 40}
                    };
                })
            }
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
