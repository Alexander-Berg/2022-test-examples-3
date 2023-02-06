const { consts, taskRunner } = require('../runner');

taskRunner(
    'Действия с файлом -> ',
    [
        {
            testId: 'diskclient-1409, 1603',
            users: ['yndx-ufo-test-52', 'yndx-ufo-test-02'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-1410, 1483',
            users: ['yndx-ufo-test-53', 'yndx-ufo-test-03'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-1411, 1482',
            users: ['yndx-ufo-test-56', 'yndx-ufo-test-06'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-2128, 1580',
            users: ['yndx-ufo-test-80', 'yndx-ufo-test-33'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-1606, 1486',
            users: ['yndx-ufo-test-54', 'yndx-ufo-test-04'],
            artifacts: [consts.FILES, consts.TRASH],
        },
        {
            testId: 'diskclient-1408, 1484',
            users: ['yndx-ufo-test-18', 'yndx-ufo-test-68'],
            artifacts: [consts.FILES, consts.TRASH],
        },
        {
            testId: 'diskclient-1605, 1485',
            users: ['yndx-ufo-test-55', 'yndx-ufo-test-05'],
            artifacts: [consts.FILES, consts.TRASH],
        },
        {
            testId: 'diskclient-723',
            users: ['yndx-ufo-test-474'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-637',
            users: ['yndx-ufo-test-475'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-613, 5075',
            users: ['yndx-ufo-test-476'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-3421',
            users: ['yndx-ufo-test-477'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-4320, diskclient-5071',
            users: ['yndx-ufo-test-219'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-592, diskclient-1190',
            users: ['yndx-ufo-test-224'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-921, diskclient-920',
            users: ['yndx-ufo-test-225'],
            artifacts: [
                consts.FILES,
                {
                    name: consts.FILES,
                    url: '/client/disk/level-1'
                },
                {
                    name: consts.FILES,
                    url: '/client/disk/level-1/level-2'
                },
                consts.FAST_TRASH
            ]
        },
        {
            // eslint-disable-next-line max-len
            testId: 'diskclient-665, diskclient-5613, diskclient-612, diskclient-5614, diskclient-3443, diskclient-5615',
            users: ['yndx-ufo-test-277'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-636, 5419',
            users: ['yndx-ufo-test-275'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-1360, 5420',
            users: ['yndx-ufo-test-407'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-1613, 5616',
            users: ['yndx-ufo-test-409'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            // eslint-disable-next-line max-len
            testId: 'diskclient-3444, 5074, 3416, 668, 5188, 621, 5189, 618, 1182, 614, 5190, 3445, 5192, 3415, 675, 5198',
            users: ['yndx-ufo-test-218'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-3275, 5194',
            users: ['yndx-ufo-test-427'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-3419, 923',
            users: ['yndx-ufo-test-424', 'yndx-ufo-test-426'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-1359, 5195',
            users: ['yndx-ufo-test-239'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-6131, 1580',
            users: ['yndx-ufo-test-33', 'yndx-ufo-test-80'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-618, 1182, 3277, 5193',
            users: ['yndx-ufo-test-232'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        },
        {
            testId: 'diskclient-1328, 6135',
            users: ['yndx-ufo-test-529'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-6121, 641',
            users: ['yndx-ufo-test-522', 'yndx-ufo-test-521'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-5123, diskclient-4997',
            users: ['yndx-ufo-test-265', 'yndx-ufo-test-250'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-listing-delete-by-del',
            users: ['yndx-ufo-test-545'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        },
        {
            testId: 'diskclient-3428',
            users: ['yndx-ufo-test-425'],
            artifacts: [consts.FILES, consts.FAST_TRASH],
        }
    ]
);
