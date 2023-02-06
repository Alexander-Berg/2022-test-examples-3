import * as path from 'path';
import _ from 'lodash';
import 'mocha';
import { expect } from 'chai';
import { setupEnvironment, restoreEnvironment } from './environment';
import { readDirectory } from './utils';

interface MoveEndToEndTest {
    name: string;
    oldPath: string;
    newPath: string;
    inputDir: string;
    outputDir?: string;
}

describe('tide-mover / move', function () {
    let argvBackup;

    beforeEach(() => {
        argvBackup = _.cloneDeep(process.argv);
        setupEnvironment();
    });

    [
        {
            name: 'should move all linked files to the new directory',
            oldPath: 'test/tide-mover/fixtures/move-basic-input/source/test.hermione.js',
            newPath: 'test/tide-mover/fixtures/move-basic-input/destination/test.hermione.js',
            inputDir: 'test/tide-mover/fixtures/move-basic-input',
        },
        {
            name: 'should move files to the new directory preserving old names',
            oldPath: 'test/tide-mover/fixtures/move-file-to-dir-input/source/test.hermione.js',
            newPath: 'test/tide-mover/fixtures/move-file-to-dir-input/destination',
            inputDir: 'test/tide-mover/fixtures/move-file-to-dir-input',
        },
        {
            name: 'should move files to the new directory and rename them',
            oldPath: 'test/tide-mover/fixtures/move-rename-input/source/test.hermione.js',
            newPath: 'test/tide-mover/fixtures/move-rename-input/destination/new-name.hermione.js',
            inputDir: 'test/tide-mover/fixtures/move-rename-input',
        },
        {
            name: 'should move the whole directory correctly',
            oldPath: 'test/tide-mover/fixtures/move-dir-input/source/nested',
            newPath: 'test/tide-mover/fixtures/move-dir-input/destination',
            inputDir: 'test/tide-mover/fixtures/move-dir-input',
        },
    ].forEach((test: MoveEndToEndTest) => {
        it(test.name, async function () {
            if (!test.outputDir) {
                if (!test.inputDir.endsWith('-input')) {
                    throw new Error(
                        "Test output dir was not provided and can't be computed automatically.",
                    );
                }
                test.outputDir = test.inputDir.slice(0, -6) + '-output';
            }

            process.argv = [
                'dummy-node-path',
                'dummy-tide-path',
                '-c',
                path.resolve('test/tide-mover/fixtures/test.tide.config.js'),
                'move',
                '--old-path',
                test.oldPath,
                '--new-path',
                test.newPath,
            ];
            const expectedDirectoryContents = await readDirectory(test.outputDir);

            const tideCompletion = require('../../tide-mover/fixtures/events').tideCompletion;
            // Запуск tide
            require('../../../bin/tide');
            await tideCompletion;

            const actualDirectoryContents = await readDirectory(test.inputDir);

            expect(actualDirectoryContents).deep.equal(expectedDirectoryContents);
        });
    });
    afterEach(() => {
        restoreEnvironment();
        process.argv = argvBackup;
    });
});
