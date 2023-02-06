before(function() {

    window.mock['recipients'] = [
        {
            params: {recipientsIds: ["\"Test Test\" <test@ya.ru>"]},
            data: {
                "\"Test Test\" <ctest@ya.ru>": {
                    "displayName": "Test Test",
                    "type": "avatar",
                    "color": "#b8c1d9",
                    "mono": "TT",
                    "local": "test",
                    "url": "//betastatic.yastatic.net/mail/socialavatars/socialavatars/v4/person.svg",
                    "urlSmall": "//betastatic.yastatic.net/mail/socialavatars/socialavatars/v4/person.svg",
                    "domain": "ya.ru",
                    "valid": true,
                    "email": "Test Test <ctest@ya.ru>",
                    "isSelf": false
                }
            }
        }
    ];

});
