const { consts, taskRunner } = require('../runner');

taskRunner(
    'Документы -> ',
    [
        {
            testId: 'diskclient-6538, 6638, 6539, 6639',
            users: ['yndx-ufo-test-567'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-6540, 6640',
            users: ['yndx-ufo-test-568'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-6561',
            users: ['yndx-ufo-test-575'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-6454, 6578, 6455, 6579, 6456, 6580, 6457, 6458, 6459, 6466, 6587',
            users: ['yndx-ufo-test-576', 'yndx-ufo-test-577', 'yndx-ufo-test-578'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-6478, 6595',
            users: ['yndx-ufo-test-579'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-6502, 6516, 6519, 6620, 6521',
            users: ['yndx-ufo-test-580', 'yndx-ufo-test-582', 'yndx-ufo-test-583', 'yndx-ufo-test-584'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        }
    ]
);
