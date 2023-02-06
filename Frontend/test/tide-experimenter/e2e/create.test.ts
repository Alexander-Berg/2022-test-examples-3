import * as path from 'path';
import _ from 'lodash';
import 'mocha';
import { assert } from 'chai';
import { setupEnvironment, restoreEnvironment, setupMockFs } from './environment';
import { readDirectory } from '../../utils';

describe('tide-experimenter / create', function () {
    let argvBackup: string[];
    const ignore = [/.*\.DS_Store/];

    beforeEach(() => {
        argvBackup = _.cloneDeep(process.argv);
        setupEnvironment();
    });

    [
        {
            name: 'should copy files from source directory',
            expName: 'Experiment-1',
            inputDir: 'test/tide-experimenter/fixtures/basic-copy',
            source: 'src/features/FeatureName/FeatureName.test',
            expFlags: 'new_flag=22',
        },
        {
            name: 'should create an experiment from scratch',
            expName: 'New-experiment',
            inputDir: 'test/tide-experimenter/fixtures/new-exp',
        },
    ].forEach((test) => {
        it(test.name, async function () {
            setupMockFs((test as any).source || '.', test.inputDir);

            process.argv = [
                'dummy-node-path',
                'dummy-tide-path',
                '-c',
                path.resolve('test/tide-experimenter/fixtures/test.tide.config.js'),
            ];

            process.argv.push('exp');
            if (test.source) {
                process.argv.push((test as any).source);
            }
            process.argv.push('--exp-name', test.expName);
            if (test.expFlags) {
                process.argv.push('--exp-flags', test.expFlags);
            }

            const expectedDirectoryContents = await readDirectory(
                path.resolve(test.inputDir + '-output'),
                undefined,
                ignore,
            );

            const tideCompletion = require('../fixtures/events').tideCompletion;
            // Запуск tide
            require('../../../bin/tide');
            await tideCompletion;

            const actualDirectoryContents = await readDirectory('output', undefined, ignore);

            assert.deepEqual(actualDirectoryContents, expectedDirectoryContents);
        });
    });
    afterEach(() => {
        restoreEnvironment();
        process.argv = argvBackup;
    });
});
