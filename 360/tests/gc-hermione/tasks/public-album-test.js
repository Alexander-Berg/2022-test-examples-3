const { consts, taskRunner } = require('../runner');

taskRunner(
    'Паблик Альбома',
    [
        {
            testId: 'diskpublic-savealbum-unauth',
            users: ['test'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: (name) => name.startsWith('TestAlbum'),
                        beforeDate: Date.now()
                    }
                },
                consts.FAST_TRASH
            ]
        }
    ]
);
