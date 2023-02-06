const { consts, taskRunner } = require('../runner');

taskRunner(
    'Изменение даты попадания в овердрафт',
    [
        /* Hard */
        {
            testId: 'diskclient-6881, diskclient-6984',
            users: [
                'yandex-team-overdraft-173',
                'yandex-team-overdraft-170',
                'yandex-team-overdraft-171',
                'yandex-team-overdraft-172',
                'yandex-team-overdraft-178',
                'yandex-team-overdraft-156',
            ],
            artifacts: [consts.OVERDRAFT_HARD, consts.RESTORE_TRASH]
        },
        {
            testId: 'diskclient-6887, diskclient-6990, diskclient-6886, diskclient-6989',
            users: [
                'yndx-ufo-test-775',
            ],
            artifacts: [consts.OVERDRAFT_HARD]
        },
        {
            testId: 'diskclient-7034,7035,7036,7037,6870,6976,6849,6956,6853,6960,6850,...',
            users: [
                'yndx-ufo-test-776',
            ],
            artifacts: [consts.OVERDRAFT_HARD]
        },

        /* Lite */
        {
            testId: 'diskclient-6819,6931,6834,6942,6823,6934,6825,6935,6827,6936,6830,6939,...',
            users: [
                'yndx-ufo-test-773',
            ],
            artifacts: [consts.OVERDRAFT_LITE]
        },
    ]
);
