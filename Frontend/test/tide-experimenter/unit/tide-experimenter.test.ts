import j from 'jscodeshift';
import { assert } from 'chai';
import yaml from 'js-yaml';
import { TideExperimenter } from '../../../src/plugins/tide-experimenter/tide-experimenter';
import { Tide } from '../../../src';
import { TideExperimenterOptions } from '../../../src/plugins/tide-experimenter/types';

describe('tide-experimenter / utils', () => {
    let experimenter: TideExperimenter;

    beforeEach(() => {
        const tide = {
            parsers: {
                hermione: {
                    parser: j,
                },
            },
        } as unknown as Tide;
        const options = {} as TideExperimenterOptions;
        experimenter = new TideExperimenter(tide, options);
    });

    describe('addExperimentFieldToAst', () => {
        it('should add experiment field to specs object', () => {
            const input = `'use strict';\nspecs({ feature: 'Feature-name' }, () => { describe('nested'); });`;
            const expected = `'use strict';
specs({
    feature: 'Feature-name',
    experiment: 'exp-name'
}, () => { describe('nested'); });`;
            const inputAst = j(input);

            experimenter.addExperimentFieldToAst(inputAst, 'exp-name');
            const actual = inputAst.toSource({ quote: 'single', tabWidth: 4 });

            assert.equal(actual, expected);
        });

        it('should transform specs string feature name to object and add experiment field', () => {
            const input = `'use strict';\nspecs('Feature-name', () => { describe('nested'); });`;
            const expected = `'use strict';
specs({
    feature: 'Feature-name',
    experiment: 'exp-name'
}, () => { describe('nested'); });`;
            const inputAst = j(input);

            experimenter.addExperimentFieldToAst(inputAst, 'exp-name');
            const actual = inputAst.toSource({ quote: 'single', tabWidth: 4 });

            assert.equal(actual, expected);
        });

        it('should throw an error if specs first argument is neither string nor object', () => {
            const input = `'use strict';\nspecs([], () => { describe('nested'); });`;
            const inputAst = j(input);

            assert.throws(() => experimenter.addExperimentFieldToAst(inputAst, 'exp-name'));
        });
    });

    describe('addFlagToAst', () => {
        it('should create exp_flags property when it is absent', () => {
            const input = `'use strict';
specs('Feature-name', () => {
    it('nested', () => {
        this.browser.yaOpenSerp({
            text: 'text',
        }, PO.element());
    });
});`;
            const expected = `'use strict';
specs('Feature-name', () => {
    it('nested', () => {
        this.browser.yaOpenSerp({
            text: 'text',
            exp_flags: 'new_flag=22'
        }, PO.element());
    });
});`;
            const inputAst = j(input);

            experimenter.addFlagToAst(inputAst, ['yaOpenSerp'], 'new_flag', '22');
            const actual = inputAst.toSource({ quote: 'single', tabWidth: 4 });

            assert.equal(actual, expected);
        });

        it('should add exp flag to existing literal', () => {
            const input = `'use strict';
specs('Feature-name', () => {
    it('nested', () => {
        this.browser.yaOpenSerp({
            text: 'text',
            exp_flags: 'existing_flag=1',
        }, PO.element());
    });
});`;
            const expected = `'use strict';
specs('Feature-name', () => {
    it('nested', () => {
        this.browser.yaOpenSerp({
            text: 'text',
            exp_flags: ['existing_flag=1', 'new_flag=22'],
        }, PO.element());
    });
});`;
            const inputAst = j(input);

            experimenter.addFlagToAst(inputAst, ['yaOpenSerp'], 'new_flag', '22');
            const actual = inputAst.toSource({ quote: 'single', tabWidth: 4 });

            assert.equal(actual, expected);
        });

        it('should add exp flag to existing array', () => {
            const input = `'use strict';
specs('Feature-name', () => {
    it('nested', () => {
        this.browser.yaOpenSerp({
            text: 'text',
            exp_flags: ['existing_flag=1'],
        }, PO.element());
    });
});`;
            const expected = `'use strict';
specs('Feature-name', () => {
    it('nested', () => {
        this.browser.yaOpenSerp({
            text: 'text',
            exp_flags: ['existing_flag=1', 'new_flag=22'],
        }, PO.element());
    });
});`;
            const inputAst = j(input);

            experimenter.addFlagToAst(inputAst, ['yaOpenSerp'], 'new_flag', '22');
            const actual = inputAst.toSource({ quote: 'single', tabWidth: 4 });

            assert.equal(actual, expected);
        });

        it('should ignore commands with other names', () => {
            const input = `'use strict';
specs('Feature-name', () => {
    it('nested', () => {
        this.browser.yaSomething({});
        this.browser.yaAnother({});
    });
});`;
            const inputAst = j(input);

            experimenter.addFlagToAst(inputAst, ['yaOpenSerp'], 'new_flag', '22');
            const actual = inputAst.toSource({ quote: 'single', tabWidth: 4 });

            assert.equal(actual, input);
        });

        it('should ignore exp_flags with invalid format', () => {
            const input = `'use strict';
specs('Feature-name', () => {
    it('nested', () => {
        this.browser.yaOpenSerp({
            text: 'text',
            exp_flags: {},
        }, PO.element());
    });
});`;
            const inputAst = j(input);

            experimenter.addFlagToAst(inputAst, ['yaOpenSerp'], 'new_flag', '22');
            const actual = inputAst.toSource({ quote: 'single', tabWidth: 4 });

            assert.equal(actual, input);
        });
    });

    describe('addExperimentFieldToTestpalm', () => {
        it('it should add experiment field after feature', () => {
            const input = `feature: Feature
params:
  rearr: 1
specs:
  Something:
    - do: something another
v-team: unknown\n`;
            const expected = `feature: Feature
experiment: exp name
params:
  rearr: 1
specs:
  Something:
    - do: something another
v-team: unknown\n`;
            const inputData = yaml.load(input);

            const actual = yaml.dump(
                experimenter.addExperimentFieldToTestpalm(inputData, 'exp name'),
            );

            assert.equal(actual, expected);
        });
    });

    describe('addFlagToTestpalm', () => {
        it('it should create exp_flags property if it is absent', () => {
            const input = `feature: Feature
params:
  rearr: 1\n`;
            const expected = `feature: Feature
params:
  rearr: 1
  exp_flags:
    - new_flag=22\n`;
            const inputData = yaml.load(input);

            experimenter.addFlagToTestpalm(inputData, 'new_flag', '22');
            const actual = yaml.dump(inputData);

            assert.equal(actual, expected);
        });

        it('it should merge flags in exp_flags array', () => {
            const input = `feature: Feature
params:
  rearr: 1
  exp_flags:
    - existing_flag=1\n`;
            const expected = `feature: Feature
params:
  rearr: 1
  exp_flags:
    - existing_flag=1
    - new_flag=22\n`;
            const inputData = yaml.load(input);

            experimenter.addFlagToTestpalm(inputData, 'new_flag', '22');
            const actual = yaml.dump(inputData);

            assert.equal(actual, expected);
        });

        it('it should create exp_flags array with existing literal', () => {
            const input = `feature: Feature
params:
  rearr: 1
  exp_flags: existing_flag=1\n`;
            const expected = `feature: Feature
params:
  rearr: 1
  exp_flags:
    - existing_flag=1
    - new_flag=22\n`;
            const inputData = yaml.load(input);

            experimenter.addFlagToTestpalm(inputData, 'new_flag', '22');
            const actual = yaml.dump(inputData);

            assert.equal(actual, expected);
        });
    });
});
