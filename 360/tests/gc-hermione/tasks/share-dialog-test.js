const { consts, taskRunner } = require('../runner');

taskRunner(
    'Общий диалог поделения',
    [
        {
            testId: 'diskclient-6346, 6409, 6347, 6410, 6349, 6412, 6352, 6399, 6351, 6406, 6386, 6402, 6388',
            users: ['yndx-ufo-test-557'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-6446, 6435',
            users: ['yndx-ufo-test-607'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-6432, 6443',
            users: ['yndx-ufo-test-595', 'yndx-ufo-test-596', 'yndx-ufo-test-597', 'yndx-ufo-test-598'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-6444, 6433',
            users: ['yndx-ufo-test-603', 'yndx-ufo-test-604', 'yndx-ufo-test-605', 'yndx-ufo-test-606'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        }
    ]
);
