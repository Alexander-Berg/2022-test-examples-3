import { EXPERIMENTS } from './experiments.data';
import { findExperiment } from '../api/experiments';

describe('experiments', () => {
    describe('#findExperiment', () => {
        it('mssng should be undefined', () => {
            expect(findExperiment(EXPERIMENTS, 'mssng')).toBeUndefined();
        });

        it('Aout should be 1', () => {
            expect(findExperiment(EXPERIMENTS, 'Aout')).toBe('1');
        });

        it('zzvs should be 2', () => {
            expect(findExperiment(EXPERIMENTS, 'zzvs')).toBe('2');
        });
    });
});
