({
    block: 'x-page',
    content: [
        {
            block: 'gemini',
            attrs: {style: 'display: inline-block;'},
            content: [
                {
                    block: 'user-account',
                    name: 'Иван Персидский'
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'user-account',
                    mods: {'has-accent-letter': 'yes'},
                    name: 'Иван Персидский'
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'user-account',
                    pic: {avatarId: '20706/84473936-5041676'},
                    mods: {
                        'has-accent-letter': 'yes',
                        'has-ticker': 'yes',
                        test: 'focus'
                    },
                    url: 'https://yandex.ru',
                    ticker: {count: 300, maxCount: 99},
                    name: 'John Doe'
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'user-account',
                    pic: {avatarId: false},
                    mods: {'has-accent-letter': 'yes', 'has-ticker': 'yes'},
                    ticker: {count: 3, maxCount: 99},
                    name: 'John Doe'
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'user-account',
                    pic: {avatarId: false},
                    mods: {
                        'has-accent-letter': 'yes',
                        'has-ticker': 'yes',
                        'hide-name': 'yes'
                    },
                    ticker: {count: 3, maxCount: 99},
                    name: 'John Doe'
                }
            ]
        }
    ]
});
