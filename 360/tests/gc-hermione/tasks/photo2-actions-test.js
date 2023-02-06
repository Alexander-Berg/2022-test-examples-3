const { NAVIGATION } = require('../../hermione/config').consts;
const { consts, taskRunner } = require('../runner');
const { prefixes } = require('../helpers/consts');

taskRunner(
    'Действия в фотосрезе -> ',
    [
        {
            testId: 'diskclient-4315, 4264',
            users: ['yndx-ufo-test-90', 'yndx-ufo-test-147'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: [prefixes.COPY, prefixes.TMP]
                    }
                },
                consts.FAST_TRASH
            ],
        },
        {
            testId: 'diskclient-4619, 4620',
            users: ['yndx-ufo-test-195', 'yndx-ufo-test-196'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: [prefixes.COPY, prefixes.TMP]
                    }
                },
                consts.FAST_TRASH
            ],
        },
        {
            testId: 'diskclient-4322',
            users: ['yndx-ufo-test-129', 'yndx-ufo-test-130'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: [prefixes.COPY, prefixes.TMP]
                    }
                },
                consts.FAST_TRASH
            ],
        },
        {
            testId: 'diskclient-5280, 5574',
            users: ['yndx-ufo-test-208', 'yndx-ufo-test-209'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: [prefixes.COPY, prefixes.TMP]
                    }
                },
                consts.FAST_TRASH
            ],
        },
        {
            testId: 'diskclient-4291, 4321',
            users: ['yndx-ufo-test-408'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: [prefixes.COPY, prefixes.TMP]
                    }
                },
                consts.FAST_TRASH
            ],
        },
        {
            testId: 'diskclient-4311, 4263',
            users: ['yndx-ufo-test-92', 'yndx-ufo-test-93'],
            artifacts: [
                {
                    name: consts.MOVE_PHOTOS,
                    filter: {
                        byName: prefixes.MOVE
                    }
                },
                {
                    name: consts.FILES,
                    filter: {
                        byName: [prefixes.MOVE, prefixes.TMP]
                    }
                },
                consts.FAST_TRASH
            ],
        },
        {
            testId: 'diskclient-4616, 4617',
            users: ['yndx-ufo-test-197', 'yndx-ufo-test-198'],
            artifacts: [
                {
                    name: consts.MOVE_PHOTOS,
                    filter: {
                        byName: prefixes.MOVE
                    }
                },
                {
                    name: consts.FILES,
                    filter: {
                        byName: [prefixes.MOVE, prefixes.TMP]
                    }
                },
                consts.FAST_TRASH
            ],
        },
        {
            testId: 'diskclient-5282, 5568',
            users: ['yndx-ufo-test-210', 'yndx-ufo-test-211'],
            artifacts: [
                {
                    name: consts.MOVE_PHOTOS,
                    filter: {
                        byName: prefixes.MOVE
                    }
                },
                {
                    name: consts.FILES,
                    filter: {
                        byName: [prefixes.MOVE, prefixes.TMP]
                    }
                },
                consts.FAST_TRASH
            ],
        },
        {
            testId: 'diskclient-4621, 4627, 4545, 5199',
            users: ['yndx-ufo-test-94', 'yndx-ufo-test-95', 'yndx-ufo-test-223'],
            artifacts: [
                {
                    name: consts.PHOTOSLICE_RENAME,
                    url: NAVIGATION.photo.url
                }
            ],
        },
        {
            testId: 'diskclient-4614, 4615',
            users: ['yndx-ufo-test-199', 'yndx-ufo-test-200'],
            artifacts: [
                {
                    name: consts.PHOTOSLICE_RENAME,
                    url: NAVIGATION.photo.url
                }
            ],
        },
        {
            testId: 'diskclient-5286, 5386',
            users: ['yndx-ufo-test-212', 'yndx-ufo-test-213'],
            artifacts: [
                {
                    name: consts.PHOTOSLICE_RENAME,
                    url: NAVIGATION.photo.url + '?filter=unbeautiful'
                }
            ],
        },
        {
            testId: 'diskclient-4475',
            users: [
                'yndx-ufo-test-593',
                'yndx-ufo-test-612',
                'yndx-ufo-test-613',
                'yndx-ufo-test-614',
                'yndx-ufo-test-615',
                'yndx-ufo-test-616',
                'yndx-ufo-test-617',
                'yndx-ufo-test-618'
            ],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: prefixes.TMP
                    }
                },
                consts.TRASH
            ],
        }
    ]
);
