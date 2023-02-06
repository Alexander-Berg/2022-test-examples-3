const isFetching = require.requireActual('../isFetching').default;

const search = {
    querying: {
        train: false,
    },
    segments: [{}],
};
const flags = {};
const ufsFlags = {__ufsTesting: true};

describe('isFetching', () => {
    it('should return true if page is fetching', () => {
        expect(isFetching(search, {fetching: 'search'}, flags)).toBe(true);
        expect(isFetching(search, {fetching: 'search'}, ufsFlags)).toBe(true);
    });

    it('should return false if page loaded (ufsTesting: false)', () => {
        expect(isFetching(search, {fetching: null}, flags)).toBe(false);
    });

    it('should return true - querying in process (ufsTesting: true)', () => {
        expect(
            isFetching(
                {
                    querying: {
                        train: true,
                    },
                    segments: [],
                },
                {fetching: null},
                ufsFlags,
            ),
        ).toBe(true);
    });

    it('should return false - querying is ended (ufsTesting: true)', () => {
        expect(
            isFetching(
                {
                    ...search,
                    segments: [],
                },
                {fetching: null},
                ufsFlags,
            ),
        ).toBe(false);
    });
});
