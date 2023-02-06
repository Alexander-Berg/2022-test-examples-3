import ISegment from '../../../interfaces/segment/ISegment';
import ISegmentFromApi from '../../../interfaces/segment/ISegmentFromApi';
import ISearchMeta from '../../../interfaces/state/search/ISearchMeta';
import DateMoment from '../../../interfaces/date/DateMoment';

import updateSegments from '../updateSegments';
import clearDuplicateTrainTariffs from '../clearDuplicateTrainTariffs';

jest.mock('../getTariffMatchWeigh', () =>
    jest.fn(
        (baseSegment, tariffSegment) =>
            baseSegment.tariffKeys[0] === tariffSegment.key,
    ),
);
jest.mock('../../segments/patchSegments', () =>
    jest.fn(({segments}) => segments),
);
jest.mock('../clearDuplicateTrainTariffs', () => jest.fn(segments => segments));
jest.mock('../updateSegment', () =>
    jest.fn((baseSegment, tariffSegments) => ({...tariffSegments[0]})),
);

const baseSegmentA = {
    tariffKeys: ['A'],
} as unknown as ISegment;

const baseSegmentB = {
    tariffKeys: ['B'],
} as unknown as ISegment;

const tariffSegmentA = {
    tariffKeys: ['A'],
    key: 'A',
    tariffs: {},
} as unknown as ISegmentFromApi;

const tariffSegmentC = {
    tariffKeys: ['C'],
    key: 'C',
    tariffs: {},
} as unknown as ISegmentFromApi;

const meta = {} as ISearchMeta;

describe('updateSegments', () => {
    it('should update base segments with tariffs; should clear duplicates', () => {
        const baseSegments = [baseSegmentA, baseSegmentB];

        const tariffSegments = [tariffSegmentA];

        const expectedResultSegments = [tariffSegmentA, baseSegmentB];

        expect(updateSegments(baseSegments, tariffSegments, meta)).toEqual(
            expectedResultSegments,
        );
        expect(clearDuplicateTrainTariffs).toHaveBeenCalled();
    });

    it('should add unmatched tariff segments to the end of result array', () => {
        const baseSegments = [baseSegmentA, baseSegmentB];

        const tariffSegments = [tariffSegmentC];

        const expectedResultSegments = [
            baseSegmentA,
            baseSegmentB,
            tariffSegmentC,
        ];

        expect(updateSegments(baseSegments, tariffSegments, meta)).toEqual(
            expectedResultSegments,
        );
        expect(clearDuplicateTrainTariffs).toHaveBeenCalled();
    });

    it('should not update and should not add ambiguous segment', () => {
        const baseSegments = [baseSegmentA, baseSegmentA];
        const tariffSegments = [tariffSegmentA];

        expect(updateSegments(baseSegments, tariffSegments, meta)).toEqual(
            baseSegments,
        );
    });

    it('Тарифы следующего дня должны добавляться в виде самостоятельных сегментов только с учетом latestDatetime', () => {
        const baseSegments: ISegment[] = [];
        const metaWithLatestDatetime: ISearchMeta = {
            context: {
                latestDatetime: '2020-12-05T01:00:00+00:00' as DateMoment, // Соответствует 04:00 5 декабря по Москве
            },
        } as ISearchMeta;
        const tariffA = {
            ...tariffSegmentA,
            departure: '2020-12-04T12:00:00+00:00' as DateMoment,
        };
        const tariffC = {
            ...tariffSegmentC,
            departure: '2020-12-05T01:10:00+00:00' as DateMoment,
        };
        const tariffSegments = [tariffA, tariffC];

        expect(
            updateSegments(
                baseSegments,
                tariffSegments,
                metaWithLatestDatetime,
            ),
        ).toEqual([tariffA]);
    });
});
