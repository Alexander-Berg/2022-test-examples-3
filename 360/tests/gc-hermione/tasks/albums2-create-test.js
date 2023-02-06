const { consts, taskRunner } = require('../runner');

taskRunner(
    'Создание альбома -> ',
    [
        {
            testId: 'diskclient-5986, 5988, 5989',
            users: ['yndx-ufo-test-331'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-5985',
            users: ['yndx-ufo-test-392'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-5737, 5841',
            users: ['yndx-ufo-test-385'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-5648, 5839',
            users: ['yndx-ufo-test-386'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-5649, 5840',
            users: ['yndx-ufo-test-387'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-5647, 5838',
            users: ['yndx-ufo-test-384'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-6022',
            users: ['yndx-ufo-test-517'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-6106, 6105, 6054, 6055, 6053, 6059, 6057, 6058, 6050, 6051, 6049, 6046, 6047, 6045, 6043, 6042, 6041', // eslint-disable-line
            users: ['yndx-ufo-test-750'],
            artifacts: [consts.ALBUMS],
        },
        {
            testId: 'diskclient-6169',
            users: ['yndx-ufo-test-760'],
            artifacts: [consts.ALBUMS],
        }
    ]
);
