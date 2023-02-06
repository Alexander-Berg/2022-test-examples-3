jest.dontMock('../utils');
jest.dontMock('../duration');
jest.dontMock('../price');
jest.dontMock('../../segments/isPriceGreater');
jest.dontMock('../../segments/getBaseTariffClassKeys');
jest.dontMock('../../segments/tariffClasses');

const time = {comparator: jest.fn(() => 0)};
const dateTime = {comparator: jest.fn(() => 0)};

jest.setMock('../time', {time, dateTime});

const SORTING_TYPES = require.requireActual('../utils').SORTING_TYPES;
const sortSegments = require.requireActual('../sortSegments').sortSegments;
const updateSortingIndices =
    require.requireActual('../sortSegments').updateSortingIndices;

const segments = [
    {
        segmentId: 'client-0',
        duration: 3000,
        transport: {
            code: 'train',
        },
        tariffs: {
            classes: {
                suite: {
                    nationalPrice: {
                        value: 1800,
                        currency: 'RUB',
                    },
                    seats: 5,
                },
                platzkarte: {
                    nationalPrice: {
                        value: 1200,
                        currency: 'RUB',
                    },
                    seats: 10,
                },
            },
        },
    },
    {
        segmentId: 'client-1',
        transport: {
            code: 'train',
        },
        duration: 2900,
    },
    {
        segmentId: 'client-2',
        duration: 3500,
        transport: {
            code: 'train',
        },
        tariffs: {
            classes: {
                suite: {
                    nationalPrice: {
                        value: 1600,
                        currency: 'RUB',
                    },
                    seats: 15,
                },
            },
        },
    },
];

const splittedSegments = [
    {
        segmentId: 'client-0-platzkarte',
        duration: 3000,
        index: 0,
        transport: {
            code: 'train',
        },
        tariffs: {
            classes: {
                platzkarte: {
                    nationalPrice: {
                        value: 1200,
                        currency: 'RUB',
                    },
                    seats: 10,
                },
            },
        },
    },
    {
        segmentId: 'client-2-suite',
        duration: 3500,
        index: 2,
        transport: {
            code: 'train',
        },
        tariffs: {
            classes: {
                suite: {
                    nationalPrice: {
                        value: 1600,
                        currency: 'RUB',
                    },
                    seats: 15,
                },
            },
        },
    },
    {
        segmentId: 'client-0-suite',
        duration: 3000,
        index: 0,
        transport: {
            code: 'train',
        },
        tariffs: {
            classes: {
                suite: {
                    nationalPrice: {
                        value: 1800,
                        currency: 'RUB',
                    },
                    seats: 5,
                },
            },
        },
    },
    {
        segmentId: 'client-1',
        duration: 2900,
        index: 1,
        transport: {
            code: 'train',
        },
    },
];

const nonModifyingSort = {
    by: SORTING_TYPES.DURATION,
    reverse: false,
};

const modifyingSort = {
    by: SORTING_TYPES.PRICE,
    reverse: false,
};

const state = {
    context: {
        from: {timezone: 'Asia/Yekaterinburg'},
        to: {timezone: 'Asia/Yekaterinburg'},
        when: {
            special: 'today',
        },
    },
    segments,
    filtering: {
        filteredSegmentIndices: [true, true, true],
    },
};

describe('sortSegments package', () => {
    describe('sortSegments function', () => {
        it('should return object with vector on nonmodifying sorting', () => {
            const result = sortSegments(segments, {
                ...state,
                sort: nonModifyingSort,
            });

            expect(result).toEqual({
                vector: [1, 0, 2],
            });
        });

        it('should return object with "vector" property on modifying sorting while prices are querying', () => {
            const result = sortSegments(segments, {
                ...state,
                queryingPrices: true,
                sort: modifyingSort,
            });

            expect(result).toEqual({
                vector: [0, 1, 2],
            });
        });

        it('should return object with segments mapping on modifying sorting', () => {
            const result = sortSegments(segments, {
                ...state,
                queryingPrices: false,
                sort: modifyingSort,
            });

            expect(result).toEqual({
                vector: [0, 1, 2, 3],
                filteredSegmentIndices: [true, true, true, true],
                segments: splittedSegments,
            });
        });

        it('should call sorting by time', () => {
            sortSegments(segments, {
                ...state,
                context: {
                    ...state.context,
                    when: {
                        special: 'tomorrow',
                    },
                },
                sort: {
                    by: SORTING_TYPES.DEPARTURE,
                },
            });

            expect(dateTime.comparator).toBeCalled();
            expect(time.comparator).not.toBeCalled();
        });

        it('should call sorting by dateTime', () => {
            sortSegments(segments, {
                ...state,
                context: {
                    ...state.context,
                    when: {
                        special: 'all-days',
                    },
                },
                sort: {
                    by: SORTING_TYPES.DEPARTURE,
                },
            });

            expect(time.comparator).toBeCalled();
            expect(dateTime.comparator).not.toBeCalled();
        });
    });

    describe('updateSortingIndices function', () => {
        it('should return current state of sortMapping on nonmodifying sorting', () => {
            const sortMapping = {
                vector: [0, 1, 2],
            };
            const result = updateSortingIndices({
                ...state,
                sortMapping,
                sort: nonModifyingSort,
            });

            expect(result).toBe(sortMapping);
        });

        it('should update sortMapping on modifying sorting', () => {
            const sortMapping = {
                vector: [0, 1, 2, 3],
                segments: splittedSegments,
                filteredSegmentIndices: [true, true, true, true],
            };
            const result = updateSortingIndices({
                ...state,
                filtering: {
                    filteredSegmentIndices: [false, true, true],
                },
                sortMapping,
                queryingPrices: false,
                sort: modifyingSort,
            });

            expect(result).toEqual({
                vector: [0, 1, 2, 3],
                segments: splittedSegments,
                filteredSegmentIndices: [false, true, false, true],
            });
        });

        it('should return current sortMapping on modifying sorting while prices are querying', () => {
            const sortMapping = {
                vector: [0, 1, 2, 3],
                segments: splittedSegments,
                filteredSegmentIndices: [true, true, true, true],
            };
            const result = updateSortingIndices({
                ...state,
                filtering: {
                    filteredSegmentIndices: [false, true, true],
                },
                sortMapping,
                queryingPrices: true,
                sort: modifyingSort,
            });

            expect(result).toEqual(sortMapping);
        });
    });
});
