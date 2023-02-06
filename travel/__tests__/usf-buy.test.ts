import moment from 'moment';

import {UFS_LINK, DOMAIN} from '../ufs-buy';

import ISegment from '../../../interfaces/segment/ISegment';
import DateMoment from '../../../interfaces/date/DateMoment';

// eslint-disable-next-line no-duplicate-imports
import getUfsBuyLink from '../ufs-buy';

const segment = {
    number: 2,
    departure: '2018-05-12T20:59:00+00:00',
    stationFrom: {
        codes: {express: 2200900},
        timezone: 'Europe/Moscow',
    },
    stationTo: {
        codes: {express: 2000100},
    },
} as unknown as ISegment;

describe('getUfsBuyLink', () => {
    it('Должна вернуть корректный url', () => {
        expect(getUfsBuyLink(segment)).toBe(
            `${UFS_LINK}/2200900/2000100?date=12.05.2018&domain=${DOMAIN}&trainNumber=2`,
        );
    });

    it('С учетом таймзоны, должна получиться ссылка на следующий день по отношению к UTC departure.', () => {
        expect(
            getUfsBuyLink({
                ...segment,
                departure: '2018-05-12T21:00:00+00:00' as DateMoment,
            }),
        ).toBe(
            `${UFS_LINK}/2200900/2000100?date=13.05.2018&domain=${DOMAIN}&trainNumber=2`,
        );
    });

    it('Если задано время отправления - вставлять его в ссылку.', () => {
        expect(
            getUfsBuyLink(segment, moment('2018-05-24T00:00:00+05:00')),
        ).toBe(
            `${UFS_LINK}/2200900/2000100?date=24.05.2018&domain=${DOMAIN}&trainNumber=2`,
        );
    });
});
