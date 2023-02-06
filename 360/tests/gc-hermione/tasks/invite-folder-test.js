const { consts, taskRunner } = require('../runner');

taskRunner(
    'invite-folder -> ',
    [
        {
            testId: 'diskclient-615',
            users: ['yndx-ufo-test-382'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-1194',
            users: ['yndx-ufo-test-383'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-798',
            users: ['yndx-ufo-test-538'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
    ]
);
