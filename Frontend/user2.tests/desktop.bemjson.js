({
    block: 'x-page',
    content: [
        {
            block: 'gemini',
            mods: {full: 'yes'},
            content: {
                block: 'user2',
                uid: 6789054321,
                yu: 123456789012345678,
                mods: {'has-logout': 'yes'},
                retpath: 'https://yandex.ru',
                passportHost: 'https://passport.yandex.ru',
                name: 'John Doe',
                subname: 'johndoe@ya.ru',
                avatarId: '20706/84473936-5041676',
                unreadCount: 25,

                accounts: [
                    {
                        block: 'user-account',
                        mods: {'has-subname': 'yes'},
                        pic: {avatarId: '21377/28004787-1253930'},
                        name: 'Jane Doe',
                        subname: {provider: 'gg'}
                    },
                    {
                        block: 'user-account',
                        mods: {'has-subname': 'yes'},
                        pic: {avatarId: '0/0-0'},
                        name: 'john-smith@ya.ru',
                        subname: 'john-smith@ya.ru'
                    }
                ]
            }
        }
    ]
});
