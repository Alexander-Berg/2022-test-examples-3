const { consts, taskRunner } = require('../runner');

taskRunner(
    'Просмотр документов в Доксах -> ',
    [
        {
            testId: 'diskclient-6797',
            users: ['yndx-ufo-test-627'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: (name) => name === 'Загрузки'
                    }
                },
                consts.FAST_TRASH
            ]
        },
        {
            testId: 'diskclient-6785, 6786, 6788',
            users: ['yndx-ufo-test-630'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-6779, 6780, 6781',
            users: ['yndx-ufo-test-631'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-6776, 6777',
            users: ['yndx-ufo-test-632', 'yndx-ufo-test-633'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: (name) => name === 'Загрузки'
                    }
                },
                consts.FAST_TRASH
            ]
        },
    ]
);
