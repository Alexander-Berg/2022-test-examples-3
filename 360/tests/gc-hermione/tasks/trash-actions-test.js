const { consts, taskRunner } = require('../runner');

taskRunner(
    'Очистка корзины',
    [
        {
            testId: 'diskclient-3784, 1509',
            users: ['yndx-ufo-test-78', 'yndx-ufo-test-31'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-1521, 642',
            users: ['yndx-ufo-test-71', 'yndx-ufo-test-21'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-3783, 3782',
            users: ['yndx-ufo-test-79', 'yndx-ufo-test-32'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
    ]
);
