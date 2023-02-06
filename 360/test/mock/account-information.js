before(function() {

    window.mock['account-information'] = [
        {
            params: {},
            data: {
                uid: "24560578",
                suid: "59670986",
                ckey: "kwjlKZ+30yVjKjXyoBdbJj+F67PMhaaQKI6I4/os1MY=",
                sids: ["2"],
                reg_date: "1390414175467",
                region_id: "1000",
                tz_offset: -240,
                email: [
                    {
                        login: 'doochik',
                        domain: 'yandex.ru'
                    },
                    {
                        login: 'doochik',
                        domain: 'yandex.com'
                    },
                    {
                        login: 'my',
                        domain: 'ya.ru'
                    },
                    {
                        login: 'my2',
                        domain: 'ya.ru'
                    },
                    {
                        login: 'my.dot',
                        domain: 'ya.ru'
                    },
                    {
                        login: 'my-dash',
                        domain: 'ya.ru'
                    },
                    {
                        login: 'my.dot-dash',
                        domain: 'ya.ru'
                    },
                    {
                        login: 'myCamelCase',
                        domain: 'ya.ru'
                    }
                ]
            }
        },
        {
            params: {},
            name: "pdd",
            data: {
                "uid": "1130000012316914",
                "suid": "1130000029269304",
                "ckey": "+4VJsJgUbzaPHlRK/iVCqirO2dRG1QdF2plWTi5c+hNS0iEsoX2uBA==",
                "db": "mdb330",
                "dc": "",
                "yandex_account": "spam.zzap@technocat.ru",
                "login": "spam.zzap",
                "region_id": "9999",
                "region_parents": [
                    "9999",
                    "213",
                    "1",
                    "3",
                    "225",
                    "10001",
                    "10000"
                ],
                "locale": "ru",
                "region_code": "ru",
                "region_phone_code": "",
                "country_phone_code": "7",
                "reg_country": "ru",
                "timezone": "Europe/Moscow",
                "reg_date": 1399369224000,
                "use_moko": "true",
                "birthday": "",
                "sids": [
                    "2"
                ],
                "kstatus": "0",
                "home": "yes",
                "email": [
                    {
                        "login": "spam.zzap",
                        "domain": "technocat.ru"
                    },
                    {
                        "login": "hello-test",
                        "domain": "technocat.ru"
                    }
                ]
            }
        },
        {
            params: {},
            name: 'yandex-account-right',
            data: {
                'yandex_account': 'test1'
            }
        },
        {
            params: {},
            name: 'yandex-account-wrong',
            data: {
                'yandex_account': 'test111'
            }
        }
    ];

});
