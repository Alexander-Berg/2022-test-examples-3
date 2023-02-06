module.exports = {
    CONFIG_INPUT: {
        host: 'localhost',
        ticket: 'SIDEBYSIDE-38696',
        ytWorkspace: 'dev',
        regular: false,
        author: 'eroshinev',
        owners: ['eroshinev'],
        notifications: [
            'experiment-prepared-email',
            'results-ready-email',
        ],
        config: {
            title: 'Навигация в турбо',
            assessmentGroup: 'tolokers',
            overlap: 400,
            poll: [
                {
                    type: 'image',
                    data: {
                        url: 'https://samadhi-layouts.s3.yandex.net/sbs-7RRSsD/1559247608369.png',
                    },
                },
                {
                    type: 'checkbox',
                    data: {
                        required: true,
                        shuffle: true,
                        question: 'Перед вами один из сайтов с результатов поиска по запросу \'Как лечить ОРВИ\'. Как вы думаете, что за блок, на который наведена стрелка?',
                        options: [
                            { text: 'Сайты на эту же тему' },
                            { text: 'Рекламные результаты' },
                            { text: 'Затрудняюсь ответить / другое' },
                        ],
                    },
                },
                {
                    type: 'scale',
                    data: {
                        leftText: 'Бесполезна',
                        question: 'Оцените по 10-бальной шкале насколько полезна статья',
                        required: true,
                        rightText: 'Полезна',
                        scale: 10,
                    },
                },
            ],
            workflowType: 'stable',
            approveMode: 'manual',
        },
    },
    CONFIG_OUTPUT: {
        main: {
            author: 'eroshinev',
            owners: ['eroshinev'],
            'is-scheduled-sbs': false,
            'sbs-name': 'Навигация в турбо',
            'st-ticket': 'SIDEBYSIDE-38696',
            'ui-host': 'localhost',
            'ui-version': undefined,
            'yt-workspace': 'dev',
            'creation-type': 'api',
            'auto-open-merger-pool': true,
            'do-skip-assessment': false,
            notifications: [
                'experiment-prepared-email',
                'results-ready-email',
            ],
            'experiment-type': 'poll',
            'abc-service': null,
            'approve-mode': 'manual',
        },
        toloka: {
            targetings: [],
            overlap: 400,
        },
        'pool-clone-info': {
            'assessment-service': 'toloka',
            'prod-assessment-env-type': 'prod',
            template: {
                production: {
                    'pool-id': 2108698,
                },
                'sandbox': {
                    'pool-id': 72494,
                },
            },
        },
    },
    MULTIPART_POLL_DATA_INPUT: {
        'questionGroups': [
            {
                'key': 'cGFnZS0zcXd6YTF1aWNmZg==',
                'poll': [
                    {
                        'type': 'radio',
                        'data': {
                            'required': true,
                            'shuffle': true,
                            'question': 'Test q',
                            'options': [
                                {
                                    'text': '1',
                                },
                                {
                                    'text': '2',
                                },
                            ],
                            'hasOther': true,
                        },
                        'key': 'cmFkaW8tNTNkemdwbmw5NTU=',
                        'error': null,
                    },
                    {
                        'type': 'scale',
                        'data': {
                            'required': true,
                            'question': 'Test scale',
                            'startsWith': -2,
                            'endsWith': 5,
                            'leftText': 'Horrible',
                            'rightText': 'Amazing',
                        },
                        'key': 'c2NhbGUtaXhzbDA4czAwdA==',
                        'error': null,
                    },
                ],
                'constraints': {
                    'bool-operator': 'and',
                    'conditions': [],
                },
            },
            {
                'key': 'cGFnZS1qbDRpcWk5MXdvbg==',
                'poll': [
                    {
                        'type': 'text',
                        'data': {
                            'text': 'Test text',
                        },
                        'key': 'dGV4dC1uY24zMTJoc3k2aw==',
                        'error': null,
                    },
                ],
                'constraints': {
                    'bool-operator': 'and',
                    'conditions': [
                        {
                            'key': 'c2NhbGUtaXhzbDA4czAwdA',
                            'compare-operator': '>',
                            'values': 0,
                        },
                    ],
                },
            },
            {
                'key': 'cGFnZS0wc2RpNHp2bGYzbw==',
                'poll': [
                    {
                        'type': 'checkbox',
                        'data': {
                            'required': true,
                            'shuffle': true,
                            'question': 'Test multiple answers',
                            'options': [
                                {
                                    'text': '1',
                                },
                                {
                                    'text': '2',
                                },
                                {
                                    'text': '3',
                                },
                            ],
                        },
                        'key': 'Y2hlY2tib3gtdmNmYWlsM2o3Yw==',
                        'error': null,
                    },
                ],
                'constraints': {
                    'bool-operator': 'and',
                    'conditions': [],
                },
            },
        ],
    },
    MULTIPART_POLL_DATA_OUTPUT: {
        'question-groups': [
            {
                'key': 'cGFnZS0zcXd6YTF1aWNmZg==',
                'poll-elements': [
                    {
                        'type': 'radio',
                        'data': {
                            'required': true,
                            'shuffle': true,
                            'question': 'Test q',
                            'options': [
                                {
                                    'text': '1',
                                },
                                {
                                    'text': '2',
                                },
                            ],
                            'hasOther': true,
                        },
                        'key': 'cmFkaW8tNTNkemdwbmw5NTU=',
                        'error': null,
                    },
                    {
                        'type': 'scale',
                        'data': {
                            'required': true,
                            'question': 'Test scale',
                            'leftText': 'Horrible',
                            'rightText': 'Amazing',
                            'scaleStart': -2,
                            'scaleEnd': 5,
                        },
                        'key': 'c2NhbGUtaXhzbDA4czAwdA==',
                        'error': null,
                    },
                ],
                'constraints': {
                    'bool-operator': 'and',
                    'conditions': [],
                },
            },
            {
                'key': 'cGFnZS1qbDRpcWk5MXdvbg==',
                'poll-elements': [
                    {
                        'type': 'text',
                        'data': {
                            'text': 'Test text',
                        },
                        'key': 'dGV4dC1uY24zMTJoc3k2aw==',
                        'error': null,
                    },
                ],
                'constraints': {
                    'bool-operator': 'and',
                    'conditions': [
                        {
                            'key': 'c2NhbGUtaXhzbDA4czAwdA',
                            'compare-operator': '>',
                            'values': 0,
                        },
                    ],
                },
            },
            {
                'key': 'cGFnZS0wc2RpNHp2bGYzbw==',
                'poll-elements': [
                    {
                        'type': 'checkbox',
                        'data': {
                            'required': true,
                            'shuffle': true,
                            'question': 'Test multiple answers',
                            'options': [
                                {
                                    'text': '1',
                                },
                                {
                                    'text': '2',
                                },
                                {
                                    'text': '3',
                                },
                            ],
                        },
                        'key': 'Y2hlY2tib3gtdmNmYWlsM2o3Yw==',
                        'error': null,
                    },
                ],
                'constraints': {
                    'bool-operator': 'and',
                    'conditions': [],
                },
            },
        ],
    },
};
