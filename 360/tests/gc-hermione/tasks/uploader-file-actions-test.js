const { consts, taskRunner } = require('../runner');

taskRunner(
    'Действия с файлами из загрузчика',
    [
        {
            testId: 'diskclient-4744',
            users: ['yndx-ufo-test-192'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4745',
            users: ['yndx-ufo-test-278'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        }
    ]
);
