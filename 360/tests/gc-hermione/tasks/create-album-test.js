const { consts, taskRunner } = require('../runner');

taskRunner(
    'Создание альбома -> ',
    [
        {
            testId: 'diskclient-781',
            users: ['yndx-ufo-test-86'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-855',
            users: ['yndx-ufo-test-87'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-1199',
            users: ['yndx-ufo-test-88'],
            artifacts: [consts.ALBUMS],
        },
    ]
);
