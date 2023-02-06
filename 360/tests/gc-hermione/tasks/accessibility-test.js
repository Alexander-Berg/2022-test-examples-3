const { NAVIGATION } = require('../../hermione/config').consts;
const { consts, taskRunner } = require('../runner');
const { prefixes } = require('../helpers/consts');

taskRunner(
    'Доступность',
    [
        {
            testId: 'diskclient-7140, 7143',
            users: ['yndx-ufo-test-639'],
            artifacts: [
                {
                    name: consts.FILES,
                    filter: {
                        byName: prefixes.TMP
                    }
                },
                {
                    name: consts.FILES,
                    url: NAVIGATION.folder('FolderForTmp').url,
                    filter: {
                        byName: prefixes.TMP
                    }
                },
                {
                    name: consts.FILES,
                    url: NAVIGATION.folder('FolderForTmpCopy').url,
                    filter: {
                        byName: prefixes.TMP
                    }
                },
                {
                    name: consts.FILES,
                    url: NAVIGATION.folder('FolderForTmpMove').url,
                    filter: {
                        byName: prefixes.TMP
                    }
                },
                consts.FAST_TRASH
            ],
        },
        {
            testId: 'diskclient-7164, 7165',
            users: ['yndx-ufo-test-639'],
            artifacts: [consts.ALBUMS],
        }
    ]
);
