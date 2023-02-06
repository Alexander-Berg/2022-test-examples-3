const { NAVIGATION } = require('../../hermione/config').consts;
const { consts, taskRunner } = require('../runner');

taskRunner(
    'Поделение в Последних файлах',
    [
        {
            testId: 'diskclient-5060, 671',
            users: ['yndx-ufo-test-189', 'yndx-ufo-test-188'],
            artifacts: [
                {
                    name: consts.FILES,
                    url: NAVIGATION.recent.url
                },
                consts.FAST_TRASH
            ]
        },
        {
            testId: 'diskclient-5037, 673',
            users: ['yndx-ufo-test-183', 'yndx-ufo-test-182'],
            artifacts: [
                {
                    name: consts.FILES,
                    url: NAVIGATION.recent.url
                },
                consts.FAST_TRASH
            ]
        },
    ]
);
