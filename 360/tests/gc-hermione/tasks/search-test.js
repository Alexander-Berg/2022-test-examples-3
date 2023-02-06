const { consts, taskRunner } = require('../runner');

taskRunner(
    'Поиск ->',
    [
        {
            testId: 'diskclient-1993, 1974',
            users: ['yndx-ufo-test-406'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-1994, 1975',
            users: ['yndx-ufo-test-273'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-1995, 1976',
            users: ['yndx-ufo-test-244'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        }
    ]
);
