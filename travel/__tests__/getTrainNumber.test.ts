import {TTrainsStoreOrderSegment} from 'projects/trains/lib/segments/types';
import {ITrainsDetails} from 'reducers/trains/order/types';

import getTrainNumber from 'projects/trains/lib/order/getTrainNumber';

describe('getTrainNumber', () => {
    it('В аргументах только segment', () => {
        expect(
            getTrainNumber({number: '001Б'} as TTrainsStoreOrderSegment, null),
        ).toBe('001Б');
    });

    it('В аргументах segment и trainDetails', () => {
        expect(
            getTrainNumber(
                {number: '001Б'} as TTrainsStoreOrderSegment,
                {ticketNumber: '002В'} as ITrainsDetails,
            ),
        ).toBe('002В');
    });

    it('В аргументах segment и trainDetails, но объекты не содержат нужных полей', () => {
        expect(
            getTrainNumber(
                {} as TTrainsStoreOrderSegment,
                {} as ITrainsDetails,
            ),
        ).toBe('');
    });
});
