before(function() {

    /* global mock */
    window.mock['message'] = [
        {
            params: {ids: '666'},
            data: {
                'mid': '666',
                'date': {
                    'chunks': {}
                },
                'fid': '1',
                'lid': [],
                'field': [
                    {
                        'type': 'from',
                        'name': 'test',
                        'email': 'test@ya.ru'
                    }
                ]
            }
        },
        {
            params: {ids: '4'},
            data: {
                'mid': '4',
                'date': {
                    'chunks': {}
                },
                'fid': '7',
                'lid': [],
                'field': [
                    {
                        'type': 'from',
                        'name': 'test',
                        'email': 'test@ya.ru'
                    }
                ]
            }
        },
        {
            params: {ids: '5'},
            data: {
                'mid': '5',
                'date': {
                    'chunks': {}
                },
                'fid': '2',
                'lid': [],
                'field': [
                    {
                        'type': 'from',
                        'name': 'test',
                        'email': 'test@ya.ru'
                    }
                ]
            }
        },
        {
            params: {ids: '6'},
            data: {
                'mid': '6',
                'date': {
                    'chunks': {}
                },
                'fid': '2',
                'lid': ['5']
            }
        },
        {
            params: {ids: '61'},
            data: {
                'mid': '61',
                'date': {
                    'chunks': {}
                },
                'fid': '2',
                lid: []
            }
        },
        // письмо с цитатами от Хабра
        {
            params: {ids: '13.1'},
            data: {
                'mid': '13.1',
                'date': {
                    'chunks': {}
                },
                'fid': '1',
                'lid': ['2170000000007984852']
            }
        },
        // thread
        {
            params: {ids: 't7'},
            data: {
                'mid': 't7',
                'date': {
                    'chunks': {}
                },
                'fid': '7',
                'count': 2,
                'tid': 't7',
                'lid': []
            }
        },
        {
            params: {ids: 'eticket1'},
            data: {
                'mid': 'eticket1',
                'date': {
                    'chunks': {}
                },
                'fid': '2',
                "types": "5\n12\n16",
                "type": [
                    5,
                    12,
                    16
                ],
                "dtype": {
                    "id": "16",
                    "name": "airticket",
                    "sender": true
                },
                "field": [
                    {
                        "name": "Яндекс.Почта",
                        "email": "hello@yandex-team.ru",
                        "type": "from",
                        "ref": "0a2356a42b64cd3859d25ae8963a3a0a",
                        "is_service": true,
                        "is_free": true
                    },
                    {
                        "name": "yt.user28@yandex.ru",
                        "email": "yt.user28@yandex.ru",
                        "type": "to",
                        "ref": "21f2fdcdee502ce2c7bc7962d0343eb6",
                        "is_service": false,
                        "is_free": true
                    }
                ],
                lid: []
            }
        },
        {
            params: {ids: '2300000004010991836'},
            data: {
                'mid': '2300000004010991836',
                'date': {
                    'chunks': {}
                },
                'fid': '2',
                "types": "5\n12\n16",
                "type": [
                    5,
                    12,
                    16
                ],
                "dtype": {
                    "id": "16",
                    "name": "airticket",
                    "sender": true
                },
                "field": [
                    {
                        "name": "Яндекс.Почта",
                        "email": "hello@yandex-team.ru",
                        "type": "from",
                        "ref": "0a2356a42b64cd3859d25ae8963a3a0a",
                        "is_service": true,
                        "is_free": true
                    },
                    {
                        "name": "yt.user28@yandex.ru",
                        "email": "yt.user28@yandex.ru",
                        "type": "to",
                        "ref": "21f2fdcdee502ce2c7bc7962d0343eb6",
                        "is_service": false,
                        "is_free": true
                    }
                ],
                lid: []
            }
        },
        // Письмо ожидает ответа и написано мной
        {
            params: {ids: 'waitforreply'},
            data: {
                mid: 'waitforreply',
                date: {
                    chunks: {}
                },
                lid: ['2480000000552883262'],
                field: [
                    {
                        'type': 'from',
                        'email': 'me@ya.ru'
                    },
                    {
                        'type': 'to',
                        'email': 'doesntmatter@ya.ru'
                    }
                ]
            }
        },
        // письмо для проверки всплывающих нотифаек о новых письмах
        {
            params: {ids: 'newmessage'},
            data: {
                'mid': 'newmessage',
                'date': {
                    'chunks': {}
                },
                'fid': '2',
                lid: []
            }
        },
        {
            params: { ids: 't52' },
            data: {
                'mid': 't52',
                'date': {
                    'chunks': {}
                },
                'fid': '7',
                'count': 2,
                'tid': 't52',
                'lid': [],
                'new': 2
            }
        },
        {
            params: { ids: 't53' },
            data: {
                'mid': 't53',
                'date': {
                    'chunks': {}
                },
                'fid': '7',
                'count': 3,
                'tid': 't53',
                'lid': [],
                'new': 3
            }
        },
        {
            params: { ids: 't54' },
            data: {
                'mid': 't54',
                'date': {
                    'chunks': {}
                },
                'fid': '7',
                'count': 4,
                'tid': 't54',
                'lid': [],
                'new': 4
            }
        },
        {
            params: { ids: '55' },
            data: {
                'mid': '55',
                'date': {
                    'chunks': {}
                },
                'fid': '7',
                'count': 1,
                'tid': 't54',
                'lid': [],
                'new': 0
            }
        },
    ];

});
