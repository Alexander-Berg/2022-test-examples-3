import * as path from 'path';
import _ from 'lodash';
import 'mocha';
import { assert } from 'chai';
import { setupEnvironment, restoreEnvironment } from './environment';
import { readDirectory } from './utils';

describe('tide-testdata-manager', function () {
    let argvBackup: string[];
    const ignore = [/.*\.DS_Store/, /.*\.json$/];

    beforeEach(() => {
        argvBackup = _.cloneDeep(process.argv);
        setupEnvironment();
    });

    [
        {
            name: 'should change the parameter value',
            oldUrl: 'exp_flags=font-size=10',
            newUrl: 'exp_flags=font-size=22',
            inputDir: 'test/tide-testdata-manager/fixtures/value-edit',
        },
        {
            name: 'should remove the parameter',
            oldUrl: 'exp_flags=font-size=10',
            newUrl: '',
            inputDir: 'test/tide-testdata-manager/fixtures/parameter-removal',
        },
        {
            name: 'should update the whole url address',
            oldUrl: '/api/method?exp_flags=font-size=10',
            newUrl: '/api/v2/new_method?exp_flags=font-size=22',
            inputDir: 'test/tide-testdata-manager/fixtures/url-path-edit',
        },
        {
            name: 'should add new flag if old value is not specified',
            oldUrl: '',
            newUrl: 'exp_flags=universe=42',
            inputDir: 'test/tide-testdata-manager/fixtures/flag-addition',
        },
    ].forEach((test) => {
        it(test.name, async function () {
            process.argv = [
                'dummy-node-path',
                'dummy-tide-path',
                '-c',
                path.resolve('test/tide-testdata-manager/fixtures/test.tide.config.js'),
                'update-url',
                test.inputDir,
                '--old-url',
                test.oldUrl,
                '--new-url',
                test.newUrl,
            ];
            const expectedDirectoryContents = await readDirectory(
                test.inputDir + '-output',
                undefined,
                ignore,
            );

            const tideCompletion = require('../fixtures/events').tideCompletion;
            // Запуск tide
            require('../../../bin/tide');
            await tideCompletion;

            const actualDirectoryContents = await readDirectory(test.inputDir, undefined, ignore);

            assert.deepEqual(actualDirectoryContents, expectedDirectoryContents);
        });
    });
    afterEach(() => {
        restoreEnvironment();
        process.argv = argvBackup;
    });
});
