import { describe, it } from 'mocha';
import { assert } from 'chai';
import { findExperimental } from '../findExperimental';
import { ExpFlags, ExpName, ExpVal, IExperimentsFound } from '../../typings';

describe('findExperimental', () => {
    it('should return experiment for valid data', () => {
        const experiment: Map<string, number> = new Map().set('getMeNumber2', 2);
        const expFlags = { getMeNumber2: '1' };

        const expected: IExperimentsFound<number, string, string | number> = [{
            experiment: {
                name: 'getMeNumber2',
                val: '1',
            },
            found: 2,
        }];
        const actual = findExperimental<number, ExpFlags, ExpName, ExpVal>(experiment, expFlags);

        assert.deepEqual(actual, expected);
    });

    it('should return empty array if there is no active flag', () => {
        const experiment: Map<string, number> = new Map().set('getMeNumber2', 2);
        const expFlags = { getMeNumber3: 1 };

        assert.isEmpty(findExperimental<number, ExpFlags, ExpName, ExpVal>(experiment, expFlags));
    });

    it('should ignore "null" flag values', () => {
        const experiment: Map<string, number> = new Map([['getMeNumber', 1]]);
        const expFlags = { getMeNumber: 'null' };

        assert.isEmpty(findExperimental<number, ExpFlags, ExpName, ExpVal>(experiment, expFlags));
    });
});
