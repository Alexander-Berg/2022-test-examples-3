const { consts, taskRunner } = require('../runner');
const { prefixes } = require('../helpers/consts');

taskRunner(
    'Действия с папкой -> ',
    [
        {
            testId: 'diskclient-1396, 1487',
            users: ['yndx-ufo-test-50', 'yndx-ufo-test-00'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-1597, 1488',
            users: ['yndx-ufo-test-61', 'yndx-ufo-test-11'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-1598, 1490',
            users: ['yndx-ufo-test-62', 'yndx-ufo-test-12'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-4723, 1273',
            users: ['yndx-ufo-test-150', 'yndx-ufo-test-148'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-1016, 1489',
            users: ['yndx-ufo-test-20', 'yndx-ufo-test-63'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-816, 1184',
            users: ['yndx-ufo-test-468'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-5158',
            users: ['yndx-ufo-test-470'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-4477, 4478',
            users: ['yndx-ufo-test-471'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-6039,6163',
            users: ['yndx-ufo-test-537'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: prefixes.TMP
                    }
                },
                consts.FAST_TRASH
            ]
        },
        {
            testId: 'diskclient-932',
            users: ['yndx-ufo-test-227'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
    ]
);
