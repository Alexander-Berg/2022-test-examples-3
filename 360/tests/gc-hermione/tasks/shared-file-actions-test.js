const { consts, taskRunner } = require('../runner');

taskRunner(
    'Публикация файлов',
    [
        {
            testId: 'diskclient-1522, 726',
            users: ['yndx-ufo-test-74', 'yndx-ufo-test-24'],
            artifacts: [consts.FILES, consts.TRASH],
        }
    ]
);

taskRunner(
    'Публикация файлов после загрузки и удаление публичных файлов',
    [
        {
            testId: 'diskclient-1187, 724',
            users: ['yndx-ufo-test-77', 'yndx-ufo-test-30'],
            artifacts: [consts.FILES, consts.TRASH],
        },
        {
            testId: 'diskclient-4799, 4738',
            users: ['yndx-ufo-test-181', 'yndx-ufo-test-180'],
            artifacts: [consts.FILES, consts.TRASH],
        },
        {
            testId: 'diskclient-1188, diskclient-725',
            users: ['yndx-ufo-test-141', 'yndx-ufo-test-142'],
            artifacts: [consts.FILES, consts.FAST_TRASH]
        }
    ]
);
