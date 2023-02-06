'use strict';

const applyFilters = require.requireActual('../../utils/applyFilters').default;

describe('utils', () => {
    describe('applyFilter', () => {
        it('all filters applied successfully', () => {
            const filtersData = {
                filter1: {filteredSegmentIndices: [false, true]},
                filter2: {filteredSegmentIndices: [true, true]},
                filter3: {filteredSegmentIndices: [false, true]},
            };

            const applyResult = applyFilters({segmentIndex: 1, filtersData});

            expect(applyResult).toBe(true);
        });

        it('first filter failed', () => {
            const filtersData = {
                filter1: {filteredSegmentIndices: [false, false]},
                filter2: {filteredSegmentIndices: [true, true]},
                filter3: {filteredSegmentIndices: [false, true]},
            };

            const applyResult = applyFilters({segmentIndex: 1, filtersData});

            expect(applyResult).toBe(false);
        });

        it('second filter failed', () => {
            const filtersData = {
                filter1: {filteredSegmentIndices: [false, true]},
                filter2: {filteredSegmentIndices: [true, false]},
                filter3: {filteredSegmentIndices: [false, true]},
            };

            const applyResult = applyFilters({segmentIndex: 1, filtersData});

            expect(applyResult).toBe(false);
        });
    });
});
