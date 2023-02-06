import {ITrainsFilters} from 'types/trains/search/filters/ITrainsFilters';

import applyFilters from '../applyFilters';

describe('filters', () => {
    describe('applyFilter', () => {
        it('all filters applied successfully', () => {
            // @ts-ignore
            const filtersData = {
                filter1: {filteredSegmentIndices: [false, true]},
                filter2: {filteredSegmentIndices: [true, true]},
                filter3: {filteredSegmentIndices: [false, true]},
            } as ITrainsFilters;

            const applyResult = applyFilters({segmentIndex: 1, filtersData});

            expect(applyResult).toBe(true);
        });

        it('first filter failed', () => {
            // @ts-ignore
            const filtersData = {
                filter1: {filteredSegmentIndices: [false, false]},
                filter2: {filteredSegmentIndices: [true, true]},
                filter3: {filteredSegmentIndices: [false, true]},
            } as ITrainsFilters;

            const applyResult = applyFilters({segmentIndex: 1, filtersData});

            expect(applyResult).toBe(false);
        });

        it('second filter failed', () => {
            // @ts-ignore
            const filtersData = {
                filter1: {filteredSegmentIndices: [false, true]},
                filter2: {filteredSegmentIndices: [true, false]},
                filter3: {filteredSegmentIndices: [false, true]},
            } as ITrainsFilters;

            const applyResult = applyFilters({segmentIndex: 1, filtersData});

            expect(applyResult).toBe(false);
        });
    });
});
