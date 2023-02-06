import {FilterTransportType} from '../../../transportType';

import highspeed from '../../highSpeedTrain';

describe('highspeed filter test', () => {
    describe('isAvailableForContext', () => {
        it('is available with all transport types', () => {
            const result = highspeed.isAvailableForContext({
                transportType: FilterTransportType.all,
            });

            expect(result).toBe(true);
        });

        it('is available with train transport type', () => {
            const result = highspeed.isAvailableForContext({
                transportType: FilterTransportType.train,
            });

            expect(result).toBe(true);
        });

        it('is not available with bus transport type', () => {
            const result = highspeed.isAvailableForContext({
                transportType: FilterTransportType.bus,
            });

            expect(result).toBe(false);
        });
    });
});
