import * as path from 'path';
import _ from 'lodash';
import 'mocha';
import { assert } from 'chai';
import { restoreEnvironment, setupEnvironment, setupMockFs } from './environment';
import { readDirectory } from '../../utils';

describe('tide-experimenter / merge', function () {
    let argvBackup: string[];
    const ignore = [/.*\.DS_Store/];

    beforeEach(() => {
        argvBackup = _.cloneDeep(process.argv);
        setupEnvironment();
    });

    [
        {
            name: 'should merge experiment with production',
            source: 'test/tide-experimenter/fixtures/merge-all/experiment',
            targetDir: 'test/tide-experimenter/fixtures/merge-all/production',
            removeFlags: 'flag-name',
            removeExp: false,
            conflictMode: 'new',
            testDir: 'test/tide-experimenter/fixtures/merge-all',
        },
        {
            name: 'should generate conflicts when option is set',
            source: 'test/tide-experimenter/fixtures/merge-conflicts/experiment',
            targetDir: 'test/tide-experimenter/fixtures/merge-conflicts/production',
            removeFlags: '',
            removeExp: false,
            conflictMode: 'both',
            testDir: 'test/tide-experimenter/fixtures/merge-conflicts',
        },
        {
            name: 'should merge an experiment with itself',
            source: 'test/tide-experimenter/fixtures/merge-same-dir',
            targetDir: 'test/tide-experimenter/fixtures/merge-same-dir',
            removeFlags: 'flag-name',
            removeExp: false,
            conflictMode: 'both',
            testDir: 'test/tide-experimenter/fixtures/merge-same-dir',
        },
        {
            name: 'should copy require dependencies',
            source: 'test/tide-experimenter/fixtures/require-dependencies/experiment',
            targetDir: 'test/tide-experimenter/fixtures/require-dependencies/production',
            removeFlags: '',
            removeExp: false,
            conflictMode: 'new',
            testDir: 'test/tide-experimenter/fixtures/require-dependencies',
        },
        {
            name: 'should merge hermione and metrics files',
            source: 'test/tide-experimenter/fixtures/merge-hermione-metrics/experiment',
            targetDir: 'test/tide-experimenter/fixtures/merge-hermione-metrics/production',
            removeFlags: '',
            removeExp: false,
            conflictMode: 'both',
            testDir: 'test/tide-experimenter/fixtures/merge-hermione-metrics',
        },
        {
            name: 'hermione / should copy filters (e.g. only.in())',
            source: 'test/tide-experimenter/fixtures/merge-hermione-constructions/experiment',
            targetDir: 'test/tide-experimenter/fixtures/merge-hermione-constructions/production',
            removeFlags: '',
            removeExp: false,
            conflictMode: 'both',
            testDir: 'test/tide-experimenter/fixtures/merge-hermione-constructions',
        },
        {
            name: 'hermione / should merge with conflictMode = both',
            source: 'test/tide-experimenter/fixtures/merge-hermione-foreach-expr-both/experiment',
            targetDir:
                'test/tide-experimenter/fixtures/merge-hermione-foreach-expr-both/production',
            removeFlags: '',
            removeExp: false,
            conflictMode: 'both',
            testDir: 'test/tide-experimenter/fixtures/merge-hermione-foreach-expr-both',
        },
        {
            name: 'hermione / should merge with conflictMode = new',
            source: 'test/tide-experimenter/fixtures/merge-hermione-foreach-expr-new/experiment',
            targetDir: 'test/tide-experimenter/fixtures/merge-hermione-foreach-expr-new/production',
            removeFlags: '',
            removeExp: false,
            conflictMode: 'new',
            testDir: 'test/tide-experimenter/fixtures/merge-hermione-foreach-expr-new',
        },
        {
            name: 'hermione / should merge with conflictMode = old',
            source: 'test/tide-experimenter/fixtures/merge-hermione-foreach-expr-old/experiment',
            targetDir: 'test/tide-experimenter/fixtures/merge-hermione-foreach-expr-old/production',
            removeFlags: '',
            removeExp: false,
            conflictMode: 'old',
            testDir: 'test/tide-experimenter/fixtures/merge-hermione-foreach-expr-old',
        },
        {
            name: 'should remove exp files if remove-exp true',
            source: 'test/tide-experimenter/fixtures/remove-exp-files/experiment',
            targetDir: 'test/tide-experimenter/fixtures/remove-exp-files/production',
            removeFlags: '',
            removeExp: true,
            conflictMode: 'new',
            testDir: 'test/tide-experimenter/fixtures/remove-exp-files',
        },
    ].forEach((test) => {
        it(test.name, async function () {
            setupMockFs('', '');

            process.argv = [
                'dummy-node-path',
                'dummy-tide-path',
                '-c',
                path.resolve('test/tide-experimenter/fixtures/test.tide.config.js'),
                '--conflict-mode',
                test.conflictMode,
                'exp:merge',
                test.source,
                '--new-path',
                test.targetDir,
                '--remove-flags',
                test.removeFlags,
                test.removeExp ? '--remove-exp' : '--no-remove-exp',
            ];

            const expectedDirectoryContents = await readDirectory(
                path.resolve(test.testDir + '-output'),
                undefined,
                ignore,
            );

            const tideCompletion = require('../fixtures/events').tideCompletion;
            // Запуск tide
            require('../../../bin/tide');
            await tideCompletion;

            const actualDirectoryContents = await readDirectory(
                path.resolve(test.testDir),
                undefined,
                ignore,
            );

            assert.deepEqual(actualDirectoryContents, expectedDirectoryContents);
        });
    });
    afterEach(() => {
        restoreEnvironment();
        process.argv = argvBackup;
    });
});
