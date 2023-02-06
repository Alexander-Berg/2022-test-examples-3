const gone = require.requireActual('../../gone').default;

describe('testing gone filter', () => {
    describe('seriealizeToQuery', () => {
        it('show all segments', () => {
            const result = gone.serializeToQuery(true);

            expect(result).toEqual({gone: 'y'});
        });

        it('hide gone segments', () => {
            const result = gone.serializeToQuery();

            expect(result).toEqual({});
        });
    });
});
