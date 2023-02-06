import moment from 'moment';

import humanizeDuration from '../../duration/humanizeDuration';

describe('humanizeDuration', () => {
    it('should round duration and then format it', () => {
        const duration = moment.duration({days: 2, minutes: 31});

        expect(humanizeDuration(duration)).toBe('2 дн. 1 ч');
    });
});
