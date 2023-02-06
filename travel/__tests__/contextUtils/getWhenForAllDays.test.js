jest.disableAutomock();

import {getWhenForAllDays} from '../../contextUtils';

describe('getWhenForAllDays', () => {
    it('В свойстве special должно быть "all-days"', () => {
        expect(getWhenForAllDays('ru').special).toBe('all-days');
    });
});
