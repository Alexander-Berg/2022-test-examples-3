import { assert } from 'chai';
import { File, Tide } from '../../../src';
import { parseConfig } from '../../../src/plugins/tide-experimenter/options';

describe('tide-experimenter / options', () => {
    describe('parseConfig', () => {
        describe('targetFilePath', () => {
            it('should generate correct path for the new stack', () => {
                const file = {
                    filePath: 'src/features/Feature/Feature.test/Feature@desktop.hermione.js',
                } as File;
                const tide = {} as Tide;
                const expected =
                    'src/experiments/exp-name/features/Feature/Feature.test/Feature@desktop.hermione.js';

                const pluginOptions = parseConfig({});
                const actual = pluginOptions.targetExpFilePath(file, 'exp-name', tide);

                assert.equal(actual, expected);
            });

            it('should generate correct path for the old stack', () => {
                const file = {
                    filePath: 'features/adapter/adapter_something@desktop.hermione.js',
                } as File;
                const tide = {} as Tide;
                const expected =
                    'experiments/exp-name/features/adapter/adapter_something@desktop.hermione.js';

                const pluginOptions = parseConfig({});
                const actual = pluginOptions.targetExpFilePath(file, 'exp-name', tide);

                assert.equal(actual, expected);
            });
        });
    });
});
