import { Experiments } from './Experiments';
import { FlagsData } from './flags';

const testIdsArray = ['t0,01,02', 't1,11,12', 't2,21,22'];
const flagsData: FlagsData = {
    testIds: testIdsArray.join(';'),
    flags: {
        f0: 'v0',
        f1: 'v1',
        f2: 'v2',
    },
    flagsMap: {
        f0: [
            { value: 'v0', testId: 't0' },
        ],
        f1: [
            { value: 'v1', testId: 't1' },
        ],
        f2: [
            { value: 'v2', testId: 't2' },
        ],
    },
};

describe('activeFlags', () => {
    it('should return flag value', () => {
        const experiments = new Experiments(flagsData);

        const flagNames = Object.keys(flagsData.flags);
        flagNames.forEach(flagName => {
            const flagValue = experiments.useExperimentFlag(flagName);
            expect(flagValue).toBe(flagsData.flags[flagName]);
        });
    });

    it('should return active testIds for used flags', () => {
        const experiments = new Experiments(flagsData);

        experiments.useExperimentFlag('f0');
        experiments.useExperimentFlag('f2');

        const activeTestIds = experiments.getActiveTestIds();

        expect(activeTestIds).toEqual([testIdsArray[0], testIdsArray[2]]);
    });
});
