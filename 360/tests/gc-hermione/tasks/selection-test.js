const { consts, taskRunner } = require('../runner');

taskRunner(
    'Выделение -> ',
    [
        {
            testId: 'diskclient-912',
            users: ['yndx-ufo-test-253'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-911',
            users: ['yndx-ufo-test-254'],
            artifacts: [
                consts.FILES,
                {
                    name: consts.FILES,
                    url: '/disk/url/folder-for-moving'
                },
                consts.FAST_TRASH
            ]
        },
        {
            testId: 'diskclient-914',
            users: [
                'yndx-ufo-test-312',
                'yndx-ufo-test-313',
                'yndx-ufo-test-314',
                'yndx-ufo-test-503',
                'yndx-ufo-test-504',
                'yndx-ufo-test-505'
            ],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        }
    ]
);
