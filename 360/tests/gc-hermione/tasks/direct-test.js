const { consts, taskRunner } = require('../runner');

taskRunner(
    'Действия с файлом -> ',
    [
        {
            testId: 'diskclient-6316',
            users: [
                'yndx-ufo-test-550',
                'yndx-ufo-test-551',
                'yndx-ufo-test-552',
                'yndx-ufo-test-553',
                'yndx-ufo-test-554'
            ],
            artifacts: [consts.FILES],
        },
        {
            testId: 'diskclient-7082',
            users: ['yndx-ufo-test-635', 'yndx-ufo-test-636', 'yndx-ufo-test-637'],
            artifacts: [consts.FILES],
        },
    ]
);
