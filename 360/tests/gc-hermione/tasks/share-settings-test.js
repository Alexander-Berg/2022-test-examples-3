const { consts, taskRunner } = require('../runner');

taskRunner(
    'Ограниченный шаринг',
    [
        {
            testId: 'diskclient-7265, diskclient-7270',
            users: ['yndx-ufo-test-781'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-7266, diskclient-7267, diskclient-7268, diskclient-7269',
            users: ['yndx-ufo-test-782'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-7271, diskclient-7272',
            users: ['yndx-ufo-test-783'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-7287',
            users: ['yndx-ufo-test-784'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-7292',
            users: ['yndx-ufo-test-785'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-7293',
            users: ['yndx-ufo-test-786'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
    ]
);
