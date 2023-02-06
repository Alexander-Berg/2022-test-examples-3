module.exports = [
    {
        description: 'KPI без частного количества сравнений',
        fixture: {
            'crosses': {
                'default-cross-value': 100,
                'custom-crosses': [],
            },
            'exp': {
                'honeypot-tasks': 1,
                'normal-tasks': 8,
                'round-robin': false,
            },
            'systems': [
                {
                    'sys-name': 'System0',
                    'sys-type': 'yandex-touch',
                },
                {
                    'sys-name': 'System1',
                    'sys-type': 'google-touch',
                },
                {
                    'sys-name': 'System2',
                    'sys-type': 'google-touch',
                },
            ],
            'honeypots': [
                {
                    'sys-name': 'System2',
                    'sys-type': 'yandex-images-touch',
                },
            ],
        },
        queriesCount: 1000,
        comparisions: 300,
        tasks: 750,
        price: 7.5,
    },
    {
        description: 'KPI с частным количеством сравнений',
        fixture: {
            'crosses': {
                'default-cross-value': 100,
                'custom-crosses': [
                    {
                        'custom-cross-value': 200,
                        'sys-one': '0',
                        'sys-two': '1',
                    },
                ],
            },
            'exp': {
                'honeypot-tasks': 1,
                'normal-tasks': 8,
                'round-robin': false,
            },
            'systems': [
                {
                    'sys-name': 'System0',
                    'sys-type': 'yandex-touch',
                },
                {
                    'sys-name': 'System1',
                    'sys-type': 'google-touch',
                },
                {
                    'sys-name': 'System2',
                    'sys-type': 'google-touch',
                },
            ],
            'honeypots': [
                {
                    'sys-name': 'System2',
                    'sys-type': 'yandex-images-touch',
                },
            ],
        },
        queriesCount: 1000,
        comparisions: 400,
        tasks: 1000,
        price: 10,
    },
    {
        description: 'round-robin без частного количества сравнений',
        fixture: {
            'crosses': {
                'default-cross-value': null,
                'custom-crosses': [],
            },
            'exp': {
                'honeypot-tasks': 1,
                'normal-tasks': 8,
                'round-robin': true,
            },
            'systems': [
                {
                    'sys-name': 'System0',
                    'sys-type': 'yandex-touch',
                },
                {
                    'sys-name': 'System1',
                    'sys-type': 'google-touch',
                },
                {
                    'sys-name': 'System2',
                    'sys-type': 'google-touch',
                },
            ],
            'honeypots': [
                {
                    'sys-name': 'System2',
                    'sys-type': 'yandex-images-touch',
                },
            ],
        },
        queriesCount: 1000,
        comparisions: 3000,
        tasks: 7500,
        price: 75,
    },
    {
        description: 'round-robin с частным количеством сравнений',
        fixture: {
            'crosses': {
                'default-cross-value': null,
                'custom-crosses': [
                    {
                        'custom-cross-value': 0,
                        'sys-one': '0',
                        'sys-two': '1',
                    },
                ],
            },
            'exp': {
                'honeypot-tasks': 1,
                'normal-tasks': 8,
                'round-robin': true,
            },
            'systems': [
                {
                    'sys-name': 'System0',
                    'sys-type': 'yandex-touch',
                },
                {
                    'sys-name': 'System1',
                    'sys-type': 'google-touch',
                },
                {
                    'sys-name': 'System2',
                    'sys-type': 'google-touch',
                },
            ],
            'honeypots': [
                {
                    'sys-name': 'System2',
                    'sys-type': 'yandex-images-touch',
                },
            ],
        },
        queriesCount: 1000,
        comparisions: 2000,
        tasks: 5000,
        price: 50,
    },
    {
        description: 'round-robin с частным количеством сравнений и нулевых дефолтным',
        fixture: {
            'crosses': {
                'default-cross-value': 0,
                'custom-crosses': [
                    {
                        'custom-cross-value': null,
                        'sys-one': '0',
                        'sys-two': '1',
                    },
                    {
                        'custom-cross-value': null,
                        'sys-one': '0',
                        'sys-two': '2',
                    },
                ],
            },
            'exp': {
                'honeypot-tasks': 1,
                'normal-tasks': 8,
                'round-robin': true,
            },
            'systems': [
                {
                    'sys-name': 'System0',
                    'sys-type': 'yandex-touch',
                },
                {
                    'sys-name': 'System1',
                    'sys-type': 'google-touch',
                },
                {
                    'sys-name': 'System2',
                    'sys-type': 'google-touch',
                },
            ],
            'honeypots': [
                {
                    'sys-name': 'System2',
                    'sys-type': 'yandex-images-touch',
                },
            ],
        },
        queriesCount: 1000,
        comparisions: 2000,
        tasks: 5000,
        price: 50,
    },
];
