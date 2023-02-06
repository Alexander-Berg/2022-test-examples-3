import SearchSegment from '../../../interfaces/state/search/SearchSegment';

import buildArchivalSegmentsVector from '../buildArchivalSegmentsVector';

const segments = [
    {
        departure: '2020-06-06T06:15:00+00:00',
    },
    {
        departure: '2020-06-08T06:13:00+00:00',
    },
    {
        departure: '2020-06-04T06:11:00+00:00',
    },
    {
        departure: '2020-06-04T06:11:11+00:00',
    },
] as SearchSegment[];

const expectedVector = [2, 3, 1, 0];

describe('buildArchivalSegmentsVector', () => {
    it('Вернет сегменты, отсортированные без учета даты', () => {
        expect(buildArchivalSegmentsVector(segments)).toEqual(expectedVector);
    });
});
