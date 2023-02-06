const LAYOUTS_INPUT = {
    systems: [
        { name: 'Синяя тема' },
        { name: 'Зеленая тема' },
        { name: 'Черная тема' },
    ],
    screens: [
        {
            name: 'Страница авторизации',
            question: 'Как вам страница авторизации?',
        },
        {
            name: 'Страница треда',
            question: 'Как вам страница треда?',
        },
    ],
    layouts: [
        {
            screens: [
                { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb1.png' },
                { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb2.png' },
                { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb3.png' },
            ],
            honeypots: [
                { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb4.png' },
                { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb5.png' },
            ],
        },
        {
            screens: [
                { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb6.png' },
                { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb7.png' },
                { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb8.png' },
            ],
            honeypots: [
                { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb9.png' },
                { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb10.png' },
            ],
        },
    ],
};

module.exports = {
    LAYOUTS_INPUT,
    LAYOUTS_OUTPUT: {
        'screen-files': [
            {
                url: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb1.png',
                'screen-id': '0',
                'sys-id': '0',
            },
            {
                url: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb2.png',
                'screen-id': '0',
                'sys-id': '1',
            },
            {
                url: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb3.png',
                'screen-id': '0',
                'sys-id': '2',
            },
            {
                url: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb4.png',
                'screen-id': '0',
                'sys-id': '3',
            },
            {
                url: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb5.png',
                'screen-id': '0',
                'sys-id': '4',
            },

            {
                url: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb6.png',
                'screen-id': '1',
                'sys-id': '0',
            },
            {
                url: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb7.png',
                'screen-id': '1',
                'sys-id': '1',
            },
            {
                url: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb8.png',
                'screen-id': '1',
                'sys-id': '2',
            },
            {
                url: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb9.png',
                'screen-id': '1',
                'sys-id': '3',
            },
            {
                url: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb10.png',
                'screen-id': '1',
                'sys-id': '4',
            },
        ],
        'screen-descriptions': [
            {
                'screen-id': '0',
                'screen-name': 'Страница авторизации',
                question: 'Как вам страница авторизации?',
            },
            {
                'screen-id': '1',
                'screen-name': 'Страница треда',
                question: 'Как вам страница треда?',
            },
        ],
    },
    CONFIG_INPUT: {
        config: {
            title: 'Мобильная почта',
            description: 'Сравнение цветовых схем для страниц мобильной почты',
            goodTasks: 5,
            badTasks: 1,
            poolTitle: 'desktop_zoom_wide',
            layouts: LAYOUTS_INPUT,
            approveMode: 'auto',
            assessmentDeviceType: 'desktop',
            useAutoHoneypots: 'yes',
        },
        ytWorkspace: 'dev',
        host: 'sbs.yandex-team.ru',
        ticket: 'SIDEBYSIDE-470',
        regular: true,
        profiles: [
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
        poolsList: [
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
            {
                poolId: 23651786,
                sandboxId: 23651786,
                title: 'desktop_colleagues_or_internal-assessors',
                profile: '3',
                platform: 'desktop',
            },
        ],
        author: 'eroshinev',
        owners: ['eroshinev'],
        calcWorkflowId: '151d33d0-cb28-4583-b44d-fca178330855',
        creationType: 'ui',
        uiVersion: 1,
        notifications: [],
    },
    CONFIG_OUTPUT: {
        exp: {
            'honeypot-tasks': 1,
            'normal-tasks': 5,
        },
        honeypots: [
            {
                'is-honeypot': true,
                'is-bad-side': true,
                'is-design-honeypot': true,
                'sys-id': '3',
                'sys-name': 'honeypot_0',
                'sys-type': 'design',
            },
            {
                'is-honeypot': true,
                'is-bad-side': true,
                'is-design-honeypot': true,
                'sys-id': '4',
                'sys-name': 'honeypot_1',
                'sys-type': 'design',
            },
        ],
        main: {
            author: 'eroshinev',
            owners: [
                'eroshinev',
            ],
            'use-auto-honeypots': true,
            'sbs-name': 'Мобильная почта',
            'merge-with-others': true,
            'ui-host': 'sbs.yandex-team.ru',
            'is-design-sbs': true,
            'is-scheduled-sbs': true,
            'use-screenshots': true,
            'st-ticket': 'SIDEBYSIDE-470',
            'yt-workspace': 'dev',
            'calc-workflow-id': '151d33d0-cb28-4583-b44d-fca178330855',
            'creation-type': 'ui',
            'ui-version': 1,
            'do-skip-assessment': false,
            notifications: [],
            'abc-service': null,
            'approve-mode': 'auto',
        },
        where: {
            'custom-toloka-view-params': {
                'templateName': 'desktop_zoom_wide',
                'zoom': 1,
                'platform': 'desktop',
            },
            'device-type': 'design',
            'worker-device-type': 'desktop',
            'screen-profile-name': 'wide',
        },
        systems: [
            {
                'sys-id': '0',
                'sys-name': 'Синяя тема',
                'sys-type': 'design',
                'is-honeypot': false,
                'is-bad-side': false,
                'is-design-honeypot': false,
            },
            {
                'sys-id': '1',
                'sys-name': 'Зеленая тема',
                'sys-type': 'design',
                'is-honeypot': false,
                'is-bad-side': false,
                'is-design-honeypot': false,
            },
            {
                'sys-id': '2',
                'sys-name': 'Черная тема',
                'sys-type': 'design',
                'is-honeypot': false,
                'is-bad-side': false,
                'is-design-honeypot': false,
            },
        ],
        'pool-clone-info': {
            'assessment-service': 'toloka',
            'prod-assessment-env-type': 'prod',
            template: {
                production: {
                    'pool-id': 2,
                },
                sandbox: {
                    'pool-id': 2,
                },
            },
        },
        toloka: {
            targetings: [],
        },
    },
};
