const { HoneypotsMode } = require('../../../../../src/types/honeypots-mode');
const { CheckEqualScreenshots } = require('../../../../../src/types/check-equal-screenshots');
const { EqualScreenshotsMetricThreshold } = require('../../../../../src/types/equal-screenshots-metric-threshold');

module.exports = {
    systemFeatures: require('./system-features.json'),
    input: {
        'title': 'kpi_ru_touch',
        'device': 'touch',
        'cross': '100',
        'tasksuiteComposition': {
            'mode': 'custom',
            'val': {
                'goodTasksCount': 2,
                'badTasksCount': 2,
                'assignmentsAcceptedCount': 1,
            },
        },
        'region': 'ru',
        'd-exps-flags': '&&sbs_plugin=base&sbs_plugin=body',
        'customCross': [
            {
                'left': 0,
                'right': 1,
                'count': '1',
            },
            {
                'left': 0,
                'right': 2,
                'count': '2',
            },
            {
                'left': 1,
                'right': 2,
                'count': '0',
            },
            {
                'left': 0,
                'right': 3,
                'count': '',
            },
        ],
        'resultsPerPage': { 'mode': 'edit', 'value': 20 },
        'filter': { 'source': 'id', 'val': 'test' },
        'checkEqualScreenshots': CheckEqualScreenshots.YES,
        'equalScreenshotsMetricThreshold': EqualScreenshotsMetricThreshold.MEDIUM,
        'poolTitle': 'touch_360_default',
        'options': [
            {
                'title': 'System0',
                'query': 'котики',
                'host': 'https://hamster.yandex.ru',
                'cgi': 'user-custom-cgi-param=1',
                'features': ['wait_all', 'test_mode_1'],
                'engine': 'yandex-web',
                'w-exps-flags': '',
                'uniqId': 'uniq235uniq236',
                'region': 'ru',
                'flags': '&test-id=1&waitall=da24562456\nafd\nafdgadfh',
            },
            {
                'title': 'System1',
                'query': 'котики',
                'cgi': 'sbs_plugin=google_ru_utf8&sbs_plugin=yandex_style',
                'features': ['hide_google_ads'],
                'engine': 'google-web',
                'w-exps-flags': '',
                'uniqId': 'uniq235uniq236',
                'pivot': true,
            },
            {
                'extSysId': '4',
                'title': 'External system',
                'oldSysTitle': 'Old title',
                'expTitle': 'Какой-то эксперимент',
                'ticket': 'SIDEBYSIDE-666',
                'engine': 'yandex-web',
                'workflow': '*************',
                'type': 'external',
            },
        ],
        'goldenset': [
            {
                'title': 'System2',
                'query': 'котики',
                'host': 'https://hamster.yandex.ru/',
                'cgi': '',
                'features': ['no_tests_da', 'remove_yandex_ads'],
                'engine': 'yandex-images',
                'w-exps-flags': '&snip=exps=snippets1874',
                'uniqId': 'uniq297uniq298',
                'device': 'desktop',
                'region': 'ru',
                'flags': '24562456\nafd\nafdgadfh',
            },
        ],
        'honeypotsMode': HoneypotsMode.CUSTOM,
        'overlap': {
            'main': 'edit',
            'value': '123',
        },
        'features': {
            'enabled_feature': 'on',
            'disabled_feature': '',
            'not_in_config': 'on',
        },
        filterReserveFactor: '50',
        targetings: {
            age: { min: 20, max: 40 },
            testRange: { min: 16, max: 35 },
            testMultiselect: ['RU','SV'],
            '1837': 1,
            'history_poll': {
                boolOperator: 'and',
                items: [
                    {
                        expId: 18312,
                        expTitle: 'Про мусор',
                        questionKey: 2,
                        question: 'Что из перечисленного Вы делали хотя бы один раз: ',
                        answer: 'Разделяете мусор в общественных местах',
                        workflowId: 'd86fe388-94e4-4b40-8f3f-491d219cd2cf',
                    },
                    {
                        expId: 18312,
                        expTitle: 'Про мусор',
                        questionKey: 2,
                        question: 'Что из перечисленного Вы делали хотя бы один раз: ',
                        answer: 'Сдаёте вещи на благотворительность',
                        workflowId: 'd86fe388-94e4-4b40-8f3f-491d219cd2cf',
                    },
                ],
            },
        },
    },

    output: {
        'main': {
            author: 'eroshinev',
            owners: [
                'eroshinev',
            ],
            'is-scheduled-sbs': true,
            'fast-design-beta': 'https://test.fdb.sbs.yandex-team.ru',
            'sbs-name': 'kpi_ru_touch',
            'st-ticket': 'SIDEBYSIDE-100500',
            'ui-host': 'sbs.yandex-team.ru',
            'yt-workspace': 'dev',
            'results-per-page': 20,
            'calc-workflow-id': '151d33d0-cb28-4583-b44d-fca178330855',
            'creation-type': 'ui',
            'ui-version': 1,
            'query-group-id': null,
        },
        'crosses': {
            'default-cross-value': 100,
            'custom-crosses': [
                {
                    'custom-cross-value': 1,
                    'sys-one': '0',
                    'sys-two': '1',
                },
                {
                    'custom-cross-value': 2,
                    'sys-one': '0',
                    'sys-two': '2',
                },
                {
                    'custom-cross-value': 0,
                    'sys-one': '1',
                    'sys-two': '2',
                },
                {
                    'custom-cross-value': null,
                    'sys-one': '0',
                    'sys-two': '3',
                },
            ],
        },
        'where': {
            'device-type': 'touch',
            'worker-device-type': 'touch',
            'domain': 'ru',
        },
        'exp': {
            'default-exp-flags': {
                'plugins': ['base', 'body'],
                'cgi-params': { 'enabled_feature': ['1'] },
            },
            'honeypot-tasks': 2,
            'normal-tasks': 2,
            'filter-reserve-factor': 1.5,
            filter: ['test'],
        },
        'systems': [
            {
                'beta': 'hamster.yandex',
                'exp-flags': {
                    'cgi-params': {
                        'user-custom-cgi-param': ['1'],
                        'test-mode': ['1'],
                        'waitall': ['da'],
                        'rearr': ['scheme_Local/Ugc/DryRun=1'],
                        'timeout': ['2000000'],
                    },
                },
                'bad-flags': null,
                'is-honeypot': false,
                'sys-id': '0',
                'sys-name': 'System0',
                'sys-type': 'yandex-web-touch',
                'is-pivot': false,
            },
            {
                'beta': 'google',
                'exp-flags': {
                    'plugins': [
                        'google_ru_utf8',
                        'yandex_style',
                        'hide_google_ads',
                    ],
                },
                'bad-flags': null,
                'is-honeypot': false,
                'sys-id': '1',
                'sys-name': 'System1',
                'sys-type': 'google-web-touch',
                'is-pivot': true,
            },
            {
                'sys-id': '2',
                'sys-name': 'External system',
                'is-external': true,
                'ext-sys': {
                    'st-ticket': 'SIDEBYSIDE-666',
                    'sys-id': '4',
                    'workflow-id': '*************',
                },
            },
        ],
        'honeypots': [
            {
                'bad-flags': {
                    'cgi-params': { 'snip': ['exps=snippets1874'] },
                },
                'beta': 'hamster.yandex',
                'exp-flags': {
                    'cgi-params': {
                        'exp_flags': ['direct_raw_parameters=aoff=1'],
                        'no-tests': ['da'],
                    },
                },
                'is-honeypot': true,
                'sys-id': '3',
                'sys-name': 'System2',
                'sys-type': 'yandex-images-touch',
            },
        ],
        'quality': {
            'check-equal-screenshots': true,
            'equal-screenshots-metric-threshold': 'medium',
        },
        'dual-honeypots': [],
        toloka: {
            'poll-targeting-config': {
                'bool-operator': 'and',
                'target-answers': [
                    {
                        answer: 'Разделяете мусор в общественных местах',
                        question: 'Что из перечисленного Вы делали хотя бы один раз: ',
                        'exp-id': {
                            'st-ticket': 'SIDEBYSIDE-18312',
                            'workflow-id': 'd86fe388-94e4-4b40-8f3f-491d219cd2cf',
                        },
                        'poll-element-key': 2,
                    },
                    {
                        answer: 'Сдаёте вещи на благотворительность',
                        question: 'Что из перечисленного Вы делали хотя бы один раз: ',
                        'exp-id': {
                            'st-ticket': 'SIDEBYSIDE-18312',
                            'workflow-id': 'd86fe388-94e4-4b40-8f3f-491d219cd2cf',
                        },
                        'poll-element-key': 2,
                    },
                ],
            },
            targetings: [
                {
                    'filter-key': '1837',
                    'filter-kind': 'skill',
                    constraint: {
                        'constraint-type': 'equal',
                        value: 1,
                    },
                },
                {
                    'filter-key': 'age',
                    'filter-kind': 'sbs',
                    constraint: {
                        'constraint-type': 'in-range',
                        'min-value': 20,
                        'max-value': 40,
                    },
                },
                {
                    'filter-key': 'testRange',
                    'filter-kind': 'sbs',
                    constraint: {
                        'constraint-type': 'in-range',
                        'min-value': 16,
                        'max-value': 35,
                    },
                },
                {
                    'filter-key': 'testMultiselect',
                    'filter-kind': 'toloka-filter-key',
                    constraint: {
                        'constraint-type': 'in-list',
                        values: ['RU', 'SV'],
                    },
                },
            ],
            overlap: 123,
        },
    },
    pluginsHost: 'https://test.fdb.sbs.yandex-team.ru',

    regular: true,
    ytWorkspace: 'dev',

    profilesFixtures: [
        {
            id: '1',
            isIphone: true,
            tolokaParams: { zoom: 0 },
        },
        {
            id: '2',
            name: 'android-medium',
            isIphone: false,
            tolokaParams: { zoom: 0 },
        },
        {
            id: '3',
            name: 'wide',
            tolokaParams: { zoom: 1 },
        },
    ],

    poolsListFixtures: [
        {
            poolId: 0,
            sandboxId: 0,
            title: 'touch_360_default',
            profile: '1',
            platform: 'touch',
        },
        {
            poolId: 1,
            sandboxId: 1,
            title: 'touch_360_comments',
            profile: '2',
            platform: 'touch',
        },
        {
            poolId: 2,
            sandboxId: 2,
            title: 'desktop_zoom_wide',
            profile: '3',
            platform: 'desktop',
        },
    ],

    feauturesFixtures: [
        {
            id: 'enabled_feature',
            getCGI: 'return val ? "&enabled_feature=1" : ""',
        },
        {
            id: 'disabled_feature',
            getCGI: 'return val ? "&disabled_feature=1" : ""',
        },
        {
            id: 'no_brand',
            getCGI: 'return val ? "&enabled_feature=1" : ""',
        },
    ],

    defaultCalcWorkflowId: '151d33d0-cb28-4583-b44d-fca178330855',
    defaultUiVersion: '1',

    currentUnixTimestampStub: 1543241468539,

    targetingConfig: {
        fields: [
            {
                type: 'range',
                key: 'age',
                category: 'sbs',
                title: 'TEST AGE RANGE',
                options: {
                    min: 0,
                    max: 100,
                },
                defaultValue: {
                    min: 0,
                    max: 100,
                },
            },
            {
                type: 'range',
                key: 'testRange',
                category: 'sbs',
                title: 'TEST RANGE',
                options: {
                    min: 0,
                    max: 100,
                },
                defaultValue: {
                    min: 0,
                    max: 100,
                },
            },
            {
                type: 'multiselect',
                key: 'testMultiselect',
                category: 'toloka-filter-key',
                title: 'TEST MULTISELECT',
                options: {
                    items: [
                        {
                            content: 'Россия',
                            value: 'RU',
                        },
                        {
                            content: 'Свящщенная',
                            value: 'SV',
                        },
                        {
                            content: 'Наша',
                            value: 'NA',
                        },
                    ],
                },
            },
            {
                type: 'select',
                key: '1837',
                category: 'skill',
                title: 'TEST SELECT',
                options: {
                    items: [
                        {
                            content: 'A',
                            value: 1,
                        },
                        {
                            content: 'B',
                            value: 2,
                        },
                    ],
                },
            },
        ],
    },
};
