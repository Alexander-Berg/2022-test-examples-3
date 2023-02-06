const { consts, taskRunner } = require('../runner');
const { prefixes } = require('../helpers/consts');

taskRunner(
    'Контекстное меню папки -> ',
    [
        {
            testId: 'diskclient-3411, 931, 6329, 6330',
            users: ['yndx-ufo-test-192'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: prefixes.TMP
                    }
                },
                consts.FAST_TRASH
            ]
        }
    ]
);
