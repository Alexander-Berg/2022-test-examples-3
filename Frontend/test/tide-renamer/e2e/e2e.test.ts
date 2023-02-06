import * as path from 'path';
import _ from 'lodash';
import 'mocha';
import { expect } from 'chai';
import { setupEnvironment, restoreEnvironment } from './environment';
import { readDirectory } from './utils';
import { EndToEndTest } from '../../../src/plugins/tide-renamer/types';

describe('tide-renamer', function () {
    let argvBackup;

    beforeEach(() => {
        argvBackup = _.cloneDeep(process.argv);
        setupEnvironment();
    });

    [
        {
            name: 'it (json)',
            oldName: JSON.stringify(['Feature one', 'Describe in specs', 'The second it case']),
            newName: JSON.stringify(['Feature one', 'Describe in specs', 'Another it case']),
            inputDir: 'test/tide-renamer/fixtures/it-input',
        },
        {
            name: 'it as a property (string)',
            oldName: JSON.stringify(['Feature one', 'Describe in specs', 'The second it case']),
            newName: JSON.stringify(['Feature one', 'Describe in specs', 'Another it case']),
            inputDir: 'test/tide-renamer/fixtures/it-property-input',
        },
        {
            name: 'it multiple match (json)',
            oldName: JSON.stringify([
                { feature: 'Feature-one', type: 'Type-one' },
                'Describe in specs',
                'The (\\w+) it case',
            ]),
            newName: JSON.stringify([
                { feature: 'Feature-one', type: 'Type-one' },
                'Describe in specs',
                'New $1 it case',
            ]),
            inputDir: 'test/tide-renamer/fixtures/it-multiple-input',
        },
        {
            name: 'describe (string)',
            oldName: 'Feature-one / Type-one Describe in specs The first it case',
            newName: 'Feature-one / Type-one The new describe title The first it case',
            inputDir: 'test/tide-renamer/fixtures/describe-input',
        },
        {
            name: 'describe with spaces (string)',
            oldName: 'Feature-one / Type One Describe in Specs The first it Case',
            newName: 'Feature-one / Type One Describe New title The first it Case',
            inputDir: 'test/tide-renamer/fixtures/describe-with-spaces-input',
        },
        {
            name: 'describe as a property (json)',
            oldName: JSON.stringify([
                { feature: 'Feature-one', type: 'Type-one' },
                'Describe in specs',
                'The first it case',
            ]),
            newName: JSON.stringify([
                { feature: 'Feature-one', type: 'Type-one' },
                'The new describe title',
                'The first it case',
            ]),
            inputDir: 'test/tide-renamer/fixtures/describe-property-input',
        },
        {
            name: 'describe-nested (json)',
            oldName: JSON.stringify([
                { feature: 'Feature-one', type: 'Type-one' },
                'Describe in specs',
                'Nested describe',
                'The first it case',
            ]),
            newName: JSON.stringify([
                { feature: 'Feature-one', type: 'Type-one' },
                'The new describe title',
                'New nested describe',
                'The first it case',
            ]),
            inputDir: 'test/tide-renamer/fixtures/describe-nested-input',
        },
        {
            name: 'describe-without-specs (json)',
            oldName: JSON.stringify(['Describe in specs', 'Nested describe', 'The first it case']),
            newName: JSON.stringify([
                'The new describe title',
                'The new inner describe',
                'The first it case',
            ]),
            inputDir: 'test/tide-renamer/fixtures/describe-without-specs-input',
        },
        {
            name: 'specs (json)',
            oldName: JSON.stringify(['Feature-name', 'Describe in specs', 'The first it case']),
            newName: JSON.stringify(['New feature', 'Describe in specs', 'The first it case']),
            inputDir: 'test/tide-renamer/fixtures/specs-input',
        },
        {
            name: 'specs-nested (json)',
            oldName: JSON.stringify(['Feature-name', 'Nested specs', 'The first it case']),
            newName: JSON.stringify(['New feature', 'Another nested specs', 'The first it case']),
            inputDir: 'test/tide-renamer/fixtures/specs-nested-input',
        },
        {
            name: 'specs-with-object (json)',
            oldName: JSON.stringify([
                { feature: 'Feature-one', type: 'Type-one' },
                'Describe in specs',
                'The first it case',
            ]),
            newName: JSON.stringify([
                { feature: 'Another feature', type: 'The new type' },
                'Describe in specs',
                'The first it case',
            ]),
            inputDir: 'test/tide-renamer/fixtures/specs-with-object-input',
        },
        {
            name: 'all parts (json)',
            oldName: JSON.stringify([
                { feature: 'Feature name', experiment: 'Some experiment' },
                'Nested specs',
                'Describe in specs',
                'The second it case',
            ]),
            newName: JSON.stringify([
                { feature: 'New feature', experiment: 'Another experiment' },
                'New specs',
                'Some other describe',
                'Renamed it',
            ]),
            inputDir: 'test/tide-renamer/fixtures/all-parts-input',
        },
        {
            name: 'all parts (string)',
            oldName:
                'Feature name / Some experiment Nested specs Describe in specs The second it case',
            newName: 'New feature / Another experiment New specs Some other describe Renamed it',
            inputDir: 'test/tide-renamer/fixtures/all-parts-input',
        },
        {
            name: 'all parts, assets, metrics (json)',
            oldName: JSON.stringify([
                { feature: 'Feature-one', type: 'Type-one' },
                'Inner-specs',
                '(The second it case)',
            ]),
            newName: JSON.stringify([
                { feature: 'Feature-one N', type: 'Type-one Type' },
                'Inner-specs Y',
                '$1 mod',
            ]),
            inputDir: 'test/tide-renamer/fixtures/all-parts-assets-metrics-input',
        },
        {
            name: 'metrics indirect renaming (string)',
            oldName:
                'Feature name / Some experiment Nested specs Describe in specs The second it case',
            newName: 'Feature name / Some experiment New specs Describe in specs Renamed it',
            inputDir: 'test/tide-renamer/fixtures/metrics-indirect-input',
        },
    ].forEach((test: EndToEndTest) => {
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
                path.resolve('test/tide-renamer/fixtures/test.tide.config.js'),
                'rename',
                '--old-name',
                test.oldName,
                '--new-name',
                test.newName,
                test.inputDir,
            ];

            const expectedDirectoryContents = await readDirectory(test.outputDir);
            const tideCompletion = require('../fixtures/events').tideCompletion;

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
