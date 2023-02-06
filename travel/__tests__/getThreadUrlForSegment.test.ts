import ISegment from '../../../interfaces/segment/ISegment';
import ITransfer from '../../../interfaces/transfer/ITransfer';
import {TransportType} from '../../transportType';
import IStateSeoQueryParams from '../../../interfaces/state/IStateSeoQueryParams';
import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';
import DateRobot from '../../../interfaces/date/DateRobot';
import ISegmentThread from '../../../interfaces/segment/ISegmentThread';
import IStateFlags from '../../../interfaces/state/flags/IStateFlags';

import applyUtm from '../../url/applyUtm';
import getThreadUrlForSegment from '../getThreadUrlForSegment';
import getThreadUrl from '../../url/getThreadUrl';
import getPlaneThreadUrl from '../../url/getPlaneThreadUrl';

jest.mock('../../url/applyUtm', () => jest.fn(() => ''));
jest.mock('../../url/getThreadUrl', () => jest.fn(() => ''));
jest.mock('../../url/getPlaneThreadUrl', () => jest.fn(() => ''));

const tld = Tld.ru;
const language = Lang.ru;

const departure = '2019-11-10T23:55:00+00:00';
const seoQueryParams = {reqId: 'reqId'} as IStateSeoQueryParams;

interface IGetSegment {
    transportType?: TransportType;
    url?: string;
    stationFromId?: number;
    stationToId?: number;
    threadUid?: string;
    number?: string; // Номер рейса
    isInterval?: boolean;
    intervalThreadDepartureFromDate?: DateRobot;
}

function getSegment({
    transportType = TransportType.train,
    url,
    stationFromId = 1,
    stationToId = 2,
    threadUid = '12',
    number,
    isInterval = false,
    intervalThreadDepartureFromDate,
}: IGetSegment): ISegment {
    const thread: Record<string, any> = {};

    if (threadUid) {
        thread.uid = threadUid;
    }

    if (number) {
        thread.number = number;
    }

    return {
        transport: {code: transportType},
        url,
        stationFrom: {id: stationFromId},
        stationTo: {id: stationToId},
        departure,
        isInterval,
        intervalThreadDepartureFromDate,
        ...(Object.keys(thread).length ? {thread} : {}),
    } as ISegment;
}

describe('getThreadUrl', () => {
    it('Для пересадок вернет undefined', () => {
        expect(
            getThreadUrlForSegment({
                segment: {
                    ...getSegment({}),
                    isTransfer: true,
                } as unknown as ITransfer,
                tld,
                language,
                seoQueryParams,
                isToCitySearchContext: false,
            }),
        ).toBeUndefined();
    });

    it('Для самолетных сегментов без url и thread вернет undefined', () => {
        expect(
            getThreadUrlForSegment({
                segment: getSegment({
                    transportType: TransportType.plane,
                    threadUid: '',
                }),
                tld,
                language,
                isToCitySearchContext: false,
            }),
        ).toBeUndefined();
    });

    it('Для самолетных сегментов с url вызовет applyUtm', () => {
        expect(
            getThreadUrlForSegment({
                segment: getSegment({
                    transportType: TransportType.plane,
                    url: 'someUrl',
                }),
                tld,
                language,
                seoQueryParams: {reqId: 'reqId'} as IStateSeoQueryParams,
                isToCitySearchContext: false,
            }),
        ).toBe('');

        expect(applyUtm as jest.Mock).toBeCalledWith(
            'someUrl',
            seoQueryParams,
            undefined,
            undefined,
        );
    });

    it('Для самолетных сегментов с thread сформирует ссылку с помощью getPlaneThreadUrl', () => {
        expect(
            getThreadUrlForSegment({
                segment: getSegment({
                    transportType: TransportType.plane,
                    number: 'SU 1400',
                }),
                tld,
                language,
                seoQueryParams: {reqId: 'reqId'} as IStateSeoQueryParams,
                isToCitySearchContext: false,
            }),
        ).toBe('');

        expect(getPlaneThreadUrl as jest.Mock).toBeCalledWith(
            expect.objectContaining({
                numberPlane: 'SU 1400',
                departure,
            }),
        );
    });

    it('Если у сегмента нет информации о нитке, то вернет undefined', () => {
        expect(
            getThreadUrlForSegment({
                segment: getSegment({threadUid: ''}),
                tld,
                language,
                isToCitySearchContext: false,
            }),
        ).toBeUndefined();
    });

    it('Если есть информация о нитке, то дернет threadUrl', () => {
        expect(
            getThreadUrlForSegment({
                segment: getSegment({}),
                tld,
                language,
                isToCitySearchContext: false,
            }),
        ).toBe('');

        expect(getThreadUrl as jest.Mock).toBeCalledWith(
            expect.objectContaining({
                tld,
                language,
                departureFromMoment: undefined,
                stationFromId: 1,
                stationToId: 2,
                threadUid: '12',
            }),
        );
    });

    it('Для поиска на все дни вернет threadUrl, если есть информация о нитке,', () => {
        expect(
            getThreadUrlForSegment({
                segment: getSegment({}),
                tld,
                language,
                seoQueryParams,
                isToCitySearchContext: false,
            }),
        ).toBe('');

        expect(getThreadUrl as jest.Mock).toBeCalledWith(
            expect.objectContaining({
                tld,
                language,
                departureFromMoment: undefined,
                stationFromId: 1,
                stationToId: 2,
                threadUid: '12',
            }),
        );
    });

    it('Свойство intervalThreadDepartureFromDate будет использовано только для интервальныйх рейсов', () => {
        const segment = getSegment({
            intervalThreadDepartureFromDate: '2020-05-29' as DateRobot,
        });

        expect(
            getThreadUrlForSegment({
                segment,
                tld,
                language,
                isToCitySearchContext: false,
            }),
        ).toBe('');

        expect(getThreadUrl as jest.Mock).toBeCalledWith(
            expect.objectContaining({
                tld,
                language,
                stationFromId: 1,
                stationToId: 2,
                threadUid: '12',
                departureFromMoment: undefined,
            }),
        );
    });

    it('Для интервальных рейсов будет использовано поле "intervalThreadDepartureFromDate"', () => {
        const segment = getSegment({
            isInterval: true,
            intervalThreadDepartureFromDate: '2020-05-29' as DateRobot,
        });

        expect(
            getThreadUrlForSegment({
                segment,
                tld,
                language,
                isToCitySearchContext: false,
            }),
        ).toBe('');

        expect(getThreadUrl as jest.Mock).toBeCalledWith(
            expect.objectContaining({
                tld,
                language,
                departureFromDateRobot: '2020-05-29',
                stationFromId: 1,
                stationToId: 2,
                threadUid: '12',
            }),
        );
    });

    it('Вернет undefined, если сегмент архивный', () => {
        expect(
            getThreadUrlForSegment({
                segment: {...getSegment({}), isArchival: true},
                tld,
                language,
                isToCitySearchContext: false,
            }),
        ).toBe(undefined);
    });

    it('Вернет каноническую ссылку, если есть canonicalUid и флаг __notCanonicalThreadUid false', () => {
        const segment = {
            ...getSegment({}),
            thread: {
                uid: '12',
                canonicalUid: 'test_canonical_uid',
            } as ISegmentThread,
        };

        expect(
            getThreadUrlForSegment({
                segment,
                tld,
                language,
                flags: {__notCanonicalThreadUid: false} as IStateFlags,
                isToCitySearchContext: false,
            }),
        ).toBe('');
        expect(getThreadUrl as jest.Mock).toBeCalledWith(
            expect.objectContaining({
                tld,
                language,
                departure: '2019-11-10T23:55:00+00:00',
                departureFromDateRobot: undefined,
                departureFromMoment: undefined,
                stationFromId: 1,
                stationToId: 2,
                threadUid: '12',
                canonicalUid: 'test_canonical_uid',
            }),
        );
    });

    it('Вернет НЕ каноническую ссылку, если есть canonicalUid но выставлен флаг __notCanonicalThreadUid', () => {
        const segment = {
            ...getSegment({}),
            thread: {
                uid: '12',
                canonicalUid: 'test_canonical_uid',
            } as ISegmentThread,
        };

        expect(
            getThreadUrlForSegment({
                segment,
                tld,
                language,
                flags: {__notCanonicalThreadUid: true} as IStateFlags,
                isToCitySearchContext: false,
            }),
        ).toBe('');
        expect(getThreadUrl as jest.Mock).toBeCalledWith(
            expect.objectContaining({
                tld,
                language,
                departure: '2019-11-10T23:55:00+00:00',
                departureFromDateRobot: undefined,
                departureFromMoment: undefined,
                stationFromId: 1,
                stationToId: 2,
                threadUid: '12',
                canonicalUid: undefined,
            }),
        );
    });

    it('Вернет НЕ каноническую ссылку, если нет canonicalUid независимо от выставленного флага', () => {
        expect(
            getThreadUrlForSegment({
                segment: getSegment({}),
                tld,
                language,
                isToCitySearchContext: false,
            }),
        ).toBe('');
        expect(getThreadUrl as jest.Mock).toBeCalledWith(
            expect.objectContaining({
                tld,
                language,
                departure: '2019-11-10T23:55:00+00:00',
                departureFromDateRobot: undefined,
                departureFromMoment: undefined,
                stationFromId: 1,
                stationToId: 2,
                threadUid: '12',
                canonicalUid: undefined,
            }),
        );
    });
});
