import {TransportType} from '../../transportType';
import ISegmentFromBackend from '../../../interfaces/segment/ISegmentFromBackend';
import ISubSegmentFromBackend from '../../../interfaces/segment/ISubSegmentFromBackend';
import ISegmentCodeshare from '../../../interfaces/segment/ISegmentCodeshare';

import patchSegmentsFromBackend from '../patchSegmentsFromBackend';

interface IGetSegments {
    subSegments?: ISubSegmentFromBackend[];
    transportType?: TransportType;
    codeshares?: ISegmentCodeshare[];
    tariffsKeys?: string[];
}

function getSegment({
    subSegments,
    transportType = TransportType.train,
    codeshares,
    tariffsKeys,
}: IGetSegments): ISegmentFromBackend {
    return {
        transport: {code: transportType},
        subSegments,
        codeshares,
        tariffsKeys,
    } as ISegmentFromBackend;
}

function getSubSegment(): ISubSegmentFromBackend {
    return {
        transport: {code: TransportType.train},
    } as ISubSegmentFromBackend;
}

describe('patchSegmentsFromBackend', () => {
    it('Должен быть добавлен признак isSubSegment для беспересадочных вагонов', () => {
        const simpleSegments = [getSegment({}), getSegment({})];

        const resultSimple = patchSegmentsFromBackend(simpleSegments);

        expect(resultSimple[0].subSegments).toBeUndefined();
        expect(resultSimple[1].subSegments).toBeUndefined();
        expect(resultSimple[0]).not.toBe(simpleSegments[0]);
        expect(resultSimple[1]).not.toBe(simpleSegments[1]);
        expect(resultSimple[0].isMetaSegment).toBe(false);
        expect(resultSimple[1].isMetaSegment).toBe(false);

        const throughSegments = [
            getSegment({}),
            getSegment({subSegments: [getSubSegment(), getSubSegment()]}),
        ];

        const result = patchSegmentsFromBackend(throughSegments);

        expect(result[0].subSegments).toBeUndefined();
        expect(result[1].isMetaSegment).toBe(true);
        expect(result[1].subSegments?.[0].isSubSegment).toBe(true);
        expect(result[1].subSegments?.[1].isSubSegment).toBe(true);
        expect(result[0]).not.toBe(simpleSegments[0]);
        expect(result[1]).not.toBe(simpleSegments[1]);
        expect(result[1].subSegments?.[0]).not.toBe(
            throughSegments[1].subSegments?.[0],
        );
        expect(result[0].isMetaSegment).toBe(false);
    });

    it(`Ключи тарифов кодщерных рейсов должны быть добавлены в массив ключей тарифов сегментов, чтобы при получении авиа-тарифов,
    тарифы кодшеров мержились к сегменту, а не создавали новых сегментов`, () => {
        const segments = [
            getSegment({
                tariffsKeys: ['1'],
            }),
            getSegment({
                tariffsKeys: ['2'],
                codeshares: [{tariffsKeys: ['3']} as ISegmentCodeshare],
            }),
        ];

        const result = patchSegmentsFromBackend(segments);

        expect(result.length).toBe(2);
        expect(result[0].tariffsKeys).toEqual(['1']);
        expect(result[1].tariffsKeys).toEqual(['2', '3']);
    });
});
