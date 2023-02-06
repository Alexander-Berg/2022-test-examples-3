const { consts, taskRunner } = require('../runner');

taskRunner(
    'Слайдер ->',
    [
        {
            testId: 'diskclient-3387, diskclient-3388, diskclient-1149, diskclient-1252',
            users: ['yndx-ufo-test-176'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-5139',
            users: ['yndx-ufo-test-281'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-1148, diskclient-1238, diskclient-3557, diskclient-5068',
            users: ['yndx-ufo-test-403'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-1165, diskclient-1237',
            users: ['yndx-ufo-test-404'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-1153, diskclient-1250',
            users: ['yndx-ufo-test-405'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        }
    ]
);
