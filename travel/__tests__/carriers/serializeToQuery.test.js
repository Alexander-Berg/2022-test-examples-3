'use strict';

jest.dontMock('../../baseFilterManager');
const carriers = require.requireActual('../../carriers').default;

describe('carriers', () => {
    describe('serializeToQuery', () => {
        it('empty value', () => {
            const query = carriers.serializeToQuery([]);

            expect(query).toEqual({
                carrier: [],
            });
        });

        it('value has single carrier', () => {
            const query = carriers.serializeToQuery(['123']);

            expect(query).toEqual({
                carrier: ['123'],
            });
        });

        it('value has two carriers', () => {
            const query = carriers.serializeToQuery(['123', '456']);

            expect(query).toEqual({
                carrier: ['123', '456'],
            });
        });
    });
});
