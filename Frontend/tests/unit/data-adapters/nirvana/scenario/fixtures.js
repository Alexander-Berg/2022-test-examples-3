const config = require('config');

module.exports = {
    CONFIG_INPUT: {
        author: 'eroshinev',
        owners: ['eroshinev'],
        regular: false,
        ticket: 'SIDEBYSIDE-28719',
        host: 'sbs.yandex-team.ru',
        ytWorkspace: 'prod',
        creationType: 'ui',
        uiVersion: 2,
        notifications: [
            'experiment-prepared-email',
            'results-ready-email',
        ],
        videostreamTplS3Path: config.videostreamTplS3Path,
        config: {
            title: 'Аудио ответ про погоду',
            scenario: 'Вы спросили у голосового помощника "Какая сегодня погода в Москве". Прослушайте ответы голосового помощника.',
            question: 'Какой вариант вам нравится больше?',
            questionGroups: [
                {
                    key: 'question-group-1',
                    poll: [
                        {
                            type: 'question',
                            data: {
                                required: true,
                                question: 'q',
                            },
                            key: 'system-question-key-0',
                        },
                    ],
                },
            ],
            overlap: {
                mode: 'edit',
                value: '20',
            },
            showDuration: {
                mode: 'edit',
                value: '40',
            },
            viewMode: 'normal',
            notificationMode: {
                preset: 'workflowOnly',
                workflowNotificationChannels: ['email', 'yandex-chats'],
            },
            experimentMode: 'sbs',
            variants: {
                type: 'media',
                items: [
                    {
                        title: '+ 15 градусов',
                        file: 'https://samadhi-layouts.s3.yandex.net/sbs-OKWFZd/index.html',
                        systemId: 'sys-0',
                    },
                    {
                        title: 'неформальный',
                        file: 'https://samadhi-layouts.s3.yandex.net/sbs-ylzqHX/index.html',
                        systemId: 'sys-1',
                    },
                ],
            },
            taskComplexity: 'content-view',
            'systems': {
                'pages': {
                    'items': [
                        {
                            'id': '0',
                            'title': 'Вы спросили у голосового помощника "Какая сегодня погода в Москве". Прослушайте ответы голосового помощника.',
                            'prototypeIds': [
                                'cHJvdG90eXBlLW1jc3ViOHhidThw',
                                'cHJvdG90eXBlLW9uN2h3dWFneTk=',
                            ],
                            'questionGroupIds': [
                                'question-group-1',
                            ],
                            'instructionId': 'aW5zdHJ1Y3Rpb24tNngwYzZuN2ZlNWU=',
                        },
                    ],
                },
                'prototypes': {
                    'items': [
                        {
                            'title': '+ 15 градусов',
                            'file': 'https://samadhi-layouts.s3.yandex.net/sbs-OKWFZd/index.html',
                            'systemId': 'sys-0',
                            'id': 'cHJvdG90eXBlLW1jc3ViOHhidThw',
                        },
                        {
                            'title': 'неформальный',
                            'file': 'https://samadhi-layouts.s3.yandex.net/sbs-ylzqHX/index.html',
                            'systemId': 'sys-1',
                            'id': 'cHJvdG90eXBlLW9uN2h3dWFneTk=',
                        },
                    ],
                    'type': 'media',
                },
                'instructions': {
                    'items': [
                        {
                            'id': 'aW5zdHJ1Y3Rpb24tNngwYzZuN2ZlNWU=',
                            'text': 'Вы спросили у голосового помощника "Какая сегодня погода в Москве". Прослушайте ответы голосового помощника.',
                        },
                    ],
                },
                'questionGroups': {
                    'items': [
                        {
                            'key': 'question-group-1',
                            'poll': [
                                {
                                    'type': 'question',
                                    'data': {
                                        'required': true,
                                        'question': 'q',
                                    },
                                    'key': 'system-question-key-0',
                                },
                            ],
                        },
                    ],
                },
                'systems': {
                    'items': [
                        {
                            'id': 'sys-0',
                            'name': '+ 15 градусов',
                        },
                        {
                            'id': 'sys-1',
                            'name': 'неформальный',
                        },
                    ],
                },
            },
            assessmentGroup: 'tolokers',
            workflowType: 'stable',
            approveMode: 'auto',
        },
    },
    CONFIG_OUTPUT: {
        'main': {
            author: 'eroshinev',
            'owners': ['eroshinev'],
            'is-scheduled-sbs': false,
            'experiment-type': 'scenario',
            'sbs-name': 'Аудио ответ про погоду',
            'assessment-device-type': 'desktop',
            'creation-type': 'ui',
            'auto-open-merger-pool': true,
            'st-ticket': 'SIDEBYSIDE-28719',
            'ui-host': 'sbs.yandex-team.ru',
            'yt-workspace': 'prod',
            'ui-version': 2,
            'do-skip-assessment': false,
            'notifications': [
                'experiment-prepared-email',
                'results-ready-email',
            ],
            'abc-service': null,
            'approve-mode': 'auto',
        },
        'systems': [
            {
                'sys-id': 'sys-0',
                'sys-name': '+ 15 градусов',
                'is-external': true,
            },
            {
                'sys-id': 'sys-1',
                'sys-name': 'неформальный',
                'is-external': true,
            },
        ],
        'toloka': {
            'overlap': 20,
            'targetings': [],
            'assignments-accepted-count': 1,
            'reward-per-assignment': 0.01,
            'show-duration': 40,
        },
        'pool-clone-info': {
            'assessment-service': 'toloka',
            'prod-assessment-env-type': 'prod',
            'template': {
                'production': {
                    'pool-id': 3347713,
                },
                'sandbox': {
                    'pool-id': 100003,
                },
            },
        },
        'poll-params': {
            'scenario-src-type': 'media',
            'media-position-mode': 'from-start',
            'media-wrapper-url': config.videostreamTplS3Path,
            'view-mode': 'normal',
            'task-kind': 'content-view',
        },
        'exp': {
            'queries-num': 1,
        },
    },
    SCENARIO_DATA_OUTPUT: {
        'scenario-list': [
            {
                'pages': [
                    {
                        'sys-id': 'sys-0',
                        'src-url': 'https://samadhi-layouts.s3.yandex.net/sbs-OKWFZd/index.html',
                        'src-type': 'native',
                        'show-duration': 40,
                    },
                    {
                        'sys-id': 'sys-1',
                        'src-url': 'https://samadhi-layouts.s3.yandex.net/sbs-ylzqHX/index.html',
                        'src-type': 'native',
                        'show-duration': 40,
                    },
                ],
                'question-groups': [
                    {
                        key: 'question-group-1',
                        'poll-elements': [
                            {
                                'data': {
                                    'options': [
                                        {
                                            'text': 'Вариант 1',
                                            'value': 'left',
                                        },
                                        {
                                            'text': 'Вариант 2',
                                            'value': 'right',
                                        },
                                    ],
                                    'required': true,
                                    'question': 'q',
                                },
                                'role': 'system',
                                'type': 'binary',
                                'key': 'system-question-key-0',
                            },
                            {
                                'data': {
                                    'required': true,
                                    'question': 'Оставьте свой комментарий',
                                },
                                'key': 'free-question-key',
                                'type': 'question',
                            },
                        ],
                    },
                ],
                'scenario-id': '0',
                'scenario-text': 'Вы спросили у голосового помощника "Какая сегодня погода в Москве". Прослушайте ответы голосового помощника.',
            },
        ],
        'scenario-mode': 'sbs',
    },
    SCENARIO_BATCH_CONFIG: {
        'title': 'Batch new format',
        'question': 'Какой вариант вам больше нравится?',
        'batchMode': true,
        'variants': {
            'type': 'link',
            'systems': [
                { 'name': 'name-0' },
                { 'name': 'name-1' },
                { 'name': 'name-2' },
            ],
            'pages': [
                {
                    'scenario': 'Какое видео более качественное?',
                    'name': 'Screen 0',
                },
                {
                    'scenario': 'Какое видео более качественное?',
                    'name': 'Screen 1',
                },
            ],
            'items': [
                {
                    'urls': [
                        { 'url': 'https://samadhi-layouts.s3.yandex.net/sbs-vkciJh/banner-url-1.html' },
                        { 'url': 'https://samadhi-layouts.s3.yandex.net/sbs-YL10Ae/bannerstorage1.html' },
                        { 'url': 'https://samadhi-layouts.s3.yandex.net/sbs-iCKQly/bannerstorage-2.html' },
                    ],
                },
                {
                    'urls': [
                        { 'url': 'https://samadhi-layouts.s3.yandex.net/sbs-vkciJh/banner-url-1.html' },
                        { 'url': 'https://samadhi-layouts.s3.yandex.net/sbs-YL10Ae/bannerstorage1.html' },
                        { 'url': 'https://samadhi-layouts.s3.yandex.net/sbs-iCKQly/bannerstorage-2.html' },
                    ],
                },
            ],
        },
        'systems': {
            'pages': {
                'items': [
                    {
                        'id': 'c2NlbmFyaW8tOTlnNTZvdHN6NDg=',
                        'prototypesType': 'link',
                        'prototypeIds': [
                            'cHJvdG90eXBlLXllcmF3b25kOWhw',
                            'cHJvdG90eXBlLTdzMW8yZWw1dDRx',
                            'cHJvdG90eXBlLXdkM3ppNmVwZXlk',
                        ],
                        'questionGroupIds': [
                            'question-group-1',
                        ],
                        'title': 'Screen 0',
                        'instructionId': 'aW5zdHJ1Y3Rpb24tYmNuOHI1ZWZ2ag==',
                    },
                    {
                        'id': 'c2NlbmFyaW8tYnAxZHF1YmNub2s=',
                        'prototypesType': 'link',
                        'prototypeIds': [
                            'cHJvdG90eXBlLWl4bzA1c3RmNXBn',
                            'cHJvdG90eXBlLXNqYzd4NjFvOGQ3',
                            'cHJvdG90eXBlLXJydDQ1bTh1aTY=',
                        ],
                        'questionGroupIds': [
                            'question-group-1',
                        ],
                        'title': 'Screen 1',
                        'instructionId': 'aW5zdHJ1Y3Rpb24tZjBvNW90MDl5NjQ=',
                    },
                ],
            },
            'prototypes': {
                'items': [
                    {
                        'url': 'https://samadhi-layouts.s3.yandex.net/sbs-vkciJh/banner-url-1.html',
                        'systemId': 'sys-0',
                        'id': 'cHJvdG90eXBlLXllcmF3b25kOWhw',
                    },
                    {
                        'url': 'https://samadhi-layouts.s3.yandex.net/sbs-YL10Ae/bannerstorage1.html',
                        'systemId': 'sys-1',
                        'id': 'cHJvdG90eXBlLTdzMW8yZWw1dDRx',
                    },
                    {
                        'url': 'https://samadhi-layouts.s3.yandex.net/sbs-iCKQly/bannerstorage-2.html',
                        'systemId': 'sys-2',
                        'id': 'cHJvdG90eXBlLXdkM3ppNmVwZXlk',
                    },
                    {
                        'url': 'https://samadhi-layouts.s3.yandex.net/sbs-vkciJh/banner-url-1.html',
                        'systemId': 'sys-0',
                        'id': 'cHJvdG90eXBlLWl4bzA1c3RmNXBn',
                    },
                    {
                        'url': 'https://samadhi-layouts.s3.yandex.net/sbs-YL10Ae/bannerstorage1.html',
                        'systemId': 'sys-1',
                        'id': 'cHJvdG90eXBlLXNqYzd4NjFvOGQ3',
                    },
                    {
                        'url': 'https://samadhi-layouts.s3.yandex.net/sbs-iCKQly/bannerstorage-2.html',
                        'systemId': 'sys-2',
                        'id': 'cHJvdG90eXBlLXJydDQ1bTh1aTY=',
                    },
                ],
                'type': 'link',
            },
            'instructions': {
                'items': [
                    {
                        'id': 'aW5zdHJ1Y3Rpb24tYmNuOHI1ZWZ2ag==',
                        'text': 'Какое видео более качественное?',
                    },
                    {
                        'id': 'aW5zdHJ1Y3Rpb24tZjBvNW90MDl5NjQ=',
                        'text': 'Какое видео более качественное?',
                    },
                ],
            },
            'questionGroups': {
                'items': [
                    {
                        'key': 'question-group-1',
                        'poll': [
                            {
                                'type': 'question',
                                'data': {
                                    'required': true,
                                    'question': "This is Hermione's question",
                                },
                                'key': 'cXVlc3Rpb24tYmJoYjY3cXJuM3U=',
                                'error': null,
                            },
                        ],
                    },
                ],
            },
            'systems': {
                'items': [
                    {
                        'name': 'name-0',
                        'systemId': 'sys-0',
                    },
                    {
                        'name': 'name-1',
                        'systemId': 'sys-1',
                    },
                    {
                        'name': 'name-2',
                        'systemId': 'sys-2',
                    },
                ],
            },
        },
        questionGroups: [
            {
                key: 'question-group-1',
                poll: [],
            },
        ],
    },
    MULTIPART_POLL_INPUT_SBS_MODE: {
        'questionGroups': [
            {
                'key': 'question-group-0',
                'poll': [
                    {
                        'type': 'question',
                        'data': {
                            'required': true,
                            'question': 'Pp?',
                        },
                        'key': 'cXVlc3Rpb24tMWEwZzlxdG05Njg=',
                        'error': null,
                    },
                    {
                        'type': 'question',
                        'data': {
                            'required': true,
                            'question': '3434',
                        },
                        'key': 'cXVlc3Rpb24tZzU2bWUwdml1NGs=',
                        'error': null,
                    },
                ],
            },
        ],
    },
    MULTIPART_POLL_OUTPUT_SBS_MODE: {
        'question-groups': [
            {
                'key': 'question-group-0',
                'poll-elements': [
                    {
                        'data': {
                            'options': [
                                {
                                    'text': 'Вариант 1',
                                    'value': 'left',
                                },
                                {
                                    'text': 'Вариант 2',
                                    'value': 'right',
                                },
                            ],
                            'required': true,
                            'question': 'Pp?',
                        },
                        'role': 'system',
                        'type': 'binary',
                        'key': 'cXVlc3Rpb24tMWEwZzlxdG05Njg=',
                    },
                    {
                        'data': {
                            'options': [
                                {
                                    'text': 'Вариант 1',
                                    'value': 'left',
                                },
                                {
                                    'text': 'Вариант 2',
                                    'value': 'right',
                                },
                            ],
                            'required': true,
                            'question': '3434',
                        },
                        'role': 'system',
                        'type': 'binary',
                        'key': 'cXVlc3Rpb24tZzU2bWUwdml1NGs=',
                    },
                    {
                        'data': {
                            'required': true,
                            'question': 'Оставьте свой комментарий',
                        },
                        'key': 'free-question-key',
                        'type': 'question',
                    },
                ],
            },
        ],
    },
    MULTIPART_POLL_INPUT_A_MODE: {
        'questionGroups': [
            {
                'key': 'question-group-0',
                'poll': [
                    {
                        'type': 'radio',
                        'data': {
                            'required': true,
                            'shuffle': true,
                            'question': '2',
                            'options': [
                                {
                                    'text': '2',
                                },
                                {
                                    'text': '3',
                                },
                            ],
                        },
                        'key': 'cmFkaW8tOWt0ZzF2dXFtaG8=',
                        'error': null,
                    },
                    {
                        'type': 'question',
                        'data': {
                            'required': true,
                            'question': 'asdf',
                        },
                        'key': 'cXVlc3Rpb24tMm90ZXNlYjN1b2g=',
                        'error': null,
                    },
                ],
                'constraints': {
                    'bool-operator': 'and',
                    'conditions': [],
                },
            },
            {
                'key': 'cGFnZS14Y2wyeGc4Yjg2aw==',
                'poll': [
                    {
                        'type': 'image',
                        'data': {
                            'url': 'https://samadhi-layouts.s3.mdst.yandex.net/sbs-dqLPSt/upload_1b2b1437b34cfd8c8bf03c5edcb67d89.jpg',
                        },
                        'key': 'aW1hZ2UtMDRzM3g1OHVzNHZm',
                        'error': null,
                    },
                ],
                'constraints': {
                    'bool-operator': 'and',
                    'conditions': [],
                },
            },
            {
                'key': 'cGFnZS1sa3MyMDI2Z2pr',
                'poll': [
                    {
                        'type': 'scale',
                        'data': {
                            'required': true,
                            'question': 'scale q',
                            'startsWith': 0,
                            'endsWith': 2,
                            'leftText': 'min',
                            'rightText': 'max',
                        },
                        'key': 'c2NhbGUtcnp6cWd6b3hhaA==',
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
    MULTIPART_POLL_OUTPUT_A_MODE: {
        'question-groups': [
            {
                'key': 'question-group-0',
                'poll-elements': [
                    {
                        'type': 'radio',
                        'data': {
                            'required': true,
                            'shuffle': true,
                            'question': '2',
                            'options': [
                                {
                                    'text': '2',
                                },
                                {
                                    'text': '3',
                                },
                            ],
                        },
                        'key': 'cmFkaW8tOWt0ZzF2dXFtaG8=',
                        'error': null,
                    },
                    {
                        'type': 'question',
                        'data': {
                            'required': true,
                            'question': 'asdf',
                        },
                        'key': 'cXVlc3Rpb24tMm90ZXNlYjN1b2g=',
                        'error': null,
                    },
                ],
                'constraints': {
                    'bool-operator': 'and',
                    'conditions': [],
                },
            },
            {
                'key': 'cGFnZS14Y2wyeGc4Yjg2aw==',
                'poll-elements': [
                    {
                        'type': 'image',
                        'data': {
                            'url': 'https://samadhi-layouts.s3.mdst.yandex.net/sbs-dqLPSt/upload_1b2b1437b34cfd8c8bf03c5edcb67d89.jpg',
                        },
                        'key': 'aW1hZ2UtMDRzM3g1OHVzNHZm',
                        'error': null,
                    },
                ],
                'constraints': {
                    'bool-operator': 'and',
                    'conditions': [],
                },
            },
            {
                'key': 'cGFnZS1sa3MyMDI2Z2pr',
                'poll-elements': [
                    {
                        'type': 'scale',
                        'data': {
                            'required': true,
                            'question': 'scale q',
                            'leftText': 'min',
                            'rightText': 'max',
                            'scaleStart': 0,
                            'scaleEnd': 2,
                        },
                        'key': 'c2NhbGUtcnp6cWd6b3hhaA==',
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
