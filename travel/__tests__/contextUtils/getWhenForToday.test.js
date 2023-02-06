jest.disableAutomock();

import {getWhenForToday} from '../../contextUtils';

describe('getWhenForToday', () => {
    it('В свойстве special должно быть "today"', () => {
        expect(getWhenForToday('ru').special).toBe('today');
    });
});
