import {TransportType} from '../../../lib/transportType';
import ISegmentFromApi from '../../../interfaces/segment/ISegmentFromApi';
import ISegmentTransport from '../../../interfaces/segment/ISegmentTransport';
import ISegmentStation from '../../../interfaces/segment/ISegmentStation';

import getSegmentUniqId from '../getSegmentUniqId';

const segment = {
    number: '882M',
    transport: {code: TransportType.suburban} as ISegmentTransport,
    stationFrom: {id: 193} as ISegmentStation,
    stationTo: {id: 213} as ISegmentStation,
    arrival: '2020-09-26T07:52:00+03:00',
    departure: '2020-09-25T07:52:00+03:00',
} as ISegmentFromApi;

describe('getSegmentUniqId', () => {
    it('Возвращае уникальный id для переданного сегмента', () => {
        expect(getSegmentUniqId(segment)).toEqual(
            'suburban-882M-193:2020-09-25T07:52:00+03:00-213:2020-09-26T07:52:00+03:00',
        );
    });
});
