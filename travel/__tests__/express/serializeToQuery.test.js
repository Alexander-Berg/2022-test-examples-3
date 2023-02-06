import express from '../../express';

describe('express', () => {
    describe('serializeToQuery', () => {
        it('show segments with price only', () => {
            const query = express.serializeToQuery(true);

            expect(query).toEqual({express: 'y'});
        });

        it('show all segments', () => {
            const query = express.serializeToQuery(false);

            expect(query).toEqual({});
        });
    });
});
