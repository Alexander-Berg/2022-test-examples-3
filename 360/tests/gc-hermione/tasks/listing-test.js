const { consts, taskRunner } = require('../runner');

taskRunner(
    'Создание документов через контекстное меню',
    [
        {
            testId: 'diskclient-854',
            users: ['yndx-ufo-test-478'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-853',
            users: ['yndx-ufo-test-479'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-852',
            users: ['yndx-ufo-test-480'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-583',
            users: ['yndx-ufo-test-481'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-707, 1180',
            users: ['yndx-ufo-test-245'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-992',
            users: ['yndx-ufo-test-264'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        }
    ]
);
