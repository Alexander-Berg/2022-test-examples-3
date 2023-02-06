const { consts, taskRunner } = require('../runner');

taskRunner(
    'Публикация папок',
    [
        {
            testId: 'diskclient-5061, 5039',
            users: ['yndx-ufo-test-191', 'yndx-ufo-test-190'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-5036, 896',
            users: ['yndx-ufo-test-179', 'yndx-ufo-test-178'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-736',
            users: ['yndx-ufo-test-222'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        }
    ]
);
