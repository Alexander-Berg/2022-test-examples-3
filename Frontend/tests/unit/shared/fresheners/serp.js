const _ = require('lodash');
const freshener = require('../../../../src/shared/fresheners/serp');
const screenProfiles = require('../../fixtures/screen-profiles.json');

const EXP = {
    filter: '',
    device: 'touch',
};

describe('search fresheners', () => {
    describe('fillSearchSystemsFilter', () => {
        let experiment;

        beforeEach(function() {
            experiment = _.cloneDeep(EXP);
        });

        it('должен привести значение к новому формату, если поле задано строкой', () => {
            const { exp } = freshener(experiment, {}, {}, screenProfiles);
            assert.deepEqual(exp.filter, { source: 'text', val: '' });
        });
    });
});
