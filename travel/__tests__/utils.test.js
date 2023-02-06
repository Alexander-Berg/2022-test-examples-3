jest.disableAutomock();

import {momentTimezone as moment} from '../../../../../reexports';
import {getDuration} from '../getDuration';
import {isRangeArrival} from '../isRangeArrival';

const timezone = 'Europe/Moscow';
const stationTo = {timezone};
const minArrival = '2018-04-29T09:40:00+00:00';
const maxArrival = '2018-04-29T11:40:00+00:00';

const arrivalMoment = moment.tz(maxArrival, timezone);
const departureMoment = moment.tz('2018-04-29T02:40:00+00:00', timezone);

const segment = {
    isMetaSegment: true,
    minArrival,
    maxArrival,
    arrivalMoment,
    departureMoment,
    stationTo,
};

describe('isRangeArrival', () => {
    it('Если это не метасегмент - вернём false', () => {
        expect(
            isRangeArrival({
                ...segment,
                isMetaSegment: false,
            }),
        ).toBe(false);
    });

    it('Если это метасегмент, но время прибытия только одно - вернём false', () => {
        expect(
            isRangeArrival({
                ...segment,
                maxArrival: minArrival,
            }),
        ).toBe(false);
    });

    it('Если это метасегмент, и время прибытия вагонов различается - вернём true', () => {
        expect(isRangeArrival(segment)).toBe(true);
    });
});

describe('getDuration', () => {
    it('Если это простой сегмент - вернем разницу между arrival и departure в секундах', () => {
        expect(
            getDuration({
                ...segment,
                isMetaSegment: false,
            }),
        ).toBe(32400); // 9 часов
    });

    it('Если это метасегмент, но время прибытия одно - вернем разницу между arrival и departure в секундах', () => {
        expect(
            getDuration({
                ...segment,
                minArrival: maxArrival,
            }),
        ).toBe(32400); // 9 часов
    });

    it('Если это метасегмент, и время прибытия вагонов различается - вернем разницу между minArrival и departure в секундах', () => {
        expect(getDuration(segment)).toBe(25200); // 7 часов
    });
});
