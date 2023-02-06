const { consts, taskRunner } = require('../runner');

taskRunner(
    'Добавление в альбом -> ',
    [
        {
            testId: 'diskclient-4513',
            users: ['yndx-ufo-test-135'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-4514',
            users: ['yndx-ufo-test-136'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-1164',
            users: ['yndx-ufo-test-137'],
            artifacts: [consts.ALBUMS],
        },
    ]
);
