import { experimentalFlags } from './ExperimentalFlags';

describe('ExperimentalFlags', () => {
    beforeEach(() => {
        experimentalFlags.clearFlags();
    });

    it('should return flag value', () => {
        experimentalFlags.setFlags({ SOME_FLAG: 1 });

        expect(experimentalFlags.getFlagValue('SOME_FLAG')).toBe(1);
        expect(experimentalFlags.getFlagValue('ANOTHER_FLAG')).toBe(undefined);
    });

    it('shpuld clear flags', () => {
        experimentalFlags.setFlags({ SOME_FLAG: 1 });

        expect(experimentalFlags.getFlagValue('SOME_FLAG')).toBe(1);

        experimentalFlags.clearFlags();

        expect(experimentalFlags.getFlagValue('SOME_FLAG')).toBe(undefined);
    });
});
