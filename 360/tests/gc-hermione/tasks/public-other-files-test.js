const { consts, taskRunner } = require('../runner');

taskRunner(
    'Паблик залимитированного исполняемого файла',
    [
        {
            testId: 'diskpublic-1666, diskpublic-1816, diskpublic-2304, diskpublic-2303',
            users: ['test'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: (name) => name === 'Загрузки',
                        beforeDate: Date.now()
                    }
                },
                consts.FAST_TRASH
            ]
        }
    ]
);
