const { consts, taskRunner } = require('../runner');

taskRunner(
    'Редактирование файла из контекстного меню загрузчика',
    [
        {
            testId: 'diskclient-4751, 4750',
            users: ['yndx-ufo-test-280'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4786, 4715',
            users: ['yndx-ufo-test-430'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4800, diskclient-4752',
            users: ['yndx-ufo-test-279', 'yndx-ufo-test-423'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4753',
            users: ['yndx-ufo-test-401'],
            artifacts: [{
                name: consts.FILES,
                url: '/client/disk/folder'
            }, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4754, 4783',
            users: ['yndx-ufo-test-410'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4718',
            users: ['yndx-ufo-test-412'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4697, 4790',
            users: ['yndx-ufo-test-413', 'yndx-ufo-test-414'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4724, 4791',
            users: ['yndx-ufo-test-415', 'yndx-ufo-test-416'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4717',
            users: ['yndx-ufo-test-411'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4735, 4789',
            users: ['yndx-ufo-test-442'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4719, 4740',
            users: ['yndx-ufo-test-730'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4755, 4756, 4784',
            users: ['yndx-ufo-test-729'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-5742, 4778, 4743',
            users: ['yndx-ufo-test-731'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-4741',
            users: ['yndx-ufo-test-752', 'yndx-ufo-test-753'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        }
    ]
);
