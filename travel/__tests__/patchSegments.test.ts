import DateSpecialValue from '../../../interfaces/date/DateSpecialValue';
import ISearchMeta from '../../../interfaces/state/search/ISearchMeta';
import ISegmentFromApi from '../../../interfaces/segment/ISegmentFromApi';
import ITransferFromApi from '../../../interfaces/transfer/ITransferFromApi';
import {TransportType} from '../../transportType';

import patchSegments from '../patchSegments';
import patchTariffs from '../tariffs/patchTariffs';

jest.mock('../tariffs/patchTariffs', () => jest.fn(tariffs => tariffs));

const baseSegment = {
    transport: {code: TransportType.train},
    stationFrom: {timezone: 'Asia/Yekaterinburg'},
    stationTo: {timezone: 'Europe/Moscow'},
};

const meta = {
    context: {
        from: {timezone: 'Asia/Yekaterinburg'},
        to: {timezone: 'Europe/Moscow'},
        time: {
            now: 1566278631782,
        },
    },
    environment: 'client',
} as ISearchMeta;

describe('patchSegments', () => {
    it('should return patched segments with "segmentId" property', () => {
        const baseSegments = [
            baseSegment as unknown as ISegmentFromApi,
            baseSegment as unknown as ITransferFromApi,
        ];
        const baseResult = patchSegments({segments: baseSegments, meta});

        expect(baseResult[0].segmentId).toEqual('client-0');
        expect(baseResult[1].segmentId).toEqual('client-1');

        const newSegments = [
            baseSegment as unknown as ISegmentFromApi,
            baseSegment as unknown as ISegmentFromApi,
        ];
        const newResult = patchSegments({segments: newSegments, meta});

        expect(newResult[0].segmentId).toEqual('client-2');
        expect(newResult[1].segmentId).toEqual('client-3');
    });

    it('should set `tariffsKeys` property for segments with `key` property', () => {
        const segments = [
            {...baseSegment, key: 'key-0'} as ISegmentFromApi,
            {...baseSegment, key: 'key-1'} as ISegmentFromApi,
            {
                ...baseSegment,
                key: 'key-2',
                isTransfer: true,
                segments: [],
            } as unknown as ITransferFromApi,
        ];
        const result = patchSegments({segments, meta});

        expect((result[0] as ISegmentFromApi).tariffsKeys).toEqual(['key-0']);
        expect((result[1] as ISegmentFromApi).tariffsKeys).toEqual(['key-1']);
        // убедимся, что свойство tariffKeys не появилось у пересадки
        expect((result[2] as ISegmentFromApi).tariffsKeys).toBeUndefined();
    });

    it('should not change `tariffsKeys` property if it is already present', () => {
        const segments = [
            {
                ...baseSegment,
                tariffsKeys: ['key-0', 'key-1'],
            } as ISegmentFromApi,
            {
                ...baseSegment,
                tariffsKeys: ['key-3', 'key-4'],
                key: 'key-4',
            } as ISegmentFromApi,
        ];

        const result = patchSegments({segments, meta});

        expect((result[0] as ISegmentFromApi).tariffsKeys).toEqual([
            'key-0',
            'key-1',
        ]);
        expect((result[1] as ISegmentFromApi).tariffsKeys).toEqual([
            'key-3',
            'key-4',
        ]);
    });

    it('should set `tariffsKeys` property if there is no `tariffsKeys` or `key` properties', () => {
        const segments = [
            {...baseSegment, title: 'segment0'} as ISegmentFromApi,
            {...baseSegment, title: 'segment1'} as ISegmentFromApi,
        ];

        const result = patchSegments({segments, meta});

        expect((result[0] as ISegmentFromApi).tariffsKeys).toEqual([]);
        expect((result[1] as ISegmentFromApi).tariffsKeys).toEqual([]);
    });

    it('Проверка корректности свойства isGone', () => {
        const segments = [
            {
                ...baseSegment,
                departure: '2019-08-20T12:30:00+05:00',
            } as ISegmentFromApi,
            {
                ...baseSegment,
                departure: '2018-08-20T12:30:00+05:00',
            } as ISegmentFromApi,
        ];

        const result = patchSegments({segments, meta});

        expect(result[0].isGone).toBe(false);
        expect(result[1].isGone).toBe(true);
    });

    it('Для поиска на все дни isGone должен быть false', () => {
        const segments = [
            {
                ...baseSegment,
                departure: '2019-08-20T12:30:00+05:00',
            } as ISegmentFromApi,
            {
                ...baseSegment,
                departure: '2018-08-20T12:30:00+05:00',
            } as ISegmentFromApi,
        ];

        const result = patchSegments({
            segments,
            meta: {
                ...meta,
                context: {
                    ...meta.context,
                    when: {
                        special: DateSpecialValue.allDays,
                    },
                },
            } as ISearchMeta,
        });

        expect(result[0].isGone).toBe(false);
        expect(result[1].isGone).toBe(false);
    });

    it('Должны патчиться тарифы сегментов и пересадок', () => {
        const segments = [
            {...baseSegment, tariffs: {}} as ISegmentFromApi,
            {
                ...baseSegment,
                segments: [
                    {...baseSegment, tariffs: {}},
                    {...baseSegment, tariffs: {}},
                ],
                isTransfer: true,
                tariffs: {},
            } as ITransferFromApi,
        ];

        patchSegments({segments, meta});

        expect(patchTariffs).toBeCalledWith(
            segments[0],
            segments[0].tariffs,
            meta,
        );
        expect(patchTariffs).toBeCalledWith(
            segments[1],
            (segments[1] as ITransferFromApi).tariffs,
            meta,
        );
        expect(patchTariffs).toBeCalledWith(
            (segments[1] as ITransferFromApi).segments[0],
            (segments[1] as ITransferFromApi).segments[0].tariffs,
            meta,
        );
        expect(patchTariffs).toBeCalledWith(
            (segments[1] as ITransferFromApi).segments[1],
            (segments[1] as ITransferFromApi).segments[1].tariffs,
            meta,
        );
    });
});
