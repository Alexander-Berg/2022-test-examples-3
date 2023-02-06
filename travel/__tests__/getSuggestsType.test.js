jest.disableAutomock();

import getSuggestsType from '../getSuggestsType';
import {ALL_TYPE, SUBURBAN_TYPE} from '../../transportType';

describe('getSuggestsType', () => {
    it('Get suggest', () => {
        expect(getSuggestsType(ALL_TYPE)).toBe('all_suggests');
        expect(getSuggestsType(SUBURBAN_TYPE)).toBe('suburban');
        expect(getSuggestsType()).toBe('by_t_type');
    });
});
